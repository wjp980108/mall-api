package com.atguigu.meet.service.auth;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.auth.AuthForgotPasswordDTO;
import com.atguigu.meet.model.dto.auth.AuthRegisterDTO;
import com.atguigu.meet.model.dto.auth.AuthLoginDTO;

/**
 * @Description
 * @Date 2026-08-12 23:57
 */
public interface AuthService {
    Response register(AuthRegisterDTO user);

    Response login(AuthLoginDTO user);

    /**
     * H5 端登录（与 login 共用校验逻辑，仅返回 token）
     */
    Response appLogin(AuthLoginDTO user);

    /**
     * 忘记密码（无需登录）：凭用户名 + 新密码直接重置
     */
    Response forgotPassword(AuthForgotPasswordDTO dto);

}