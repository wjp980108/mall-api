package com.atguigu.meet.model.dto.roborder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 抢购订单用户账单 PDF 导出 DTO（管理端算账读模型）
 * <p>
 * 与列表接口查询口径一致，但无需分页参数。
 */
@Data
@Schema(description = "抢购订单用户账单 PDF 导出参数")
public class RobOrderBillExportDTO {

    /** 选中日期（yyyy-MM-dd），空则取当天 */
    @Schema(description = "选中日期（yyyy-MM-dd），为空时取当天")
    private String date;

    /** 关键词（买家姓名/手机号 模糊匹配） */
    @Schema(description = "搜索关键词（买家姓名或手机号）")
    private String keyword;
}
