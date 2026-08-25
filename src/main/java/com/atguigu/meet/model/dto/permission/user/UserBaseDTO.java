package com.atguigu.meet.model.dto.permission.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;

/**
 * @Description
 * @Date 2026-05-11 14:56
 */
@Data
public class UserBaseDTO {
    @Length(min = 2, max = 20, message = "用户名长度 2-20 位")
    private String username;

    @Length(min = 2, max = 20, message = "昵称长度 2-20 位")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Length(min = 6, max = 20, message = "密码长度 6-20 位")
    private String password;

    private Integer gender;

    private Integer age;

    private String avatar;

    private LocalDate birthday;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    private Boolean status;
}