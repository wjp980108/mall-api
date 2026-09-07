package com.atguigu.meet.enums;

import lombok.Getter;

/**
 * 积分流水变动类型枚举（t_user_points_flow.flow_type）
 * 1收入 2冲回(取消/转移收回) 3转让转出 4转让转入
 */
@Getter
public enum PointsFlowType {

    INCOME(1, "收入"),
    REVERSE(2, "冲回"),
    TRANSFER_OUT(3, "转让转出"),
    TRANSFER_IN(4, "转让转入");

    private final int code;
    private final String desc;

    PointsFlowType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static PointsFlowType of(Integer code) {
        if (code == null) return null;
        for (PointsFlowType t : values()) {
            if (t.code == code) {
                return t;
            }
        }
        return null;
    }

    /**
     * 安全获取变动类型中文名，code 为 null 或未知时返回「未知」
     */
    public static String descOf(Integer code) {
        PointsFlowType t = of(code);
        return t == null ? "未知" : t.getDesc();
    }
}
