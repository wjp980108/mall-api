package com.atguigu.meet.model.entity.roborder;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 抢购订单实体（对应 t_rob_order）
 * <p>
 * 基于场次商品关联（t_session_product）的平台抢购订单，下单即成交，极简两态（1正常/2已取消）。
 * 场次/商品/买家/推荐人/金额全部在下单瞬间快照冻结：改收益比例不影响历史订单。
 */
@Data
@TableName("t_rob_order")
@Schema(description = "抢购订单数据")
public class RobOrder extends Model<RobOrder> {

    @TableId(type = IdType.AUTO)
    @Schema(description = "订单ID")
    private Long id;

    /** 订单编号（22位） */
    @Schema(description = "订单编号")
    private String orderNo;

    /** 场次商品关联ID（库存扣减/回滚锚点），关联 t_session_product.id */
    @Schema(description = "场次商品关联ID")
    private Long sessionProductId;

    /** 场次ID快照 */
    @Schema(description = "场次ID")
    private Long sessionId;

    /** 场次名称快照 */
    @Schema(description = "场次名称")
    private String sessionName;

    /** 场次每日抢购开始时间快照 */
    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "场次抢购开始时间")
    private LocalTime rushStartTime;

    /** 场次每日抢购结束时间快照 */
    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "场次抢购结束时间")
    private LocalTime rushEndTime;

    /** 商品ID快照 */
    @Schema(description = "商品ID")
    private Long goodsId;

    /** 商品名称快照 */
    @Schema(description = "商品名称")
    private String goodsName;

    /** 商品货号快照 */
    @Schema(description = "商品货号")
    private String goodsSn;

    /** 商品缩略图URL快照 */
    @Schema(description = "商品缩略图URL")
    private String goodsThumb;

    /** 商品缩略图存储平台 */
    @Schema(description = "商品缩略图存储平台")
    private String goodsThumbPlatform;

    /** 商品单价快照 */
    @Schema(description = "商品单价")
    private BigDecimal unitPrice;

    /**
     * 下单时点回写前的商品付款金额旧值（单价口径，对齐 t_consign_goods.payment_amount 精度）。
     * 与回款/付款本金在同一读旧值时点冻结，成交单价回写商品后本值不变；
     * 取消订单链式回滚商品付款金额时作为"首轮成交前基数"兜底。无实例默认值，DB 列 DEFAULT 0 兜底。
     */
    @Schema(description = "下单时点商品付款金额旧值快照(单价口径)")
    private BigDecimal prevPaymentAmount;

    /** 购买数量 */
    @Schema(description = "购买数量")
    private Integer quantity;

    /** 订单实付总额 = 单价 × 数量 */
    @Schema(description = "订单实付总额")
    private BigDecimal totalAmount;

    /** 利润池上限 = 总额 × 订单利润比例% */
    @Schema(description = "利润池上限")
    private BigDecimal profitAmount;

    /** 推荐奖 = 总额 × 推荐奖比例%（发邀请人） */
    @Schema(description = "推荐奖")
    private BigDecimal recommendAmount;

    /** 自购奖 = 总额 × 自购奖励比例% */
    @Schema(description = "自购奖")
    private BigDecimal selfBuyAmount;

    /** 自购奖金 = 自购奖 × 自购奖金占比%（计入可用积分） */
    @Schema(description = "自购奖金")
    private BigDecimal selfBuyBonusAmount;

    /** 购物券 = 自购奖 × 购物券占比%（计入购物券积分） */
    @Schema(description = "购物券金额")
    private BigDecimal selfBuyCouponAmount;

    /** 买家ID */
    @Schema(description = "买家ID")
    private Long buyerId;

    /** 买家姓名快照 */
    @Schema(description = "买家姓名")
    private String buyerName;

    /** 买家手机号快照 */
    @Schema(description = "买家手机号")
    private String buyerPhone;

    /** 买家头像URL快照 */
    @Schema(description = "买家头像URL")
    private String buyerAvatar;

    /** 买家头像存储平台 */
    @Schema(description = "买家头像存储平台")
    private String buyerAvatarPlatform;

    /** 推荐人（买家邀请人）ID快照，无则为空 */
    @Schema(description = "推荐人ID")
    private Long inviterId;

    /** 推荐人姓名快照 */
    @Schema(description = "推荐人姓名")
    private String inviterName;

    /** 收货人姓名快照（下单时取 t_user_address，不存地址外键；地址事后改/删不影响历史订单） */
    @Schema(description = "收货人姓名快照")
    private String receiverName;

    /** 收货人手机号快照（下单时取 t_user_address，不存地址外键） */
    @Schema(description = "收货人手机号快照")
    private String receiverPhone;

    /** 收货地址完整字符串快照（下单时取 t_user_address，不存地址外键） */
    @Schema(description = "收货地址快照")
    private String receiveAddress;

    /**
     * 订单状态：1正常 2已取消
     * @see com.atguigu.meet.enums.RobOrderStatus
     */
    @Schema(description = "订单状态 1正常 2已取消")
    private Integer orderStatus;

    /**
     * 收款回款状态：0未收款/1已收款/2已回款/3无效
     * <p>与 order_status 解耦的独立资金状态机：正向 0→1→2 不可逆，取消订单时统一置 3 无效。
     * 下单不显式写，靠 DB 列 DEFAULT 0 兜底。
     * @see com.atguigu.meet.enums.RobOrderPayStatus
     */
    @Schema(description = "收款回款状态 0未收款 1已收款 2已回款 3无效")
    private Integer payStatus;

    /** 确认收款操作人ID（关联 sys_user.id，确认收款时写入，取消订单时保留不清空） */
    @Schema(description = "确认收款操作人ID")
    private Long receiptOperateUserId;

    /** 确认收款时间（确认收款动作发生时写入） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "确认收款时间")
    private LocalDateTime receiptOperateTime;

    /** 确认回款操作人ID（关联 sys_user.id，确认回款时写入，取消订单时保留不清空） */
    @Schema(description = "确认回款操作人ID")
    private Long paybackOperateUserId;

    /** 确认回款时间（确认回款动作发生时写入） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "确认回款时间")
    private LocalDateTime paybackOperateTime;

    /** 逻辑删除 0未删 1已删 */
    @JsonIgnore
    @TableLogic
    private Integer isDeleted = 0;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
