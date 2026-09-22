package com.atguigu.meet.service.roborder;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.mapper.roborder.RobOrderBillMapper;
import com.atguigu.meet.model.dto.roborder.RobOrderBillPageQueryDTO;
import com.atguigu.meet.model.vo.PageResultVO;
import com.atguigu.meet.model.vo.roborder.RobOrderBillVO;
import com.atguigu.meet.service.roborder.impl.RobOrderBillServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RobOrderBillServiceImpl 单元测试
 * <p>
 * 聚焦账单分页的日期解析、派生字段计算、关键词透传与空查询行为。
 */
@ExtendWith(MockitoExtension.class)
class RobOrderBillServiceImplTest {

    @Mock
    private RobOrderBillMapper robOrderBillMapper;

    @InjectMocks
    private RobOrderBillServiceImpl robOrderBillService;

    private RobOrderBillPageQueryDTO query(int pageNum, int pageSize, String date, String keyword) {
        RobOrderBillPageQueryDTO dto = new RobOrderBillPageQueryDTO();
        dto.setPageNum(pageNum);
        dto.setPageSize(pageSize);
        dto.setDate(date);
        dto.setKeyword(keyword);
        return dto;
    }

    private RobOrderBillVO bill(long userId, String todayPurchase, String todayPayment, String todayReceipt,
                                String todayShare, String yesterdayPurchase, String yesterdayPayment,
                                String yesterdayReceipt) {
        RobOrderBillVO vo = new RobOrderBillVO();
        vo.setUserId(userId);
        vo.setUserName("用户" + userId);
        vo.setPhone("1380000" + userId);
        vo.setTodayPurchaseAmount(new BigDecimal(todayPurchase));
        vo.setTodayPaymentAmount(new BigDecimal(todayPayment));
        vo.setTodayReceiptAmount(new BigDecimal(todayReceipt));
        vo.setTodayShareAmount(new BigDecimal(todayShare));
        vo.setYesterdayPurchaseAmount(new BigDecimal(yesterdayPurchase));
        vo.setYesterdayPaymentAmount(new BigDecimal(yesterdayPayment));
        vo.setYesterdayReceiptAmount(new BigDecimal(yesterdayReceipt));
        return vo;
    }

    @Test
    @SuppressWarnings("unchecked")
    void getBillPage_bothDaysWithData_calculatesDerivedAmounts() {
        RobOrderBillPageQueryDTO dto = query(1, 10, "2026-09-22", null);

        RobOrderBillVO vo = bill(1L, "300.00", "300.00", "380.00", "30.00", "200.00", "200.00", "250.00");
        Page<RobOrderBillVO> page = new Page<>(1, 10);
        page.setRecords(List.of(vo));
        page.setTotal(1L);
        when(robOrderBillMapper.selectBillPage(any(), any(), any(), any(), any(), any())).thenReturn(page);

        Response resp = robOrderBillService.getBillPage(dto);
        assertEquals(200, resp.getCode());

        PageResultVO<RobOrderBillVO> result = (PageResultVO<RobOrderBillVO>) resp.getData();
        assertNotNull(result);
        assertEquals(1, result.getList().size());

        RobOrderBillVO actual = result.getList().get(0);
        // 今日寄售 = 今日回款 - 今日付款 = 380 - 300 = 80
        assertEquals("80.00", actual.getTodayConsignmentAmount().toPlainString());
        // 昨日寄售 = 昨日回款 - 昨日付款 = 250 - 200 = 50
        assertEquals("50.00", actual.getYesterdayConsignmentAmount().toPlainString());
        // 应付款 = 今日付款 - 昨日回款 = 300 - 250 = 50
        assertEquals("50.00", actual.getPayableAmount().toPlainString());

        // 原始指标保持不变
        assertEquals("300.00", actual.getTodayPurchaseAmount().toPlainString());
        assertEquals("30.00", actual.getTodayShareAmount().toPlainString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getBillPage_onlyTodayData_payableEqualsTodayPayment() {
        RobOrderBillPageQueryDTO dto = query(1, 10, "2026-09-22", null);

        // 昨日全部无数据，Mapper 层以 0 兜底
        RobOrderBillVO vo = bill(1L, "300.00", "300.00", "320.00", "30.00", "0.00", "0.00", "0.00");
        Page<RobOrderBillVO> page = new Page<>(1, 10);
        page.setRecords(List.of(vo));
        page.setTotal(1L);
        when(robOrderBillMapper.selectBillPage(any(), any(), any(), any(), any(), any())).thenReturn(page);

        Response resp = robOrderBillService.getBillPage(dto);
        PageResultVO<RobOrderBillVO> result = (PageResultVO<RobOrderBillVO>) resp.getData();
        RobOrderBillVO actual = result.getList().get(0);

        assertEquals("20.00", actual.getTodayConsignmentAmount().toPlainString());
        assertEquals("0.00", actual.getYesterdayConsignmentAmount().toPlainString());
        // 应付款 = 300 - 0 = 300
        assertEquals("300.00", actual.getPayableAmount().toPlainString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getBillPage_onlyYesterdayData_payableEqualsNegativeYesterdayReceipt() {
        RobOrderBillPageQueryDTO dto = query(1, 10, "2026-09-22", null);

        // 今日全部无数据
        RobOrderBillVO vo = bill(1L, "0.00", "0.00", "0.00", "0.00", "200.00", "200.00", "250.00");
        Page<RobOrderBillVO> page = new Page<>(1, 10);
        page.setRecords(List.of(vo));
        page.setTotal(1L);
        when(robOrderBillMapper.selectBillPage(any(), any(), any(), any(), any(), any())).thenReturn(page);

        Response resp = robOrderBillService.getBillPage(dto);
        PageResultVO<RobOrderBillVO> result = (PageResultVO<RobOrderBillVO>) resp.getData();
        RobOrderBillVO actual = result.getList().get(0);

        assertEquals("0.00", actual.getTodayConsignmentAmount().toPlainString());
        assertEquals("50.00", actual.getYesterdayConsignmentAmount().toPlainString());
        // 应付款 = 0 - 250 = -250
        assertEquals("-250.00", actual.getPayableAmount().toPlainString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getBillPage_emptyDate_usesTodayAndReturnsEmptyList() {
        RobOrderBillPageQueryDTO dto = query(1, 10, null, null);

        Page<RobOrderBillVO> emptyPage = new Page<>(1, 10);
        emptyPage.setRecords(List.of());
        emptyPage.setTotal(0L);
        when(robOrderBillMapper.selectBillPage(any(), any(), any(), any(), any(), any())).thenReturn(emptyPage);

        Response resp = robOrderBillService.getBillPage(dto);
        assertEquals(200, resp.getCode());

        PageResultVO<RobOrderBillVO> result = (PageResultVO<RobOrderBillVO>) resp.getData();
        assertNotNull(result);
        assertTrue(result.getList().isEmpty());
        assertEquals(0L, result.getTotal());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getBillPage_withKeyword_passesKeywordToMapper() {
        RobOrderBillPageQueryDTO dto = query(1, 10, "2026-09-22", "138");

        Page<RobOrderBillVO> emptyPage = new Page<>(1, 10);
        emptyPage.setRecords(List.of());
        emptyPage.setTotal(0L);
        when(robOrderBillMapper.selectBillPage(any(), any(), any(), any(), any(), any())).thenReturn(emptyPage);

        robOrderBillService.getBillPage(dto);

        ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
        verify(robOrderBillMapper).selectBillPage(any(), any(), any(), any(), any(), keywordCaptor.capture());
        assertEquals("138", keywordCaptor.getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getBillPage_passesCorrectTimeRange() {
        RobOrderBillPageQueryDTO dto = query(1, 10, "2026-09-22", null);

        Page<RobOrderBillVO> emptyPage = new Page<>(1, 10);
        emptyPage.setRecords(List.of());
        emptyPage.setTotal(0L);
        when(robOrderBillMapper.selectBillPage(any(), any(), any(), any(), any(), any())).thenReturn(emptyPage);

        robOrderBillService.getBillPage(dto);

        ArgumentCaptor<LocalDateTime> todayStartCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> todayEndCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> yesterdayStartCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> yesterdayEndCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(robOrderBillMapper).selectBillPage(any(), todayStartCaptor.capture(), todayEndCaptor.capture(),
                yesterdayStartCaptor.capture(), yesterdayEndCaptor.capture(), any());

        assertEquals(LocalDateTime.of(2026, 9, 22, 0, 0, 0), todayStartCaptor.getValue());
        assertEquals(LocalDateTime.of(2026, 9, 22, 23, 59, 59, 999_000_000), todayEndCaptor.getValue());
        assertEquals(LocalDateTime.of(2026, 9, 21, 0, 0, 0), yesterdayStartCaptor.getValue());
        assertEquals(LocalDateTime.of(2026, 9, 21, 23, 59, 59, 999_000_000), yesterdayEndCaptor.getValue());
    }

    @Test
    void getBillPage_paginationMetadataIsReturned() {
        RobOrderBillPageQueryDTO dto = query(2, 5, "2026-09-22", null);

        RobOrderBillVO vo = bill(1L, "100.00", "100.00", "120.00", "10.00", "0.00", "0.00", "0.00");
        Page<RobOrderBillVO> page = new Page<>(2, 5);
        page.setRecords(List.of(vo));
        page.setTotal(11L);
        page.setPages(3L);
        when(robOrderBillMapper.selectBillPage(any(), any(), any(), any(), any(), any())).thenReturn(page);

        Response resp = robOrderBillService.getBillPage(dto);
        PageResultVO<RobOrderBillVO> result = (PageResultVO<RobOrderBillVO>) resp.getData();

        assertEquals(11L, result.getTotal());
        assertEquals(3L, result.getPages());
        assertEquals(2L, result.getCurrent());
        assertEquals(5L, result.getSize());
    }
}
