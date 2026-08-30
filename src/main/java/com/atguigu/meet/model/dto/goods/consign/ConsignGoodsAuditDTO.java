package com.atguigu.meet.model.dto.goods.consign;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 托售商品委托代卖审核 DTO（后台管理员使用）
 */
@Data
public class ConsignGoodsAuditDTO {

    @NotNull(message = "商品ID不能为空")
    private Long goodsId;

    /** 审核结果 true通过 false驳回 */
    @NotNull(message = "审核结果不能为空")
    private Boolean pass;

    /** 审核备注（驳回原因等，可选） */
    private String remark;
}
