package com.atguigu.meet.model.entity.goods.consign;

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
 * 委托代卖事件全生命周期快照实体（对应 t_consign_record）
 * <p>
 * 一条记录 = 一次委托事件全生命周期：
 * 1待审核 -> 2审核通过·已上架 -> 3已卖出(终态) / 4未售出下架(终态) / 5审核驳回(终态)
 * <p>
 * 红线：本表仅存历史快照履历，商品当前业务状态永远以主表 t_consign_goods 为准；
 * 快照字段（发起委托/成交/下架）写入后永不修改；一次委托申请只生成1条记录，后续节点 UPDATE 该条。
 */
@Data
@TableName("t_consign_record")
public class ConsignRecord extends Model<ConsignRecord> {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 主表 t_consign_goods 主键ID */
    private Long consignGoodsId;

    // ====================== 发起委托时快照（冻结，永不更新） ======================

    /** 委托人(本轮卖家)会员ID */
    private Long memberId;

    /** 快照-委托人昵称 */
    private String memberName;

    /** 快照-委托时商品名称 */
    private String goodsName;

    /** 快照-委托时商品价格 */
    private BigDecimal goodsPrice;

    /** 快照-委托时商品缩略图 */
    private String coverImg;

    /** 所属场次ID(冗余) */
    private Long sessionId;

    // ====================== 生命周期状态 ======================

    /**
     * 委托记录状态
     * 1待审核 2审核通过·已上架 3已卖出 4未售出下架 5审核驳回
     */
    private Integer recordStatus;

    // ====================== 审核字段 ======================

    /** 驳回原因 */
    private String rejectReason;

    /** 发起委托申请时间 */
    private LocalDateTime applyTime;

    /** 审核时间 */
    private LocalDateTime auditTime;

    /** 审核管理员ID */
    private Long auditOperatorId;

    /** 审核管理员名称 */
    private String auditOperatorName;

    // ====================== 成交字段（卖出时快照） ======================

    /** 卖出时间 */
    private LocalDateTime soldTime;

    /** 快照-成交价 */
    private BigDecimal soldPrice;

    /** 快照-买家ID */
    private Long buyerId;

    /** 快照-买家昵称 */
    private String buyerName;

    /** 快照-买家手机号 */
    private String buyerPhone;

    // ====================== 下架字段（未售出下架时） ======================

    /** 下架时间 */
    private LocalDateTime delistTime;

    /** 下架原因:超时未售出/卖家主动取消/后台下架等 */
    private String delistReason;

    /** 备注 */
    private String remark;

    // ====================== 通用字段 ======================

    /** 逻辑删除 0正常 1删除 */
    @JsonIgnore
    @TableLogic
    private Integer isDeleted = 0;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
