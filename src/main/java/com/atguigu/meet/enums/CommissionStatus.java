package com.atguigu.meet.enums;

import lombok.Getter;

/**
 * 分佣结算状态枚举
 * 对应 sys_invite_record.commission_status
 * 0待结算 1已结算 2已取消
 */
@Getter
public enum CommissionStatus {

    PENDING(0, "待结算"),
    SETTLED(1, "已结算"),
    CANCEL(2, "已取消");

    private final int code;
    private final String desc;

    CommissionStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static CommissionStatus of(Integer code) {
        if (code == null) return null;
        for (CommissionStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }

    public static String descOf(Integer code) {
        CommissionStatus s = of(code);
        return s == null ? "未知" : s.getDesc();
    }
}
