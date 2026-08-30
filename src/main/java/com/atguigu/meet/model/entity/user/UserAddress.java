package com.atguigu.meet.model.entity.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.atguigu.meet.config.jackson.Integer01ToBooleanSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户收货地址簿实体（对应 t_user_address）
 */
@Data
@TableName("t_user_address")
public class UserAddress extends Model<UserAddress> {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属用户ID 关联sys_user.id */
    private Long userId;

    /** 收货人姓名 */
    private String receiverName;

    /** 收货人手机号 */
    private String receiverPhone;

    /** 收货地址完整拼接字符串 */
    private String address;

    /** 是否默认 0否 1是（禁止实例默认值：避免 updateById 时被静默清零，由 DB 列 DEFAULT 0 兜底） */
    @JsonSerialize(using = Integer01ToBooleanSerializer.class)
    private Integer isDefault;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 逻辑删除 0未删 1已删 */
    @JsonIgnore
    @TableLogic
    private Integer isDeleted = 0;
}
