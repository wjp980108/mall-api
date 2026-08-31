package com.atguigu.meet.service.user.impl;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.enums.Gender;
import com.atguigu.meet.mapper.permission.user.UserMapper;
import com.atguigu.meet.model.dto.user.AppChangePasswordDTO;
import com.atguigu.meet.model.entity.permission.user.SysUser;
import com.atguigu.meet.model.vo.permission.user.UserVO;
import com.atguigu.meet.service.user.AppUserService;
import com.atguigu.meet.utils.AdminContext;
import com.atguigu.meet.utils.BeanConvertUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * H5 端用户中心 Service 实现
 * <p>
 * 当前登录用户从 {@link AdminContext} 获取（JwtAuthenticationFilter 每次请求已与 DB 核对）。
 */
@Service
@Slf4j
public class AppUserServiceImpl extends ServiceImpl<UserMapper, SysUser> implements AppUserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Response getCurrentUserInfo() {
        Long userId = AdminContext.getLoginUserId();
        if (userId == null) {
            return Response.fail(401, "未登录");
        }
        SysUser user = getById(userId);
        if (user == null) {
            return Response.fail(404, "用户不存在");
        }
        UserVO userVO = new UserVO();
        BeanConvertUtils.copyProperties(user, userVO);
        userVO.setGenderName(Gender.descOf(userVO.getGender()));
        return Response.ok(userVO);
    }

    @Override
    public Response changePassword(AppChangePasswordDTO dto) {
        Long userId = AdminContext.getLoginUserId();
        if (userId == null) {
            return Response.fail(401, "未登录");
        }
        SysUser user = getById(userId);
        if (user == null) {
            return Response.fail(404, "用户不存在");
        }
        // 手机号必须与当前登录用户一致，防止凭他人手机号越权改密
        if (!dto.getPhone().equals(user.getPhone())) {
            return Response.fail(500, "手机号与当前登录用户不匹配");
        }
        // 目标字段更新，避免实体内联默认值（gender/status）被覆盖
        lambdaUpdate()
                .set(SysUser::getPassword, passwordEncoder.encode(dto.getPassword()))
                .eq(SysUser::getId, userId)
                .update();
        log.info("[用户中心] H5 修改密码成功，userId={}", userId);
        return Response.ok("密码修改成功", null);
    }
}
