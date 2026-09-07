package com.atguigu.meet.model.dto.roborder;

import com.atguigu.meet.utils.TimeRangeUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 抢购订单分页查询 DTO（管理端单列表）
 */
@Data
@Schema(description = "抢购订单分页查询参数")
public class RobOrderPageQueryDTO {

    @Schema(description = "分页页码", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分页页码不能为空")
    private Integer pageNum;

    @Schema(description = "每页条数", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "每页条数不能为空")
    private Integer pageSize;

    /** 下单时间范围（yyyy-MM-dd 起, yyyy-MM-dd 止） */
    @Schema(description = "下单时间范围", example = "2026-09-01,2026-09-07")
    private List<String> timeRange;

    /** 所属场次ID（下拉筛选） */
    @Schema(description = "所属场次ID")
    private Long sessionId;

    /** 关键词（买家姓名/手机号/用户ID/商品名称 模糊匹配） */
    @Schema(description = "搜索关键词（姓名、手机号、用户ID或商品名称）")
    private String keyword;

    /** 金额模糊匹配（订单总额） */
    @Schema(description = "金额模糊匹配")
    private BigDecimal amount;

    /** 订单状态：1正常 2已取消；不传查全部 */
    @Schema(description = "订单状态 1正常 2已取消")
    private Integer orderStatus;

    /**
     * 兼容 GET 请求参数绑定，支持将逗号分隔字符串解析为 List
     */
    public void setTimeRange(String timeRangeStr) {
        this.timeRange = TimeRangeUtils.parseTimeRange(timeRangeStr);
    }
}
