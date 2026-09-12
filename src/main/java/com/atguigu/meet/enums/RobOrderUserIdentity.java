package com.atguigu.meet.enums;

import lombok.Getter;

/**
 * 抢购订单冲回用户身份枚举（积分不足提示用）
 * 1推荐人（冲回推荐奖） 2买家（冲回自购奖金）
 */
@Getter
public enum RobOrderUserIdentity {

    INVITER(1, "推荐人"),
    BUYER(2, "买家");

    private final int code;
    private final String desc;

    RobOrderUserIdentity(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
