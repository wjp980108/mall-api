package com.atguigu.meet.model.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * H5 端修改密码请求DTO
 * 仅需接收手机号 + 新密码；手机号须与当前登录用户一致（服务端校验，防越权改密）
 */
@Data
@Schema(description = "H5端修改密码请求参数")
public class AppChangePasswordDTO {

    /** 当前登录用户绑定的手机号 */
    @Schema(description = "当前登录用户绑定的手机号", example = "13800138000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /** 新密码 */
    @Schema(description = "新密码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    @Length(min = 6, max = 20, message = "密码长度 6-20 位")
    private String password;
}