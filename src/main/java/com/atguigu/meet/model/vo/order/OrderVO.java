package com.atguigu.meet.model.vo.order;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单列表返回VO
 * <p>手机号均脱敏展示（中间4位替换为****）</p>
 */
@Data
public class OrderVO {

    private Long id;

    /** 唯一订单编号 */
    private String orderNo;

    /** 商品ID */
    private Long goodsId;

    /** 商品名称 */
    private String goodsName;

    /** 卖方会员ID */
    private Long sellerId;

    /** 卖家姓名 */
    private String sellerName;

    /** 卖家手机号（脱敏：138****8888） */
    private String sellerPhone;

    /** 买方会员ID */
    private Long buyerId;

    /** 买家姓名 */
    private String buyerName;

    /** 买家手机号（脱敏：138****8888） */
    private String buyerPhone;

    /** 抢购成交价格 */
    private BigDecimal rushPrice;

    /** 收货地址 */
    private String receiveAddress;

    /**
     * 订单状态：1待付款 2已付款 3已确认 4已代售 5已取消
     */
    private Integer orderStatus;

    /** 上架手续费 */
    private BigDecimal putCommission;

    /** 优惠券抵扣金额 */
    private BigDecimal couponAmount;

    /** 支付凭证图片地址 */
    private String payVoucherUrl;

    /** 付款截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payDeadline;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
