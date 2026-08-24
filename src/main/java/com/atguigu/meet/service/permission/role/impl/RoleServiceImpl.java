package com.atguigu.meet.service.permission.role.impl;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.exception.BusinessException;
import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.mapper.permission.role.SysRoleMapper;
import com.atguigu.meet.mapper.permission.role.SysRoleMenuMapper;
import com.atguigu.meet.mapper.permission.userRole.SysUserRoleMapper;
import com.atguigu.meet.model.dto.permission.role.RoleAssignMenuDTO;
import com.atguigu.meet.model.dto.permission.role.RolePageQueryDTO;
import com.atguigu.meet.model.dto.permission.role.RoleSaveDTO;
import com.atguigu.meet.model.dto.permission.role.RoleStatusDTO;
import com.atguigu.meet.model.dto.permission.role.RoleUpdateDTO;
import com.atguigu.meet.model.entity.permission.role.SysRole;
import com.atguigu.meet.model.entity.permission.role.SysRoleMenu;
import com.atguigu.meet.model.vo.PageResultVO;
import com.atguigu.meet.model.vo.permission.role.RoleVO;
import com.atguigu.meet.service.auth.PermissionCacheService;
import com.atguigu.meet.service.permission.role.RoleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import com.atguigu.meet.utils.BeanConvertUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * 角色管理 Service 实现
 */
@Service
@Slf4j
public class RoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements RoleService {

    @Autowired
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private PermissionCacheService permissionCacheService;

    @Override
    public Response getPageList(RolePageQueryDTO parameter) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(parameter.getRoleName())) {
            wrapper.like(SysRole::getRoleName, parameter.getRoleName());
        }
        if (StringUtils.hasText(parameter.getRoleCode())) {
            wrapper.like(SysRole::getRoleCode, parameter.getRoleCode());
        }
        if (parameter.getStatus() != null) {
            wrapper.eq(SysRole::getStatus, Boolean.TRUE.equals(parameter.getStatus()) ? 1 : 0);
        }
        wrapper.orderByDesc(SysRole::getCreateTime);

        IPage<SysRole> page = new Page<>(parameter.getPageNum(), parameter.getPageSize());
        IPage<SysRole> result = page(page, wrapper);

        // 转为 RoleVO 列表（status 自动转 Boolean），并填充每个角色已分配的 menuIds
        List<SysRole> roles = result.getRecords();
        List<Long> roleIds = roles.stream().map(SysRole::getId).collect(Collectors.toList());

        Map<Long, List<Long>> roleIdToMenuIds = new HashMap<>();
        if (!roleIds.isEmpty()) {
            LambdaQueryWrapper<SysRoleMenu> rmWrapper = new LambdaQueryWrapper<>();
            rmWrapper.in(SysRoleMenu::getRoleId, roleIds);
            List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(rmWrapper);
            roleIdToMenuIds.putAll(roleMenus.stream().collect(Collectors.groupingBy(
                    SysRoleMenu::getRoleId,
                    Collectors.mapping(SysRoleMenu::getMenuId, Collectors.toList())
            )));
        }

        List<RoleVO> voList = roles.stream().map(role -> {
            RoleVO vo = new RoleVO();
            BeanConvertUtils.copyProperties(role, vo);
            vo.setMenuIds(roleIdToMenuIds.getOrDefault(role.getId(), List.of()));
            return vo;
        }).collect(Collectors.toList());

        PageResultVO<RoleVO> pageResult = new PageResultVO<>();
        pageResult.setList(voList);
        pageResult.setTotal(result.getTotal());
        pageResult.setPages(result.getPages());
        pageResult.setCurrent(result.getCurrent());
        pageResult.setSize(result.getSize());
        return Response.ok(pageResult);
    }

    @Override
    public Response getAllRoles() {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getStatus, 1);
        wrapper.orderByDesc(SysRole::getCreateTime);
        List<SysRole> roles = list(wrapper);
        return Response.ok(roles);
    }

    @Override
    public Response getRoleList() {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SysRole::getCreateTime);
        List<SysRole> roles = list(wrapper);
        List<RoleVO> voList = roles.stream().map(role -> {
            RoleVO vo = new RoleVO();
            BeanConvertUtils.copyProperties(role, vo);
            return vo;
        }).collect(Collectors.toList());
        return Response.ok(voList);
    }

    @Override
    public Response getRoleById(Long id) {
        SysRole role = getById(id);
        if (role == null) {
            return Response.fail(500, "角色不存在");
        }
        RoleVO vo = new RoleVO();
        BeanConvertUtils.copyProperties(role, vo);
        // 查询角色已分配的菜单ID列表
        List<Long> menuIds = sysRoleMenuMapper.selectMenuIdsByRoleId(id);
        vo.setMenuIds(menuIds);
        return Response.ok(vo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response addRole(RoleSaveDTO dto) {
        // 校验角色编码唯一
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getRoleCode, dto.getRoleCode());
        if (count(wrapper) > 0) {
            return Response.fail(500, "角色编码已存在");
        }
        SysRole role = new SysRole();
        BeanConvertUtils.copyProperties(dto, role);
        save(role);

        // 同时分配菜单（若传入）
        if (dto.getMenuIds() != null && !dto.getMenuIds().isEmpty()) {
            assignRoleMenus(role.getId(), dto.getMenuIds());
        }

        log.info("[角色管理] 新增角色成功，roleId={}, roleName={}, menuIds={}",
                role.getId(), role.getRoleName(), dto.getMenuIds());
        return Response.ok("新增角色成功", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response updateRole(RoleUpdateDTO dto) {
        SysRole existRole = getById(dto.getId());
        if (existRole == null) {
            return Response.fail(500, "角色不存在");
        }
        // 如果修改了角色编码，校验唯一性
        if (!existRole.getRoleCode().equals(dto.getRoleCode())) {
            LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysRole::getRoleCode, dto.getRoleCode());
            wrapper.ne(SysRole::getId, dto.getId());
            if (count(wrapper) > 0) {
                return Response.fail(500, "角色编码已存在");
            }
        }
        SysRole role = new SysRole();
        BeanConvertUtils.copyProperties(dto, role);
        updateById(role);

        // 同时更新菜单分配（仅当 menuIds 字段传入时全量覆盖，null 表示不修改已有菜单关联）
        if (dto.getMenuIds() != null) {
            assignRoleMenus(dto.getId(), dto.getMenuIds());
            // 菜单权限变更后失效所有用户权限缓存
            permissionCacheService.invalidateAllPermissions();
        }

        log.info("[角色管理] 修改角色成功，roleId={}, menuIds={}", dto.getId(), dto.getMenuIds());
        return Response.ok("修改角色成功", null);
    }

    /**
     * 全量分配角色菜单（先删后插）
     */
    private void assignRoleMenus(Long roleId, List<Long> menuIds) {
        sysRoleMenuMapper.deleteByRoleId(roleId);
        if (menuIds != null && !menuIds.isEmpty()) {
            for (Long menuId : menuIds) {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(roleId);
                rm.setMenuId(menuId);
                sysRoleMenuMapper.insert(rm);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response deleteRole(Long id) {
        SysRole role = getById(id);
        if (role == null) {
            return Response.fail(500, "角色不存在");
        }
        // 删除角色
        removeById(id);
        // 删除角色-菜单关联
        sysRoleMenuMapper.deleteByRoleId(id);
        log.info("[角色管理] 删除角色成功，roleId={}, roleName={}", id, role.getRoleName());
        return Response.ok("删除角色成功", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response updateStatus(RoleStatusDTO dto) {
        SysRole existRole = getById(dto.getId());
        if (existRole == null) {
            return Response.fail(500, "角色不存在");
        }
        // 超级管理员角色不允许禁用
        if (PermissionConst.ROLE_SUPER_ADMIN.equals(existRole.getRoleCode())) {
            return Response.fail(500, "超级管理员角色不允许禁用");
        }
        SysRole role = new SysRole();
        role.setId(dto.getId());
        role.setStatus(Boolean.TRUE.equals(dto.getStatus()) ? 1 : 0);
        updateById(role);

        // 状态变更后失效该角色下所有用户的权限缓存
        List<Long> userIds = sysUserRoleMapper.selectUserIdsByRoleId(dto.getId());
        if (userIds != null && !userIds.isEmpty()) {
            for (Long userId : userIds) {
                permissionCacheService.invalidateUserPermissions(userId);
            }
        }
        log.info("[角色管理] 角色启停成功，roleId={}, {}->{}，关联用户数={}",
                dto.getId(), existRole.getStatus() == 1, dto.getStatus(), userIds == null ? 0 : userIds.size());
        return Response.ok("角色启停成功", null);
    }

    @Override
    public Response getRoleMenuIds(Long roleId) {
        List<Long> menuIds = sysRoleMenuMapper.selectMenuIdsByRoleId(roleId);
        return Response.ok(menuIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response assignMenus(RoleAssignMenuDTO dto) {
        SysRole role = getById(dto.getRoleId());
        if (role == null) {
            return Response.fail(500, "角色不存在");
        }

        // 先删除旧关联
        sysRoleMenuMapper.deleteByRoleId(dto.getRoleId());

        // 再批量插入新关联
        if (dto.getMenuIds() != null && !dto.getMenuIds().isEmpty()) {
            List<SysRoleMenu> roleMenus = dto.getMenuIds().stream().map(menuId -> {
                SysRoleMenu rm = new SysRoleMenu();
                rm.setRoleId(dto.getRoleId());
                rm.setMenuId(menuId);
                return rm;
            }).collect(Collectors.toList());

            for (SysRoleMenu rm : roleMenus) {
                sysRoleMenuMapper.insert(rm);
            }
        }

        // 清除该角色下所有用户的权限缓存
        permissionCacheService.invalidateAllPermissions();

        log.info("[角色管理] 分配菜单成功，roleId={}, menuIds={}", dto.getRoleId(), dto.getMenuIds());
        return Response.ok("分配菜单成功", null);
    }
}