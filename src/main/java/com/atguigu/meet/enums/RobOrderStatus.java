package com.atguigu.meet.enums;

import lombok.Getter;

/**
 * 抢购订单状态枚举（t_rob_order.order_status）
 * 下单即成交的极简两态：1正常 2已取消
 */
@Getter
public enum RobOrderStatus {

    NORMAL(1, "正常"),
    CANCEL(2, "已取消");

    private final int code;
    private final String desc;

    RobOrderStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static RobOrderStatus of(Integer code) {
        if (code == null) return null;
        for (RobOrderStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }

    /**
     * 安全获取状态中文名，code 为 null 或未知时返回「未知」
     */
    public static String descOf(Integer code) {
        RobOrderStatus s = of(code);
        return s == null ? "未知" : s.getDesc();
    }
}
