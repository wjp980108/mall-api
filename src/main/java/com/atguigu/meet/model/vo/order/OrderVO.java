package com.atguigu.meet.model.vo.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单列表返回VO
 * <p>手机号原文展示（不做脱敏）</p>
 */
@Data
@Schema(description = "订单响应数据")
public class OrderVO {

    @Schema(description = "订单ID")
    private Long id;

    /** 唯一订单编号 */
    @Schema(description = "订单编号")
    private String orderNo;

    /** 商品ID */
    @Schema(description = "商品ID")
    private Long goodsId;

    /** 商品名称 */
    @Schema(description = "商品名称")
    private String goodsName;

    /** 卖方会员ID */
    @Schema(description = "卖方会员ID")
    private Long sellerId;

    /** 卖家姓名 */
    @Schema(description = "卖家姓名")
    private String sellerName;

    /** 卖家手机号（脱敏：138****8888） */
    @Schema(description = "卖家手机号")
    private String sellerPhone;

    /** 买方会员ID */
    @Schema(description = "买方会员ID")
    private Long buyerId;

    /** 买家姓名 */
    @Schema(description = "买家姓名")
    private String buyerName;

    /** 买家手机号 */
    @Schema(description = "买家手机号")
    private String buyerPhone;

    /** 抢购成交价格 */
    @Schema(description = "成交价格")
    private BigDecimal rushPrice;

    /** 收货地址 */
    @Schema(description = "收货地址")
    private String receiveAddress;

    /**
     * 订单状态：1待付款 2已付款 3已确认 4已完成 5已取消
     * @see com.atguigu.meet.enums.OrderStatus
     */
    @Schema(description = "订单状态：1待付款 2已付款 3已确认 4已完成 5已取消")
    private Integer orderStatus;

    /** 订单状态中文名：待付款/已付款/已确认/已完成/已取消（由 Service 层通过枚举组装） */
    @Schema(description = "订单状态中文名")
    private String orderStatusName;

    /**
     * 取消来源：1待付款取消 2已付款取消 3代售中取消
     */
    @Schema(description = "取消来源：1待付款取消 2已付款取消 3代售中取消")
    private Integer cancelSource;

    /** 取消来源中文名（由 Service 层组装） */
    @Schema(description = "取消来源中文名")
    private String cancelSourceName;

    /** 上架手续费 */
    @Schema(description = "上架手续费")
    private BigDecimal putCommission;

    /** 优惠券抵扣金额 */
    @Schema(description = "优惠券抵扣金额")
    private BigDecimal couponAmount;

    /** 支付凭证图片地址 */
    @Schema(description = "支付凭证URL")
    private String payVoucherUrl;

    /** 支付凭证存储平台:local-1/aliyun-oss-1等 */
    @Schema(description = "支付凭证存储平台")
    private String payVoucherPlatform;

    /** 付款截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "付款截止时间")
    private LocalDateTime payDeadline;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}