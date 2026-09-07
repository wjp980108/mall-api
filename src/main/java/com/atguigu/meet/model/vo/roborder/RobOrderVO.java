package com.atguigu.meet.model.vo.roborder;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 抢购订单 VO（管理端列表/详情 + C端我的订单/详情共用）
 * <p>全部为下单快照字段，列表无需 JOIN 其他表。
 */
@Data
@Schema(description = "抢购订单数据")
public class RobOrderVO {

    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "场次商品关联ID")
    private Long sessionProductId;

    @Schema(description = "场次ID")
    private Long sessionId;

    @Schema(description = "场次名称")
    private String sessionName;

    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "场次抢购开始时间")
    private LocalTime rushStartTime;

    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "场次抢购结束时间")
    private LocalTime rushEndTime;

    @Schema(description = "商品ID")
    private Long goodsId;

    @Schema(description = "商品名称")
    private String goodsName;

    @Schema(description = "商品货号")
    private String goodsSn;

    @Schema(description = "商品缩略图URL")
    private String goodsThumb;

    @Schema(description = "商品单价")
    private BigDecimal unitPrice;

    @Schema(description = "购买数量")
    private Integer quantity;

    @Schema(description = "订单实付总额")
    private BigDecimal totalAmount;

    @Schema(description = "利润池上限")
    private BigDecimal profitAmount;

    @Schema(description = "推荐奖金额")
    private BigDecimal recommendAmount;

    @Schema(description = "自购奖金额")
    private BigDecimal selfBuyAmount;

    @Schema(description = "自购奖金金额")
    private BigDecimal selfBuyBonusAmount;

    @Schema(description = "购物券金额")
    private BigDecimal selfBuyCouponAmount;

    @Schema(description = "买家ID")
    private Long buyerId;

    @Schema(description = "买家姓名")
    private String buyerName;

    @Schema(description = "买家手机号")
    private String buyerPhone;

    @Schema(description = "买家头像URL")
    private String buyerAvatar;

    @Schema(description = "推荐人ID")
    private Long inviterId;

    @Schema(description = "推荐人姓名")
    private String inviterName;

    @Schema(description = "订单状态 1正常 2已取消")
    private Integer orderStatus;

    @Schema(description = "订单状态中文名")
    private String orderStatusName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "下单时间")
    private LocalDateTime createTime;
}
