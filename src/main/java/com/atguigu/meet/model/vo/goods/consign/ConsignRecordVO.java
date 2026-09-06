package com.atguigu.meet.model.vo.goods.consign;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "委托记录响应数据")
public class ConsignRecordVO {
    @Schema(description = "记录ID")
    private Long id;

    /** 主表 t_consign_goods 主键ID */
    @Schema(description = "托售商品ID")
    private Long consignGoodsId;

    // ====================== 发起委托时快照 ======================
    @Schema(description = "会员ID")
    private Long memberId;
    @Schema(description = "会员名称")
    private String memberName;
    @Schema(description = "商品名称")
    private String goodsName;
    @Schema(description = "商品价格")
    private BigDecimal goodsPrice;
    @Schema(description = "封面图URL")
    private String coverImg;
    @Schema(description = "场次ID")
    private Long sessionId;

    // ====================== 生命周期状态 ======================
    /**
     * 委托记录状态 1待审核 2审核通过·已上架 3已卖出 4未售出下架 5审核驳回
     * @see com.atguigu.meet.enums.RecordStatus
     */
    @Schema(description = "委托记录状态 1待审核 2审核通过·已上架 3已卖出 4未售出下架 5审核驳回")
    private Integer recordStatus;
    /** 记录状态中文名（由 Service 层通过枚举组装） */
    @Schema(description = "记录状态中文名")
    private String recordStatusName;

    // ====================== 审核字段 ======================
    @Schema(description = "驳回原因")
    private String rejectReason;
    @Schema(description = "申请时间")
    private LocalDateTime applyTime;
    @Schema(description = "审核时间")
    private LocalDateTime auditTime;
    @Schema(description = "审核人ID")
    private Long auditOperatorId;
    @Schema(description = "审核人名称")
    private String auditOperatorName;

    // ====================== 成交字段 ======================
    @Schema(description = "成交时间")
    private LocalDateTime soldTime;
    @Schema(description = "成交价格")
    private BigDecimal soldPrice;
    @Schema(description = "买家ID")
    private Long buyerId;
    @Schema(description = "买家名称")
    private String buyerName;
    @Schema(description = "买家手机号")
    private String buyerPhone;

    // ====================== 下架字段 ======================
    @Schema(description = "下架时间")
    private LocalDateTime delistTime;
    @Schema(description = "下架原因")
    private String delistReason;

    @Schema(description = "备注")
    private String remark;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}