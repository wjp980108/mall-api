package com.atguigu.meet.model.dto.general.config;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 系统配置项保存 DTO（分组全量保存时的单个配置项）
 */
@Data
@Schema(description = "系统配置项保存参数")
public class SysConfigItemSaveDTO {

    /** 配置项ID（已存在的记录传入，新增项为空） */
    @Schema(description = "配置项ID", example = "1")
    private Long id;

    @Schema(description = "配置键名", example = "site_name", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "配置键名不能为空")
    @Size(max = 128, message = "配置键名长度不能超过128")
    private String configKey;

    @Schema(description = "配置标题", example = "站点名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "配置标题不能为空")
    @Size(max = 256, message = "配置标题长度不能超过256")
    private String configTitle;

    /** 配置值（字符串、数字、布尔、JSON统一按字符串提交） */
    @Schema(description = "配置值", example = "我的网站")
    private String configValue;

    /** 值类型: string/number/boolean/json */
    @Schema(description = "值类型", example = "string", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"string", "number", "boolean", "json"})
    @NotBlank(message = "值类型不能为空")
    private String valueType;

    /** 同分组下排序号 */
    @Schema(description = "排序号", example = "100")
    private Integer sort;

    /** 配置项备注说明 */
    @Schema(description = "备注说明", example = "站点名称配置")
    @Size(max = 512, message = "备注长度不能超过512")
    private String remark;
}