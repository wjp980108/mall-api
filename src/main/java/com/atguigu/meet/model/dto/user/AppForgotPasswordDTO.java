package com.atguigu.meet.model.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * H5 端忘记密码（重置密码）请求DTO
 * <p>
 * 无需登录：凭用户名（account 字段）+ 新密码直接重置，服务端按用户名匹配账号。
 */
@Data
@Schema(description = "H5端忘记密码请求参数")
public class AppForgotPasswordDTO {

    /** 账号绑定的用户名 */
    @Schema(description = "账号绑定的用户名", example = "zhangsan", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    private String account;

    /** 新密码 */
    @Schema(description = "新密码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "密码不能为空")
    @Length(min = 6, max = 20, message = "密码长度 6-20 位")
    private String password;
}
