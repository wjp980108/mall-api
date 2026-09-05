package com.atguigu.meet.model.dto.seckill.session;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 抢购场次分页查询DTO
 */
@Data
@Schema(description = "抢购场次分页查询参数")
public class SessionPageQueryDTO {
    @Schema(description = "分页页码", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分页页码不能为空")
    private Integer pageNum;
    
    @Schema(description = "每页条数", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "每页条数不能为空")
    private Integer pageSize;

    /** 场次名称（模糊查询） */
    @Schema(description = "场次名称（模糊查询）", example = "上午场")
    private String sessionName;
    
    /** 场次状态 true开启 false关闭 */
    @Schema(description = "场次状态", example = "true")
    private Boolean sessionStatus;
}