package com.atguigu.meet.controller.permission.role;

import com.atguigu.meet.annotation.RequirePermission;
import com.atguigu.meet.common.Response;
import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.model.dto.permission.role.RoleAssignMenuDTO;
import com.atguigu.meet.model.dto.permission.role.RolePageQueryDTO;
import com.atguigu.meet.model.dto.permission.role.RoleSaveDTO;
import com.atguigu.meet.model.dto.permission.role.RoleStatusDTO;
import com.atguigu.meet.model.dto.permission.role.RoleUpdateDTO;
import com.atguigu.meet.service.permission.role.RoleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 角色管理接口
 */
@RestController
@RequestMapping("/roles")
@Validated
public class RoleController {
    @Autowired
    private RoleService roleService;

    /** 角色分页列表 */
    @GetMapping
    @RequirePermission(PermissionConst.ROLE_QUERY)
    public Response getPageList(@Valid RolePageQueryDTO parameter) {
        return roleService.getPageList(parameter);
    }

    /** 所有启用角色（下拉框用） */
    @GetMapping("/all")
    @RequirePermission(PermissionConst.ROLE_QUERY)
    public Response getAllRoles() {
        return roleService.getAllRoles();
    }

    /** 根据ID查角色（含已分配菜单ID列表） */
    @GetMapping("/{id}")
    @RequirePermission(PermissionConst.ROLE_QUERY)
    public Response getRoleById(@PathVariable Long id) {
        return roleService.getRoleById(id);
    }

    /** 新增角色 */
    @PostMapping
    @RequirePermission(PermissionConst.ROLE_ADD)
    public Response addRole(@RequestBody @Valid RoleSaveDTO dto) {
        return roleService.addRole(dto);
    }

    /** 修改角色 */
    @PutMapping
    @RequirePermission(PermissionConst.ROLE_UPDATE)
    public Response updateRole(@RequestBody @Valid RoleUpdateDTO dto) {
        return roleService.updateRole(dto);
    }

    /** 删除角色 */
    @DeleteMapping("/{id}")
    @RequirePermission(PermissionConst.ROLE_DELETE)
    public Response deleteRole(@PathVariable Long id) {
        return roleService.deleteRole(id);
    }

    /** 启用/禁用角色 */
    @PatchMapping("/status")
    @RequirePermission(PermissionConst.ROLE_STATUS)
    public Response updateStatus(@RequestBody @Valid RoleStatusDTO dto) {
        return roleService.updateStatus(dto);
    }

    /** 查询角色已分配的菜单ID列表 */
    @GetMapping("/{roleId}/menus")
    @RequirePermission(PermissionConst.ROLE_QUERY)
    public Response getRoleMenuIds(@PathVariable Long roleId) {
        return roleService.getRoleMenuIds(roleId);
    }

    /** 给角色分配菜单（全量覆盖） */
    @PutMapping("/menus")
    @RequirePermission(PermissionConst.ROLE_ASSIGN_MENU)
    public Response assignMenus(@RequestBody @Valid RoleAssignMenuDTO dto) {
        return roleService.assignMenus(dto);
    }
}