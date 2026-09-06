package com.atguigu.meet.model.entity.permission.invite;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 邀请明细流水（分佣核心：记录每次邀请注册行为，预留分佣字段）
 */
@Data
@TableName("sys_invite_record")
@Schema(description = "邀请明细流水数据")
public class SysInviteRecord extends Model<SysInviteRecord> {
    @TableId(type = IdType.AUTO)
    @Schema(description = "流水ID")
    private Long id;

    /** 使用的邀请码 */
    @Schema(description = "邀请码")
    private String inviteCode;

    /** 邀请人ID */
    @Schema(description = "邀请人ID")
    private Long inviterId;

    /** 被邀请人ID（新注册用户） */
    @Schema(description = "被邀请人ID")
    private Long inviteeId;

    /** 被邀请人手机号（冗余，便于查询） */
    @Schema(description = "被邀请人手机号")
    private String inviteePhone;

    /** 0已邀请待注册 1已注册 2已取消 */
    @Schema(description = "状态 0已邀请待注册 1已注册 2已取消")
    private Integer status = 1;

    /** 分佣金额（预留） */
    @Schema(description = "分佣金额")
    private BigDecimal commissionAmount = BigDecimal.ZERO;

    /** 分佣状态 0待结算 1已结算 2已取消（预留） */
    @Schema(description = "分佣状态 0待结算 1已结算 2已取消")
    private Integer commissionStatus = 0;

    /** 结算时间（预留） */
    @Schema(description = "结算时间")
    private LocalDateTime settleTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @JsonIgnore
    @TableLogic
    private Integer isDeleted = 0;
}