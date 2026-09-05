package com.atguigu.meet.controller.app.auth;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.auth.AuthLoginDTO;
import com.atguigu.meet.model.dto.auth.AuthRegisterDTO;
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
 * H5 端注册/登录
 * <p>
 * 注册入口仅对 H5 开放：注册成功后默认绑定 MEMBER(会员) 角色。
 * 登录独立提供 /app/auth/login（校验逻辑与后台共用），返回 token + 用户基本信息。
 */
@RestController
@RequestMapping("/app/auth")
@Tag(name = "H5端注册登录", description = "H5端用户注册、登录接口")
public class AppAuthController {

    @Autowired
    private AuthService authService;

    /**
     * H5 端注册
     * （默认绑定 id=3 的 MEMBER 角色，支持选填邀请码）
     */
    @Operation(summary = "H5端注册", description = "用户注册，默认绑定MEMBER角色，支持选填邀请码")
    @PostMapping("register")
    public Response register(@RequestBody @Valid AuthRegisterDTO user) {
        return authService.register(user);
    }

    /**
     * H5 端登录
     * （账号支持手机号/用户名，仅返回 token）
     */
    @Operation(summary = "H5端登录", description = "使用账号密码登录，返回token")
    @PostMapping("login")
    public Response login(@RequestBody @Valid AuthLoginDTO user) {
        return authService.appLogin(user);
    }
}