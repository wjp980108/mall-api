package com.atguigu.meet.enums;

import lombok.Getter;

/**
 * 订单操作类型枚举（对应 t_order_operate_log.operate_type tinyint）
 * <p>
 * 对齐项目约定：code=tinyint 入库、desc=中文描述（列表展示/备注用）。
 * 与 {@link GoodsOperateType} / {@link ConsignGoodsOperateType} 保持同一风格。
 */
@Getter
public enum OrderOperateType {

    UPLOAD_VOUCHER(1, "上传凭证"),
    CONFIRM_RECEIVE(2, "确认收款"),
    CANCEL_ORDER(3, "取消订单"),
    DELETE_ORDER(4, "删除订单"),
    TIMEOUT_CANCEL(5, "超时自动取消"),
    PLACE_ORDER(6, "下单");

    private final int code;
    private final String desc;

    OrderOperateType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static OrderOperateType of(int code) {
        for (OrderOperateType t : values()) {
            if (t.code == code) {
                return t;
            }
        }
        return null;
    }
}
