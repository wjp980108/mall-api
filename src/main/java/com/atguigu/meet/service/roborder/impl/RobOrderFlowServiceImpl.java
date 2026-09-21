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
import com.atguigu.meet.model.entity.general.settings.SysSettings;
import com.atguigu.meet.model.vo.roborder.RobOrderFlowDetailVO;
import com.atguigu.meet.model.vo.roborder.RobOrderFlowEventVO;
import com.atguigu.meet.model.vo.roborder.RobOrderFlowPageResultVO;
import com.atguigu.meet.model.vo.roborder.RobOrderFlowSummaryVO;
import com.atguigu.meet.model.vo.roborder.RobOrderFlowVO;
import com.atguigu.meet.service.general.settings.SysSettingsService;
import com.atguigu.meet.service.roborder.RobOrderFlowService;
import com.atguigu.meet.utils.BeanConvertUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单流水 Service 实现：事件轴算账读模型，纯只读，不改订单/库存/积分/审计任何数据。
 */
@Service
public class RobOrderFlowServiceImpl implements RobOrderFlowService {

    /** 技术服务费率（0.2%） */
    private static final BigDecimal TECH_SERVICE_FEE_RATE = new BigDecimal("0.2");
    /** 站长服务费率（1.2%） */
    private static final BigDecimal STATION_SERVICE_FEE_RATE = new BigDecimal("1.2");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Autowired
    private RobOrderFlowMapper robOrderFlowMapper;
    @Autowired
    private RobOrderMapper robOrderMapper;
    @Autowired
    private RobOrderOperateLogMapper operateLogMapper;
    @Autowired
    private SysSettingsService sysSettingsService;

    @Override
    public Response getFlowPage(RobOrderFlowPageQueryDTO parameter) {
        LocalDateTime[] range = parseRange(parameter.getTimeRange());

        // 单层拆行行级分页：total/pages 按事件物理行计（转移翻倍），全局事件时间倒序、同刻正向(+)在红冲(-)之上
        Page<RobOrderFlowVO> page = new Page<>(parameter.getPageNum(), parameter.getPageSize());
        IPage<RobOrderFlowVO> flowPage = robOrderFlowMapper.selectFlowPage(
                page, parameter.getOperateType(), range[0], range[1], parameter.getKeyword());
        flowPage.getRecords()
                .forEach(vo -> vo.setEventTypeName(RobOrderOperateType.descOf(vo.getEventType())));

        // 当前筛选条件（含事件类型、keyword）下全部匹配事件的带符号金额合计
        BigDecimal totalAmount = robOrderFlowMapper.selectFlowTotalAmount(
                parameter.getOperateType(), range[0], range[1], parameter.getKeyword());

        RobOrderFlowPageResultVO pageResult = new RobOrderFlowPageResultVO();
        pageResult.setList(flowPage.getRecords());
        // 分页元数据以事件物理行计
        pageResult.setTotal(flowPage.getTotal());
        pageResult.setPages(flowPage.getPages());
        pageResult.setCurrent(flowPage.getCurrent());
        pageResult.setSize(flowPage.getSize());
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
        RobOrderFlowSummaryVO.FlowAmountSummary net = buildNet(income, reversal);
        vo.setNet(net);

        // 顶层总额（净额口径）
        BigDecimal totalReceipt = nz(net.getReceiptRoundAmount());
        BigDecimal totalPayment = nz(net.getPaymentAmount());
        vo.setTotalReceiptAmount(totalReceipt);
        vo.setTotalPaymentAmount(totalPayment);

        // 三项平台服务费：基数 = 净订单总额
        BigDecimal base = nz(net.getTotalAmount());
        SysSettings settings = sysSettingsService.get();
        BigDecimal recommendRate = (settings != null && settings.getRecommendRate() != null)
                ? settings.getRecommendRate() : BigDecimal.ZERO;
        BigDecimal salesAward = percent(base, recommendRate);
        BigDecimal techServiceFee = percent(base, TECH_SERVICE_FEE_RATE);
        BigDecimal stationServiceFee = percent(base, STATION_SERVICE_FEE_RATE);
        vo.setSalesAward(salesAward);
        vo.setTechServiceFee(techServiceFee);
        vo.setStationServiceFee(stationServiceFee);

        // 订单利润差 = (回款总 - 付款总) + 销售奖 + 技术服务费 + 站长服务费
        BigDecimal orderProfitDiff = totalReceipt.subtract(totalPayment)
                .add(salesAward).add(techServiceFee).add(stationServiceFee);
        vo.setOrderProfitDiff(orderProfitDiff);

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
        net.setReceiptRoundAmount(nz(income.getReceiptRoundAmount()).subtract(nz(reversal.getReceiptRoundAmount())));
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

    /** 按百分比计算金额（rate 为百分数，如 0.2 表示 0.2%），保留两位小数四舍五入 */
    private BigDecimal percent(BigDecimal base, BigDecimal rate) {
        if (base == null || rate == null) {
            return BigDecimal.ZERO;
        }
        return base.multiply(rate).divide(HUNDRED, 2, RoundingMode.HALF_UP);
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
