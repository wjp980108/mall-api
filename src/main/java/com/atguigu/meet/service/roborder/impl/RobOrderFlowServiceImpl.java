package com.atguigu.meet.service.roborder.impl;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.enums.RobOrderOperateType;
import com.atguigu.meet.enums.RobOrderStatus;
import com.atguigu.meet.mapper.roborder.RobOrderFlowMapper;
import com.atguigu.meet.mapper.roborder.RobOrderMapper;
import com.atguigu.meet.mapper.roborder.RobOrderOperateLogMapper;
import com.atguigu.meet.model.dto.roborder.RobOrderFlowPageQueryDTO;
import com.atguigu.meet.model.entity.roborder.RobOrder;
import com.atguigu.meet.model.entity.roborder.RobOrderOperateLog;
import com.atguigu.meet.model.vo.roborder.RobOrderFlowDetailVO;
import com.atguigu.meet.model.vo.roborder.RobOrderFlowEventVO;
import com.atguigu.meet.model.vo.roborder.RobOrderFlowGroupVO;
import com.atguigu.meet.model.vo.roborder.RobOrderFlowPageResultVO;
import com.atguigu.meet.model.vo.roborder.RobOrderFlowSummaryVO;
import com.atguigu.meet.model.vo.roborder.RobOrderFlowVO;
import com.atguigu.meet.service.roborder.RobOrderFlowService;
import com.atguigu.meet.utils.BeanConvertUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单流水 Service 实现：事件轴算账读模型，纯只读，不改订单/库存/积分/审计任何数据。
 */
@Service
public class RobOrderFlowServiceImpl implements RobOrderFlowService {

    @Autowired
    private RobOrderFlowMapper robOrderFlowMapper;
    @Autowired
    private RobOrderMapper robOrderMapper;
    @Autowired
    private RobOrderOperateLogMapper operateLogMapper;

    @Override
    public Response getFlowPage(RobOrderFlowPageQueryDTO parameter) {
        LocalDateTime[] range = parseRange(parameter.getTimeRange());

        // 第 1 层：订单组分页（页大小=订单组数，total=组总数，转移拆行不翻倍）
        Page<RobOrderFlowGroupVO> groupPage = new Page<>(parameter.getPageNum(), parameter.getPageSize());
        IPage<RobOrderFlowGroupVO> groups = robOrderFlowMapper.selectFlowOrderGroups(groupPage,
                parameter.getOperateType(), range[0], range[1]);

        // 第 2 层：按组序整组取事件行并拼装平铺 list（同组连续、组间顺序锁定为第 1 层顺序，组内 SQL 已保正序）
        List<RobOrderFlowVO> flatRows = new ArrayList<>();
        if (!groups.getRecords().isEmpty()) {
            List<Long> orderIds = groups.getRecords().stream()
                    .map(RobOrderFlowGroupVO::getOrderId)
                    .toList();
            List<RobOrderFlowVO> rows = robOrderFlowMapper.selectFlowRowsByOrderIds(
                    orderIds, parameter.getOperateType(), range[0], range[1]);
            Map<Long, List<RobOrderFlowVO>> rowsByOrder = rows.stream()
                    .collect(Collectors.groupingBy(RobOrderFlowVO::getOrderId));
            for (Long orderId : orderIds) {
                flatRows.addAll(rowsByOrder.getOrDefault(orderId, List.of()));
            }
        }
        flatRows.forEach(vo -> vo.setEventTypeName(RobOrderOperateType.descOf(vo.getEventType())));

        // 当前筛选条件（含事件类型）下全部匹配事件的带符号金额合计
        BigDecimal totalAmount = robOrderFlowMapper.selectFlowTotalAmount(
                parameter.getOperateType(), range[0], range[1]);

        RobOrderFlowPageResultVO pageResult = new RobOrderFlowPageResultVO();
        pageResult.setList(flatRows);
        // 分页元数据以订单组计（非拆行行数）
        pageResult.setTotal(groups.getTotal());
        pageResult.setPages(groups.getPages());
        pageResult.setCurrent(groups.getCurrent());
        pageResult.setSize(groups.getSize());
        pageResult.setTotalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO);
        return Response.ok(pageResult);
    }

    @Override
    public Response getFlowSummary(List<String> timeRange) {
        LocalDateTime[] range = parseRange(timeRange);
        RobOrderFlowSummaryVO vo = robOrderFlowMapper.selectFlowSummary(range[0], range[1]);
        if (vo == null) {
            vo = new RobOrderFlowSummaryVO();
        }
        RobOrderFlowSummaryVO.FlowAmountSummary income =
                vo.getIncome() != null ? vo.getIncome() : new RobOrderFlowSummaryVO.FlowAmountSummary();
        RobOrderFlowSummaryVO.FlowAmountSummary reversal =
                vo.getReversal() != null ? vo.getReversal() : new RobOrderFlowSummaryVO.FlowAmountSummary();
        vo.setIncome(income);
        vo.setReversal(reversal);
        vo.setNet(buildNet(income, reversal));
        return Response.ok(vo);
    }

    @Override
    public Response getFlowDetail(Long orderId) {
        RobOrder order = robOrderMapper.selectById(orderId);
        if (order == null) {
            return Response.fail(500, "订单不存在");
        }
        RobOrderFlowDetailVO detail = new RobOrderFlowDetailVO();
        BeanConvertUtils.copyProperties(order, detail);
        detail.setOrderStatusName(RobOrderStatus.descOf(order.getOrderStatus()));

        List<RobOrderOperateLog> logs = operateLogMapper.selectList(
                new LambdaQueryWrapper<RobOrderOperateLog>()
                        .eq(RobOrderOperateLog::getOrderId, orderId)
                        .orderByAsc(RobOrderOperateLog::getCreateTime)
                        .orderByAsc(RobOrderOperateLog::getId));
        detail.setEvents(logs.stream().map(this::toEventVO).toList());
        return Response.ok(detail);
    }

    // ====================== 私有方法 ======================

    private RobOrderFlowEventVO toEventVO(RobOrderOperateLog log) {
        RobOrderFlowEventVO event = new RobOrderFlowEventVO();
        event.setLogId(log.getId());
        event.setEventType(log.getOperateType());
        event.setEventTypeName(RobOrderOperateType.descOf(log.getOperateType()));
        event.setBeforeStatus(log.getBeforeStatus());
        event.setBeforeStatusName(RobOrderStatus.descOf(log.getBeforeStatus()));
        event.setAfterStatus(log.getAfterStatus());
        event.setAfterStatusName(RobOrderStatus.descOf(log.getAfterStatus()));
        event.setOperatorId(log.getOperateUserId());
        event.setOperatorName(log.getOperateUserName());
        event.setRemark(log.getRemark());
        event.setEventTime(log.getCreateTime());
        return event;
    }

    /**
     * 净额 = 成交 - 红冲（逐字段 BigDecimal 相减，空值兜底 0）
     */
    private RobOrderFlowSummaryVO.FlowAmountSummary buildNet(
            RobOrderFlowSummaryVO.FlowAmountSummary income,
            RobOrderFlowSummaryVO.FlowAmountSummary reversal) {
        RobOrderFlowSummaryVO.FlowAmountSummary net = new RobOrderFlowSummaryVO.FlowAmountSummary();
        net.setCount(nz(income.getCount()) - nz(reversal.getCount()));
        net.setQuantity(nz(income.getQuantity()) - nz(reversal.getQuantity()));
        net.setTotalAmount(nz(income.getTotalAmount()).subtract(nz(reversal.getTotalAmount())));
        net.setProfitAmount(nz(income.getProfitAmount()).subtract(nz(reversal.getProfitAmount())));
        net.setRecommendAmount(nz(income.getRecommendAmount()).subtract(nz(reversal.getRecommendAmount())));
        net.setSelfBuyAmount(nz(income.getSelfBuyAmount()).subtract(nz(reversal.getSelfBuyAmount())));
        net.setSelfBuyBonusAmount(nz(income.getSelfBuyBonusAmount()).subtract(nz(reversal.getSelfBuyBonusAmount())));
        net.setSelfBuyCouponAmount(nz(income.getSelfBuyCouponAmount()).subtract(nz(reversal.getSelfBuyCouponAmount())));
        net.setReceiptAmount(nz(income.getReceiptAmount()).subtract(nz(reversal.getReceiptAmount())));
        net.setPaymentAmount(nz(income.getPaymentAmount()).subtract(nz(reversal.getPaymentAmount())));
        return net;
    }

    private long nz(Long v) {
        return v == null ? 0L : v;
    }

    private int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * 解析事件时间区间：
     * 空 → 不限时间（查全部）；单日期 → 起止同为该日；多日期 → 起首日 00:00:00 至止日 23:59:59。
     */
    private LocalDateTime[] parseRange(List<String> timeRange) {
        if (timeRange == null || timeRange.isEmpty()) {
            return new LocalDateTime[]{null, null};
        }
        String startDate = timeRange.get(0);
        String endDate = timeRange.size() > 1 ? timeRange.get(timeRange.size() - 1) : startDate;
        LocalDateTime start = startDate != null ? LocalDate.parse(startDate).atStartOfDay() : null;
        LocalDateTime end = endDate != null ? LocalDate.parse(endDate).atTime(23, 59, 59) : null;
        return new LocalDateTime[]{start, end};
    }
}
