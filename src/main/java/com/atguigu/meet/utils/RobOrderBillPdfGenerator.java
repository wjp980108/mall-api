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
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 抢购订单用户账单 PDF 生成器（基于 OpenPDF）。
 * <p>
 * A4 横向、红色标题栏、蓝色表头、网格线；支持金额整数展示与特定条件标红。
 * PDF 由两部分组成：
 * 1. 进货明细主表——末行为红底黑字合计行（8 个资金列纵向求和、今日分享留空、应付款纵向求和），
 *    数据行正数应付款带「+」前缀；
 * 2. 欠余合计表（另起一页）——按应付款正负分边（正→左「收+」，负→右「付-」，0 不出现），
 *    短侧空白补齐，两侧各自合计，左合计 + 右合计 = 主表应付款合计。
 * 中文字体思源黑体 Regular（SourceHanSansSC-Regular.otf）随项目内嵌，
 * 跨平台零配置，SIL OFL 1.1 许可证允许免费商用。
 */
public class RobOrderBillPdfGenerator {

    private static final String FONT_RESOURCE = "fonts/SourceHanSansSC-Regular.otf";

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

    /**
     * 欠余合计表列：姓名｜收+｜红色装饰宽列｜姓名｜付-
     */
    private static final String[] BALANCE_HEADERS = {"姓名", "收+", "", "姓名", "付-"};
    private static final float[] BALANCE_COLUMN_WIDTHS = {1.2f, 1.0f, 1.1f, 1.2f, 1.0f};
    private static final float BALANCE_WIDTH_PERCENTAGE = 58f;

    private static final Color TITLE_BG = Color.RED;
    private static final Color TITLE_TEXT = Color.WHITE;
    private static final Color BALANCE_TITLE_TEXT = Color.BLACK;
    private static final Color HEADER_BG = new Color(70, 130, 180);
    private static final Color HEADER_TEXT = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(200, 200, 200);
    private static final Color RED_TEXT = Color.RED;
    private static final Color TOTAL_BG = Color.RED;
    private static final Color TOTAL_TEXT = Color.BLACK;

    private static final float FONT_SIZE_TITLE = 16f;
    private static final float FONT_SIZE_BALANCE_HEADER = 11f;
    private static final float FONT_SIZE_HEADER = 9f;
    private static final float FONT_SIZE_BODY = 9f;

    private final String title;
    private final String balanceTitle;

    /**
     * @param title        主表 PDF 标题（进货明细表）
     * @param balanceTitle 欠余合计表标题（含零填充日期，如「欠余合计表 - 2026年09月18日」）
     */
    public RobOrderBillPdfGenerator(String title, String balanceTitle) {
        this.title = title;
        this.balanceTitle = balanceTitle;
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

            BaseFont baseFont = loadEmbeddedFont();
            Font titleFont = new Font(baseFont, FONT_SIZE_TITLE, Font.BOLD, TITLE_TEXT);
            Font balanceTitleFont = new Font(baseFont, FONT_SIZE_TITLE, Font.BOLD, BALANCE_TITLE_TEXT);
            Font headerFont = new Font(baseFont, FONT_SIZE_HEADER, Font.BOLD, HEADER_TEXT);
            Font balanceHeaderFont = new Font(baseFont, FONT_SIZE_BALANCE_HEADER, Font.BOLD, BALANCE_TITLE_TEXT);
            Font bodyFont = new Font(baseFont, FONT_SIZE_BODY, Font.NORMAL, Color.BLACK);
            Font redBodyFont = new Font(baseFont, FONT_SIZE_BODY, Font.NORMAL, RED_TEXT);
            Font totalFont = new Font(baseFont, FONT_SIZE_BODY, Font.BOLD, TOTAL_TEXT);

            BillTotals totals = calculate(records);

            addTitle(document, title, titleFont);
            addMainTable(document, totals, records, headerFont, bodyFont, redBodyFont, totalFont);

            // 欠余合计表另起一页
            document.newPage();
            addTitle(document, balanceTitle, balanceTitleFont);
            addBalanceTable(document, totals, balanceHeaderFont, bodyFont, totalFont);

            document.close();
        } catch (DocumentException e) {
            throw new BusinessException("生成账单 PDF 失败：" + e.getMessage());
        }
    }

    // ==================== 纯计算（package-private，便于无渲染单测） ====================

    /**
     * 主表 8 个资金列与应付款的纵向合计，以及欠余表左右分组。
     */
    static final class BillTotals {
        /** 列序：今日买入/今日寄售/今日付款/今日回款/昨日买入/昨日寄售/昨日付款/昨日回款 */
        final long[] columnTotals;
        /** 主表应付款合计 */
        final long payableTotal;
        /** 左组「收+」：应付款为正的买家（保序） */
        final List<RobOrderBillVO> receivableList;
        /** 右组「付-」：应付款为负的买家（保序） */
        final List<RobOrderBillVO> payableList;
        /** 左组合计（正） */
        final long receivableTotal;
        /** 右组合计（负） */
        final long payableSideTotal;

        BillTotals(long[] columnTotals, long payableTotal,
                   List<RobOrderBillVO> receivableList, List<RobOrderBillVO> payableList,
                   long receivableTotal, long payableSideTotal) {
            this.columnTotals = columnTotals;
            this.payableTotal = payableTotal;
            this.receivableList = receivableList;
            this.payableList = payableList;
            this.receivableTotal = receivableTotal;
            this.payableSideTotal = payableSideTotal;
        }
    }

    /**
     * 基于归一化后的记录计算合计与欠余分组。正数应付款入左组，负数入右组，0 剔除；
     * 组内保持入参顺序。金额按四舍五入取整后参与求和（与数据行展示口径一致）。
     */
    static BillTotals calculate(List<RobOrderBillVO> records) {
        long[] columnTotals = new long[8];
        long payableTotal = 0;
        List<RobOrderBillVO> receivableList = new ArrayList<>();
        List<RobOrderBillVO> payableList = new ArrayList<>();

        for (RobOrderBillVO vo : records) {
            columnTotals[0] += asLong(vo.getTodayPurchaseAmount());
            columnTotals[1] += asLong(vo.getTodayConsignmentAmount());
            columnTotals[2] += asLong(vo.getTodayPaymentAmount());
            columnTotals[3] += asLong(vo.getTodayReceiptAmount());
            columnTotals[4] += asLong(vo.getYesterdayPurchaseAmount());
            columnTotals[5] += asLong(vo.getYesterdayConsignmentAmount());
            columnTotals[6] += asLong(vo.getYesterdayPaymentAmount());
            columnTotals[7] += asLong(vo.getYesterdayReceiptAmount());

            long payable = asLong(vo.getPayableAmount());
            payableTotal += payable;
            if (payable > 0) {
                receivableList.add(vo);
            } else if (payable < 0) {
                payableList.add(vo);
            }
        }

        long receivableTotal = receivableList.stream().mapToLong(v -> asLong(v.getPayableAmount())).sum();
        long payableSideTotal = payableList.stream().mapToLong(v -> asLong(v.getPayableAmount())).sum();

        return new BillTotals(columnTotals, payableTotal, receivableList, payableList,
                receivableTotal, payableSideTotal);
    }

    private static long asLong(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }
        return amount.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    // ==================== 主表 ====================

    private void addTitle(Document document, String titleText, Font titleFont) throws DocumentException {
        PdfPTable titleTable = new PdfPTable(1);
        titleTable.setWidthPercentage(100);

        PdfPCell cell = new PdfPCell(new Phrase(titleText, titleFont));
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

    private void addMainTable(Document document, BillTotals totals, List<RobOrderBillVO> records,
                              Font headerFont, Font bodyFont, Font redBodyFont, Font totalFont)
            throws DocumentException {
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
            table.addCell(createDataCell(formatPayable(vo.getPayableAmount()), bodyFont));
        }

        addMainTotalsRow(table, totals, totalFont);

        document.add(table);
    }

    /**
     * 主表合计行：前 3 列合并为「合计」；8 个资金列求和；今日分享空白；应付款求和。红底黑字加粗。
     */
    private void addMainTotalsRow(PdfPTable table, BillTotals totals, Font totalFont) {
        PdfPCell labelCell = createCell("合计", totalFont, Element.ALIGN_LEFT, TOTAL_BG);
        labelCell.setColspan(3);
        table.addCell(labelCell);

        for (long columnTotal : totals.columnTotals) {
            table.addCell(createCell(Long.toString(columnTotal), totalFont, Element.ALIGN_CENTER, TOTAL_BG));
        }

        // 今日分享：红底空白格（保留边框，不做合计）
        table.addCell(createCell("", totalFont, Element.ALIGN_CENTER, TOTAL_BG));

        // 应付款合计
        table.addCell(createCell(Long.toString(totals.payableTotal), totalFont, Element.ALIGN_CENTER, TOTAL_BG));
    }

    // ==================== 欠余合计表 ====================

    private void addBalanceTable(Document document, BillTotals totals,
                                 Font balanceHeaderFont, Font bodyFont, Font totalFont) throws DocumentException {
        PdfPTable table = new PdfPTable(BALANCE_HEADERS.length);
        table.setWidthPercentage(BALANCE_WIDTH_PERCENTAGE);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.setWidths(BALANCE_COLUMN_WIDTHS);
        table.setSpacingBefore(5);
        table.setSpacingAfter(5);

        // 表头：姓名｜收+｜红色装饰｜姓名｜付-
        for (int i = 0; i < BALANCE_HEADERS.length; i++) {
            if (i == 2) {
                table.addCell(createDecorCell());
            } else {
                table.addCell(createCell(BALANCE_HEADERS[i], balanceHeaderFont,
                        Element.ALIGN_CENTER, TOTAL_BG));
            }
        }

        // 数据行：按较多一侧对齐，少侧补空白行
        int rowCount = Math.max(totals.receivableList.size(), totals.payableList.size());
        for (int i = 0; i < rowCount; i++) {
            if (i < totals.receivableList.size()) {
                RobOrderBillVO vo = totals.receivableList.get(i);
                table.addCell(createDataCell(vo.getUserName(), bodyFont));
                // 左组金额不带正号
                table.addCell(createDataCell(formatAmount(vo.getPayableAmount()), bodyFont));
            } else {
                table.addCell(createCell("", bodyFont, Element.ALIGN_CENTER, Color.WHITE));
                table.addCell(createCell("", bodyFont, Element.ALIGN_CENTER, Color.WHITE));
            }

            table.addCell(createDecorCell());

            if (i < totals.payableList.size()) {
                RobOrderBillVO vo = totals.payableList.get(i);
                table.addCell(createDataCell(vo.getUserName(), bodyFont));
                // 右组金额为原负值（自带负号）
                table.addCell(createDataCell(formatAmount(vo.getPayableAmount()), bodyFont));
            } else {
                table.addCell(createCell("", bodyFont, Element.ALIGN_CENTER, Color.WHITE));
                table.addCell(createCell("", bodyFont, Element.ALIGN_CENTER, Color.WHITE));
            }
        }

        // 两侧合计行
        table.addCell(createCell("合计", totalFont, Element.ALIGN_CENTER, TOTAL_BG));
        table.addCell(createCell(Long.toString(totals.receivableTotal), totalFont, Element.ALIGN_CENTER, TOTAL_BG));
        table.addCell(createDecorCell());
        table.addCell(createCell("合计", totalFont, Element.ALIGN_CENTER, TOTAL_BG));
        table.addCell(createCell(Long.toString(totals.payableSideTotal), totalFont, Element.ALIGN_CENTER, TOTAL_BG));

        document.add(table);
    }

    /**
     * 红色装饰列单元格：红底无边框，使整列呈现连续红色竖条。
     */
    private PdfPCell createDecorCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(TOTAL_BG);
        cell.setBorder(Rectangle.NO_BORDER);
        return cell;
    }

    // ==================== 通用单元格与格式化 ====================

    private PdfPCell createCell(String text, Font font, int alignment, Color bgColor) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(bgColor);
        cell.setPadding(5);
        cell.setBorderColor(BORDER_COLOR);
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

    /**
     * 主表数据行应付款展示口径：正数显式带「+」前缀，负数/零维持 toPlainString 结果。
     * 欠余表金额不走此方法（左组不带正号）。package-private 便于无渲染单测。
     */
    static String formatPayable(BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        return value.signum() > 0 ? "+" + value.toPlainString() : value.toPlainString();
    }

    /**
     * 从 classpath 加载内嵌字体，BaseFont.EMBEDDED 确保 PDF 跨环境正常显示中文。
     * <p>
     * OpenPDF 2.0 支持通过 byte[] 加载字体（createFont 的重载签名：
     * createFont(String name, String encoding, boolean embedded, boolean cached,
     * byte[] ttfAfm, byte[] pfb)），TTF/OTF 字体传入 ttfAfm 参数，pfb 传 null。
     */
    private BaseFont loadEmbeddedFont() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = RobOrderBillPdfGenerator.class.getClassLoader();
        }
        try (InputStream is = cl.getResourceAsStream(FONT_RESOURCE)) {
            if (is == null) {
                throw new IOException("PDF 内嵌字体不存在: " + FONT_RESOURCE);
            }
            byte[] fontBytes = is.readAllBytes();
            return BaseFont.createFont(
                    "SourceHanSansSC-Regular.otf",
                    BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED,
                    true,      // cached
                    fontBytes, // ttfAfm：TTF/OTF 字体文件字节
                    null       // pfb：Type1 字体用，TTF/OTF 传 null
            );
        } catch (IOException | DocumentException e) {
            throw new BusinessException("加载 PDF 内嵌字体失败: " + e.getMessage());
        }
    }
}
