package com.atguigu.meet.model.vo.roborder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 取消/转移订单积分不足提示 VO（data 非空即需要二次确认）
 */
@Data
@Schema(description = "积分不足提示数据（非空表示存在冲回缺口，需用户二次确认）")
public class PointsInsufficientVO {

    /** 可用积分是否存在冲回缺口 */
    @Schema(description = "可用积分是否不足：任一推荐奖/自购奖金冲回项余额小于待冲回金额")
    private Boolean pointsInsufficient;

    /** 购物券积分是否存在冲回缺口 */
    @Schema(description = "购物券积分是否不足：任一购物券冲回项余额小于待冲回金额")
    private Boolean couponInsufficient;
}
