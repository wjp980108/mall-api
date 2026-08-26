package com.atguigu.meet.model.entity.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 抢购订单主表实体
 */
@Data
@TableName("t_order")
public class Order extends Model<Order> {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 唯一订单编号 */
    private String orderNo;

    /** 商品ID */
    private Long goodsId;

    /** 商品名称【下单快照】 */
    private String goodsName;

    /** 卖方会员ID 关联sys_user.id */
    private Long sellerId;

    /** 卖家姓名【下单快照】 */
    private String sellerName;

    /** 卖家手机号【下单快照】 */
    private String sellerPhone;

    /** 买方会员ID 关联sys_user.id */
    private Long buyerId;

    /** 买家姓名【下单快照】 */
    private String buyerName;

    /** 买家手机号【下单快照】 */
    private String buyerPhone;

    /** 抢购成交价格 */
    private BigDecimal rushPrice;

    /** 收货地址完整拼接字符串 */
    private String receiveAddress;

    /**
     * 订单状态：1待付款 2已付款 3已确认 4已代售 5已取消
     * @see com.atguigu.meet.enums.OrderStatus
     */
    private Integer orderStatus;

    /** 上架手续费 */
    private BigDecimal putCommission;

    /** 优惠券抵扣金额 */
    private BigDecimal couponAmount;

    /** 支付凭证图片地址 */
    private String payVoucherUrl;

    /** 支付凭证存储平台:local-1/aliyun-oss-1等 */
    private String payVoucherPlatform;

    /** 付款截止时间（倒计时） */
    private LocalDateTime payDeadline;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 逻辑删除 0未删除 1已删除 */
    @JsonIgnore
    @TableLogic
    private Integer isDeleted = 0;
}
