package com.atguigu.meet.controller.auth;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.auth.AuthLoginDTO;
import com.atguigu.meet.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
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
    public Response login(@RequestBody @Valid AuthLoginDTO user) {
        return authService.login(user);
    }
}