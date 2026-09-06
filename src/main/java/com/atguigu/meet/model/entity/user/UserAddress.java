package com.atguigu.meet.model.entity.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.atguigu.meet.config.jackson.Integer01ToBooleanSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户收货地址簿实体（对应 t_user_address）
 */
@Data
@TableName("t_user_address")
@Schema(description = "用户收货地址")
public class UserAddress extends Model<UserAddress> {

    @TableId(type = IdType.AUTO)
    @Schema(description = "地址ID")
    private Long id;

    /** 所属用户ID 关联sys_user.id */
    @Schema(description = "所属用户ID")
    private Long userId;

    /** 收货人姓名 */
    @Schema(description = "收货人姓名")
    private String receiverName;

    /** 收货人手机号 */
    @Schema(description = "收货人手机号")
    private String receiverPhone;

    /** 收货地址完整拼接字符串 */
    @Schema(description = "收货地址")
    private String address;

    /** 是否默认 0否 1是（禁止实例默认值：避免 updateById 时被静默清零，由 DB 列 DEFAULT 0 兜底） */
    @JsonSerialize(using = Integer01ToBooleanSerializer.class)
    @Schema(description = "是否默认 0否 1是")
    private Integer isDefault;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除 0未删 1已删 */
    @JsonIgnore
    @TableLogic
    private Integer isDeleted = 0;
}