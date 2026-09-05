package com.atguigu.meet.model.dto.goods.consign;

import com.atguigu.meet.utils.TimeRangeUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 抢购托售商品分页查询DTO
 */
@Data
@Schema(description = "托售商品分页查询参数")
public class ConsignGoodsPageQueryDTO {
    @Schema(description = "分页页码", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分页页码不能为空")
    private Integer pageNum;
    
    @Schema(description = "每页条数", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "每页条数不能为空")
    private Integer pageSize;

    /** 商品名称（模糊查询） */
    @Schema(description = "商品名称（模糊查询）", example = "iPhone")
    private String goodsName;
    
    /** 委托人ID（精确查询） */
    @Schema(description = "委托人ID", example = "1")
    private Long memberId;
    
    /** 所属场次ID（精确查询） */
    @Schema(description = "场次ID", example = "1")
    private Long sessionId;
    
    /** 商品业务状态 1-5 1挂卖中 2已抢购待付款 3等待确认付款 4待处理 5委托代卖*/
    @Schema(description = "商品业务状态", example = "1", allowableValues = {"1", "2", "3", "4", "5"})
    private Integer goodsStatus;
    
    /** 委托状态 0未委托 1委托代卖中 */
    @Schema(description = "委托状态", example = "0", allowableValues = {"0", "1"})
    private Integer entrustStatus;
    
    /** 审核状态 0无需审核 1待审核 2审核通过 3审核驳回 */
    @Schema(description = "审核状态", example = "0", allowableValues = {"0", "1", "2", "3"})
    private Integer auditStatus;
    
    /** 上下架状态 0下架 1上架 */
    @Schema(description = "上下架状态", example = "1", allowableValues = {"0", "1"})
    private Integer onlineStatus;
    
    /**
     * 创建时间范围，格式 yyyy-MM-dd
     * <p>数组第一项为开始日期（补 00:00:00），第二项为结束日期（补 23:59:59）</p>
     * <p>GET 请求兼容：支持 JSON 数组字符串（如 ["2026-08-19","2026-08-19"]）或逗号分隔字符串</p>
     */
    @Schema(description = "创建时间范围", example = "[\"2026-08-19\", \"2026-08-19\"]")
    private List<String> timeRange;

    /**
     * 兼容 GET 请求参数绑定，支持将字符串解析为 List
     * <p>解析逻辑见 {@link TimeRangeUtils#parseTimeRange(String)}</p>
     */
    public void setTimeRange(String timeRangeStr) {
        this.timeRange = TimeRangeUtils.parseTimeRange(timeRangeStr);
    }
}