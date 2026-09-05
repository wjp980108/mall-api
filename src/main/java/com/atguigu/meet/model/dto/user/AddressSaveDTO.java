package com.atguigu.meet.model.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户新增收货地址 DTO
 */
@Data
@Schema(description = "用户新增收货地址参数")
public class AddressSaveDTO {

    @Schema(description = "收货人姓名", example = "张三", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "收货人姓名不能为空")
    private String receiverName;

    @Schema(description = "收货人手机号", example = "13800138000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "收货人手机号不能为空")
    private String receiverPhone;

    @Schema(description = "收货地址", example = "北京市朝阳区xxx街道xxx号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "收货地址不能为空")
    private String address;

    /** 是否设为默认 1是 0否（可选，不传视为0） */
    @Schema(description = "是否设为默认", example = "1", allowableValues = {"0", "1"})
    private Integer isDefault;
}