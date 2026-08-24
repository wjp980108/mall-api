package com.atguigu.meet.model.dto.order;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 订单通用操作 DTO（取消订单 / 删除订单 / 确认收款）
 */
@Data
public class OrderOperateDTO {

    @NotNull(message = "订单ID不能为空")
    private Long id;

    /** 操作备注（可选） */
    private String remark;
}
