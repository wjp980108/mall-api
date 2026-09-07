package com.atguigu.meet.model.vo.points;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 我的积分余额 VO
 */
@Data
@Schema(description = "积分余额数据")
public class PointsBalanceVO {

    /** 可用积分（推荐奖+自购奖金），可转让 */
    @Schema(description = "可用积分（推荐奖+自购奖金），可转让")
    private BigDecimal points;

    /** 购物券积分（自购返券），锁死不可转让 */
    @Schema(description = "购物券积分（自购返券），锁死不可转让")
    private BigDecimal couponPoints;
}
