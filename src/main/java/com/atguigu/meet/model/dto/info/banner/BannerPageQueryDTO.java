package com.atguigu.meet.model.dto.info.banner;

import com.atguigu.meet.utils.TimeRangeUtils;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 轮播图分页查询DTO
 */
@Data
public class BannerPageQueryDTO {
    @NotNull(message = "分页页码不能为空")
    private Integer pageNum;
    @NotNull(message = "每页条数不能为空")
    private Integer pageSize;

    /** 轮播位置：home=首页 seckill=抢购 */
    private String position;
    /** 状态 1启用 0禁用 */
    private Integer status;
    /**
     * 创建时间范围，格式 yyyy-MM-dd
     * <p>数组第一项为开始日期（补 00:00:00），第二项为结束日期（补 23:59:59）</p>
     * <p>GET 请求兼容：支持 JSON 数组字符串（如 ["2026-08-19","2026-08-19"]）或逗号分隔字符串</p>
     */
    private List<String> timeRange;

    /**
     * 兼容 GET 请求参数绑定，支持将字符串解析为 List
     * <p>解析逻辑见 {@link TimeRangeUtils#parseTimeRange(String)}</p>
     */
    public void setTimeRange(String timeRangeStr) {
        this.timeRange = TimeRangeUtils.parseTimeRange(timeRangeStr);
    }
}