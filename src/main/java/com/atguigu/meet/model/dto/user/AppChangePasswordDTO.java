package com.atguigu.meet.model.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * H5 端修改密码请求DTO
 * <p>
 * 用户身份由 token 解析，前端仅需传新密码。
 */
@Data
@Schema(description = "H5端修改密码请求参数")
public class AppChangePasswordDTO {

    /** 新密码 */
    @Schema(description = "新密码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    @Length(min = 6, max = 20, message = "密码长度 6-20 位")
    private String password;
}
