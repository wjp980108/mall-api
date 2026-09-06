package com.atguigu.meet.model.entity.permission.userRole;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户-角色关联
 */
@Data
@TableName("sys_user_role")
@Schema(description = "用户角色关联数据")
public class SysUserRole extends Model<SysUserRole> {
    @TableId(type = IdType.AUTO)
    @Schema(description = "关联ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "角色ID")
    private Long roleId;
}