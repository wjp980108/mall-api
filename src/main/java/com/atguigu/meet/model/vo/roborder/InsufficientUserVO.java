package com.atguigu.meet.model.vo.roborder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 取消/转移订单积分不足提示 - 单个冲回用户信息
 */
@Data
@Schema(description = "冲回用户信息（含可用积分是否不足标志）")
public class InsufficientUserVO {

    /** 用户ID */
    @Schema(description = "用户ID")
    private Long userId;

    /** 用户昵称（无昵称时为用户名，查不到用户时为 null） */
    @Schema(description = "用户昵称（无昵称时为用户名，用户不存在时为 null）")
    private String nickname;

    /** 用户手机号（用户不存在时为 null） */
    @Schema(description = "用户手机号（用户不存在时为 null）")
    private String phone;

    /** 订单身份：1=推荐人（冲回推荐奖） 2=买家（冲回自购奖金） */
    @Schema(description = "订单身份：1=推荐人（冲回推荐奖） 2=买家（冲回自购奖金）")
    private Integer identityType;

    /** 身份名称：推荐人 / 买家 */
    @Schema(description = "身份名称：推荐人 / 买家")
    private String identityName;

    /** 该用户可用积分是否不足 */
    @Schema(description = "该用户可用积分是否不足：待冲回金额大于当前可用积分余额")
    private Boolean pointsInsufficient;
}
