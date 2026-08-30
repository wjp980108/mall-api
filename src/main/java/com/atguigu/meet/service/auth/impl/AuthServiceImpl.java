package com.atguigu.meet.service.auth.impl;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.exception.BusinessException;
import com.atguigu.meet.mapper.permission.user.UserMapper;
import com.atguigu.meet.mapper.permission.menu.SysMenuMapper;
import com.atguigu.meet.model.dto.auth.AuthRegisterDTO;
import com.atguigu.meet.model.dto.auth.AuthLoginDTO;
import com.atguigu.meet.model.entity.permission.invite.SysInviteCode;
import com.atguigu.meet.model.entity.permission.menu.SysMenu;
import com.atguigu.meet.model.entity.permission.user.SysUser;
import com.atguigu.meet.model.vo.permission.user.UserVO;
import com.atguigu.meet.service.auth.AuthService;
import com.atguigu.meet.service.auth.PermissionCacheService;
import com.atguigu.meet.service.permission.invite.InviteCodeService;
import com.atguigu.meet.utils.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import com.atguigu.meet.utils.BeanConvertUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Description
 * @Date 2026-08-12 23:57
 */
@Service
@Slf4j
public class AuthServiceImpl extends ServiceImpl<UserMapper, SysUser> implements AuthService {
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SysMenuMapper sysMenuMapper;

    @Autowired
    private InviteCodeService inviteCodeService;

    @Autowired
    private PermissionCacheService permissionCacheService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response register(AuthRegisterDTO authRegisterDTO) {
        // 1. 校验用户名是否已存在
        SysUser existUsername = getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, authRegisterDTO.getUsername()));
        if (existUsername != null) {
            return Response.fail(500, "用户名已存在");
        }

        // 1.1 系统保留用户名校验（内置超级管理员账户名不可被占用，大小写不敏感 + trim）
        if (PermissionConst.isReservedSuperAdminName(authRegisterDTO.getUsername())) {
            return Response.fail(500, "该用户名为系统保留，请更换用户名");
        }

        // 2. 校验手机号是否已注册
        LambdaQueryWrapper<SysUser> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(SysUser::getPhone, authRegisterDTO.getPhone());
        SysUser existUser = getOne(lambdaQueryWrapper);
        if (existUser != null) {
            return Response.fail(500, "用户已存在");
        }

        // 3. 校验邀请码有效性（邀请码非必填，未传则跳过）
        String inviteCodeStr = authRegisterDTO.getInviteCode();
        SysInviteCode inviteCode = null;
        if (inviteCodeStr != null && !inviteCodeStr.isEmpty()) {
            inviteCode = inviteCodeService.validateInviteCode(inviteCodeStr);
        }

        // 4. 创建用户
        SysUser user = new SysUser();
        String encodePwd = passwordEncoder.encode(authRegisterDTO.getPassword());
        BeanConvertUtils.copyProperties(authRegisterDTO, user);
        user.setPassword(encodePwd);
        if (inviteCode != null) {
            user.setInviterId(inviteCode.getInviterId());
        }
        userMapper.insert(user);

        // 5. 处理邀请流水 + 核销邀请码（更新已邀请人数，名额满则自动停用）
        if (inviteCode != null) {
            inviteCodeService.processInviteRecord(inviteCode, user.getId(), user.getPhone());
        }

        UserVO userVO = new UserVO();
        BeanConvertUtils.copyProperties(user, userVO);
        return Response.ok("创建用户成功", userVO);
    }

    @Override
    public Response login(AuthLoginDTO authLoginDTO) {
        String account = authLoginDTO.getAccount();
        // 支持手机号或用户名登录（内置超管账户 admin 无手机号绑定要求，可直接用用户名登录）
        LambdaQueryWrapper<SysUser> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(SysUser::getPhone, account)
                .or()
                .eq(SysUser::getUsername, account)
                .last("LIMIT 1");
        SysUser existUser = getOne(lambdaQueryWrapper);
        if (existUser == null) throw new BusinessException("当前用户不存在");
        if (!"1".equals(existUser.getStatus())) throw new BusinessException("当前用户已被禁用");
        boolean bool = passwordEncoder.matches(authLoginDTO.getPassword(), existUser.getPassword());
        if (!bool) throw new BusinessException("用户账号密码不正确");

        // 登录成功后先清除该用户的 Redis 权限缓存，强制下次鉴权时从 DB 重新加载最新权限
        permissionCacheService.invalidateUserPermissions(existUser.getId());

        Map<String, Object> claims = new HashMap<>();
        claims.put("username", existUser.getUsername());
        claims.put("phone", existUser.getPhone());
        claims.put("status", existUser.getStatus());

        // 查询用户角色
        List<String> roleCodes = sysMenuMapper.selectRoleCodesByUserId(existUser.getId());
        // 内置超级管理员（不关联任何角色）与 SUPER_ADMIN 角色用户同等地位：拥有全部权限
        // 注意：此处必须用 DB 查询到的真实 username 精确匹配（大小写敏感），才是真正的内置超管
        boolean isSuperAdmin = roleCodes.contains(PermissionConst.ROLE_SUPER_ADMIN)
                || PermissionConst.SUPER_ADMIN_USERNAME.equals(existUser.getUsername());

        // 查询当前用户的权限码列表
        List<String> permissions;
        if (isSuperAdmin) {
            // 超级管理员获取所有权限
            LambdaQueryWrapper<SysMenu> menuWrapper = new LambdaQueryWrapper<>();
            menuWrapper.eq(SysMenu::getStatus, 1)
                    .eq(SysMenu::getIsDeleted, 0)
                    .isNotNull(SysMenu::getPerm)
                    .ne(SysMenu::getPerm, "");
            List<SysMenu> allMenus = sysMenuMapper.selectList(menuWrapper);
            permissions = allMenus.stream()
                    .map(SysMenu::getPerm)
                    .collect(Collectors.toList());
            log.info("[登录] 超级管理员登录，加载所有权限，userId={}, 权限数={}", existUser.getId(), permissions.size());
        } else {
            permissions = sysMenuMapper.selectPermsByUserId(existUser.getId());
        }

        claims.put("permissions", permissions);
        String token = jwtUtil.generateToken(existUser.getId(), claims);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        return Response.ok(200, "用户登录成功", data);
    }

}