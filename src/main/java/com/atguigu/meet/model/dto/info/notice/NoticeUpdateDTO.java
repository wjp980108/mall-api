package com.atguigu.meet.model.dto.info.notice;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 公告修改DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "公告修改参数")
public class NoticeUpdateDTO extends NoticeSaveDTO {
    @Schema(description = "公告ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "公告ID不能为空")
    private Long id;
}