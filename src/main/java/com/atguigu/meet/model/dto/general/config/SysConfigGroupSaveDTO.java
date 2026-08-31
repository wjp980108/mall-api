package com.atguigu.meet.model.dto.general.config;

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
public class SysConfigGroupSaveDTO {

    /** 配置分组标识: base/member/pay/email */
    @NotBlank(message = "配置分组不能为空")
    @Size(max = 64, message = "配置分组长度不能超过64")
    private String configGroup;

    /** 分组展示名称（新增配置项时落库用） */
    @Size(max = 128, message = "分组名称长度不能超过128")
    private String configGroupName;

    /** 整个分组下全部配置项 */
    @NotEmpty(message = "配置项列表不能为空")
    @Valid
    private List<SysConfigItemSaveDTO> items;
}
