package com.atguigu.meet.model.dto.points;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 积分明细分页查询 DTO（C 端我的资产）
 */
@Data
@Schema(description = "积分明细分页查询参数")
public class PointsFlowPageQueryDTO {

    @Schema(description = "分页页码", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分页页码不能为空")
    private Integer pageNum;

    @Schema(description = "每页条数", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "每页条数不能为空")
    private Integer pageSize;

    /**
     * 业务类型筛选：1推荐奖 2自购奖 3购物券奖 4积分对冲；不传查全部
     */
    @Schema(description = "业务类型 1推荐奖 2自购奖 3购物券奖 4积分对冲（不传查全部）", example = "1")
    private Integer bizType;
}
