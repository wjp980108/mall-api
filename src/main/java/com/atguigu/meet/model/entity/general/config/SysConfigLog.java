package com.atguigu.meet.model.entity.general.config;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统配置变更日志 (sys_config_log)
 * <p>
 * 追溯配置变更记录：记录旧值、新值、操作人，参数出错可回查责任人。
 */
@Data
@TableName("sys_config_log")
@Schema(description = "系统配置变更日志数据")
public class SysConfigLog extends Model<SysConfigLog> {

    @TableId(type = IdType.AUTO)
    @Schema(description = "日志ID")
    private Long id;

    /** 配置分组编码 */
    @Schema(description = "配置分组编码")
    private String configGroup;

    /** 配置键 */
    @Schema(description = "配置键")
    private String configKey;

    /** 修改前的值 */
    @Schema(description = "修改前的值")
    private String oldValue;

    /** 修改后的值 */
    @Schema(description = "修改后的值")
    private String newValue;

    /** 操作人ID */
    @Schema(description = "操作人ID")
    private Long operatorId;

    /** 操作人名称 */
    @Schema(description = "操作人名称")
    private String operatorName;

    /** 操作时间 */
    @Schema(description = "操作时间")
    private LocalDateTime createTime;
}