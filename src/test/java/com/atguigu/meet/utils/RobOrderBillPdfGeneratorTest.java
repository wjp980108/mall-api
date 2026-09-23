package com.atguigu.meet.utils;

import com.atguigu.meet.model.vo.roborder.RobOrderBillVO;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RobOrderBillPdfGenerator 单元测试。
 * <p>
 * 字体已内嵌到 classpath（思源黑体），跨平台零配置，任何环境均可运行。
 */
class RobOrderBillPdfGeneratorTest {

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

    @Test
    void generate_withData_producesNonEmptyPdf() throws Exception {
        RobOrderBillPdfGenerator generator = new RobOrderBillPdfGenerator("测试进货明细表 - 2026年9月22日");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        generator.generate(List.of(bill(1L, 100, 0), bill(2L, 0, 200)), out);

        byte[] pdfBytes = out.toByteArray();
        assertTrue(pdfBytes.length > 0, "生成的 PDF 不应为空");
        // PDF 文件头魔数
        assertTrue(new String(pdfBytes, 0, 4, java.nio.charset.StandardCharsets.UTF_8).startsWith("%PDF"), "应为有效 PDF 文件");
    }

    @Test
    void generate_emptyData_producesNonEmptyPdf() throws Exception {
        RobOrderBillPdfGenerator generator = new RobOrderBillPdfGenerator("测试进货明细表 - 2026年9月22日");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        generator.generate(List.of(), out);

        byte[] pdfBytes = out.toByteArray();
        assertTrue(pdfBytes.length > 0, "空数据生成的 PDF 仍应包含标题与表头");
        assertTrue(new String(pdfBytes, 0, 4, java.nio.charset.StandardCharsets.UTF_8).startsWith("%PDF"), "应为有效 PDF 文件");
    }
}
