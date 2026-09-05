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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "角色管理", description = "角色CRUD及菜单权限分配接口")
public class RoleController {
    @Autowired
    private RoleService roleService;

    /** 角色分页列表 */
    @GetMapping
    @RequirePermission(PermissionConst.ROLE_QUERY)
    @Operation(summary = "角色分页列表", description = "分页查询角色列表")
    public Response getPageList(@Valid RolePageQueryDTO parameter) {
        return roleService.getPageList(parameter);
    }

    /** 所有启用角色（下拉框用） */
    @GetMapping("/all")
    @RequirePermission(PermissionConst.ROLE_QUERY)
    @Operation(summary = "所有启用角色", description = "获取所有启用的角色（下拉框用）")
    public Response getAllRoles() {
        return roleService.getAllRoles();
    }

    /** 根据ID查角色（含已分配菜单ID列表） */
    @GetMapping("/{id}")
    @RequirePermission(PermissionConst.ROLE_QUERY)
    @Operation(summary = "角色详情", description = "根据ID查询角色详情（含已分配菜单ID列表）")
    public Response getRoleById(@PathVariable Long id) {
        return roleService.getRoleById(id);
    }

    /** 新增角色 */
    @PostMapping
    @RequirePermission(PermissionConst.ROLE_ADD)
    @Operation(summary = "新增角色", description = "创建新角色")
    public Response addRole(@RequestBody @Valid RoleSaveDTO dto) {
        return roleService.addRole(dto);
    }

    /** 修改角色 */
    @PutMapping
    @RequirePermission(PermissionConst.ROLE_UPDATE)
    @Operation(summary = "修改角色", description = "更新角色信息")
    public Response updateRole(@RequestBody @Valid RoleUpdateDTO dto) {
        return roleService.updateRole(dto);
    }

    /** 删除角色 */
    @DeleteMapping("/{id}")
    @RequirePermission(PermissionConst.ROLE_DELETE)
    @Operation(summary = "删除角色", description = "删除角色")
    public Response deleteRole(@PathVariable Long id) {
        return roleService.deleteRole(id);
    }

    /** 启用/禁用角色 */
    @PatchMapping("/status")
    @RequirePermission(PermissionConst.ROLE_STATUS)
    @Operation(summary = "更新角色状态", description = "启用或禁用角色")
    public Response updateStatus(@RequestBody @Valid RoleStatusDTO dto) {
        return roleService.updateStatus(dto);
    }

    /** 查询角色已分配的菜单ID列表 */
    @GetMapping("/{roleId}/menus")
    @RequirePermission(PermissionConst.ROLE_QUERY)
    @Operation(summary = "查询角色菜单", description = "查询角色已分配的菜单ID列表")
    public Response getRoleMenuIds(@PathVariable Long roleId) {
        return roleService.getRoleMenuIds(roleId);
    }

    /** 给角色分配菜单（全量覆盖） */
    @PutMapping("/menus")
    @RequirePermission(PermissionConst.ROLE_ASSIGN_MENU)
    @Operation(summary = "分配角色菜单", description = "给角色分配菜单（全量覆盖）")
    public Response assignMenus(@RequestBody @Valid RoleAssignMenuDTO dto) {
        return roleService.assignMenus(dto);
    }
}