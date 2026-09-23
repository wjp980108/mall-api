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
 * 与「订单管理」（运营操作视角）刻意隔离：本类全部为只读接口，以订单创建日为轴，
 * 下单为正、取消红冲为负、转移拆红冲（原买家、负额）与正向（新买家、正额）两行，历史账目不随后续操作改写。
 * 明细分页以事件物理行为单位、全局按订单创建时间倒序，同一转移的正向行在上、红冲行在下。
 * 仅要求登录态，不做按钮权限校验（与后台查询类接口惯例一致）。
 */
@RestController
@RequestMapping("/robOrderFlows")
@Validated
@Tag(name = "订单流水", description = "按订单创建日查询订单资金流水：事件明细分页、成交/红冲/净额汇总、单笔订单事件时间线（纯只读）")
public class RobOrderFlowController {

    @Autowired
    private RobOrderFlowService robOrderFlowService;

    /**
     * 事件流水分页（按订单创建时间倒序）
     *
     * <p>分页单位为<b>事件物理行</b>：下单/取消事件各一行，转移事件拆两行，分页 total/pages 按拆行行数计
     * （转移拆两行会使 total 相应增加），pageSize 即每页行数。{@code list} 为平铺事件行，
     * 全局按订单创建时间倒序（并列按日志 ID 倒序）；同一转移事件时间相同，正向行(新买家,正)在上、
     * 红冲行(原买家,负)在下。不按订单聚合：同一订单行不保证连续（其下单行排在转移行下方），
     * 转移两行允许被翻页边界切开。买家取各事件发生时落库的快照（下单行永远显示真实下单人，不随后续转移漂移）。
     *
     * @param parameter 分页 + 订单创建日期范围/事件类型/买家关键词：
     *                  pageNum/pageSize 必填（页大小=事件行数）；
     *                  timeRange 订单创建日期范围（yyyy-MM-dd 起,止，逗号分隔；不传默认查全部，
     *                  查单日起止传同一天）；
     *                  operateType 事件类型（1下单 2取消订单 3转移订单，不传查全部）；
     *                  keyword 买家模糊查询关键词（姓名/手机号/买家ID，按事件行展示买家匹配；不传查全部）；
     *                  筛选直接作用于事件行——只筛下单时只返回下单行，筛转移时每次转移仍返回红冲+正向两行
     * @return 平铺事件行分页（全局按订单创建时间倒序，同订单行不保证连续），每行含事件类型及中文名、事件时间、
     *         订单创建时间、订单ID/编号、商品（图/名/货号）、事件买家（姓名/手机号）、场次、数量、
     *         带符号订单总额（下单正/取消负/转移红冲负+正向正）、操作人、备注；
     *         分页字段 total/pages/current/size 以事件物理行计；
     *         另含 totalAmount 字段：当前筛选条件（含事件类型）下全部匹配事件的带符号金额合计
     *         （非仅当前页；下单正/取消负/转移两行抵消为0；筛取消时为负，无匹配为0）
     */
    @GetMapping
    @Operation(summary = "订单流水分页（按订单创建时间倒序）", description = "分页单位=事件物理行：total/pages 按拆行行数计（转移拆两行相应增加），pageSize=行数；list 平铺，全局按订单创建时间倒序（并列按日志 id 倒序），同一转移正向行(+)在红冲行(-)之前；不按订单聚合，同订单行不保证连续、转移两行可跨页切开；买家取事件发生时快照（下单行不随后续转移漂移）；事件类型筛选直接作用于行。响应另含 totalAmount：当前筛选结果全部事件的带符号金额合计（受事件类型筛选影响）")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RobOrderFlowPageResultVO.class)))
    public Response getFlowPage(@Valid RobOrderFlowPageQueryDTO parameter) {
        return robOrderFlowService.getFlowPage(parameter);
    }

    /**
     * 订单流水区间汇总
     *
     * <p>对订单创建时间落在区间内的事件做成交/红冲条件聚合：成交 income 统计下单事件与转移正向视角（均正额），
     * 红冲 reversal 统计取消事件与转移红冲视角（取正表示冲销规模）；转移在两组各计一次，净额中自我抵消，资金池规模不变。
     * 跨日取消的红冲行与原始下单行均归属到订单创建日。
     *
     * @param timeRange 订单创建日期范围字符串（格式 yyyy-MM-dd,yyyy-MM-dd，逗号分隔；
     *                  不传默认查全部；起止相同即查单日）
     * @return 三组汇总（income 成交 / reversal 红冲 / net 净额=成交-红冲），
     *         每组含：笔数 count、商品件数 quantity、订单总额 totalAmount、利润池 profitAmount、
     *         推荐奖 recommendAmount、自购奖 selfBuyAmount、自购奖金 selfBuyBonusAmount、
     *         购物券 selfBuyCouponAmount、回款取整金额 receiptRoundAmount、付款金额 paymentAmount；
     *         另含顶层字段：回款总金额 totalReceiptAmount、付款总金额 totalPaymentAmount（净额口径）、
     *         销售奖 salesAward（净订单总额×推荐奖比例）、技术服务费 techServiceFee（净订单总额×0.2%）、
     *         站长服务费 stationServiceFee（净订单总额×1.2%）、订单利润差 orderProfitDiff
     *         （=(回款总-付款总)+销售奖+技术服务费+站长服务费）；区间无事件时全部字段为 0（不为 null）
     */
    @GetMapping("/summary")
    @Operation(summary = "订单流水区间汇总", description = "返回成交(下单+转移正向)/红冲(取消+转移红冲)/净额三组：笔数、商品件数、订单总额、利润池、推荐奖、自购奖、自购奖金、购物券、回款取整/付款金额；另含顶层：回款/付款总金额(净额)、销售奖(×推荐奖比例)、技术服务费(×0.2%)、站长服务费(×1.2%)、订单利润差；按订单创建日聚合，时间参数不传默认查全部")
    public Response<RobOrderFlowSummaryVO> getFlowSummary(
            @Parameter(description = "订单创建时间范围 yyyy-MM-dd,yyyy-MM-dd（不传默认查全部）", example = "2026-09-12,2026-09-12")
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
