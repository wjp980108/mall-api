package com.atguigu.meet.controller.roborder;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.roborder.RobOrderFlowPageQueryDTO;
import com.atguigu.meet.model.vo.roborder.RobOrderFlowDetailVO;
import com.atguigu.meet.model.vo.roborder.RobOrderFlowPageResultVO;
import com.atguigu.meet.model.vo.roborder.RobOrderFlowSummaryVO;
import com.atguigu.meet.model.vo.roborder.RobOrderFlowVO;
import com.atguigu.meet.service.roborder.RobOrderFlowService;
import com.atguigu.meet.utils.TimeRangeUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单流水（管理端算账读模型）
 * <p>
 * 与「订单管理」（运营操作视角）刻意隔离：本类全部为只读接口，以订单操作事件发生日为轴，
 * 下单为正、取消红冲为负、转移拆红冲（原买家、负额）与正向（新买家、正额）两行，历史账目不随后续操作改写。
 * 明细分页以订单组为单位，同一订单事件行连续、组内按「下单→红冲→正向」正序。
 * 仅要求登录态，不做按钮权限校验（与后台查询类接口惯例一致）。
 */
@RestController
@RequestMapping("/robOrderFlows")
@Validated
@Tag(name = "订单流水", description = "按事件发生日查询订单资金流水：事件明细分页、成交/红冲/净额汇总、单笔订单事件时间线（纯只读）")
public class RobOrderFlowController {

    @Autowired
    private RobOrderFlowService robOrderFlowService;

    /**
     * 事件流水分页（按订单组）
     *
     * <p>分页单位为<b>订单组</b>：筛选条件下至少含一条匹配事件的订单为一组，分页 total/pages 以组计数
     * （转移拆两行不再使 total 翻倍），同组事件行不会被翻页切开。{@code list} 仍为平铺事件行：
     * 同一订单的行连续出现，组间按组内最新匹配事件倒序（最近变动的订单在最前），
     * 组内按事件时间正序排列为「下单(正) → 转移红冲(原买家,负) → 转移正向(新买家,正)」。
     * 买家取各事件发生时落库的快照（下单行永远显示真实下单人，不随后续转移漂移）。
     *
     * @param parameter 分页 + 事件日期范围/事件类型：
     *                  pageNum/pageSize 必填（页大小=订单组数，每页实际渲染行数可大于组数）；
     *                  timeRange 事件发生日期范围（yyyy-MM-dd 起,止，逗号分隔；不传默认查全部，
     *                  查单日起止传同一天）；
     *                  operateType 事件类型（1下单 2取消订单 3转移订单，不传查全部）；
     *                  筛选同时作用于组定位与组内行——只筛下单时仅含下单事件的订单入组且组内只返回下单行
     * @return 平铺事件行分页（同订单行连续，可按相邻 orderId 合并单元格），每行含事件类型及中文名、事件时间、
     *         订单ID/编号、商品（图/名/货号）、事件买家（姓名/手机号）、场次、数量、
     *         带符号订单总额（下单正/取消负/转移红冲负+正向正）、操作人、备注；
     *         分页字段 total/pages/current/size 以订单组计；
     *         另含 totalAmount 字段：当前筛选条件（含事件类型）下全部匹配事件的带符号金额合计
     *         （非仅当前页；下单正/取消负/转移两行抵消为0；筛取消时为负，无匹配为0）
     */
    @GetMapping
    @Operation(summary = "订单流水分页（按订单组）", description = "分页单位=订单组：至少含一条匹配事件的订单为一组，total/pages 以组计数（转移拆行不翻倍），同组行不跨页；list 保持平铺，同订单行连续，组间按最新匹配事件倒序、组内按事件时间正序（下单+ → 转移红冲- → 转移正向+，链式转移按时间成对排列）；买家取事件发生时快照（下单行不随后续转移漂移）；事件类型筛选同时作用于组定位与组内行。响应另含 totalAmount：当前筛选结果全部事件的带符号金额合计（受事件类型筛选影响）")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RobOrderFlowPageResultVO.class)))
    public Response getFlowPage(@Valid RobOrderFlowPageQueryDTO parameter) {
        return robOrderFlowService.getFlowPage(parameter);
    }

    /**
     * 订单流水区间汇总
     *
     * <p>对事件发生时间落在区间内的事件做成交/红冲条件聚合：成交 income 统计下单事件与转移正向视角（均正额），
     * 红冲 reversal 统计取消事件与转移红冲视角（取正表示冲销规模）；转移在两组各计一次，净额中自我抵消，资金池规模不变。
     * 跨日取消只体现在取消发生日（取消日净额可为负），下单日数字保持不变。
     *
     * @param timeRange 事件发生日期范围字符串（格式 yyyy-MM-dd,yyyy-MM-dd，逗号分隔；
     *                  不传默认查全部；起止相同即查单日）
     * @return 三组汇总（income 成交 / reversal 红冲 / net 净额=成交-红冲），
     *         每组含：笔数 count、商品件数 quantity、订单总额 totalAmount、利润池 profitAmount、
     *         推荐奖 recommendAmount、自购奖 selfBuyAmount、自购奖金 selfBuyBonusAmount、
     *         购物券 selfBuyCouponAmount；区间无事件时全部字段为 0（不为 null）
     */
    @GetMapping("/summary")
    @Operation(summary = "订单流水区间汇总", description = "返回成交(下单+转移正向)/红冲(取消+转移红冲)/净额三组：笔数、商品件数、订单总额、利润池、推荐奖、自购奖、自购奖金、购物券、回款/付款金额；转移在成交与红冲各计一次且金额相等，净额不变；时间参数不传默认查全部")
    public Response<RobOrderFlowSummaryVO> getFlowSummary(
            @Parameter(description = "事件发生时间范围 yyyy-MM-dd,yyyy-MM-dd（不传默认查全部）", example = "2026-09-12,2026-09-12")
            @RequestParam(required = false) String timeRange) {
        return robOrderFlowService.getFlowSummary(TimeRangeUtils.parseTimeRange(timeRange));
    }

    /**
     * 单笔订单流水详情
     *
     * <p>返回一笔订单下单时的完整业务/金额快照及其全部操作事件时间线。
     * 快照金额为下单原值（转移后买家快照为当前买家），事件时间线可完整还原
     * 下单 → 取消 / 转移的发生过程。
     *
     * @param orderId 订单ID（t_rob_order.id，路径参数）
     * @return 订单快照：订单编号、场次、商品、买家、推荐人、收货信息、数量、
     *         订单总额/利润池/推荐奖/自购奖/自购奖金/购物券、当前状态；
     *         events 事件时间线按发生时间正序，每条含事件类型及中文名、操作前后状态及中文名、
     *         操作人ID/名称、备注、事件时间；订单不存在（含已删除）返回业务失败「订单不存在」
     */
    @GetMapping("/{orderId}")
    @Operation(summary = "单笔订单流水详情", description = "订单下单全金额/业务快照 + 下单/取消/转移事件时间线（按事件发生时间正序，含操作人与备注）")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RobOrderFlowDetailVO.class)))
    public Response<RobOrderFlowDetailVO> getFlowDetail(@PathVariable Long orderId) {
        return robOrderFlowService.getFlowDetail(orderId);
    }
}
