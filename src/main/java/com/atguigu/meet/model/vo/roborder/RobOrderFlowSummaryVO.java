package com.atguigu.meet.model.vo.roborder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单流水区间汇总 VO（管理端算账读模型）
 * <p>
 * 三组口径：成交（下单事件，正额）、红冲（取消事件，金额取正表示冲销规模）、
 * 净额（成交 - 红冲）。转移事件不参与资金聚合。
 */
@Data
@Schema(description = "订单流水区间汇总")
public class RobOrderFlowSummaryVO {

    @Schema(description = "成交（下单事件）")
    private FlowAmountSummary income;

    @Schema(description = "红冲（取消事件，金额为冲销规模正数）")
    private FlowAmountSummary reversal;

    @Schema(description = "净额（成交 - 红冲）")
    private FlowAmountSummary net;

    /**
     * 一组金额聚合指标
     */
    @Data
    @Schema(description = "流水金额聚合")
    public static class FlowAmountSummary {

        @Schema(description = "笔数")
        private Long count = 0L;

        @Schema(description = "商品件数")
        private Integer quantity = 0;

        @Schema(description = "订单实付总额")
        private BigDecimal totalAmount = BigDecimal.ZERO;

        @Schema(description = "利润池金额")
        private BigDecimal profitAmount = BigDecimal.ZERO;

        @Schema(description = "推荐奖金额")
        private BigDecimal recommendAmount = BigDecimal.ZERO;

        @Schema(description = "自购奖金额")
        private BigDecimal selfBuyAmount = BigDecimal.ZERO;

        @Schema(description = "自购奖金金额")
        private BigDecimal selfBuyBonusAmount = BigDecimal.ZERO;

        @Schema(description = "购物券金额")
        private BigDecimal selfBuyCouponAmount = BigDecimal.ZERO;
    }
}
