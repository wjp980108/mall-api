package com.atguigu.meet.model.dto.seckill.session;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 抢购场次分页查询DTO
 */
@Data
public class SessionPageQueryDTO {
    @NotNull(message = "分页页码不能为空")
    private Integer pageNum;
    @NotNull(message = "每页条数不能为空")
    private Integer pageSize;

    /** 场次名称（模糊查询） */
    private String sessionName;
    /** 场次状态 true开启 false关闭 */
    private Boolean sessionStatus;
}
