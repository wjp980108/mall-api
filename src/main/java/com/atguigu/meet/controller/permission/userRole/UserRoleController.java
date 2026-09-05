package com.atguigu.meet.controller.permission.userRole;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.permission.userRole.UserAssignRoleDTO;
import com.atguigu.meet.service.permission.userRole.UserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户-角色关联接口
 */
@RestController
@RequestMapping("/user-roles")
@Validated
@Tag(name = "用户角色关联", description = "用户与角色的关联关系管理")
public class UserRoleController {
    @Autowired
    private UserRoleService userRoleService;

    /** 查询用户已分配的角色ID列表 */
    @GetMapping("/{userId}/roles")
    @Operation(summary = "查询用户角色", description = "查询用户已分配的角色ID列表")
    public Response getUserRoleIds(@PathVariable Long userId) {
        return userRoleService.getUserRoleIds(userId);
    }

    /** 给用户分配角色（全量覆盖） */
    @PutMapping("/roles")
    @Operation(summary = "分配用户角色", description = "给用户分配角色（全量覆盖）")
    public Response assignRoles(@RequestBody @Valid UserAssignRoleDTO dto) {
        return userRoleService.assignRoles(dto);
    }
}