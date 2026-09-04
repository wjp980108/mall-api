package com.atguigu.meet.filter;

import com.atguigu.meet.config.BuiltinSuperAdminIdCache;
import com.atguigu.meet.config.JwtSecurityProperties;
import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.mapper.permission.menu.SysMenuMapper;
import com.atguigu.meet.mapper.permission.user.UserMapper;
import com.atguigu.meet.model.entity.permission.user.AdminUser;
import com.atguigu.meet.model.entity.permission.user.SysUser;
import com.atguigu.meet.service.auth.PermissionCacheService;
import com.atguigu.meet.utils.AdminContext;
import com.atguigu.meet.utils.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.util.PathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Description 解析Token、认证用户、加载用户权限（Redis优先+DB兜底）
 * @Date 2026-05-18 16:38
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtSecurityProperties jwtSecurityProperties;

    @Autowired
    private PathMatcher pathMatcher;

    @Autowired
    private AuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    private PermissionCacheService permissionCacheService;

    @Autowired
    private SysMenuMapper sysMenuMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BuiltinSuperAdminIdCache builtinSuperAdminIdCache;

    private String getTokenFormRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        // —— 请求进入前先清理上一个请求残留的 ThreadLocal（Tomcat 线程池复用会导致越权串号）。
        //    即使是白名单路径，也必须先清理再放行，防止上一个已登录请求的 AdminUser 泄漏到本请求被业务读到。
        SecurityContextHolder.clearContext();
        AdminContext.remove();

        // ====================== 1. 判断接口 uri 是否无需 token, 无 token 直接放行, 交给 Security 拦截
        // ======================
        // 注意:getRequestURI() 含 context-path(如 /api/auth/login),而 public-paths 配的是不含
        // context-path 的路径(/auth/login),
        // 必须先去掉 context-path,否则白名单永远匹配不上,带 token 时会进入校验分支被 401。
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        final String pathForMatch;
        if (StringUtils.hasText(contextPath) && uri.startsWith(contextPath)) {
            pathForMatch = uri.substring(contextPath.length());
        } else {
            // 未配置 context-path 或 uri 不带前缀时，直接用整个 uri 参与白名单匹配（否则会被设为 ""，所有白名单都失效）
            pathForMatch = uri;
        }
        if (jwtSecurityProperties
                .getPublicPaths()
                .stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, pathForMatch))) {
            // 白名单直接放行：不读取 token、不写 AdminContext / SecurityContext
            filterChain.doFilter(request, response);
            return;
        }
        // ====================== 2. 需要 token ======================
        String token = getTokenFormRequest(request);
        log.info("[JWT] Processing request - URI: {}, Token: {}", uri, token);
        try {
            if (token != null && !"undefined".equals(token)) {
                // ====================== 3. 校验 Token 是否合法 ======================
                boolean isValid = jwtUtil.isTokenValid(token);
                if (!isValid) {
                    authenticationEntryPoint.commence(request, response, new AuthenticationServiceException("令牌无效"));
                    return;
                }
                // ====================== 4. 解析 Token 拿到 userId（步骤1）======================
                Long userId = jwtUtil.extractUserId(token);
                // —— 安全加固：不信任 JWT claims 中的 username/phone，以 DB 实时查询结果为准。
                //    防止：①密钥泄露后伪造 username=admin 的 token；②DB 中 username 被篡改后与 JWT 不一致。
                SysUser dbUser = userMapper.selectById(userId);
                if (dbUser == null) {
                    log.warn("[JWT] Token中的userId在DB不存在，拒绝放行，userId={}", userId);
                    authenticationEntryPoint.commence(request, response, new AuthenticationServiceException("用户不存在"));
                    return;
                }
                if (!"1".equals(dbUser.getStatus())) {
                    log.warn("[JWT] 账号已禁用，拒绝放行，userId={}", userId);
                    authenticationEntryPoint.commence(request, response, new AuthenticationServiceException("当前用户已被禁用"));
                    return;
                }
                // 可信身份（来自 DB，而非 JWT claims）
                String realUsername = dbUser.getUsername();
                String realPhone = dbUser.getPhone();
                // 内置超管判定：三因子同时成立才算可信（缺一不可）
                //  ① userId 命中启动时从 DB 加载的 BuiltinSuperAdminIdCache（主键不可变，最可靠依据）；
                //  ② DB 实时查询出的 username 精确 == "admin"（大小写敏感，防止主键被重建但DB username被他人占用的极端情况）；
                //  ③ 保留名校验 isReservedSuperAdminName 通过（兜底保护变体）。
                boolean isBuiltinSuper = builtinSuperAdminIdCache.isBuiltinAdmin(userId)
                        && PermissionConst.SUPER_ADMIN_USERNAME.equals(realUsername)
                        && PermissionConst.isReservedSuperAdminName(realUsername);
                log.info("[JWT] Token解析+DB核对成功，userId={}, username={}, builtinSuperAdmin={}", userId, realUsername, isBuiltinSuper);

                // ====================== 5. 从Redis/DB获取用户权限集合（步骤2+3）======================
                // Redis 优先 -> Redis 无则执行多表联查 -> 写入 Redis 并设置过期时间
                Set<String> permissions = permissionCacheService.getUserPermissions(userId);
                log.info("[JWT] 权限加载完成，userId={}, 权限集合={}", userId, permissions);

                // ====================== 5.1 获取用户角色编码集合 ======================
                List<String> roleCodeList = sysMenuMapper.selectRoleCodesByUserId(userId);
                Set<String> roleCodes = new HashSet<>(roleCodeList);
                log.info("[JWT] 角色加载完成，userId={}, 角色集合={}", userId, roleCodes);

                // 构建 Spring Security 的授权信息
                List<GrantedAuthority> authorities = permissions.stream()
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                HashMap<String, Object> userinfo = new HashMap<>();
                userinfo.put("userId", userId);
                userinfo.put("phone", realPhone);
                userinfo.put("username", realUsername);

                // ====================== 6. 构建认证信息，告诉 Spring Security：这个人已登录！并携带其权限
                // ======================
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userinfo,
                        null,
                        authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                // 存入用户信息上下文（含权限集合，供 @RequirePermission AOP 切面直接使用）
                AdminUser adminUser = new AdminUser();
                adminUser.setUserId(userId);
                adminUser.setPhone(realPhone);
                adminUser.setUsername(realUsername);
                adminUser.setBuiltinSuperAdmin(isBuiltinSuper);
                adminUser.setPermissions(permissions);
                adminUser.setRoleCodes(roleCodes);
                AdminContext.set(adminUser);
                log.info("[JWT] 用户上下文已设置，userId={}, builtinSuperAdmin={}", userId, isBuiltinSuper);
            }
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            // 任何异常都清空认证信息，避免上下文泄漏
            SecurityContextHolder.clearContext();
            AdminContext.remove();
            log.error("[JWT] 令牌验证失败: {}", ex.getMessage(), ex);
            authenticationEntryPoint.commence(request, response,
                    new AuthenticationServiceException("令牌验证失败: " + ex.getMessage()));
            return;
        } finally {
            // ====================== 7. 请求结束务必清理 ThreadLocal，防止 Tomcat 线程池复用导致
            //    上一个请求的登录身份 / 权限 串号到下一个请求（表现为"有时候 401，有时候手机号/账号不一致"）。
            SecurityContextHolder.clearContext();
            AdminContext.remove();
        }
    }
}
