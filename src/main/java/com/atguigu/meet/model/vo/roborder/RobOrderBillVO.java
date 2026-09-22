package com.atguigu.meet.model.vo.roborder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 抢购订单用户账单 VO（管理端算账读模型）
 * <p>
 * 按买家聚合选中日期（今日）及前一日（昨日）的资金指标，金额字段无数据时为 0 不为 null。
 */
@Data
@Schema(description = "抢购订单用户账单")
public class RobOrderBillVO {

    @Schema(description = "买家ID")
    private Long userId;

    @Schema(description = "买家姓名快照")
    private String userName;

    @Schema(description = "买家手机号快照")
    private String phone;

    @Schema(description = "今日买入金额")
    private BigDecimal todayPurchaseAmount = BigDecimal.ZERO;

    @Schema(description = "今日寄售金额（今日回款 - 今日付款）")
    private BigDecimal todayConsignmentAmount = BigDecimal.ZERO;

    @Schema(description = "今日付款金额")
    private BigDecimal todayPaymentAmount = BigDecimal.ZERO;

    @Schema(description = "今日回款金额")
    private BigDecimal todayReceiptAmount = BigDecimal.ZERO;

    @Schema(description = "今日分享金额")
    private BigDecimal todayShareAmount = BigDecimal.ZERO;

    @Schema(description = "昨日买入金额")
    private BigDecimal yesterdayPurchaseAmount = BigDecimal.ZERO;

    @Schema(description = "昨日寄售金额（昨日回款 - 昨日付款）")
    private BigDecimal yesterdayConsignmentAmount = BigDecimal.ZERO;

    @Schema(description = "昨日付款金额")
    private BigDecimal yesterdayPaymentAmount = BigDecimal.ZERO;

    @Schema(description = "昨日回款金额")
    private BigDecimal yesterdayReceiptAmount = BigDecimal.ZERO;

    @Schema(description = "应付款（今日付款 - 昨日回款）")
    private BigDecimal payableAmount = BigDecimal.ZERO;
}
