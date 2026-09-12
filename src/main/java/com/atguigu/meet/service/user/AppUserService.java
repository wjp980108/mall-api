package com.atguigu.meet.service.user;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.user.AppChangePasswordDTO;
import com.atguigu.meet.model.dto.user.AppForgotPasswordDTO;
import com.atguigu.meet.model.dto.user.AppUpdateUserInfoDTO;

/**
 * H5 端用户中心 Service（当前用户信息、修改用户信息、修改密码、忘记密码）
 */
public interface AppUserService {

    /** 查询当前登录用户信息 */
    Response getCurrentUserInfo();

    /** 修改当前登录用户信息（昵称、邮箱、性别、年龄、生日、头像等；未传字段不更新） */
    Response updateCurrentUserInfo(AppUpdateUserInfoDTO dto);

    /** 修改当前登录用户密码（用户身份由 token 解析，前端仅需传新密码） */
    Response changePassword(AppChangePasswordDTO dto);

    /** 忘记密码/重置密码（无需登录，凭用户名 account + 新密码重置） */
    Response forgotPassword(AppForgotPasswordDTO dto);
}
