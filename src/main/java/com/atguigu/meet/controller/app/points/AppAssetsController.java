package com.atguigu.meet.controller.app.points;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.points.PointsFlowPageQueryDTO;
import com.atguigu.meet.model.dto.points.PointsTransferDTO;
import com.atguigu.meet.model.vo.points.PointsBalanceVO;
import com.atguigu.meet.service.points.UserPointsService;
import com.atguigu.meet.utils.AdminContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * H5 我的资产（积分）
 * <p>
 * 依赖 JWT 登录态，当前用户 ID 从 {@link AdminContext} 取；
 * 双余额：可用积分（推荐奖+自购奖金，可转让）与购物券积分（锁死不可转）。
 */
@RestController
@RequestMapping("/app/assets")
@Validated
@Tag(name = "H5我的资产", description = "积分余额、积分明细(推荐奖/自购奖/购物券奖/积分对冲)、积分转让")
public class AppAssetsController {

    @Autowired
    private UserPointsService userPointsService;

    /**
     * 我的积分余额
     *
     * @return 可用积分 + 购物券积分
     */
    @GetMapping("/points")
    @Operation(summary = "我的积分余额", description = "返回当前用户可用积分（推荐奖+自购奖金，可转让）与购物券积分（锁死不可转）")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = PointsBalanceVO.class)))
    public Response<PointsBalanceVO> getPointsBalance() {
        return userPointsService.getBalance(AdminContext.getLoginUserId());
    }

    /**
     * 积分明细分页
     *
     * @param parameter 分页 + 业务类型(1推荐奖 2自购奖 3购物券奖 4积分对冲)
     * @return 积分流水分页
     */
    @GetMapping("/points/flow")
    @Operation(summary = "积分明细分页", description = "查询当前用户积分明细，可按业务类型筛选：1推荐奖 2自购奖 3购物券奖 4积分对冲(转让)")
    public Response getPointsFlow(@Valid PointsFlowPageQueryDTO parameter) {
        return userPointsService.pageFlow(AdminContext.getLoginUserId(),
                parameter.getBizType(), parameter.getPageNum(), parameter.getPageSize());
    }

    /**
     * 积分转让
     *
     * @param dto 对方手机号 + 转让数量（仅可用积分）
     * @return 转让结果
     */
    @PostMapping("/points/transfer")
    @Operation(summary = "积分转让", description = "输入对方手机号与数量转让可用积分：校验对方已注册/不能转给自己/余额充足，购物券积分不参与；双方各写一条积分对冲流水")
    public Response<Void> transferPoints(@RequestBody @Valid PointsTransferDTO dto) {
        return userPointsService.transfer(AdminContext.getLoginUserId(), dto.getPhone(), dto.getAmount());
    }
}
