package com.atguigu.meet.model.entity.roborder;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 抢购订单操作审计日志实体（对应 t_rob_order_operate_log）
 * <p>
 * operate_type：1下单 2取消订单 3转移订单；结构对齐 t_order_operate_log，不做逻辑删除。
 */
@Data
@TableName("t_rob_order_operate_log")
@Schema(description = "抢购订单操作日志数据")
public class RobOrderOperateLog extends Model<RobOrderOperateLog> {

    @TableId(type = IdType.AUTO)
    @Schema(description = "日志ID")
    private Long id;

    /** 订单ID，关联 t_rob_order.id */
    @Schema(description = "订单ID")
    private Long orderId;

    /** 操作前订单状态 */
    @Schema(description = "操作前订单状态")
    private Integer beforeStatus;

    /** 操作后订单状态 */
    @Schema(description = "操作后订单状态")
    private Integer afterStatus;

    /**
     * 操作类型：1下单 2取消订单 3转移订单
     * @see com.atguigu.meet.enums.RobOrderOperateType
     */
    @Schema(description = "操作类型 1下单 2取消订单 3转移订单")
    private Integer operateType;

    /** 操作类型中文描述（冗余展示） */
    @Schema(description = "操作类型描述")
    private String operateDesc;

    /** 操作人ID（管理员/会员） */
    @Schema(description = "操作人ID")
    private Long operateUserId;

    /** 操作人名称快照 */
    @Schema(description = "操作人名称")
    private String operateUserName;

    /** 回款金额（下单事件：自购奖金+付款金额×数量；取消/转移事件：同订单下单事件行同值正数，展示符号由流水查询层决定。无实例默认值，DB 列 DEFAULT 0 兜底） */
    @Schema(description = "回款金额")
    private BigDecimal receiptAmount;

    /**
     * 付款金额（本金总额口径 = 商品付款单价 t_consign_goods.payment_amount × 购买数量）。
     * 下单事件：下单时点商品付款单价旧值×数量；取消/转移事件：同订单下单事件行同值正数。
     * 注意与 {@link com.atguigu.meet.model.entity.goods.consign.ConsignGoods#getPaymentAmount()}
     * 的单价口径区分：本字段为总额口径 P×N。无实例默认值，DB 列 DEFAULT 0 兜底。
     */
    @Schema(description = "付款金额(本金总额口径：商品付款单价×数量)")
    private BigDecimal paymentAmount;

    /**
     * 事件发生时买家ID快照：下单事件=下单买家，取消事件=取消时订单当前买家，转移事件=转入新买家（正向行用）。
     * 供流水查询的下单/取消/转移正向行展示，防止订单转移后历史行买家漂移；存量历史行为 NULL，查询层 COALESCE 兜底。
     */
    @Schema(description = "事件买家ID(下单=下单买家/取消=取消时买家/转移=新买家)")
    private Long buyerId;

    /** 事件发生时买家名称快照（下单/取消/转移正向行买家） */
    @Schema(description = "事件买家名称快照")
    private String buyerName;

    /** 事件发生时买家手机号快照（下单/取消/转移正向行买家） */
    @Schema(description = "事件买家手机号快照")
    private String buyerPhone;

    /** 转移前买家ID（仅转移事件在订单 UPDATE 前写入的快照；下单/取消事件为 NULL） */
    @Schema(description = "转移前买家ID(仅转移事件快照)")
    private Long prevBuyerId;

    /** 转移前买家名称快照（仅转移事件） */
    @Schema(description = "转移前买家名称快照(仅转移事件)")
    private String prevBuyerName;

    /** 转移前买家手机号快照（仅转移事件） */
    @Schema(description = "转移前买家手机号快照(仅转移事件)")
    private String prevBuyerPhone;

    /** 转移前推荐人ID（仅转移事件在订单 UPDATE 前写入的快照） */
    @Schema(description = "转移前推荐人ID(仅转移事件快照)")
    private Long prevInviterId;

    /** 操作备注（转移记录原买家→新买家） */
    @Schema(description = "操作备注")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
