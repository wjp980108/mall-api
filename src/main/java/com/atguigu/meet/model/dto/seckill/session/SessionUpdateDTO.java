package com.atguigu.meet.model.dto.seckill.session;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 抢购场次修改DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "抢购场次修改参数")
public class SessionUpdateDTO extends SessionSaveDTO {
    @Schema(description = "场次ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "场次ID不能为空")
    private Long id;
}