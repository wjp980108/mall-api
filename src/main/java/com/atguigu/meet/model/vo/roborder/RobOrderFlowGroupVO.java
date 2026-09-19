package com.atguigu.meet.model.vo.roborder;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单流水订单组分页中间 VO（管理端算账读模型）
 * <p>
 * 流水分页的分页单位为"订单组"：在筛选条件下至少含一条匹配事件的订单为一组。
 * 本 VO 仅承载第 1 层组分页结果（组定位与组间排序），不直接对外返回；
 * 第 2 层按 orderId 整组取事件行后，由 Service 拼装为平铺的 {@link RobOrderFlowVO} 列表。
 */
@Data
@Schema(description = "订单流水订单组分页项（内部中间结果）")
public class RobOrderFlowGroupVO {

    /** 订单ID（一组的标识） */
    @Schema(description = "订单ID")
    private Long orderId;

    /** 组内最新匹配事件发生时间（组间倒序排序键） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "组内最新匹配事件时间")
    private LocalDateTime latestEventTime;

    /** 组内最新匹配事件日志ID（同秒并列时的组间倒序兜底键） */
    @Schema(description = "组内最新匹配事件日志ID")
    private Long latestLogId;
}
