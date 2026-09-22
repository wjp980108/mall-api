package com.atguigu.meet.model.dto.roborder;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 抢购订单用户账单分页查询 DTO（管理端算账读模型）
 * <p>
 * 按选中日期及其前一天对买家进行资金指标聚合，支持关键词搜索与分页。
 */
@Data
@Schema(description = "抢购订单用户账单分页查询参数")
public class RobOrderBillPageQueryDTO {

    @Schema(description = "分页页码", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分页页码不能为空")
    private Integer pageNum;

    @Schema(description = "每页条数", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "每页条数不能为空")
    private Integer pageSize;

    /** 选中日期（yyyy-MM-dd），空则取当天 */
    @Schema(description = "选中日期（yyyy-MM-dd），为空时取当天")
    private String date;

    /** 关键词（买家姓名/手机号 模糊匹配） */
    @Schema(description = "搜索关键词（买家姓名或手机号）")
    private String keyword;
}
