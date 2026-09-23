package com.atguigu.meet.utils;

import com.atguigu.meet.model.vo.roborder.RobOrderBillVO;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RobOrderBillPdfGenerator 单元测试。
 * <p>
 * 字体已内嵌到 classpath（思源黑体），跨平台零配置，任何环境均可运行。
 * 数值口径（合计行、欠余分边、勾稽关系）通过 package-private 纯计算入口断言，
 * 不引入 PDF 文本解析依赖；渲染层仅断言 PDF 魔数与非空。
 */
class RobOrderBillPdfGeneratorTest {

    private static final String TITLE = "测试进货明细表 - 2026年9月22日";
    private static final String BALANCE_TITLE = "欠余合计表 - 2026年09月22日";

    private RobOrderBillVO bill(long userId, long todayPurchase, long yesterdayPurchase) {
        RobOrderBillVO vo = new RobOrderBillVO();
        vo.setUserId(userId);
        vo.setUserName("用户" + userId);
        vo.setPhone("1380000" + userId);
        vo.setTodayPurchaseAmount(BigDecimal.valueOf(todayPurchase));
        vo.setTodayPaymentAmount(BigDecimal.ZERO);
        vo.setTodayReceiptAmount(BigDecimal.ZERO);
        vo.setTodayShareAmount(BigDecimal.ZERO);
        vo.setYesterdayPurchaseAmount(BigDecimal.valueOf(yesterdayPurchase));
        vo.setYesterdayPaymentAmount(BigDecimal.ZERO);
        vo.setYesterdayReceiptAmount(BigDecimal.ZERO);
        // 派生字段由 Service 层计算，此处手动设置以聚焦 PDF 生成
        vo.setTodayConsignmentAmount(BigDecimal.ZERO);
        vo.setYesterdayConsignmentAmount(BigDecimal.ZERO);
        vo.setPayableAmount(BigDecimal.ZERO);
        return vo;
    }

    /**
     * 构造一个全字段可控的账单行。
     */
    private RobOrderBillVO fullBill(long userId, String name,
                                    long todayPurchase, long todayConsignment, long todayPayment, long todayReceipt,
                                    long yesterdayPurchase, long yesterdayConsignment,
                                    long yesterdayPayment, long yesterdayReceipt,
                                    long share, long payable) {
        RobOrderBillVO vo = new RobOrderBillVO();
        vo.setUserId(userId);
        vo.setUserName(name);
        vo.setPhone("1390000" + userId);
        vo.setTodayPurchaseAmount(BigDecimal.valueOf(todayPurchase));
        vo.setTodayConsignmentAmount(BigDecimal.valueOf(todayConsignment));
        vo.setTodayPaymentAmount(BigDecimal.valueOf(todayPayment));
        vo.setTodayReceiptAmount(BigDecimal.valueOf(todayReceipt));
        vo.setYesterdayPurchaseAmount(BigDecimal.valueOf(yesterdayPurchase));
        vo.setYesterdayConsignmentAmount(BigDecimal.valueOf(yesterdayConsignment));
        vo.setYesterdayPaymentAmount(BigDecimal.valueOf(yesterdayPayment));
        vo.setYesterdayReceiptAmount(BigDecimal.valueOf(yesterdayReceipt));
        vo.setTodayShareAmount(BigDecimal.valueOf(share));
        vo.setPayableAmount(BigDecimal.valueOf(payable));
        return vo;
    }

    @Test
    void generate_withData_producesNonEmptyPdf() throws Exception {
        RobOrderBillPdfGenerator generator = new RobOrderBillPdfGenerator(TITLE, BALANCE_TITLE);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        generator.generate(List.of(bill(1L, 100, 0), bill(2L, 0, 200)), out);

        byte[] pdfBytes = out.toByteArray();
        assertTrue(pdfBytes.length > 0, "生成的 PDF 不应为空");
        // PDF 文件头魔数
        assertTrue(new String(pdfBytes, 0, 4, java.nio.charset.StandardCharsets.UTF_8).startsWith("%PDF"), "应为有效 PDF 文件");
    }

    @Test
    void generate_emptyData_producesNonEmptyPdf() throws Exception {
        RobOrderBillPdfGenerator generator = new RobOrderBillPdfGenerator(TITLE, BALANCE_TITLE);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        generator.generate(List.of(), out);

        byte[] pdfBytes = out.toByteArray();
        assertTrue(pdfBytes.length > 0, "空数据生成的 PDF 仍应包含标题与表头");
        assertTrue(new String(pdfBytes, 0, 4, java.nio.charset.StandardCharsets.UTF_8).startsWith("%PDF"), "应为有效 PDF 文件");
    }

    @Test
    void calculate_mixedPayable_partitionsAndSums() {
        // A 应付款 +300（收+）；B 应付款 -100（付-）；C 应付款 0（不入欠余表）
        RobOrderBillVO a = fullBill(1L, "甲", 1000, 10, 300, 310, 0, 0, 0, 0, 25, 300);
        RobOrderBillVO b = fullBill(2L, "乙", 0, 0, 0, 0, 500, 20, 80, 100, 10, -100);
        RobOrderBillVO c = fullBill(3L, "丙", 200, 0, 200, 200, 0, 0, 0, 200, 5, 0);

        RobOrderBillPdfGenerator.BillTotals totals =
                RobOrderBillPdfGenerator.calculate(List.of(a, b, c));

        // 8 个资金列纵向合计
        assertArrayEquals(new long[]{1200, 10, 500, 510, 500, 20, 80, 300}, totals.columnTotals);
        // 应付款合计 = 300 - 100 + 0 = 200
        assertEquals(200, totals.payableTotal);

        // 分边与组内保序
        assertEquals(List.of("甲"), totals.receivableList.stream().map(RobOrderBillVO::getUserName).toList());
        assertEquals(List.of("乙"), totals.payableList.stream().map(RobOrderBillVO::getUserName).toList());
        assertEquals(300, totals.receivableTotal);
        assertEquals(-100, totals.payableSideTotal);

        // 勾稽：左合计 + 右合计 = 主表应付款合计
        assertEquals(totals.payableTotal, totals.receivableTotal + totals.payableSideTotal);
    }

    @Test
    void calculate_unevenSides_preservesOrderAndPaddingCount() {
        // 左 2（正）右 3（负）：右侧多 1 行，渲染时左侧需补 1 个空白行
        RobOrderBillVO l1 = bill(10L, 0, 0);
        l1.setUserName("左一");
        l1.setPayableAmount(BigDecimal.valueOf(100));
        RobOrderBillVO l2 = bill(11L, 0, 0);
        l2.setUserName("左二");
        l2.setPayableAmount(BigDecimal.valueOf(200));
        RobOrderBillVO r1 = bill(20L, 0, 0);
        r1.setUserName("右一");
        r1.setPayableAmount(BigDecimal.valueOf(-90));
        RobOrderBillVO r2 = bill(21L, 0, 0);
        r2.setUserName("右二");
        r2.setPayableAmount(BigDecimal.valueOf(-90));
        RobOrderBillVO r3 = bill(22L, 0, 0);
        r3.setUserName("右三");
        r3.setPayableAmount(BigDecimal.valueOf(-200));

        RobOrderBillPdfGenerator.BillTotals totals =
                RobOrderBillPdfGenerator.calculate(List.of(l1, r1, l2, r2, r3));

        assertEquals(List.of("左一", "左二"),
                totals.receivableList.stream().map(RobOrderBillVO::getUserName).toList());
        assertEquals(List.of("右一", "右二", "右三"),
                totals.payableList.stream().map(RobOrderBillVO::getUserName).toList());
        assertEquals(300, totals.receivableTotal);
        assertEquals(-380, totals.payableSideTotal);
        assertEquals(-80, totals.payableTotal);
        // 渲染行数取较多一侧，少侧补齐数量 = 右 - 左 = 1
        int renderRows = Math.max(totals.receivableList.size(), totals.payableList.size());
        assertEquals(3, renderRows);
        assertEquals(1, renderRows - totals.receivableList.size());
    }

    @Test
    void calculate_allNegative_leftSideIsZero() {
        RobOrderBillVO a = bill(1L, 0, 0);
        a.setPayableAmount(BigDecimal.valueOf(-100));
        RobOrderBillVO b = bill(2L, 0, 0);
        b.setPayableAmount(BigDecimal.valueOf(-380));

        RobOrderBillPdfGenerator.BillTotals totals =
                RobOrderBillPdfGenerator.calculate(List.of(a, b));

        assertTrue(totals.receivableList.isEmpty());
        assertEquals(0, totals.receivableTotal);
        assertEquals(-480, totals.payableSideTotal);
        assertEquals(-480, totals.payableTotal);
    }

    @Test
    void calculate_emptyRecords_allZero() {
        RobOrderBillPdfGenerator.BillTotals totals = RobOrderBillPdfGenerator.calculate(List.of());

        assertArrayEquals(new long[8], totals.columnTotals);
        assertEquals(0, totals.payableTotal);
        assertTrue(totals.receivableList.isEmpty());
        assertTrue(totals.payableList.isEmpty());
        assertEquals(0, totals.receivableTotal);
        assertEquals(0, totals.payableSideTotal);
    }

    @Test
    void formatPayable_signPrefixRules() {
        assertEquals("+300", RobOrderBillPdfGenerator.formatPayable(new BigDecimal("300")));
        assertEquals("-380", RobOrderBillPdfGenerator.formatPayable(new BigDecimal("-380")));
        assertEquals("0", RobOrderBillPdfGenerator.formatPayable(BigDecimal.ZERO));
        assertEquals("0", RobOrderBillPdfGenerator.formatPayable(null));
    }
}
