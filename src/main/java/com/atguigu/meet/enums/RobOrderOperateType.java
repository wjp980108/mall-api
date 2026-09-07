package com.atguigu.meet.enums;

import lombok.Getter;

/**
 * 抢购订单操作类型枚举（t_rob_order_operate_log.operate_type）
 * 1下单 2取消订单 3转移订单
 */
@Getter
public enum RobOrderOperateType {

    PLACE_ORDER(1, "用户抢购下单"),
    CANCEL_ORDER(2, "取消订单"),
    TRANSFER_ORDER(3, "转移订单");

    private final int code;
    private final String desc;

    RobOrderOperateType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static RobOrderOperateType of(Integer code) {
        if (code == null) return null;
        for (RobOrderOperateType t : values()) {
            if (t.code == code) {
                return t;
            }
        }
        return null;
    }

    /**
     * 安全获取操作类型中文名，code 为 null 或未知时返回「未知」
     */
    public static String descOf(Integer code) {
        RobOrderOperateType t = of(code);
        return t == null ? "未知" : t.getDesc();
    }
}
