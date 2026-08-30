package com.atguigu.meet.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户新增收货地址 DTO
 */
@Data
public class AddressSaveDTO {

    @NotBlank(message = "收货人姓名不能为空")
    private String receiverName;

    @NotBlank(message = "收货人手机号不能为空")
    private String receiverPhone;

    @NotBlank(message = "收货地址不能为空")
    private String address;

    /** 是否设为默认 1是 0否（可选，不传视为0） */
    private Integer isDefault;
}
