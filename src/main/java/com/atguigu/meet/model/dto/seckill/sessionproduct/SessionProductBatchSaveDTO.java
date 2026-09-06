package com.atguigu.meet.model.dto.seckill.sessionproduct;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 场次商品关联批量新增DTO（一个场次一次关联 n 个商品，每个商品指定库存）
 */
@Data
@Schema(description = "场次商品关联批量新增参数")
public class SessionProductBatchSaveDTO {

    /** 场次ID */
    @Schema(description = "场次ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "场次ID不能为空")
    private Long sessionId;

    /** 关联商品列表（每个商品含 goodsId + stock） */
    @Schema(description = "关联商品列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "关联商品列表不能为空")
    @Size(max = 100, message = "单次关联商品数量不能超过100")
    @Valid
    private List<Item> items;

    /**
     * 单个关联商品项
     */
    @Data
    @Schema(description = "关联商品项")
    public static class Item {

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
}
