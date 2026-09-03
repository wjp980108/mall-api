package com.atguigu.meet.model.dto.goods.consign;

import com.atguigu.meet.utils.TimeRangeUtils;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 委托代卖事件记录分页查询DTO
 * <p>
 * 查询条件均基于快照字段：商品名模糊、委托人ID、记录状态、申请时间范围。
 */
@Data
public class ConsignRecordPageQueryDTO {
    @NotNull(message = "分页页码不能为空")
    private Integer pageNum;
    @NotNull(message = "每页条数不能为空")
    private Integer pageSize;

    /** 主表商品ID（精确查询） */
    private Long consignGoodsId;
    /** 快照商品名称（模糊查询） */
    private String goodsName;
    /** 委托人会员ID（精确查询） */
    private Long memberId;
    /** 买家会员ID（精确查询，用于查某人买过的委托记录） */
    private Long buyerId;
    /** 记录状态 1待审核 2审核通过已上架 3已卖出 4未售出下架 5审核驳回 */
    private Integer recordStatus;
    /**
     * 申请时间范围，格式 yyyy-MM-dd
     * <p>数组第一项为开始日期（补 00:00:00），第二项为结束日期（补 23:59:59）</p>
     * <p>GET 请求兼容：支持 JSON 数组字符串或逗号分隔字符串</p>
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
