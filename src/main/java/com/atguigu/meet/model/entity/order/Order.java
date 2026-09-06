package com.atguigu.meet.model.entity.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 抢购订单主表实体
 */
@Data
@TableName("t_order")
@Schema(description = "订单数据")
public class Order extends Model<Order> {

    @TableId(type = IdType.AUTO)
    @Schema(description = "订单ID")
    private Long id;

    /** 唯一订单编号 */
    @Schema(description = "订单编号")
    private String orderNo;

    /** 商品ID */
    @Schema(description = "商品ID")
    private Long goodsId;

    /** 商品名称【下单快照】 */
    @Schema(description = "商品名称")
    private String goodsName;

    /** 卖方会员ID 关联sys_user.id */
    @Schema(description = "卖方会员ID")
    private Long sellerId;

    /** 卖家姓名【下单快照】 */
    @Schema(description = "卖家姓名")
    private String sellerName;

    /** 卖家手机号【下单快照】 */
    @Schema(description = "卖家手机号")
    private String sellerPhone;

    /** 买方会员ID 关联sys_user.id */
    @Schema(description = "买方会员ID")
    private Long buyerId;

    /** 买家姓名【下单快照】 */
    @Schema(description = "买家姓名")
    private String buyerName;

    /** 买家手机号【下单快照】 */
    @Schema(description = "买家手机号")
    private String buyerPhone;

    /** 抢购成交价格 */
    @Schema(description = "抢购成交价格")
    private BigDecimal rushPrice;

    /** 收货地址完整拼接字符串 */
    @Schema(description = "收货地址")
    private String receiveAddress;

    /**
     * 订单状态：1待付款 2已付款 3已确认 4已完成 5已取消
     * @see com.atguigu.meet.enums.OrderStatus
     */
    @Schema(description = "订单状态 1待付款 2已付款 3已确认 4已完成 5已取消")
    private Integer orderStatus;

    /**
     * 取消来源：1待付款取消 2已付款取消 3代售中取消
     */
    @Schema(description = "取消来源：1待付款取消 2已付款取消 3代售中取消")
    private Integer cancelSource;

    /** 上架手续费 */
    @Schema(description = "上架手续费")
    private BigDecimal putCommission;

    /** 优惠券抵扣金额 */
    @Schema(description = "优惠券抵扣金额")
    private BigDecimal couponAmount;

    /** 支付凭证图片地址 */
    @Schema(description = "支付凭证图片地址")
    private String payVoucherUrl;

    /** 支付凭证存储平台:local-1/aliyun-oss-1等 */
    @Schema(description = "支付凭证存储平台")
    private String payVoucherPlatform;

    /** 付款截止时间（倒计时） */
    @Schema(description = "付款截止时间")
    private LocalDateTime payDeadline;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除 0未删除 1已删除 */
    @JsonIgnore
    @TableLogic
    private Integer isDeleted = 0;
}