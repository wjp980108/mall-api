package com.atguigu.meet.model.dto.roborder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "批量确认付款/回款参数")
public class RobOrderBatchConfirmPayDTO {

    @NotEmpty(message = "订单ID列表不能为空")
    @Schema(description = "订单ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<@NotNull(message = "订单ID不能为空") Long> orderIds;

    @NotNull(message = "动作不能为空")
    @Min(value = 1, message = "动作仅支持 1=确认付款 2=确认回款")
    @Max(value = 2, message = "动作仅支持 1=确认付款 2=确认回款")
    @Schema(description = "动作 1确认付款 2确认回款", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer action;
}
