package com.atguigu.meet.service.user;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.user.AppChangePasswordDTO;
import com.atguigu.meet.model.dto.user.AppUpdateUserInfoDTO;

/**
 * H5 端用户中心 Service（当前用户信息、修改用户信息、修改密码）
 */
public interface AppUserService {

    /** 查询当前登录用户信息 */
    Response getCurrentUserInfo();

    /** 修改当前登录用户信息（昵称、邮箱、性别、年龄、生日、头像等；未传字段不更新） */
    Response updateCurrentUserInfo(AppUpdateUserInfoDTO dto);

    /** 修改当前登录用户密码（仅需手机号 + 新密码，手机号须与登录用户一致） */
    Response changePassword(AppChangePasswordDTO dto);
}
