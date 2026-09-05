package com.atguigu.meet.model.dto.general.config;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 系统配置分组全量保存 DTO
 * <p>
 * 前端点击【确定】一次性提交整个分组下全部配置项数组，
 * 禁止单条配置单独保存（保证分组页面状态一致，避免并发覆盖）。
 */
@Data
@Schema(description = "系统配置分组全量保存参数")
public class SysConfigGroupSaveDTO {

    /** 配置分组标识: base/member/pay/email */
    @Schema(description = "配置分组", example = "base", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "配置分组不能为空")
    @Size(max = 64, message = "配置分组长度不能超过64")
    private String configGroup;

    /** 分组展示名称（新增配置项时落库用） */
    @Schema(description = "分组名称", example = "基础配置")
    @Size(max = 128, message = "分组名称长度不能超过128")
    private String configGroupName;

    /** 整个分组下全部配置项 */
    @Schema(description = "配置项列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "配置项列表不能为空")
    @Valid
    private List<SysConfigItemSaveDTO> items;
}