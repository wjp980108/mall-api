package com.atguigu.meet.model.dto.order;

import com.atguigu.meet.utils.TimeRangeUtils;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 订单分页查询DTO（所有列表通用，orderStatus由Controller层内部固定/覆盖）
 */
@Data
public class AllOrderQueryDTO {

    @NotNull(message = "分页页码不能为空")
    private Integer pageNum;

    @NotNull(message = "每页条数不能为空")
    private Integer pageSize;

    /** 订单编号（模糊查询） */
    private String orderNo;

    /** 商品名称（模糊查询） */
    private String goodsName;

    /** 买家姓名（模糊查询） */
    private String buyerName;

    /** 买家手机号（模糊查询） */
    private String buyerPhone;

    /** 卖家姓名（模糊查询） */
    private String sellerName;

    /** 卖家手机号（模糊查询） */
    private String sellerPhone;

    /**
     * 订单状态：1待付款 2已付款 3已确认 4已代售 5已取消
     * 仅 /list/all 接口允许前端传，其他4个列表接口内部固定该值
     */
    private Integer orderStatus;

    /**
     * 下单创建时间范围，格式 yyyy-MM-dd
     * <p>数组第一项为开始日期（补 00:00:00），第二项为结束日期（补 23:59:59）</p>
     */
    private List<String> timeRange;

    /**
     * 兼容 GET 请求参数绑定，支持将字符串解析为 List
     */
    public void setTimeRange(String timeRangeStr) {
        this.timeRange = TimeRangeUtils.parseTimeRange(timeRangeStr);
    }
}
