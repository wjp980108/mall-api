package com.atguigu.meet.model.dto.roborder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理端确认收款/回款 DTO
 * <p>
 * 单接口承载两种动作，按 action 取值分流：
 * <ul>
 *   <li>action=1 确认收款：未收款(0)→已收款(1)，写 receipt_* 审计字段，需 ROB_ORDER_CONFIRM_RECEIPT 权限</li>
 *   <li>action=2 确认回款：已收款(1)→已回款(2)，写 payback_* 审计字段，需 ROB_ORDER_CONFIRM_PAYBACK 权限</li>
 * </ul>
 * 状态机正向 0→1→2 不可逆；取消订单时 pay_status 已统一置 3 无效，不可再操作。
 */
@Data
@Schema(description = "确认收款/回款请求")
public class RobOrderConfirmPayDTO {

    /** 订单ID */
    @Schema(description = "订单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /** 动作：1=确认收款 2=确认回款 */
    @Schema(description = "动作 1确认收款 2确认回款", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "动作不能为空")
    private Integer action;
}
