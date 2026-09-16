package com.atguigu.meet.controller.permission.user;

import com.atguigu.meet.annotation.RequirePermission;
import com.atguigu.meet.common.Response;
import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.model.dto.permission.user.UserChangePasswordDTO;
import com.atguigu.meet.model.dto.permission.user.UserCreateDTO;
import com.atguigu.meet.model.dto.permission.user.UserDeleteDTO;
import com.atguigu.meet.model.dto.permission.user.UserPageQueryDTO;
import com.atguigu.meet.model.dto.permission.user.UserProfileUpdateDTO;
import com.atguigu.meet.model.dto.permission.user.UserStatusDTO;
import com.atguigu.meet.model.dto.permission.user.UserUpdateDTO;
import com.atguigu.meet.model.vo.OptionVO;
import com.atguigu.meet.model.vo.permission.menu.MenuVO;
import com.atguigu.meet.model.vo.permission.user.AdminUserPointsVO;
import com.atguigu.meet.model.vo.permission.user.UserLoginVO;
import com.atguigu.meet.model.vo.permission.user.UserRelationVO;
import com.atguigu.meet.model.vo.permission.user.UserVO;
import com.atguigu.meet.service.permission.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
    public Response<Void> createUser(@RequestBody @Valid UserCreateDTO userCreateDTO) {
        return userService.createUser(userCreateDTO);
    }

    /**
     * 用户列表分页查询
     */
    @GetMapping
    @Operation(summary = "用户分页列表", description = "分页查询用户列表")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserVO.class)))
    public Response<UserVO> pageList(@Valid UserPageQueryDTO parameter) {
        return userService.getPageList(parameter);
    }

    /**
     * 查询用户上下级邀请关系（一级直邀，不递归）
     *
     * @param id 目标用户ID
     * @return 上级简化对象 + 直邀下级简化对象数组
     */
    @GetMapping("relations/{id}")
    @Operation(summary = "查询用户上下级邀请关系", description = "返回指定用户的直接上级与直接下级邀请关系（仅一级直邀，不递归；上级为内置超管或无邀请人时 upline 为 null）")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserRelationVO.class)))
    public Response<UserRelationVO> getRelations(@PathVariable("id") Long id) {
        return userService.getRelations(id);
    }

    /**
     * 查询用户积分详情（合并 C 端余额 + 流水契约为后台单接口，只读，不初始化账户）
     *
     * @param id       目标用户ID
     * @param pageNum  分页页码（默认 1）
     * @param pageSize 每页条数（默认 10）
     * @param bizType  业务类型筛选：1推荐奖 2自购奖 3购物券奖 4积分对冲；不传查全部
     * @return 流水分页（分页字段平铺最外层）+ 余额（balance 字段）
     */
    @GetMapping("points/{id}")
    @Operation(summary = "查询用户积分详情", description = "合并返回指定用户的积分余额与流水分页（分页字段 list/total/pages/current/size 平铺最外层，余额以 balance 字段附加；管理端只读核查专用，不初始化账户；流水字段、排序、中文名与 C 端 /app/assets/points/flow 同口径）")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AdminUserPointsVO.class)))
    public Response<AdminUserPointsVO> getPointsDetail(@PathVariable("id") Long id,
                                                       @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
                                                       @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
                                                       @RequestParam(value = "bizType", required = false) Integer bizType) {
        return userService.getPointsDetail(id, bizType, pageNum, pageSize);
    }

    /**
     * 用户下拉选项列表（委托人, 仅启用用户）
     */
    @GetMapping("/options")
    @Operation(summary = "用户下拉选项", description = "获取启用的用户下拉选项列表")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = OptionVO.class)))
    public Response<OptionVO<Long>> getUserOptions() {
        return userService.getUserOptions();
    }

    /**
     * 批量删除用户
     */
    @DeleteMapping
    @RequirePermission(PermissionConst.USER_DELETE)
    @Operation(summary = "批量删除用户", description = "批量删除用户")
    public Response<Void> deleteUser(@RequestBody @Valid UserDeleteDTO userDeleteDTO) {
        return userService.deleteUserByIds(userDeleteDTO);
    }

    /**
     * 更新用户信息
     */
    @PutMapping
    @RequirePermission(PermissionConst.USER_UPDATE)
    @Operation(summary = "更新用户", description = "更新用户信息")
    public Response<Void> updateUser(@RequestBody @Valid UserUpdateDTO userUpdateDTO) {
        return userService.updateUser(userUpdateDTO);
    }

    /**
     * 启用/禁用用户
     */
    @PatchMapping("/status")
    @RequirePermission(PermissionConst.USER_STATUS)
    @Operation(summary = "更新用户状态", description = "启用或禁用用户")
    public Response<Void> updateStatus(@RequestBody @Valid UserStatusDTO userStatusDTO) {
        return userService.updateStatus(userStatusDTO);
    }

    /**
     * 将指定用户转为老会员（member_type 0→1）
     *
     * @param id 目标用户ID
     * @return 转换结果
     */
    @PostMapping("/toOldMember/{id}")
    @RequirePermission(PermissionConst.USER_TO_OLD)
    @Operation(summary = "转老会员", description = "将指定新会员手动转为老会员")
    public Response<Void> toOldMember(@PathVariable("id") Long id) {
        return userService.toOldMember(id);
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
    public Response<Void> uploadUserAvatar(@RequestParam("file") MultipartFile file,
                                     @RequestParam(value = "platform", required = false) String platform) {
        return userService.uploadUserAvatar(file, platform);
    }

    /**
     * 当前登录用户的信息
     */
    @GetMapping("user-info")
    @Operation(summary = "当前用户信息", description = "获取当前登录用户的信息")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserLoginVO.class)))
    public Response<UserLoginVO> getCurrentUserInfo() {
        return userService.getCurrentUserInfo();
    }

    /**
     * 当前登录用户修改个人信息（仅登录即可，无需管理权限）
     * <p>
     * 可修改昵称、手机号、邮箱、性别、头像等；未传字段不更新；
     * 手机号需符合格式且不能与其他用户撞号（内置超管 admin 初始无手机号，可通过此接口首次绑定）；
     * 用户名/密码/状态不可通过此接口修改。
     */
    @PutMapping("profile")
    @Operation(summary = "修改当前用户信息", description = "当前登录用户修改自己的昵称、手机号、邮箱、性别、头像等信息（手机号格式校验+撞号校验，未传字段不更新）")
    public Response<Void> updateCurrentUserInfo(@RequestBody @Valid UserProfileUpdateDTO dto) {
        return userService.updateCurrentUserInfo(dto);
    }

    /**
     * 当前登录用户修改密码（仅登录即可，无需管理权限）
     * <p>
     * 用户身份由 token 解析，前端仅需传新密码。
     */
    @PutMapping("password")
    @Operation(summary = "修改当前用户密码", description = "修改当前登录用户密码，用户身份由 token 解析，仅需传新密码")
    public Response<Void> changePassword(@RequestBody @Valid UserChangePasswordDTO dto) {
        return userService.changePassword(dto);
    }

    /**
     * 当前登录用户的菜单
     */
    @GetMapping("user-menus")
    @Operation(summary = "当前用户菜单", description = "获取当前登录用户的菜单权限")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MenuVO.class)))
    public Response<MenuVO> getCurrentUserMenus() {
        return userService.getCurrentUserMenus();
    }
}