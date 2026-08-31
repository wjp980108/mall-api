package com.atguigu.meet.model.vo.general.config;

import lombok.Data;

import java.io.Serializable;

/**
 * 系统配置项 VO（分组查询接口返回 / Redis 缓存存储结构）
 * <p>
 * 纯 POJO：Redis 中按分组缓存本 VO 的 JSON 数组，fastjson 反序列化无实体/时间格式负担。
 */
@Data
public class SysConfigVO implements Serializable {

    private Long id;

    /** 配置分组标识 */
    private String configGroup;

    /** 分组展示名称 */
    private String configGroupName;

    /** 配置键名: site.order_number */
    private String configKey;

    /** 配置标题(前端页面展示文字) */
    private String configTitle;

    /** 配置值(复杂类型为 JSON 字符串) */
    private String configValue;

    /** 值类型: string/number/boolean/json */
    private String valueType;

    /** 同分组下排序号 */
    private Integer sort;

    /** 配置项备注说明 */
    private String remark;
}
