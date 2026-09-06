package com.atguigu.meet.model.dto.seckill.sessionproduct;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 场次商品关联新增DTO（关联商品并设置该场次库存）
 */
@Data
@Schema(description = "场次商品关联新增参数")
public class SessionProductSaveDTO {

    /** 场次ID */
    @Schema(description = "场次ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "场次ID不能为空")
    private Long sessionId;

    /** 抢购商品ID */
    @Schema(description = "抢购商品ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "抢购商品ID不能为空")
    private Long goodsId;

    /** 该场次该商品的抢购库存 */
    @Schema(description = "该场次该商品的抢购库存", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "抢购库存不能为空")
    @Min(value = 0, message = "抢购库存不能为负数")
    private Integer stock;

    /** 排序号（不传默认0） */
    @Schema(description = "排序号", example = "100")
    @Min(value = 0, message = "排序号不能为负数")
    private Integer sort;
}
