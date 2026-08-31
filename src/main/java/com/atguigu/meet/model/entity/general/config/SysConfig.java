package com.atguigu.meet.model.entity.general.config;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
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
public class SysConfig extends Model<SysConfig> {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配置分组标识: base/member/pay/email */
    private String configGroup;

    /** 分组展示名称: 基础配置、会员配置、支付配置 */
    private String configGroupName;

    /** 配置键名，对应页面变量名: site.order_number */
    private String configKey;

    /** 配置标题(前端页面展示文字) */
    private String configTitle;

    /** 配置值(字符串、数字、布尔、JSON数组、JSON对象统一存字符串) */
    private String configValue;

    /** 值类型: string/number/boolean/json */
    private String valueType;

    /** 同分组下排序号，控制页面从上到下展示顺序 */
    private Integer sort;

    /** 配置项备注说明 */
    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
