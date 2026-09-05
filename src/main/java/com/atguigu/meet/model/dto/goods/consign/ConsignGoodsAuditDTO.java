package com.atguigu.meet.model.dto.goods.consign;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 托售商品委托代卖审核 DTO（后台管理员使用）
 */
@Data
@Schema(description = "托售商品审核参数")
public class ConsignGoodsAuditDTO {

    @Schema(description = "商品ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "商品ID不能为空")
    private Long goodsId;

    /** 审核结果 true通过 false驳回 */
    @Schema(description = "审核结果", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "审核结果不能为空")
    private Boolean pass;

    /** 审核备注（驳回原因等，可选） */
    @Schema(description = "审核备注", example = "审核通过")
    private String remark;
}