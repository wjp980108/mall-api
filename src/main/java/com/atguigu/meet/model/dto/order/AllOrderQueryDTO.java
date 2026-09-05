package com.atguigu.meet.model.dto.order;

import com.atguigu.meet.utils.TimeRangeUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 订单分页查询DTO（所有列表通用，orderStatus由Controller层内部固定/覆盖）
 */
@Data
@Schema(description = "订单分页查询参数")
public class AllOrderQueryDTO {

    @Schema(description = "分页页码", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分页页码不能为空")
    private Integer pageNum;

    @Schema(description = "每页条数", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "每页条数不能为空")
    private Integer pageSize;

    /** 订单编号（模糊查询） */
    @Schema(description = "订单编号（模糊查询）", example = "ORD20260101")
    private String orderNo;

    /** 商品名称（模糊查询） */
    @Schema(description = "商品名称（模糊查询）", example = "iPhone")
    private String goodsName;

    /** 买家姓名（模糊查询） */
    @Schema(description = "买家姓名（模糊查询）", example = "张三")
    private String buyerName;

    /** 买家手机号（模糊查询） */
    @Schema(description = "买家手机号（模糊查询）", example = "138")
    private String buyerPhone;

    /** 卖家姓名（模糊查询） */
    @Schema(description = "卖家姓名（模糊查询）", example = "李四")
    private String sellerName;

    /** 卖家手机号（模糊查询） */
    @Schema(description = "卖家手机号（模糊查询）", example = "139")
    private String sellerPhone;

    /**
     * 订单状态：1待付款 2已付款 3已确认 4已代售 5已取消
     * 仅 /list/all 接口允许前端传，其他4个列表接口内部固定该值
     */
    @Schema(description = "订单状态：1待付款 2已付款 3已确认 4已代售 5已取消", example = "1", allowableValues = {"1", "2", "3", "4", "5"})
    private Integer orderStatus;

    /**
     * 下单创建时间范围，格式 yyyy-MM-dd
     * <p>数组第一项为开始日期（补 00:00:00），第二项为结束日期（补 23:59:59）</p>
     */
    @Schema(description = "下单时间范围", example = "2026-01-01,2026-01-31")
    private List<String> timeRange;

    /**
     * 兼容 GET 请求参数绑定，支持将字符串解析为 List
     */
    public void setTimeRange(String timeRangeStr) {
        this.timeRange = TimeRangeUtils.parseTimeRange(timeRangeStr);
    }
}