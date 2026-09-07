package com.atguigu.meet.enums;

import lombok.Getter;

/**
 * 积分流水业务类型枚举（t_user_points_flow.biz_type）
 * 1推荐奖 2自购奖(自购奖金) 3购物券奖 4积分对冲(积分转让)
 */
@Getter
public enum PointsBizType {

    RECOMMEND(1, "推荐奖"),
    SELF_BUY(2, "自购奖"),
    COUPON(3, "购物券奖"),
    TRANSFER(4, "积分对冲");

    private final int code;
    private final String desc;

    PointsBizType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static PointsBizType of(Integer code) {
        if (code == null) return null;
        for (PointsBizType t : values()) {
            if (t.code == code) {
                return t;
            }
        }
        return null;
    }

    /**
     * 安全获取业务类型中文名，code 为 null 或未知时返回「未知」
     */
    public static String descOf(Integer code) {
        PointsBizType t = of(code);
        return t == null ? "未知" : t.getDesc();
    }
}
