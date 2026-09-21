package com.atguigu.meet.enums;

import lombok.Getter;

/**
 * 抢购订单收款回款状态枚举（t_rob_order.pay_status）
 * <p>
 * 与订单业务状态（{@link RobOrderStatus}）解耦的独立资金状态机：
 * 正向 0→1→2 不可逆，取消订单时统一置 3 无效（不管原值是 0/1/2），3 为单向终态。
 * <ul>
 *   <li>0 未收款：下单默认值（DB 列 DEFAULT 0 兜底，下单链路不显式写）</li>
 *   <li>1 已收款：确认收款动作（action=1）成功后置入，并写 receipt_* 审计字段</li>
 *   <li>2 已回款：确认回款动作（action=2）成功后置入，并写 payback_* 审计字段</li>
 *   <li>3 无效：取消订单时统一置入，receipt_* 与 payback_* 保留不清空供追溯</li>
 * </ul>
 */
@Getter
public enum RobOrderPayStatus {

    UNPAID(0, "未收款"),
    RECEIVED(1, "已收款"),
    PAID_BACK(2, "已回款"),
    INVALID(3, "无效");

    private final int code;
    private final String desc;

    RobOrderPayStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static RobOrderPayStatus of(Integer code) {
        if (code == null) return null;
        for (RobOrderPayStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }

    /**
     * 安全获取状态中文名，code 为 null 或未知时返回 null
     * <p>用于 RobOrderVO.payStatusName 由服务层回填（{@code vo.setPayStatusName(descOf(vo.getPayStatus()))}）。
     */
    public static String descOf(Integer code) {
        RobOrderPayStatus s = of(code);
        return s == null ? null : s.getDesc();
    }
}
