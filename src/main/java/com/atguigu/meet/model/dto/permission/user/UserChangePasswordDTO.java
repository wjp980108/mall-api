package com.atguigu.meet.model.dto.permission.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 当前登录用户修改密码请求DTO
 * <p>
 * 需接收账号（手机号或用户名）+ 新密码；账号须与当前登录用户一致（服务端校验，防越权改密）。
 */
@Data
@Schema(description = "当前用户修改密码参数")
public class UserChangePasswordDTO {

    /** 当前登录用户绑定的账号（手机号或用户名） */
    @Schema(description = "当前登录用户绑定的账号（手机号或用户名）", example = "13800138000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "账号不能为空")
    private String account;

    /** 新密码 */
    @Schema(description = "新密码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    @Length(min = 6, max = 20, message = "密码长度 6-20 位")
    private String password;
}