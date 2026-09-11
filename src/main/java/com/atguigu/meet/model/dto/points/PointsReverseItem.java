package com.atguigu.meet.model.dto.points;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 积分冲回项（余额预检用）：一个（用户 × 账户类型 × 金额）组合
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PointsReverseItem {

    /** 被冲回用户ID */
    private Long userId;

    /** 账户类型：1可用积分 2购物券积分（PointsAccountType.code） */
    private Integer accountType;

    /** 待冲回金额（正数） */
    private BigDecimal amount;
}
