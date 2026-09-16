package com.atguigu.meet.model.vo.permission.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 简化用户 VO（上下级邀请关系核查用，仅含必要字段）
 */
@Data
@Schema(description = "简化用户数据（上下级关系核查用）")
public class SimpleUserVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "我的邀请码")
    private String inviteCode;

    @Schema(description = "状态 1启用 0禁用")
    private Boolean status;

    @Schema(description = "会员类型 0新会员 1老会员")
    private Integer memberType;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
