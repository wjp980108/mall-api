package com.atguigu.meet.enums;

import lombok.Getter;

/**
 * 订单取消来源枚举
 * 1待付款取消 2已付款取消 3代售中取消 4超时自动取消
 */
@Getter
public enum CancelSource {

    WAIT_PAY_CANCEL(1, "待付款取消"),
    PAID_CANCEL(2, "已付款取消"),
    AGENT_SALE_CANCEL(3, "代售中取消"),
    TIMEOUT_CANCEL(4, "超时自动取消");

    private final int code;
    private final String desc;

    CancelSource(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static CancelSource of(Integer code) {
        if (code == null) return null;
        for (CancelSource s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }

    /**
     * 安全获取取消来源中文名，code 为 null 或未知时返回空字符串
     */
    public static String descOf(Integer code) {
        CancelSource s = of(code);
        return s == null ? "" : s.getDesc();
    }
}