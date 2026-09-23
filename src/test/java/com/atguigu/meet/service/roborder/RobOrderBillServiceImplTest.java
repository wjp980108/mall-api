package com.atguigu.meet.service.roborder;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.mapper.roborder.RobOrderBillMapper;
import com.atguigu.meet.model.dto.roborder.RobOrderBillExportDTO;
import com.atguigu.meet.model.dto.roborder.RobOrderBillPageQueryDTO;
import com.atguigu.meet.model.entity.general.settings.SysSettings;
import com.atguigu.meet.model.vo.PageResultVO;
import com.atguigu.meet.model.vo.roborder.RobOrderBillVO;
import com.atguigu.meet.service.general.settings.SysSettingsService;
import com.atguigu.meet.service.roborder.impl.RobOrderBillServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Mock
    private SysSettingsService sysSettingsService;

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

        RobOrderBillVO vo = bill(1L, "300", "300", "380.00", "30", "200.00", "200.00", "250.00");
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
        assertEquals("80", actual.getTodayConsignmentAmount().toPlainString());
        // 昨日寄售 = 昨日回款 - 昨日付款 = 250 - 200 = 50
        assertEquals("50", actual.getYesterdayConsignmentAmount().toPlainString());
        // 应付款 = 今日付款 - 昨日回款 = 300 - 250 = 50
        assertEquals("50", actual.getPayableAmount().toPlainString());

        // 原始指标保持不变
        assertEquals("300", actual.getTodayPurchaseAmount().toPlainString());
        assertEquals("30", actual.getTodayShareAmount().toPlainString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getBillPage_onlyTodayData_payableEqualsTodayPayment() {
        RobOrderBillPageQueryDTO dto = query(1, 10, "2026-09-22", null);

        // 昨日全部无数据，Mapper 层以 0 兜底
        RobOrderBillVO vo = bill(1L, "300", "300", "320.00", "30", "0", "0", "0");
        Page<RobOrderBillVO> page = new Page<>(1, 10);
        page.setRecords(List.of(vo));
        page.setTotal(1L);
        when(robOrderBillMapper.selectBillPage(any(), any(), any(), any(), any(), any())).thenReturn(page);

        Response resp = robOrderBillService.getBillPage(dto);
        PageResultVO<RobOrderBillVO> result = (PageResultVO<RobOrderBillVO>) resp.getData();
        RobOrderBillVO actual = result.getList().get(0);

        assertEquals("20", actual.getTodayConsignmentAmount().toPlainString());
        assertEquals("0", actual.getYesterdayConsignmentAmount().toPlainString());
        // 应付款 = 300 - 0 = 300
        assertEquals("300", actual.getPayableAmount().toPlainString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getBillPage_onlyYesterdayData_payableEqualsNegativeYesterdayReceipt() {
        RobOrderBillPageQueryDTO dto = query(1, 10, "2026-09-22", null);

        // 今日全部无数据
        RobOrderBillVO vo = bill(1L, "0", "0", "0", "0", "200.00", "200.00", "250.00");
        Page<RobOrderBillVO> page = new Page<>(1, 10);
        page.setRecords(List.of(vo));
        page.setTotal(1L);
        when(robOrderBillMapper.selectBillPage(any(), any(), any(), any(), any(), any())).thenReturn(page);

        Response resp = robOrderBillService.getBillPage(dto);
        PageResultVO<RobOrderBillVO> result = (PageResultVO<RobOrderBillVO>) resp.getData();
        RobOrderBillVO actual = result.getList().get(0);

        assertEquals("0", actual.getTodayConsignmentAmount().toPlainString());
        assertEquals("50", actual.getYesterdayConsignmentAmount().toPlainString());
        // 应付款 = 0 - 250 = -250
        assertEquals("-250", actual.getPayableAmount().toPlainString());
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

        RobOrderBillVO vo = bill(1L, "100.00", "100.00", "120.00", "10.00", "0", "0", "0");
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

    @Test
    @SuppressWarnings("unchecked")
    void getBillPage_amountsAreRoundedHalfUp() {
        RobOrderBillPageQueryDTO dto = query(1, 10, "2026-09-22", null);

        // 原始字段含小数，验证四舍五入后计算派生字段
        RobOrderBillVO vo = bill(1L, "1234.56", "100.40", "200.50", "99.99", "50.49", "20.00", "80.51");
        Page<RobOrderBillVO> page = new Page<>(1, 10);
        page.setRecords(List.of(vo));
        page.setTotal(1L);
        when(robOrderBillMapper.selectBillPage(any(), any(), any(), any(), any(), any())).thenReturn(page);

        Response resp = robOrderBillService.getBillPage(dto);
        PageResultVO<RobOrderBillVO> result = (PageResultVO<RobOrderBillVO>) resp.getData();
        RobOrderBillVO actual = result.getList().get(0);

        // 原始字段四舍五入：.56 -> 1235, .40 -> 100, .50 -> 201, .99 -> 100, .49 -> 50, .51 -> 81
        assertEquals("1235", actual.getTodayPurchaseAmount().toPlainString());
        assertEquals("100", actual.getTodayPaymentAmount().toPlainString());
        assertEquals("201", actual.getTodayReceiptAmount().toPlainString());
        assertEquals("100", actual.getTodayShareAmount().toPlainString());
        assertEquals("50", actual.getYesterdayPurchaseAmount().toPlainString());
        assertEquals("20", actual.getYesterdayPaymentAmount().toPlainString());
        assertEquals("81", actual.getYesterdayReceiptAmount().toPlainString());

        // 派生字段基于取整后的原始字段计算
        // 今日寄售 = 201 - 100 = 101
        assertEquals("101", actual.getTodayConsignmentAmount().toPlainString());
        // 昨日寄售 = 81 - 20 = 61
        assertEquals("61", actual.getYesterdayConsignmentAmount().toPlainString());
        // 应付款 = 100 - 81 = 19
        assertEquals("19", actual.getPayableAmount().toPlainString());
    }

    @Test
    void exportRobOrderBillsPdf_withSiteName_setsCorrectFilename() throws Exception {
        RobOrderBillExportDTO dto = new RobOrderBillExportDTO();
        dto.setDate("2026-09-22");
        dto.setKeyword(null);

        SysSettings settings = new SysSettings();
        settings.setSiteName("五谷丰登商贸55酒水");
        when(sysSettingsService.get()).thenReturn(settings);
        when(robOrderBillMapper.selectBillList(any(), any(), any(), any(), any())).thenReturn(List.of());

        MockHttpServletResponse response = new MockHttpServletResponse();
        robOrderBillService.exportRobOrderBillsPdf(dto, response);

        assertTrue(response.getContentType().startsWith("application/pdf"), "Content-Type 应为 application/pdf");
        String filename = extractFilename(response.getHeader("Content-Disposition"));
        assertEquals("五谷丰登商贸55酒水进货明细表_2026-09-22.pdf", filename);
        assertTrue(response.getContentAsByteArray().length > 0, "应写出 PDF 内容");
    }

    @Test
    void exportRobOrderBillsPdf_withoutSiteName_setsDefaultFilename() throws Exception {
        RobOrderBillExportDTO dto = new RobOrderBillExportDTO();
        dto.setDate("2026-09-22");

        SysSettings settings = new SysSettings();
        settings.setSiteName(null);
        when(sysSettingsService.get()).thenReturn(settings);
        when(robOrderBillMapper.selectBillList(any(), any(), any(), any(), any())).thenReturn(List.of());

        MockHttpServletResponse response = new MockHttpServletResponse();
        robOrderBillService.exportRobOrderBillsPdf(dto, response);

        String filename = extractFilename(response.getHeader("Content-Disposition"));
        assertEquals("进货明细表_2026-09-22.pdf", filename, "站点名为空时文件名应省略站点名");
        assertTrue(!filename.contains("null"), "文件名中不应出现 null");
    }

    @Test
    void buildBalanceTitle_zeroPadsMonthAndDay() {
        // 欠余表标题日期零填充（与主表标题的非零填充格式刻意不同）
        String title = ReflectionTestUtils.invokeMethod(
                robOrderBillService, "buildBalanceTitle", LocalDate.of(2026, 9, 5));
        assertEquals("欠余合计表 - 2026年09月05日", title);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getBillPage_cancelSameDay_netAmountsAreZero() {
        RobOrderBillPageQueryDTO dto = query(1, 10, "2026-09-22", null);

        // 当日创建并当日取消：买入/分享/付款/回款净额均为 0
        RobOrderBillVO vo = bill(1L, "0", "0", "0", "0", "0", "0", "0");
        Page<RobOrderBillVO> page = new Page<>(1, 10);
        page.setRecords(List.of(vo));
        page.setTotal(1L);
        when(robOrderBillMapper.selectBillPage(any(), any(), any(), any(), any(), any())).thenReturn(page);

        Response resp = robOrderBillService.getBillPage(dto);
        PageResultVO<RobOrderBillVO> result = (PageResultVO<RobOrderBillVO>) resp.getData();
        RobOrderBillVO actual = result.getList().get(0);

        assertEquals("0", actual.getTodayConsignmentAmount().toPlainString());
        assertEquals("0", actual.getYesterdayConsignmentAmount().toPlainString());
        assertEquals("0", actual.getPayableAmount().toPlainString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getBillPage_cancelCrossDay_belongsToYesterday() {
        RobOrderBillPageQueryDTO dto = query(1, 10, "2026-09-22", null);

        // 昨日创建并昨日取消：昨日买入/付款/回款净额均为 0；今日无数据
        RobOrderBillVO vo = bill(1L, "0", "0", "0", "0", "0", "0", "0");
        Page<RobOrderBillVO> page = new Page<>(1, 10);
        page.setRecords(List.of(vo));
        page.setTotal(1L);
        when(robOrderBillMapper.selectBillPage(any(), any(), any(), any(), any(), any())).thenReturn(page);

        Response resp = robOrderBillService.getBillPage(dto);
        PageResultVO<RobOrderBillVO> result = (PageResultVO<RobOrderBillVO>) resp.getData();
        RobOrderBillVO actual = result.getList().get(0);

        // 跨日取消不影响今日指标，应付款 = 今日净付款 0 - 昨日净回款 0 = 0
        assertEquals("0", actual.getTodayConsignmentAmount().toPlainString());
        assertEquals("0", actual.getYesterdayConsignmentAmount().toPlainString());
        assertEquals("0", actual.getPayableAmount().toPlainString());
    }

    @Test
    @SuppressWarnings("unchecked")
    void getBillPage_transferSplit_originalAndNewBuyer() {
        RobOrderBillPageQueryDTO dto = query(1, 10, "2026-09-22", null);

        // 转移拆分：原买家 A 今日买入 -100，新买家 B 今日买入 +100
        RobOrderBillVO originalBuyer = bill(1L, "-100", "-80", "-100", "-10", "0", "0", "0");
        originalBuyer.setUserName("原买家");
        RobOrderBillVO newBuyer = bill(2L, "100", "80", "100", "10", "0", "0", "0");
        newBuyer.setUserName("新买家");

        Page<RobOrderBillVO> page = new Page<>(1, 10);
        page.setRecords(List.of(originalBuyer, newBuyer));
        page.setTotal(2L);
        when(robOrderBillMapper.selectBillPage(any(), any(), any(), any(), any(), any())).thenReturn(page);

        Response resp = robOrderBillService.getBillPage(dto);
        PageResultVO<RobOrderBillVO> result = (PageResultVO<RobOrderBillVO>) resp.getData();
        assertEquals(2, result.getList().size());

        RobOrderBillVO original = result.getList().get(0);
        RobOrderBillVO transferred = result.getList().get(1);

        // 原买家红冲为负
        assertEquals("-100", original.getTodayPurchaseAmount().toPlainString());
        assertEquals("-80", original.getTodayPaymentAmount().toPlainString());
        assertEquals("-100", original.getTodayReceiptAmount().toPlainString());
        assertEquals("-10", original.getTodayShareAmount().toPlainString());
        // 新买家正向为正
        assertEquals("100", transferred.getTodayPurchaseAmount().toPlainString());
        assertEquals("80", transferred.getTodayPaymentAmount().toPlainString());
        assertEquals("100", transferred.getTodayReceiptAmount().toPlainString());
        assertEquals("10", transferred.getTodayShareAmount().toPlainString());
    }

    private String extractFilename(String contentDisposition) {
        String prefix = "filename*=UTF-8''";
        int idx = contentDisposition.indexOf(prefix);
        if (idx < 0) {
            throw new IllegalArgumentException("Content-Disposition 中未找到 filename*=UTF-8''：" + contentDisposition);
        }
        String encoded = contentDisposition.substring(idx + prefix.length());
        return java.net.URLDecoder.decode(encoded, java.nio.charset.StandardCharsets.UTF_8);
    }
}
