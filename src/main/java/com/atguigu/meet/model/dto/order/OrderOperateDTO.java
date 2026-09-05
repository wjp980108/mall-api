package com.atguigu.meet.model.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 订单通用操作 DTO（取消订单 / 删除订单 / 确认收款）
 */
@Data
@Schema(description = "订单通用操作参数")
public class OrderOperateDTO {

    @Schema(description = "订单ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "订单ID不能为空")
    private Long id;

    /** 操作备注（可选） */
    @Schema(description = "操作备注", example = "用户申请取消")
    private String remark;
}