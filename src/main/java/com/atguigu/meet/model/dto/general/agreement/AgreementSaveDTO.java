package com.atguigu.meet.model.dto.general.agreement;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户协议保存DTO（表内仅一条生效记录，新增/修改共用）
 */
@Data
@Schema(description = "用户协议保存参数")
public class AgreementSaveDTO {

    /** 协议标题 */
    @Schema(description = "协议标题", example = "用户注册协议", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "协议标题不能为空")
    @Size(max = 200, message = "协议标题不能超过200字")
    private String title;

    /** 协议富文本内容(html) */
    @Schema(description = "协议内容", example = "<h1>用户注册协议</h1>", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "协议内容不能为空")
    private String content;
}