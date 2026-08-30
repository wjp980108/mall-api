package com.atguigu.meet.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户修改收货地址 DTO
 */
@Data
public class AddressUpdateDTO {

    @NotNull(message = "地址ID不能为空")
    private Long id;

    @NotBlank(message = "收货人姓名不能为空")
    private String receiverName;

    @NotBlank(message = "收货人手机号不能为空")
    private String receiverPhone;

    @NotBlank(message = "收货地址不能为空")
    private String address;

    /** 是否设为默认 1是 0否（可选） */
    private Integer isDefault;
}
