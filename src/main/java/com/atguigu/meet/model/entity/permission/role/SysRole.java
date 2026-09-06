package com.atguigu.meet.model.entity.permission.role;

import com.atguigu.meet.model.entity.permission.menu.SysMenu;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
import java.util.List;

/**
 * 系统角色（权限分组载体：管理员、普通用户、运营等）
 */
@Data
@TableName("sys_role")
@Schema(description = "角色数据")
public class SysRole extends Model<SysRole> {
    @TableId(type = IdType.AUTO)
    @Schema(description = "角色ID")
    private Long id;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色编码")
    private String roleCode;

    @JsonSerialize(using = Integer01ToBooleanSerializer.class)
    @Schema(description = "状态 0禁用 1启用")
    private Integer status = 1;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @JsonIgnore
    @TableLogic
    private Integer isDeleted = 0;

    /** 角色拥有的菜单/权限（非数据库字段） */
    @TableField(exist = false)
    @Schema(description = "菜单权限列表")
    private List<SysMenu> menus;
}