package com.atguigu.meet.controller.roborder;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.roborder.RobOrderBillPageQueryDTO;
import com.atguigu.meet.model.vo.roborder.RobOrderBillVO;
import com.atguigu.meet.service.roborder.RobOrderBillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 抢购订单用户账单（管理端算账读模型）
 * <p>
 * 只读接口：按选中日期及其前一天，对每个有数据的买家聚合买入、付款、回款、寄售、分享及应付款金额。
 * 与后台查询类接口惯例一致，仅要求登录态，不做按钮权限校验。
 */
@RestController
@RequestMapping("/robOrderBills")
@Validated
@Tag(name = "抢购订单用户账单", description = "按选中日期与前一天对比，按买家聚合资金指标（买入/付款/回款/寄售/分享/应付款）")
public class RobOrderBillController {

    @Autowired
    private RobOrderBillService robOrderBillService;

    /**
     * 账单分页列表
     *
     * @param parameter 分页 + 选中日期（yyyy-MM-dd，空则取当天）+ 关键词（买家姓名/手机号）
     * @return 按买家聚合的账单分页，空查询返回空列表
     */
    @GetMapping
    @Operation(summary = "抢购订单用户账单分页", description = "按选中日期及其前一天，按买家聚合资金指标；date 为空取当天；keyword 支持买家姓名/手机号模糊匹配；无数据买家不出现")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = RobOrderBillVO.class)))
    public Response getBillPage(@Valid RobOrderBillPageQueryDTO parameter) {
        return robOrderBillService.getBillPage(parameter);
    }
}
