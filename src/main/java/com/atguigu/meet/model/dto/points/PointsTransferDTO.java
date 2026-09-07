package com.atguigu.meet.model.dto.points;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 积分转让 DTO（C 端：输入对方手机号 + 转让数量）
 */
@Data
@Schema(description = "积分转让参数")
public class PointsTransferDTO {

    /** 对方手机号 */
    @Schema(description = "对方手机号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "对方手机号不能为空")
    private String phone;

    /** 转让积分数量（仅可用积分） */
    @Schema(description = "转让积分数量（仅可用积分）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "转让数量不能为空")
    @DecimalMin(value = "0.01", message = "转让数量必须大于0")
    private BigDecimal amount;
}
