package com.atguigu.meet.enums;

import lombok.Getter;

/**
 * 委托代卖事件记录状态枚举
 * 对应 t_consign_record.record_status
 * 1待审核 2审核通过·已上架 3已卖出 4未售出下架 5审核驳回
 * <p>
 * 1->2 审核通过；1->5 审核驳回；2->3 卖出成交；2->4 未售出下架
 * 3/4/5 均为终态，商品再次委托生成全新独立记录。
 */
@Getter
public enum RecordStatus {

    PENDING_AUDIT(1, "待审核"),
    ON_SHELF(2, "审核通过·已上架"),
    SOLD(3, "已卖出"),
    DELIST(4, "未售出下架"),
    REJECTED(5, "审核驳回");

    private final int code;
    private final String desc;

    RecordStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static RecordStatus of(Integer code) {
        if (code == null) return null;
        for (RecordStatus s : values()) {
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
        RecordStatus s = of(code);
        return s == null ? "未知" : s.getDesc();
    }
}
