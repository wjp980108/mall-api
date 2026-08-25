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
public class SysUser extends Model<SysUser> {
    @TableId(type = IdType.AUTO) // 关键！指定主键自增
    private Long id;

    private String username;

    @JsonIgnore
    private String password;

    private String nickname;

    private String email;

    private String phone;

    private Integer age;

    private Integer gender = 0;

    private String avatar;

    private LocalDate birthday;

    @TableField(jdbcType = JdbcType.INTEGER)
    @JsonSerialize(using = String01ToBooleanSerializer.class)
    private String status = "1";

    /** 邀请人ID（sys_user.id） */
    private Long inviterId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @JsonIgnore
    @TableLogic
    private Integer isDeleted = 0;

}