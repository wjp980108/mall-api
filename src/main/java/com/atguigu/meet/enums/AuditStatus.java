package com.atguigu.meet.enums;

import lombok.Getter;

/**
 * 托售商品委托审核状态枚举
 * 对应 t_consign_goods.audit_status
 * 0无需审核 1待审核 2审核通过 3审核驳回
 */
@Getter
public enum AuditStatus {

    NONE(0, "无需审核"),
    WAIT_AUDIT(1, "待审核"),
    PASS(2, "审核通过"),
    REJECT(3, "审核驳回");

    private final int code;
    private final String desc;

    AuditStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static AuditStatus of(Integer code) {
        if (code == null) return null;
        for (AuditStatus s : values()) {
            if (s.code == code) {
                return s;
            }
        }
        return null;
    }

    /** 安全获取审核状态中文名，code 为 null 或未知时返回「未知」 */
    public static String descOf(Integer code) {
        AuditStatus s = of(code);
        return s == null ? "未知" : s.getDesc();
    }
}
