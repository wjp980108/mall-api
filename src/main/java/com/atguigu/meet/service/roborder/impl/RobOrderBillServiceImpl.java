package com.atguigu.meet.service.roborder.impl;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.mapper.roborder.RobOrderBillMapper;
import com.atguigu.meet.model.dto.roborder.RobOrderBillPageQueryDTO;
import com.atguigu.meet.model.vo.PageResultVO;
import com.atguigu.meet.model.vo.roborder.RobOrderBillVO;
import com.atguigu.meet.service.roborder.RobOrderBillService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 抢购订单用户账单 Service 实现（管理端算账读模型，纯只读）
 * <p>
 * 在 Mapper 层一次聚合两日指标，Service 层仅做日期解析、派生字段计算与分页包装。
 */
@Service
public class RobOrderBillServiceImpl implements RobOrderBillService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Autowired
    private RobOrderBillMapper robOrderBillMapper;

    @Override
    public Response getBillPage(RobOrderBillPageQueryDTO parameter) {
        LocalDate selectedDate = parseSelectedDate(parameter.getDate());
        LocalDate previousDate = selectedDate.minusDays(1);

        LocalDateTime todayStart = selectedDate.atStartOfDay();
        LocalDateTime todayEnd = selectedDate.atTime(LocalTime.of(23, 59, 59, 999_000_000));
        LocalDateTime yesterdayStart = previousDate.atStartOfDay();
        LocalDateTime yesterdayEnd = previousDate.atTime(LocalTime.of(23, 59, 59, 999_000_000));

        Page<RobOrderBillVO> page = new Page<>(parameter.getPageNum(), parameter.getPageSize());
        IPage<RobOrderBillVO> billPage = robOrderBillMapper.selectBillPage(
                page, todayStart, todayEnd, yesterdayStart, yesterdayEnd, parameter.getKeyword());

        List<RobOrderBillVO> records = billPage.getRecords();
        records.forEach(this::calculateDerivedAmounts);

        return Response.ok(PageResultVO.of(billPage));
    }

    /**
     * 解析选中日期，空字符串或 null 时取当天。
     */
    private LocalDate parseSelectedDate(String dateStr) {
        if (!StringUtils.hasText(dateStr)) {
            return LocalDate.now();
        }
        return LocalDate.parse(dateStr, DATE_FORMATTER);
    }

    /**
     * 计算派生金额字段：今日/昨日寄售金额、应付款。
     */
    private void calculateDerivedAmounts(RobOrderBillVO vo) {
        BigDecimal todayPayment = nz(vo.getTodayPaymentAmount());
        BigDecimal todayReceipt = nz(vo.getTodayReceiptAmount());
        BigDecimal yesterdayPayment = nz(vo.getYesterdayPaymentAmount());
        BigDecimal yesterdayReceipt = nz(vo.getYesterdayReceiptAmount());

        vo.setTodayConsignmentAmount(todayReceipt.subtract(todayPayment));
        vo.setYesterdayConsignmentAmount(yesterdayReceipt.subtract(yesterdayPayment));
        vo.setPayableAmount(todayPayment.subtract(yesterdayReceipt));
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
