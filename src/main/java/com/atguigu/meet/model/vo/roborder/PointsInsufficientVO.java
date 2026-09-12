package com.atguigu.meet.model.vo.roborder;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 取消/转移订单积分不足提示 VO（data 非空即需要二次确认）
 */
@Data
@Schema(description = "积分不足提示数据（非空表示存在可用积分冲回缺口，需用户二次确认）；users 按冲回用户逐个返回信息与不足标志")
public class PointsInsufficientVO {

    /** 冲回用户信息列表（仅含本单存在可用积分冲回项的用户） */
    @Schema(description = "冲回用户信息列表：每个存在可用积分冲回项的用户一项，含 userId/nickname/phone/pointsInsufficient")
    private List<InsufficientUserVO> users;
}
