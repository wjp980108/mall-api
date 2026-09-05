package com.atguigu.meet.model.dto.permission.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * @Description
 * @Date 2026-08-13 16:57
 */
@Data
@Schema(description = "用户批量删除参数")
public class UserDeleteDTO {
    @Schema(description = "用户ID数组", example = "[1, 2, 3]", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "用户ids不能为空")
    private Long[] userIds;
}