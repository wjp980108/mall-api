package com.atguigu.meet.controller.app.goods;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.service.goods.consign.ConsignRecordService;
import com.atguigu.meet.utils.AdminContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 托代卖记录查询
 * （我的委托 / 我的买入）
 * <p>
 * 不走 {@code @RequirePermission}（后台 RBAC），依赖 JWT 登录态；
 * 当前用户ID 从 {@link AdminContext} 取，Service 层按 memberId/buyerId 过滤，防越权。
 */
@RestController
@RequestMapping("/app/consign-record")
@Validated
public class AppConsignRecordController {

    @Autowired
    private ConsignRecordService consignRecordService;

    /**
     * 我的委托记录
     * （作为委托人发起的委托履历，按申请时间倒序）
     */
    @GetMapping("/my-consign")
    public Response listMyConsign(@RequestParam(defaultValue = "1") Integer pageNum,
                                  @RequestParam(defaultValue = "10") Integer pageSize) {
        return consignRecordService.listMyConsign(AdminContext.getLoginUserId(), pageNum, pageSize);
    }

    /**
     * 我的买入记录
     * （作为买家已成交的委托记录，按卖出时间倒序）
     */
    @GetMapping("/my-bought")
    public Response listMyBought(@RequestParam(defaultValue = "1") Integer pageNum,
                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        return consignRecordService.listMyBought(AdminContext.getLoginUserId(), pageNum, pageSize);
    }
}
