package com.atguigu.meet.model.entity.permission.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户操作审计日志实体
 * 记录管理端用户写操作与定时任务系统自动禁用流水
 * <p>
 * operate_type 存数字 code（1创建用户 2编辑用户 3启用用户 4禁用用户 5删除用户 6转老会员 7到期未转化自动禁用）
 * operate_desc 存中文描述（用于列表展示，避免每次解析枚举）
 * 对齐 t_order_operate_log 的数据模型；定时任务系统操作 operate_user_id=NULL、operate_user_name='SYSTEM'
 */
@Data
@TableName("sys_user_operate_log")
@Schema(description = "用户操作日志数据")
public class UserOperateLog extends Model<UserOperateLog> {

    @TableId(type = IdType.AUTO)
    @Schema(description = "日志ID")
    private Long id;

    /** 目标用户ID 关联sys_user.id */
    @Schema(description = "目标用户ID")
    private Long userId;

    /** 操作前账号状态（非状态类操作为null） */
    @Schema(description = "操作前账号状态")
    private Integer beforeStatus;

    /** 操作后账号状态（非状态类操作为null） */
    @Schema(description = "操作后账号状态")
    private Integer afterStatus;

    /**
     * 操作类型数字编码：1创建用户 2编辑用户 3启用用户 4禁用用户 5删除用户 6转老会员 7到期未转化自动禁用
     * @see com.atguigu.meet.enums.UserOperateType#getCode()
     */
    @Schema(description = "操作类型 1创建用户 2编辑用户 3启用用户 4禁用用户 5删除用户 6转老会员 7到期未转化自动禁用")
    private Integer operateType;

    /**
     * 操作类型中文描述（冗余展示列）
     * @see com.atguigu.meet.enums.UserOperateType#getDesc()
     */
    @Schema(description = "操作类型描述")
    private String operateDesc;

    /** 操作人管理员ID 关联sys_user.id；定时任务系统操作为null */
    @Schema(description = "操作人ID")
    private Long operateUserId;

    /** 操作人名称快照；定时任务系统操作固定为SYSTEM */
    @Schema(description = "操作人名称")
    private String operateUserName;

    /** 操作备注（关键入参JSON摘要） */
    @Schema(description = "操作备注")
    private String remark;

    @JsonIgnore
    @TableLogic
    private Integer isDeleted = 0;

    @Schema(description = "操作时间")
    private LocalDateTime createTime;
}
