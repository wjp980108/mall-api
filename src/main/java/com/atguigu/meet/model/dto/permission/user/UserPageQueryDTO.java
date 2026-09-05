package com.atguigu.meet.model.dto.permission.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Description
 * @Date 2026-05-09 16:46
 */
@Data
@Schema(description = "用户分页查询参数")
public class UserPageQueryDTO {
    // 分页参数
    @Schema(description = "分页页码", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "分页页码不能为空")
    private Integer pageNum;
    
    @Schema(description = "每页条数", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "每页条数不能为空")
    private Integer pageSize;

    // 业务查询条件
    @Schema(description = "用户名（模糊查询）", example = "admin")
    private String username;
    
    @Schema(description = "年龄", example = "25")
    private Integer age;
    
    @Schema(description = "手机号", example = "13800138000")
    private String phone;
}