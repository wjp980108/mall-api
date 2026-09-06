package com.atguigu.meet.model.entity.permission.role;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 角色-菜单关联(多对多：一个角色绑定多个菜单/按钮)
 */
@Data
@TableName("sys_role_menu")
@Schema(description = "角色菜单关联数据")
public class SysRoleMenu extends Model<SysRoleMenu> {
    @TableId(type = IdType.AUTO)
    @Schema(description = "关联ID")
    private Long id;

    /** 角色ID(sys_role.id) */
    @Schema(description = "角色ID")
    private Long roleId;

    /** 菜单/权限ID(sys_menu.id) */
    @Schema(description = "菜单/权限ID")
    private Long menuId;
}