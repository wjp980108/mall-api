package com.atguigu.meet.controller.auth;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.auth.AuthForgotPasswordDTO;
import com.atguigu.meet.model.dto.auth.AuthLoginDTO;
import com.atguigu.meet.model.vo.permission.user.UserLoginVO;
import com.atguigu.meet.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录
 * @Description
 * @Date 2026-08-12 22:59
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "后台登录", description = "后台管理员登录接口")
public class AuthController {
    @Autowired
    private AuthService authService;

    /**
     * 登录
     */
    @Operation(summary = "后台登录", description = "管理员使用账号密码登录后台")
    @PostMapping("login")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserLoginVO.class)))
    public Response<UserLoginVO> login(@RequestBody @Valid AuthLoginDTO user) {
        return authService.login(user);
    }

    /**
     * 忘记密码（无需登录，public-paths 已放行）
     * <p>
     * 凭用户名（account 字段）+ 新密码直接重置；属于认证范畴，故放在 /auth 而非用户管理模块。
     */
    @Operation(summary = "忘记密码", description = "凭用户名（account 字段）+ 新密码直接重置后台账号密码，无需登录")
    @PutMapping("forgot-password")
    public Response<Void> forgotPassword(@RequestBody @Valid AuthForgotPasswordDTO dto) {
        return authService.forgotPassword(dto);
    }
}