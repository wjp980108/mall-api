package com.atguigu.meet.service.roborder.impl;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.exception.BusinessException;
import com.atguigu.meet.mapper.roborder.RobOrderBillMapper;
import com.atguigu.meet.model.dto.roborder.RobOrderBillExportDTO;
import com.atguigu.meet.model.dto.roborder.RobOrderBillPageQueryDTO;
import com.atguigu.meet.model.entity.general.settings.SysSettings;
import com.atguigu.meet.model.vo.PageResultVO;
import com.atguigu.meet.model.vo.roborder.RobOrderBillVO;
import com.atguigu.meet.service.general.settings.SysSettingsService;
import com.atguigu.meet.service.roborder.RobOrderBillService;
import com.atguigu.meet.utils.RobOrderBillPdfGenerator;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 抢购订单用户账单 Service 实现（管理端算账读模型，纯只读）
 * <p>
 * 在 Mapper 层一次聚合两日指标，Service 层做日期解析、金额归一化、派生字段计算与分页/PDF 导出包装。
 */
@Service
public class RobOrderBillServiceImpl implements RobOrderBillService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter CHINESE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy年M月d日");
    private static final String TITLE_SUFFIX = "进货明细表";
    private static final String FILE_NAME_SEPARATOR = "_";

    @Autowired
    private RobOrderBillMapper robOrderBillMapper;

    @Autowired
    private SysSettingsService sysSettingsService;

    @Value("${roborder.bill.pdf.font-path}")
    private String pdfFontPath;

    @Override
    public Response getBillPage(RobOrderBillPageQueryDTO parameter) {
        DateRange range = parseDateRange(parameter.getDate());

        Page<RobOrderBillVO> page = new Page<>(parameter.getPageNum(), parameter.getPageSize());
        IPage<RobOrderBillVO> billPage = robOrderBillMapper.selectBillPage(
                page, range.todayStart, range.todayEnd, range.yesterdayStart, range.yesterdayEnd, parameter.getKeyword());

        List<RobOrderBillVO> records = billPage.getRecords();
        records.forEach(this::normalizeAmounts);

        return Response.ok(PageResultVO.of(billPage));
    }

    @Override
    public void exportRobOrderBillsPdf(RobOrderBillExportDTO parameter, HttpServletResponse response) {
        DateRange range = parseDateRange(parameter.getDate());

        List<RobOrderBillVO> records = robOrderBillMapper.selectBillList(
                range.todayStart, range.todayEnd, range.yesterdayStart, range.yesterdayEnd, parameter.getKeyword());
        records.forEach(this::normalizeAmounts);

        String siteName = resolveSiteName();
        String title = buildTitle(siteName, range.selectedDate);
        String filename = buildFilename(siteName, range.selectedDate);

        validateFontFile();

        response.setContentType("application/pdf");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodeFilename(filename));

        try (OutputStream out = response.getOutputStream()) {
            RobOrderBillPdfGenerator generator = new RobOrderBillPdfGenerator(pdfFontPath, title);
            generator.generate(records, out);
            out.flush();
        } catch (IOException e) {
            throw new BusinessException("导出账单 PDF 失败：写入响应流异常，" + e.getMessage());
        }
    }

    /**
     * 金额归一化：Mapper 已按事件行带符号净额口径聚合（下单/转移正向为正，取消/转移红冲为负），
     * 先对原始聚合字段四舍五入取整，再计算寄售与应付款派生字段。
     * <p>
     * 公式：今日寄售 = 今日净回款 - 今日净付款；
     *      昨日寄售 = 昨日净回款 - 昨日净付款；
     *      应付款 = 今日净付款 - 昨日净回款。
     */
    private void normalizeAmounts(RobOrderBillVO vo) {
        vo.setTodayPurchaseAmount(round(vo.getTodayPurchaseAmount()));
        vo.setTodayPaymentAmount(round(vo.getTodayPaymentAmount()));
        vo.setTodayReceiptAmount(round(vo.getTodayReceiptAmount()));
        vo.setTodayShareAmount(round(vo.getTodayShareAmount()));
        vo.setYesterdayPurchaseAmount(round(vo.getYesterdayPurchaseAmount()));
        vo.setYesterdayPaymentAmount(round(vo.getYesterdayPaymentAmount()));
        vo.setYesterdayReceiptAmount(round(vo.getYesterdayReceiptAmount()));

        BigDecimal todayPayment = vo.getTodayPaymentAmount();
        BigDecimal todayReceipt = vo.getTodayReceiptAmount();
        BigDecimal yesterdayPayment = vo.getYesterdayPaymentAmount();
        BigDecimal yesterdayReceipt = vo.getYesterdayReceiptAmount();

        vo.setTodayConsignmentAmount(todayReceipt.subtract(todayPayment));
        vo.setYesterdayConsignmentAmount(yesterdayReceipt.subtract(yesterdayPayment));
        vo.setPayableAmount(todayPayment.subtract(yesterdayReceipt));
    }

    private BigDecimal round(BigDecimal v) {
        return nz(v).setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * 解析选中日期及其前一日的时间范围。
     */
    private DateRange parseDateRange(String dateStr) {
        LocalDate selectedDate = parseSelectedDate(dateStr);
        LocalDate previousDate = selectedDate.minusDays(1);

        return new DateRange(
                selectedDate,
                selectedDate.atStartOfDay(),
                selectedDate.atTime(LocalTime.of(23, 59, 59, 999_000_000)),
                previousDate.atStartOfDay(),
                previousDate.atTime(LocalTime.of(23, 59, 59, 999_000_000))
        );
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

    private String resolveSiteName() {
        SysSettings settings = sysSettingsService.get();
        return settings == null ? null : settings.getSiteName();
    }

    private String buildTitle(String siteName, LocalDate selectedDate) {
        String prefix = StringUtils.hasText(siteName) ? siteName + TITLE_SUFFIX : TITLE_SUFFIX;
        return prefix + " - " + selectedDate.format(CHINESE_DATE_FORMATTER);
    }

    private String buildFilename(String siteName, LocalDate selectedDate) {
        String prefix = StringUtils.hasText(siteName) ? siteName + TITLE_SUFFIX : TITLE_SUFFIX;
        return prefix + FILE_NAME_SEPARATOR + selectedDate.format(DATE_FORMATTER) + ".pdf";
    }

    private String encodeFilename(String filename) {
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8);
        // RFC 5987 要求空格编码为 %20 而非 +
        return encoded.replace("+", "%20");
    }

    private void validateFontFile() {
        if (!StringUtils.hasText(pdfFontPath)) {
            throw new BusinessException("PDF 中文字体路径未配置，请设置 roborder.bill.pdf.font-path");
        }
        File fontFile = new File(pdfFontPath);
        if (!fontFile.exists() || !fontFile.isFile()) {
            throw new BusinessException("PDF 中文字体文件不存在：" + pdfFontPath);
        }
    }

    private record DateRange(LocalDate selectedDate,
                             LocalDateTime todayStart,
                             LocalDateTime todayEnd,
                             LocalDateTime yesterdayStart,
                             LocalDateTime yesterdayEnd) {
    }
}
