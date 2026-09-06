package com.atguigu.meet.model.entity.general.config;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统动态配置 (sys_config)
 * <p>
 * 设计要点：
 * - uk_group_key(config_group, config_key) 数据库唯一索引保证分组内键唯一；
 * - config_value 统一存字符串，复选框/键值表格等复杂类型存标准 JSON 字符串；
 * - 读取走 Redis 缓存(key: sys_config:group:{group})，缓存失效回查数据库。
 */
@Data
@TableName("sys_config")
@Schema(description = "系统配置数据")
public class SysConfig extends Model<SysConfig> {

    @TableId(type = IdType.AUTO)
    @Schema(description = "配置ID")
    private Long id;

    /** 配置分组标识: base/member/pay/email */
    @Schema(description = "配置分组标识")
    private String configGroup;

    /** 分组展示名称: 基础配置、会员配置、支付配置 */
    @Schema(description = "分组展示名称")
    private String configGroupName;

    /** 配置键名，对应页面变量名: site.order_number */
    @Schema(description = "配置键名")
    private String configKey;

    /** 配置标题(前端页面展示文字) */
    @Schema(description = "配置标题")
    private String configTitle;

    /** 配置值(字符串、数字、布尔、JSON数组、JSON对象统一存字符串) */
    @Schema(description = "配置值")
    private String configValue;

    /** 值类型: string/number/boolean/json */
    @Schema(description = "值类型")
    private String valueType;

    /** 同分组下排序号，控制页面从上到下展示顺序 */
    @Schema(description = "排序号")
    private Integer sort;

    /** 配置项备注说明 */
    @Schema(description = "备注说明")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}