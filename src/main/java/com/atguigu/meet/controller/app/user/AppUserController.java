package com.atguigu.meet.controller.app.user;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.user.AppChangePasswordDTO;
import com.atguigu.meet.service.user.AppUserService;
import com.atguigu.meet.utils.AdminContext;
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
 * 查询当前用户信息、修改密码；当前登录用户从 {@link AdminContext} 获取。
 */
@RestController
@RequestMapping("/app/user")
public class AppUserController {

    @Autowired
    private AppUserService appUserService;

    /**
     * 查询当前登录用户信息
     */
    @GetMapping("info")
    public Response info() {
        return appUserService.getCurrentUserInfo();
    }

    /**
     * 修改密码
     * （仅需手机号 + 新密码；手机号须与当前登录用户一致，防越权）
     */
    @PutMapping("password")
    public Response changePassword(@RequestBody @Valid AppChangePasswordDTO dto) {
        return appUserService.changePassword(dto);
    }
}
