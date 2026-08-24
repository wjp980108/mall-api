package com.atguigu.meet.enums;

import lombok.Getter;

/**
 * 订单状态枚举
 * 1待付款 2已付款 3已确认 4已代售 5已取消
 */
@Getter
public enum OrderStatus {

    WAIT_PAY(1, "待付款"),
    PAID(2, "已付款"),
    CONFIRMED(3, "已确认"),
    AGENT_SALE(4, "已代售"),
    CANCEL(5, "已取消");

    private final int code;
    private final String desc;

    OrderStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static OrderStatus of(int code) {
        for (OrderStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }

    /**
     * 校验状态流转是否合法
     */
    public static boolean canTransition(int fromCode, int toCode) {
        OrderStatus from = of(fromCode);
        OrderStatus to = of(toCode);
        if (from == null || to == null) {
            return false;
        }
        switch (from) {
            case WAIT_PAY:
                return to == PAID || to == CANCEL;
            case PAID:
                return to == CONFIRMED || to == CANCEL;
            case CONFIRMED:
                return to == AGENT_SALE;
            default:
                return false;
        }
    }
}
