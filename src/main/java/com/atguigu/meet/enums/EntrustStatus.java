package com.atguigu.meet.enums;

import lombok.Getter;

/**
 * 托售商品委托状态枚举
 * 对应 t_consign_goods.entrust_status
 * 0未委托 1委托代卖中
 */
@Getter
public enum EntrustStatus {

    NOT_ENTRUST(0, "未委托"),
    ENTRUSTING(1, "委托代卖中");

    private final int code;
    private final String desc;

    EntrustStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static EntrustStatus of(Integer code) {
        if (code == null) return null;
        for (EntrustStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }

    /** 安全获取委托状态中文名，code 为 null 或未知时返回「未知」 */
    public static String descOf(Integer code) {
        EntrustStatus s = of(code);
        return s == null ? "未知" : s.getDesc();
    }
}
