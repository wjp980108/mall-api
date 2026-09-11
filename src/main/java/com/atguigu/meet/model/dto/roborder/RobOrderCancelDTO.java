package com.atguigu.meet.model.dto.roborder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理端取消订单 DTO（回滚库存与积分，积分不足时需二次确认）
 */
@Data
@Schema(description = "取消订单参数")
public class RobOrderCancelDTO {

    /** 订单ID */
    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /** 用户确认积分不足仍继续（true 时允许冲回为负余额） */
    @Schema(description = "确认积分不足仍继续：不传或 false 时余额不足将返回积分不足提示等待二次确认")
    private Boolean confirmInsufficient;
}
