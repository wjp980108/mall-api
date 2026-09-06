package com.atguigu.meet.model.entity.permission.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.atguigu.meet.config.jackson.String01ToBooleanSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.apache.ibatis.type.JdbcType;

import java.time.LocalDate;
import java.time.LocalDateTime;


/**
 * @Description
 * @Date 2026-08-13 15:15
 */
@Data
@TableName("sys_user")
@Schema(description = "用户数据")
public class SysUser extends Model<SysUser> {
    @TableId(type = IdType.AUTO)
    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @JsonIgnore
    private String password;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "年龄")
    private Integer age;

    @Schema(description = "性别 0未知 1男 2女")
    private Integer gender = 0;

    @Schema(description = "头像URL")
    private String avatar;

    /** 头像存储平台:local-1/aliyun-oss-1等 */
    @Schema(description = "头像存储平台")
    private String avatarPlatform;

    @Schema(description = "生日")
    private LocalDate birthday;

    @TableField(jdbcType = JdbcType.INTEGER)
    @JsonSerialize(using = String01ToBooleanSerializer.class)
    @Schema(description = "状态")
    private String status = "1";

    /** 邀请人ID（sys_user.id） */
    @Schema(description = "邀请人ID")
    private Long inviterId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @JsonIgnore
    @TableLogic
    private Integer isDeleted = 0;

}