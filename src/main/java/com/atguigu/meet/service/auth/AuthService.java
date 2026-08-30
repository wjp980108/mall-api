package com.atguigu.meet.service.auth;

import com.atguigu.meet.common.Response;
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
     * H5 端登录（与 login 共用校验逻辑，额外返回用户基本信息供 H5 前端展示）
     */
    Response appLogin(AuthLoginDTO user);

}
