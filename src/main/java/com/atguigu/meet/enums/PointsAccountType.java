package com.atguigu.meet.enums;

import lombok.Getter;

/**
 * 积分账户类型枚举（t_user_points_flow.account_type）
 * 1可用积分(points，推荐奖+自购奖金，可转让) 2购物券积分(coupon_points，锁死不可转)
 */
@Getter
public enum PointsAccountType {

    POINTS(1, "可用积分"),
    COUPON(2, "购物券积分");

    private final int code;
    private final String desc;

    PointsAccountType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static PointsAccountType of(Integer code) {
        if (code == null) return null;
        for (PointsAccountType t : values()) {
            if (t.code == code) {
                return t;
            }
        }
        return null;
    }

    /**
     * 安全获取账户类型中文名，code 为 null 或未知时返回「未知」
     */
    public static String descOf(Integer code) {
        PointsAccountType t = of(code);
        return t == null ? "未知" : t.getDesc();
    }
}
