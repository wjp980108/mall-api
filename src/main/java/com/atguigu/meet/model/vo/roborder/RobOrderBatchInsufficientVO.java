package com.atguigu.meet.model.vo.roborder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "批量取消订单中积分不足的订单")
public class RobOrderBatchInsufficientVO {

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "积分不足用户")
    private PointsInsufficientVO pointsInsufficient;
}
