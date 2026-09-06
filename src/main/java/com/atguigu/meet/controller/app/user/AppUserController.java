package com.atguigu.meet.controller.app.user;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.user.AppChangePasswordDTO;
import com.atguigu.meet.model.dto.user.AppForgotPasswordDTO;
import com.atguigu.meet.model.dto.user.AppUpdateUserInfoDTO;
import com.atguigu.meet.model.vo.permission.user.UserVO;
import com.atguigu.meet.service.user.AppUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * H5 端用户中心
 * <p>
 * 查询当前用户信息、修改用户信息、修改密码、忘记密码（重置密码无需登录）；
 * 登录态接口的当前用户从 AdminContext 获取。
 */
@RestController
@RequestMapping("/app/user")
@Tag(name = "H5端用户中心", description = "用户信息查询、修改、密码管理")
public class AppUserController {

    @Autowired
    private AppUserService appUserService;

    /**
     * 查询当前登录用户信息
     */
    @Operation(summary = "查询当前用户信息", description = "获取当前登录用户的详细信息")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserVO.class)))
    @GetMapping("info")
    public Response<UserVO> info() {
        return appUserService.getCurrentUserInfo();
    }

    /**
     * 修改用户信息
     * （昵称、邮箱、性别、年龄、生日、头像等；未传字段不更新）
     */
    @PutMapping("info")
    @Operation(summary = "修改用户信息", description = "修改当前用户的昵称、邮箱、性别、年龄、生日、头像等信息")
    public Response<Void> updateUserInfo(@RequestBody @Valid AppUpdateUserInfoDTO dto) {
        return appUserService.updateCurrentUserInfo(dto);
    }

    /**
     * 修改密码
     * （仅需手机号 + 新密码；手机号须与当前登录用户一致，防越权）
     */
    @PutMapping("password")
    @Operation(summary = "修改密码", description = "修改当前用户密码，需验证手机号与当前登录用户一致")
    public Response<Void> changePassword(@RequestBody @Valid AppChangePasswordDTO dto) {
        return appUserService.changePassword(dto);
    }

    /**
     * 忘记密码
     *
     * @param dto 忘记密码请求参数（手机号 + 新密码）
     * @return 重置结果提示
     */
    @PutMapping("forgot-password")
    @Operation(summary = "忘记密码", description = "凭注册手机号 + 新密码直接重置，无需登录")
    public Response<Void> forgotPassword(@RequestBody @Valid AppForgotPasswordDTO dto) {
        return appUserService.forgotPassword(dto);
    }
}