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
 * 下单为正、取消红冲为负、转移为 0 元事件，历史账目不随后续操作改写。
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
     * 事件流水分页
     *
     * <p>以订单操作事件的发生时间（t_rob_order_operate_log.create_time，非订单下单时间）为轴，
     * 分页返回下单/取消/转移事件，用于按日算账。金额取订单下单时冻结的快照：
     * 下单为正、取消为负（红冲）、转移为 0；取消事件归属取消实际发生日，
     * 历史日流水不会因后续取消而被改写。
     *
     * @param parameter 分页 + 事件日期范围/事件类型：
     *                  pageNum/pageSize 必填；
     *                  timeRange 事件发生日期范围（yyyy-MM-dd 起,止，逗号分隔；不传默认今日
     *                  [00:00:00, 23:59:59]，东八区；查单日起止传同一天）；
     *                  operateType 事件类型（1下单 2取消订单 3转移订单，不传查全部）
     * @return 事件行分页，每行含事件类型及中文名、事件时间、订单ID/编号、商品（图/名/货号）、
     *         买家（姓名/手机号）、场次、数量、带符号订单总额（下单正/取消负/转移0）、操作人、备注；
     *         另含 totalAmount 字段：当前筛选条件（含事件类型）下全部匹配事件的带符号金额合计
     *         （非仅当前页；下单正/取消负/转移0；筛取消时为负，无匹配为0）
     */
    @GetMapping
    @Operation(summary = "订单流水分页", description = "按事件发生时间（默认今日）分页返回下单/取消/转移事件；下单金额为正、取消为负(红冲)、转移为0；支持事件类型筛选。响应另含 totalAmount：当前筛选结果全部事件的带符号金额合计（受事件类型筛选影响）")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RobOrderFlowPageResultVO.class)))
    public Response getFlowPage(@Valid RobOrderFlowPageQueryDTO parameter) {
        return robOrderFlowService.getFlowPage(parameter);
    }

    /**
     * 订单流水区间汇总
     *
     * <p>对事件发生时间落在区间内的事件做成交（下单）/红冲（取消）条件聚合，转移事件不计入。
     * 跨日取消只体现在取消发生日（取消日净额可为负），下单日数字保持不变。
     *
     * @param timeRange 事件发生日期范围字符串（格式 yyyy-MM-dd,yyyy-MM-dd，逗号分隔；
     *                  不传默认今日；起止相同即查单日）
     * @return 三组汇总（income 成交 / reversal 红冲 / net 净额=成交-红冲），
     *         每组含：笔数 count、商品件数 quantity、订单总额 totalAmount、利润池 profitAmount、
     *         推荐奖 recommendAmount、自购奖 selfBuyAmount、自购奖金 selfBuyBonusAmount、
     *         购物券 selfBuyCouponAmount；区间无事件时全部字段为 0（不为 null）
     */
    @GetMapping("/summary")
    @Operation(summary = "订单流水区间汇总", description = "返回成交(下单)/红冲(取消)/净额三组：笔数、商品件数、订单总额、利润池、推荐奖、自购奖、自购奖金、购物券；时间参数不传默认今日")
    public Response<RobOrderFlowSummaryVO> getFlowSummary(
            @Parameter(description = "事件发生时间范围 yyyy-MM-dd,yyyy-MM-dd（不传默认今日）", example = "2026-09-12,2026-09-12")
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
