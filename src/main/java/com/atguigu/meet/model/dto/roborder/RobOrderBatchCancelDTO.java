package com.atguigu.meet.model.dto.roborder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "批量取消订单参数")
public class RobOrderBatchCancelDTO {

    @NotEmpty(message = "订单ID列表不能为空")
    @Schema(description = "订单ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<@NotNull(message = "订单ID不能为空") Long> orderIds;

    @Schema(description = "确认积分不足仍继续；不传或 false 时返回所有积分不足订单供二次确认")
    private Boolean confirmInsufficient;
}
