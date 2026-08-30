package com.atguigu.meet.model.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * @Description
 * @Date 2026-05-18 10:29
 */
@Data
public class AuthLoginDTO {
    /**
     * 账号：手机号或用户名
     */
    @NotBlank(message = "账号不能为空")
    @Length(min = 2, max = 20, message = "账号长度 2-20 位")
    private String account;

    @NotBlank(message = "密码不能为空")
    @Length(min = 6, max = 20, message = "密码长度 6-20 位")
    private String password;
}
