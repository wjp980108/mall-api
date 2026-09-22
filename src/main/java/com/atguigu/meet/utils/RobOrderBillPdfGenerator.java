package com.atguigu.meet.utils;

import com.atguigu.meet.exception.BusinessException;
import com.atguigu.meet.model.vo.roborder.RobOrderBillVO;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.List;

/**
 * 抢购订单用户账单 PDF 生成器（基于 OpenPDF）。
 * <p>
 * A4 横向、红色标题栏、蓝色表头、网格线；支持金额整数展示与特定条件标红。
 */
public class RobOrderBillPdfGenerator {

    private static final String[] HEADERS = {
            "序号", "姓名", "电话",
            "今日买入", "今日寄售", "今日付款", "今日回款",
            "昨日买入", "昨日寄售", "昨日付款", "昨日回款",
            "今日分享", "应付款"
    };

    private static final float[] COLUMN_WIDTHS = {
            0.8f, 1.2f, 1.8f,
            1.0f, 1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 1.0f, 1.0f,
            1.0f, 1.0f
    };

    private static final Color TITLE_BG = Color.RED;
    private static final Color TITLE_TEXT = Color.WHITE;
    private static final Color HEADER_BG = new Color(70, 130, 180);
    private static final Color HEADER_TEXT = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(200, 200, 200);
    private static final Color RED_TEXT = Color.RED;

    private static final float FONT_SIZE_TITLE = 16f;
    private static final float FONT_SIZE_HEADER = 9f;
    private static final float FONT_SIZE_BODY = 9f;

    private final String fontPath;
    private final String title;

    /**
     * @param fontPath 中文字体 TTF 文件路径
     * @param title    PDF 标题
     */
    public RobOrderBillPdfGenerator(String fontPath, String title) {
        this.fontPath = fontPath;
        this.title = title;
    }

    /**
     * 生成 PDF 并写入输出流。
     *
     * @param records 账单数据（已归一化）
     * @param out     目标输出流
     */
    public void generate(List<RobOrderBillVO> records, OutputStream out) throws IOException {
        Document document = new Document(PageSize.A4.rotate(), 20, 20, 20, 20);
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            BaseFont baseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            Font titleFont = new Font(baseFont, FONT_SIZE_TITLE, Font.BOLD, TITLE_TEXT);
            Font headerFont = new Font(baseFont, FONT_SIZE_HEADER, Font.BOLD, HEADER_TEXT);
            Font bodyFont = new Font(baseFont, FONT_SIZE_BODY, Font.NORMAL, Color.BLACK);
            Font redBodyFont = new Font(baseFont, FONT_SIZE_BODY, Font.NORMAL, RED_TEXT);

            addTitle(document, titleFont);
            addTable(document, records, baseFont, headerFont, bodyFont, redBodyFont);

            document.close();
        } catch (DocumentException e) {
            throw new BusinessException("生成账单 PDF 失败：" + e.getMessage());
        }
    }

    private void addTitle(Document document, Font titleFont) throws DocumentException {
        PdfPTable titleTable = new PdfPTable(1);
        titleTable.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell(new Phrase(title, titleFont));
        cell.setBackgroundColor(TITLE_BG);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(10);
        cell.setBorder(Rectangle.NO_BORDER);
        titleTable.addCell(cell);

        document.add(titleTable);
        // 标题与表格之间留空
        document.add(new Paragraph(" ", new Font(titleFont.getBaseFont(), 6)));
    }

    private void addTable(Document document, List<RobOrderBillVO> records,
                          BaseFont baseFont, Font headerFont, Font bodyFont, Font redBodyFont) throws DocumentException {
        PdfPTable table = new PdfPTable(HEADERS.length);
        table.setWidthPercentage(100);
        table.setWidths(COLUMN_WIDTHS);
        table.setSpacingBefore(5);
        table.setSpacingAfter(5);

        // 表头
        for (String header : HEADERS) {
            PdfPCell cell = createCell(header, headerFont, Element.ALIGN_CENTER, HEADER_BG);
            cell.setBorderColor(BORDER_COLOR);
            table.addCell(cell);
        }

        // 数据行
        int index = 1;
        for (RobOrderBillVO vo : records) {
            boolean red = isRed(vo);

            table.addCell(createDataCell(String.valueOf(index++), bodyFont));
            table.addCell(createDataCell(vo.getUserName(), red ? redBodyFont : bodyFont));
            table.addCell(createDataCell(vo.getPhone(), red ? redBodyFont : bodyFont));

            table.addCell(createDataCell(formatAmount(vo.getTodayPurchaseAmount()), bodyFont));
            table.addCell(createDataCell(formatAmount(vo.getTodayConsignmentAmount()), bodyFont));
            table.addCell(createDataCell(formatAmount(vo.getTodayPaymentAmount()), bodyFont));
            table.addCell(createDataCell(formatAmount(vo.getTodayReceiptAmount()), bodyFont));
            table.addCell(createDataCell(formatAmount(vo.getYesterdayPurchaseAmount()), bodyFont));
            table.addCell(createDataCell(formatAmount(vo.getYesterdayConsignmentAmount()), bodyFont));
            table.addCell(createDataCell(formatAmount(vo.getYesterdayPaymentAmount()), bodyFont));
            table.addCell(createDataCell(formatAmount(vo.getYesterdayReceiptAmount()), bodyFont));
            table.addCell(createDataCell(formatAmount(vo.getTodayShareAmount()), bodyFont));
            table.addCell(createDataCell(formatAmount(vo.getPayableAmount()), bodyFont));
        }

        document.add(table);
    }

    private PdfPCell createCell(String text, Font font, int alignment, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(bgColor);
        cell.setPadding(5);
        return cell;
    }

    private PdfPCell createDataCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        cell.setBorderColor(BORDER_COLOR);
        return cell;
    }

    private boolean isRed(RobOrderBillVO vo) {
        return vo.getYesterdayPurchaseAmount() != null
                && vo.getYesterdayPurchaseAmount().compareTo(BigDecimal.ZERO) > 0
                && vo.getTodayPurchaseAmount() != null
                && vo.getTodayPurchaseAmount().compareTo(BigDecimal.ZERO) == 0;
    }

    private String formatAmount(BigDecimal amount) {
        return amount == null ? "0" : amount.toPlainString();
    }
}
