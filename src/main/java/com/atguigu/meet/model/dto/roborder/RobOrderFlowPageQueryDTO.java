package com.atguigu.meet.model.dto.roborder;

import com.atguigu.meet.utils.TimeRangeUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 订单流水分页查询 DTO（管理端算账读模型）
 * <p>
 * 查询轴为订单操作事件的发生时间（t_rob_order_operate_log.create_time），
 * 与运营视角的 {@link RobOrderPageQueryDTO}（按订单下单日）刻意隔离。
 */
@Data
@Schema(description = "订单流水分页查询参数")
public class RobOrderFlowPageQueryDTO {

    @Schema(description = "分页页码", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分页页码不能为空")
    private Integer pageNum;

    @Schema(description = "每页条数", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "每页条数不能为空")
    private Integer pageSize;

    /** 事件发生时间范围（yyyy-MM-dd 起, yyyy-MM-dd 止）；不传默认当日 */
    @Schema(description = "事件发生时间范围（不传默认今日）", example = "2026-09-12,2026-09-12")
    private List<String> timeRange;

    /** 事件类型：1下单 2取消订单 3转移订单；不传查全部 */
    @Schema(description = "事件类型 1下单 2取消订单 3转移订单")
    private Integer operateType;

    /**
     * 兼容 GET 请求参数绑定，支持将逗号分隔字符串解析为 List
     */
    public void setTimeRange(String timeRangeStr) {
        this.timeRange = TimeRangeUtils.parseTimeRange(timeRangeStr);
    }
}
