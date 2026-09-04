package com.atguigu.meet.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * H5 端忘记密码（重置密码）请求DTO
 * <p>
 * 无需登录：凭注册手机号 + 新密码直接重置，服务端按手机号匹配账号。
 */
@Data
public class AppForgotPasswordDTO {

    /** 注册账号绑定的手机号 */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 新密码 */
    @NotBlank(message = "密码不能为空")
    @Length(min = 6, max = 20, message = "密码长度 6-20 位")
    private String password;
}
