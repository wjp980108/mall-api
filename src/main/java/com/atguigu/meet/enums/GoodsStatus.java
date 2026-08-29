package com.atguigu.meet.enums;

import lombok.Getter;

/**
 * 托售商品业务状态枚举
 * 对应 t_consign_goods.goods_status
 * 1挂卖中 2已抢购待付款 3等待确认付款 4待处理 5委托代卖 6委托发货
 */
@Getter
public enum GoodsStatus {

    ON_SALE(1, "挂卖中"),
    WAIT_PAY(2, "已抢购待付款"),
    WAIT_CONFIRM(3, "等待确认付款"),
    PENDING(4, "待处理"),
    AGENT_SALE(5, "委托代卖"),
    AGENT_SHIP(6, "委托发货");

    private final int code;
    private final String desc;

    GoodsStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static GoodsStatus of(Integer code) {
        if (code == null) return null;
        for (GoodsStatus s : values()) {
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
        GoodsStatus s = of(code);
        return s == null ? "未知" : s.getDesc();
    }
}
