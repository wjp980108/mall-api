package com.atguigu.meet.model.dto.goods.consign;

import com.atguigu.meet.utils.TimeRangeUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 委托代卖事件记录分页查询DTO
 * <p>
 * 查询条件均基于快照字段：商品名模糊、委托人ID、记录状态、申请时间范围。
 */
@Data
@Schema(description = "委托代卖记录分页查询参数")
public class ConsignRecordPageQueryDTO {
    @Schema(description = "分页页码", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分页页码不能为空")
    private Integer pageNum;
    
    @Schema(description = "每页条数", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "每页条数不能为空")
    private Integer pageSize;

    /** 主表商品ID（精确查询） */
    @Schema(description = "托售商品ID", example = "1")
    private Long consignGoodsId;
    
    /** 快照商品名称（模糊查询） */
    @Schema(description = "商品名称（模糊查询）", example = "iPhone")
    private String goodsName;
    
    /** 委托人会员ID（精确查询） */
    @Schema(description = "委托人ID", example = "1")
    private Long memberId;
    
    /** 买家会员ID（精确查询，用于查某人买过的委托记录） */
    @Schema(description = "买家ID", example = "2")
    private Long buyerId;
    
    /** 记录状态 1待审核 2审核通过已上架 3已卖出 4未售出下架 5审核驳回 */
    @Schema(description = "记录状态", example = "1", allowableValues = {"1", "2", "3", "4", "5"})
    private Integer recordStatus;
    
    /**
     * 申请时间范围，格式 yyyy-MM-dd
     * <p>数组第一项为开始日期（补 00:00:00），第二项为结束日期（补 23:59:59）</p>
     * <p>GET 请求兼容：支持 JSON 数组字符串或逗号分隔字符串</p>
     */
    @Schema(description = "申请时间范围", example = "[\"2026-08-19\", \"2026-08-19\"]")
    private List<String> timeRange;

    /**
     * 兼容 GET 请求参数绑定，支持将字符串解析为 List
     * <p>解析逻辑见 {@link TimeRangeUtils#parseTimeRange(String)}</p>
     */
    public void setTimeRange(String timeRangeStr) {
        this.timeRange = TimeRangeUtils.parseTimeRange(timeRangeStr);
    }
}