package com.atguigu.meet.model.dto.roborder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理端转移订单 DTO（更换买家并划转积分权益）
 */
@Data
@Schema(description = "转移订单参数")
public class RobOrderTransferDTO {

    /** 订单ID */
    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /** 新买家用户ID（sys_user.id） */
    @Schema(description = "新买家用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "新买家不能为空")
    private Long newBuyerId;
}
