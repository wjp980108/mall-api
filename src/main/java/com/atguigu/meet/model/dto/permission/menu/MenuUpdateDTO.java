package com.atguigu.meet.model.dto.permission.menu;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单修改DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "菜单修改参数")
public class MenuUpdateDTO extends MenuSaveDTO {
    @Schema(description = "菜单ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "菜单ID不能为空")
    private Long id;
}