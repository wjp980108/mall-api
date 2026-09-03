package com.atguigu.meet.model.vo.goods.consign;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 委托代卖事件记录响应VO
 * <p>
 * 全部字段为发起委托/成交/下架时的历史快照，写入后永不修改；
 * recordStatusName 由 Service 层通过 {@link com.atguigu.meet.enums.RecordStatus#descOf} 组装。
 */
@Data
public class ConsignRecordVO {
    private Long id;

    /** 主表 t_consign_goods 主键ID */
    private Long consignGoodsId;

    // ====================== 发起委托时快照 ======================
    private Long memberId;
    private String memberName;
    private String goodsName;
    private BigDecimal goodsPrice;
    private String coverImg;
    private Long sessionId;

    // ====================== 生命周期状态 ======================
    /**
     * 委托记录状态 1待审核 2审核通过·已上架 3已卖出 4未售出下架 5审核驳回
     * @see com.atguigu.meet.enums.RecordStatus
     */
    private Integer recordStatus;
    /** 记录状态中文名（由 Service 层通过枚举组装） */
    private String recordStatusName;

    // ====================== 审核字段 ======================
    private String rejectReason;
    private LocalDateTime applyTime;
    private LocalDateTime auditTime;
    private Long auditOperatorId;
    private String auditOperatorName;

    // ====================== 成交字段 ======================
    private LocalDateTime soldTime;
    private BigDecimal soldPrice;
    private Long buyerId;
    private String buyerName;
    private String buyerPhone;

    // ====================== 下架字段 ======================
    private LocalDateTime delistTime;
    private String delistReason;

    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
