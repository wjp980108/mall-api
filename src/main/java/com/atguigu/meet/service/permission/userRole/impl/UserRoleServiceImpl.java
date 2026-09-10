package com.atguigu.meet.service.permission.userRole.impl;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.mapper.permission.role.SysRoleMapper;
import com.atguigu.meet.mapper.permission.userRole.SysUserRoleMapper;
import com.atguigu.meet.model.dto.permission.userRole.UserAssignRoleDTO;
import com.atguigu.meet.model.entity.permission.role.SysRole;
import com.atguigu.meet.model.entity.permission.userRole.SysUserRole;
import com.atguigu.meet.service.auth.PermissionCacheService;
import com.atguigu.meet.service.permission.userRole.UserRoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户-角色关联 Service 实现
 */
@Service
@Slf4j
public class UserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole> implements UserRoleService {

    @Autowired
    private PermissionCacheService permissionCacheService;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Override
    public Response getUserRoleIds(Long userId) {
        List<Long> roleIds = baseMapper.selectRoleIdsByUserId(userId);
        return Response.ok(roleIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response assignRoles(UserAssignRoleDTO dto) {
        Response syncResult = syncUserRoles(dto.getUserId(), dto.getRoleIds());
        if (syncResult.getCode() != 200) {
            return syncResult;
        }
        return Response.ok("分配角色成功", null);
    }

    /**
     * 同步用户角色绑定（编辑用户 / 独立分配接口共用）。
     * <p>去重 -> 校验角色存在且启用 -> 删除旧关联 -> 插入新关联 -> 失效权限缓存。
     * 全流程在同一事务内，校验失败或写入异常均整体回滚，不产生部分写入。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response syncUserRoles(Long userId, List<Long> roleIds) {
        if (userId == null) {
            return Response.fail(500, "用户ID不能为空");
        }

        // 1. 去重（LinkedHashSet 保留传入顺序，避免撞 sys_user_role 唯一键 uk_user_role）
        List<Long> targetRoleIds = (roleIds == null)
                ? new ArrayList<>()
                : new ArrayList<>(new LinkedHashSet<>(roleIds));

        // 2. 非空时校验角色全部存在且为启用状态（校验在删旧之前，失败直接返回，不触碰关联表）
        if (!targetRoleIds.isEmpty()) {
            LambdaQueryWrapper<SysRole> roleWrapper = Wrappers.lambdaQuery(SysRole.class);
            roleWrapper.in(SysRole::getId, targetRoleIds).eq(SysRole::getStatus, 1);
            long validCount = sysRoleMapper.selectCount(roleWrapper);
            if (validCount != targetRoleIds.size()) {
                return Response.fail(500, "包含无效或已禁用的角色ID");
            }
        }

        // 3. 删除旧关联（先删后插，保证全量覆盖且不重复插入）
        baseMapper.deleteByUserId(userId);

        // 4. 插入去重后的新关联
        if (!targetRoleIds.isEmpty()) {
            List<SysUserRole> userRoles = targetRoleIds.stream().map(rid -> {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(rid);
                return ur;
            }).collect(Collectors.toList());
            for (SysUserRole ur : userRoles) {
                baseMapper.insert(ur);
            }
        }

        // 5. 失效该用户权限缓存，使其下次请求按新角色重新加载权限
        permissionCacheService.invalidateUserPermissions(userId);

        log.info("[用户角色] 同步角色成功，userId={}, roleIds={}", userId, targetRoleIds);
        return Response.ok("角色同步成功", targetRoleIds);
    }
}
