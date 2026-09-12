package com.atguigu.meet.service.roborder;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.roborder.RobOrderFlowPageQueryDTO;

import java.util.List;

/**
 * 订单流水 Service（管理端算账读模型，纯只读）
 */
public interface RobOrderFlowService {

    /**
     * 事件流水分页：按事件发生时间区间（不传默认今日）+ 可选事件类型
     */
    Response getFlowPage(RobOrderFlowPageQueryDTO parameter);

    /**
     * 区间汇总：成交/红冲/净额（笔数、件数、六个金额口径），空区间全部为 0
     */
    Response getFlowSummary(List<String> timeRange);

    /**
     * 单笔订单流水详情：订单下单全快照 + 事件时间线（正序）
     */
    Response getFlowDetail(Long orderId);
}
