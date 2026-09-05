package com.atguigu.meet.controller.permission.user;

import com.atguigu.meet.annotation.RequirePermission;
import com.atguigu.meet.common.Response;
import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.model.dto.permission.user.UserCreateDTO;
import com.atguigu.meet.model.dto.permission.user.UserDeleteDTO;
import com.atguigu.meet.model.dto.permission.user.UserPageQueryDTO;
import com.atguigu.meet.model.dto.permission.user.UserStatusDTO;
import com.atguigu.meet.model.dto.permission.user.UserUpdateDTO;
import com.atguigu.meet.service.permission.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 用户管理接口
 * <p>
 * 权限标识统一使用 {@link PermissionConst} 常量维护
 */
@RestController
@RequestMapping("/users")
@Validated
@Tag(name = "用户管理", description = "后台用户管理接口")
public class UserController {
    @Autowired
    private UserService userService;

    /**
     * 创建用户（角色由前端传入，事务保证用户与角色关联同时写入）
     */
    @PostMapping
    @RequirePermission(PermissionConst.USER_ADD)
    @Operation(summary = "创建用户", description = "创建新用户（角色由前端传入）")
    public Response createUser(@RequestBody @Valid UserCreateDTO userCreateDTO) {
        return userService.createUser(userCreateDTO);
    }

    /**
     * 用户列表分页查询
     */
    @GetMapping
    @RequirePermission(PermissionConst.USER_QUERY)
    @Operation(summary = "用户分页列表", description = "分页查询用户列表")
    public Response pageList(@Valid UserPageQueryDTO parameter) {
        return userService.getPageList(parameter);
    }

    /**
     * 用户下拉选项列表（委托人, 仅启用用户）
     */
    @GetMapping("/options")
    @RequirePermission(PermissionConst.USER_QUERY)
    @Operation(summary = "用户下拉选项", description = "获取启用的用户下拉选项列表")
    public Response getUserOptions() {
        return userService.getUserOptions();
    }

    /**
     * 批量删除用户
     */
    @DeleteMapping
    @RequirePermission(PermissionConst.USER_DELETE)
    @Operation(summary = "批量删除用户", description = "批量删除用户")
    public Response deleteUser(@RequestBody @Valid UserDeleteDTO userDeleteDTO) {
        return userService.deleteUserByIds(userDeleteDTO);
    }

    /**
     * 更新用户信息
     */
    @PutMapping
    @RequirePermission(PermissionConst.USER_UPDATE)
    @Operation(summary = "更新用户", description = "更新用户信息")
    public Response updateUser(@RequestBody @Valid UserUpdateDTO userUpdateDTO) {
        return userService.updateUser(userUpdateDTO);
    }

    /**
     * 启用/禁用用户
     */
    @PatchMapping("/status")
    @RequirePermission(PermissionConst.USER_STATUS)
    @Operation(summary = "更新用户状态", description = "启用或禁用用户")
    public Response updateStatus(@RequestBody @Valid UserStatusDTO userStatusDTO) {
        return userService.updateStatus(userStatusDTO);
    }

    /**
     * 上传当前登录用户头像
     *
     * @param platform 存储平台: local-1 / aliyun-oss-1 / qiniu-kodo-1 / minio-1 / tencent-cos-1
     *                 为空时使用 application.yml 中 default-platform
     */
    @PostMapping("avatar")
    @RequirePermission(PermissionConst.USER_UPDATE)
    @Operation(summary = "上传用户头像", description = "上传当前登录用户头像")
    public Response uploadUserAvatar(@RequestParam("file") MultipartFile file,
                                     @RequestParam(value = "platform", required = false) String platform) {
        return userService.uploadUserAvatar(file, platform);
    }

    /**
     * 当前登录用户的信息
     */
    @GetMapping("user-info")
    @Operation(summary = "当前用户信息", description = "获取当前登录用户的信息")
    public Response getCurrentUserInfo() {
        return userService.getCurrentUserInfo();
    }

    /**
     * 当前登录用户的菜单
     */
    @GetMapping("user-menus")
    @Operation(summary = "当前用户菜单", description = "获取当前登录用户的菜单权限")
    public Response getCurrentUserMenus() {
        return userService.getCurrentUserMenus();
    }
}