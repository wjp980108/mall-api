package com.atguigu.meet.model.vo.roborder;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 单笔订单流水详情中的事件时间线项 VO
 */
@Data
@Schema(description = "订单操作事件时间线项")
public class RobOrderFlowEventVO {

    @Schema(description = "审计日志ID")
    private Long logId;

    @Schema(description = "事件类型 1下单 2取消订单 3转移订单")
    private Integer eventType;

    @Schema(description = "事件类型中文名")
    private String eventTypeName;

    @Schema(description = "操作前订单状态 1正常 2已取消")
    private Integer beforeStatus;

    @Schema(description = "操作前订单状态中文名")
    private String beforeStatusName;

    @Schema(description = "操作后订单状态 1正常 2已取消")
    private Integer afterStatus;

    @Schema(description = "操作后订单状态中文名")
    private String afterStatusName;

    @Schema(description = "操作人ID")
    private Long operatorId;

    @Schema(description = "操作人名称")
    private String operatorName;

    @Schema(description = "操作备注")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "事件发生时间")
    private LocalDateTime eventTime;
}
