package com.atguigu.meet.model.dto.general.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 系统配置项保存 DTO（分组全量保存时的单个配置项）
 */
@Data
public class SysConfigItemSaveDTO {

    /** 配置项ID（已存在的记录传入，新增项为空） */
    private Long id;

    @NotBlank(message = "配置键名不能为空")
    @Size(max = 128, message = "配置键名长度不能超过128")
    private String configKey;

    @NotBlank(message = "配置标题不能为空")
    @Size(max = 256, message = "配置标题长度不能超过256")
    private String configTitle;

    /** 配置值（字符串、数字、布尔、JSON统一按字符串提交） */
    private String configValue;

    /** 值类型: string/number/boolean/json */
    @NotBlank(message = "值类型不能为空")
    private String valueType;

    /** 同分组下排序号 */
    private Integer sort;

    /** 配置项备注说明 */
    @Size(max = 512, message = "备注长度不能超过512")
    private String remark;
}
