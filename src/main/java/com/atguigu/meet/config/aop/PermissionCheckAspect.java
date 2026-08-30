package com.atguigu.meet.config.aop;

import com.atguigu.meet.annotation.RequirePermission;
import com.atguigu.meet.config.BuiltinSuperAdminIdCache;
import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.exception.BusinessException;
import com.atguigu.meet.model.entity.permission.user.AdminUser;
import com.atguigu.meet.service.auth.PermissionCacheService;
import com.atguigu.meet.utils.AdminContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;

/**
 * 权限校验 AOP 切面
 * <p>
 * 执行流程：
 * 1. 拦截 @RequirePermission 注解（步骤4：获取当前请求接口的权限标识符）
 * 2. 从 AdminContext 取出 JWT 过滤器已加载好的权限集合（优先用内存中的，减少Redis查询）
 * 3. 如果内存中没有权限集合，再走 PermissionCacheService 查 Redis/DB
 * 4. 对比用户权限与接口要求的权限，按 AND/OR 模式校验（步骤5）
 * 5. 有权限 -> 放行执行业务；无权限 -> 抛出 403 异常
 */
@Aspect
@Component
@Order(1)
@Slf4j
public class PermissionCheckAspect {

    @Autowired
    private PermissionCacheService permissionCacheService;

    @Autowired
    private BuiltinSuperAdminIdCache builtinSuperAdminIdCache;

    /**
     * 切入点：拦截所有标记 @RequirePermission 的方法或类
     */
    @Pointcut("@annotation(com.atguigu.meet.annotation.RequirePermission) " +
            "|| @within(com.atguigu.meet.annotation.RequirePermission)")
    public void permissionPointcut() {
    }

    @Around("permissionPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // ====================== 步骤4：获取当前请求接口的权限标识符（注解方式）======================
        RequirePermission annotation = getRequirePermissionAnnotation(joinPoint);
        if (annotation == null) {
            log.debug("[权限校验] 接口无@RequirePermission注解，直接放行");
            return joinPoint.proceed();
        }

        // 获取当前登录用户ID
        Long userId = AdminContext.getLoginUserId();
        if (userId == null) {
            log.warn("[权限校验] 用户未登录，无法校验权限");
            throw new BusinessException(401, "未登录，请先进行身份验证");
        }

        // ====================== 系统内置超级管理员（DB实时双因子校验通过的 admin 账户）直接放行 ======================
        // —— 安全说明：放行条件三重判定，缺一不可：
        //    ① builtinSuperAdmin == true（JwtAuthenticationFilter 从 DB 实时查询写入的可信标记，三因子均通过才为 true）；
        //    ② userId 命中 BuiltinSuperAdminIdCache 缓存（自增主键永久不变，是最核心的身份锚点）；
        //    ③ AdminUser.username 仍精确 == "admin"（冗余校验，防御标记被伪造）。
        //    以上条件确保：即使 JWT 被恶意伪造为 username=admin / userId 伪造 / 其他用户 username 被 DB 直连篡改为 admin，
        //    只要「userId 不是那条被启动时扫描到的 admin 账户主键」就永远无法跳过权限校验。
        AdminUser adminUser = AdminContext.get();
        if (adminUser != null && adminUser.isBuiltinSuperAdmin()
                && builtinSuperAdminIdCache.isBuiltinAdmin(adminUser.getUserId())
                && PermissionConst.SUPER_ADMIN_USERNAME.equals(adminUser.getUsername())) {
            log.info("[权限校验] 内置超级管理员（userId缓存+DB双因子校验通过），直接放行，userId={}", userId);
            return joinPoint.proceed();
        }

        // ====================== 超级管理员角色直接放行 ======================
        Set<String> roleCodes = adminUser.getRoleCodes();
        if (roleCodes != null && roleCodes.contains(PermissionConst.ROLE_SUPER_ADMIN)) {
            log.info("[权限校验] 超级管理员角色，直接放行，userId={}", userId);
            return joinPoint.proceed();
        }

        // 接口要求的权限标识
        String[] requiredPerms = annotation.value();
        if (requiredPerms == null || requiredPerms.length == 0) {
            log.debug("[权限校验] 注解value为空，直接放行，userId={}", userId);
            return joinPoint.proceed();
        }

        // ====================== 步骤5：判断用户权限集合是否包含该标识符 ======================
        Set<String> userPerms = getUserPermissions(userId);
        log.info("[权限校验] 开始校验，userId={}, 要求权限={}, 用户权限={}, 模式={}",
                userId, Arrays.toString(requiredPerms), userPerms, annotation.mode());

        boolean hasPermission = checkPermission(userPerms, requiredPerms, annotation.mode());
        if (!hasPermission) {
            log.warn("[权限校验] 权限不足！userId={}, 需要权限={}, 用户实际权限={}",
                    userId, Arrays.toString(requiredPerms), userPerms);
            throw new BusinessException(403, annotation.message());
        }

        // 包含：放行执行业务
        log.info("[权限校验] 权限校验通过，userId={}, 接口权限={}", userId, Arrays.toString(requiredPerms));
        return joinPoint.proceed();
    }

    /**
     * 获取用户权限集合：
     * 1. 优先从 AdminContext（ThreadLocal内存）获取，零成本
     * 2. 内存中没有（null或空集合），再查 Redis + DB
     */
    private Set<String> getUserPermissions(Long userId) {
        Set<String> perms = AdminContext.getLoginUserPermissions();
        if (perms != null && !perms.isEmpty()) {
            log.debug("[权限校验] 从AdminContext获取权限，userId={}, 权限数={}", userId, perms.size());
            return perms;
        }
        log.info("[权限校验] AdminContext中无有效权限数据，走Redis/DB查询，userId={}", userId);
        try {
            Set<String> dbPerms = permissionCacheService.getUserPermissions(userId);
            log.info("[权限校验] Redis/DB查询完成，userId={}, 权限数={}", userId, dbPerms.size());
            return dbPerms;
        } catch (Exception e) {
            log.error("[权限校验] 获取用户权限失败，userId={}", userId, e);
            throw new BusinessException(500, "权限校验失败：" + e.getMessage());
        }
    }

    /**
     * 从方法或类上获取 @RequirePermission 注解（方法优先）
     */
    private RequirePermission getRequirePermissionAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 优先取方法上的注解
        RequirePermission methodAnno = method.getAnnotation(RequirePermission.class);
        if (methodAnno != null) {
            return methodAnno;
        }

        // 方法没有取类上的
        Class<?> targetClass = joinPoint.getTarget().getClass();
        RequirePermission classAnno = targetClass.getAnnotation(RequirePermission.class);
        return classAnno;
    }

    /**
     * 校验用户是否拥有所需权限
     *
     * @param userPerms     用户实际拥有的权限集合
     * @param requiredPerms 接口要求的权限数组
     * @param mode          校验模式（AND / OR）
     * @return 是否有权限
     */
    private boolean checkPermission(Set<String> userPerms, String[] requiredPerms, RequirePermission.Mode mode) {
        if (userPerms == null || userPerms.isEmpty()) {
            return false;
        }
        if (mode == RequirePermission.Mode.AND) {
            // AND 模式：必须全部拥有
            for (String perm : requiredPerms) {
                if (!userPerms.contains(perm)) {
                    return false;
                }
            }
            return true;
        } else {
            // OR 模式：拥有任意一个即可
            for (String perm : requiredPerms) {
                if (userPerms.contains(perm)) {
                    return true;
                }
            }
            return false;
        }
    }
}
