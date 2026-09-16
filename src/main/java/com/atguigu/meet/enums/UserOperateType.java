package com.atguigu.meet.enums;

import lombok.Getter;

/**
 * 用户操作类型枚举（对应 sys_user_operate_log.operate_type tinyint）
 * <p>
 * 对齐项目约定：code=tinyint 入库、desc=中文描述（列表展示/备注用）。
 * 与 {@link OrderOperateType} / {@link GoodsOperateType} 保持同一风格。
 */
@Getter
public enum UserOperateType {

    CREATE(1, "创建用户"),
    UPDATE(2, "编辑用户"),
    ENABLE(3, "启用用户"),
    DISABLE(4, "禁用用户"),
    DELETE(5, "删除用户"),
    TO_OLD_MEMBER(6, "转老会员"),
    SYSTEM_AUTO_DISABLE(7, "到期未转化自动禁用");

    private final int code;
    private final String desc;

    UserOperateType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static UserOperateType of(int code) {
        for (UserOperateType t : values()) {
            if (t.code == code) {
                return t;
            }
        }
        return null;
    }
}
