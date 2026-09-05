package com.atguigu.meet.model.dto.info.notice;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 公告新增DTO
 */
@Data
@Schema(description = "公告新增参数")
public class NoticeSaveDTO {

    @Schema(description = "公告标题", example = "系统维护通知", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "公告标题不能为空")
    private String title;

    @Schema(description = "公告内容", example = "<p>系统将于今晚维护</p>", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "公告内容不能为空")
    private String content;

    /** 排序，数值越大越靠前展示 */
    @Schema(description = "排序", example = "100")
    private Integer sort;

    /** 状态：false-禁用，true-启用 */
    @Schema(description = "状态", example = "true")
    private Boolean status;
}