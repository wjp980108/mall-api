package com.atguigu.meet.model.dto.goods.list;

import com.atguigu.meet.utils.TimeRangeUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 商品分页查询DTO
 */
@Data
@Schema(description = "商品分页查询参数")
public class GoodsPageQueryDTO {
    @Schema(description = "分页页码", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分页页码不能为空")
    private Integer pageNum;
    
    @Schema(description = "每页条数", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "每页条数不能为空")
    private Integer pageSize;

    /** 商品名称（模糊查询） */
    @Schema(description = "商品名称（模糊查询）", example = "iPhone")
    private String goodsName;
    
    /** 商品种类名称（模糊查询） */
    @Schema(description = "商品种类名称（模糊查询）", example = "手机")
    private String categoryName;
    
    /** 商品货号（精确查询） */
    @Schema(description = "商品货号", example = "IP15PRO001")
    private String goodsSn;
    
    /** 商品状态 0=下架 1=已上架 */
    @Schema(description = "商品状态", example = "1", allowableValues = {"0", "1"})
    private Integer status;
    
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