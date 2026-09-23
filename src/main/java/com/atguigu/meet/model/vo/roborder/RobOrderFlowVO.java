package com.atguigu.meet.model.vo.roborder;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单流水事件行 VO（管理端算账读模型）
 * <p>
 * 一行 = 一条订单操作事件（下单/取消/转移，转移拆红冲+正向两行）。商品/场次/金额取订单下单快照，
 * 金额按事件视角带符号：下单为正、取消为负（红冲）、转移红冲为负/正向为正。
 */
@Data
@Schema(description = "订单流水事件行")
public class RobOrderFlowVO {

    @Schema(description = "审计日志ID")
    private Long logId;

    @Schema(description = "事件类型 1下单 2取消订单 3转移订单")
    private Integer eventType;

    @Schema(description = "事件类型中文名")
    private String eventTypeName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "事件发生时间")
    private LocalDateTime eventTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "订单创建时间")
    private LocalDateTime orderCreateTime;

    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "商品ID")
    private Long goodsId;

    @Schema(description = "商品名称")
    private String goodsName;

    @Schema(description = "商品货号")
    private String goodsSn;

    @Schema(description = "商品缩略图URL")
    private String goodsThumb;

    @Schema(description = "场次ID")
    private Long sessionId;

    @Schema(description = "场次名称")
    private String sessionName;

    @Schema(description = "买家ID")
    private Long buyerId;

    @Schema(description = "买家姓名")
    private String buyerName;

    @Schema(description = "买家手机号")
    private String buyerPhone;

    @Schema(description = "带符号购买数量：下单为正/取消为负/转移红冲负、正向正")
    private Integer quantity;

    @Schema(description = "商品单价快照")
    private BigDecimal unitPrice;

    @Schema(description = "带符号订单总额：下单正/取消负(红冲)/转移红冲负、正向正")
    private BigDecimal signedTotalAmount;

    /** 带符号回款取整值（事件行落库值按事件视角打符号）：下单为正/取消为负(红冲)/转移红冲负、正向正；取整口径为回款金额 HALF_UP 整数；存量事件行为0 */
    @Schema(description = "带符号回款取整值：下单正/取消负(红冲)/转移红冲负、正向正")
    private BigDecimal receiptRoundAmount;

    /** 带符号付款金额（本金总额口径 P×N，事件行落库值按事件视角打符号）：下单为正/取消为负(红冲)/转移红冲负、正向正；存量事件行为0 */
    @Schema(description = "带符号付款金额(本金总额)：下单正/取消负(红冲)/转移红冲负、正向正")
    private BigDecimal paymentAmount;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人名称")
    private String operatorName;

    @Schema(description = "操作备注（转移记录原买家→新买家）")
    private String remark;
}
