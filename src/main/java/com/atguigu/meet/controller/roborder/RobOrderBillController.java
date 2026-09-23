package com.atguigu.meet.controller.roborder;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.roborder.RobOrderBillExportDTO;
import com.atguigu.meet.model.dto.roborder.RobOrderBillPageQueryDTO;
import com.atguigu.meet.model.vo.roborder.RobOrderBillVO;
import com.atguigu.meet.service.roborder.RobOrderBillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 抢购订单用户账单（管理端算账读模型）
 * <p>
 * 只读接口：按选中日期及其前一天，对每个有事件行的买家聚合买入、付款、回款、寄售、分享及应付款金额。
 * 取消订单在创建日计入并同日红冲，转移订单在创建日对原买家红冲、对新买家正向计入；金额字段均为净额口径。
 * 与后台查询类接口惯例一致，仅要求登录态，不做按钮权限校验。
 */
@RestController
@RequestMapping("/robOrderBills")
@Validated
@Tag(name = "抢购订单用户账单", description = "按选中日期与前一天对比，按买家聚合净额资金指标（买入/付款/回款/寄售/分享/应付款）；取消/转移按订单创建日归集红冲")
public class RobOrderBillController {

    @Autowired
    private RobOrderBillService robOrderBillService;

    /**
     * 账单分页列表
     *
     * <p>金额按订单创建日归属，以事件行带符号净额聚合：下单/转移正向为正，取消/转移红冲为负。
     * 今日创建的订单当日取消时，今日买入/分享/付款/回款净额均为 0；昨日创建的订单今日取消时，
     * 红冲行归属昨日，今日指标不受影响；转移订单在创建日对原买家红冲、对新买家正向计入。
     *
     * @param parameter 分页 + 选中日期（yyyy-MM-dd，空则取当天）+ 关键词（买家姓名/手机号）
     * @return 按买家聚合的账单分页，空查询返回空列表
     */
    @GetMapping
    @Operation(summary = "抢购订单用户账单分页", description = "按选中日期及其前一天，按买家聚合净额资金指标；取消/转移按订单创建日归集红冲；date 为空取当天；keyword 支持买家姓名/手机号模糊匹配；无事件行买家不出现")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RobOrderBillVO.class)))
    public Response getBillPage(@Valid RobOrderBillPageQueryDTO parameter) {
        return robOrderBillService.getBillPage(parameter);
    }

    /**
     * 导出账单 PDF
     *
     * <p>数据口径与账单分页列表一致：按订单创建日净额聚合，取消/转移订单按创建日归集红冲。
     *
     * @param parameter 选中日期（yyyy-MM-dd，空则取当天）+ 关键词（买家姓名/手机号）
     * @param response  HTTP 响应流
     */
    @GetMapping("/exportRobOrderBillsPDF")
    @Operation(summary = "导出抢购订单用户账单 PDF", description = "按选中日期及其前一天导出账单 PDF；数据口径与列表一致（按订单创建日净额聚合，取消/转移按创建日归集红冲）；date 为空取当天；keyword 支持买家姓名/手机号模糊匹配；无数据返回空表 PDF")
    public void exportRobOrderBillsPDF(RobOrderBillExportDTO parameter, HttpServletResponse response) {
        robOrderBillService.exportRobOrderBillsPdf(parameter, response);
    }
}
