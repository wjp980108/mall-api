package com.atguigu.meet.model.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * @Description
 * @Date 2026-05-18 10:29
 */
@Data
@Schema(description = "用户登录请求参数")
public class AuthLoginDTO {
    /**
     * 账号：手机号或用户名
     */
    @Schema(description = "账号（手机号或用户名）", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "账号不能为空")
    @Length(min = 2, max = 20, message = "账号长度 2-20 位")
    private String account;

    @Schema(description = "密码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    @Length(min = 6, max = 20, message = "密码长度 6-20 位")
    private String password;
}