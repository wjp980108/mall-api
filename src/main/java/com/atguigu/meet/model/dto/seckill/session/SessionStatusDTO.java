package com.atguigu.meet.model.dto.seckill.session;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 场次启用/禁用DTO
 */
@Data
public class SessionStatusDTO {
    @NotNull(message = "场次ID不能为空")
    private Long id;

    /** 目标状态 true启用 false禁用 */
    @NotNull(message = "目标状态不能为空")
    private Boolean status;
}
