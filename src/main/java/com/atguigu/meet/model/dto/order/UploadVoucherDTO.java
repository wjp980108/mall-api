package com.atguigu.meet.model.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 待付款订单上传支付凭证 DTO
 */
@Data
public class UploadVoucherDTO {

    @NotNull(message = "订单ID不能为空")
    private Long id;

    /** 支付凭证图片地址 */
    @NotBlank(message = "支付凭证不能为空")
    private String payVoucherUrl;

    /** 支付凭证存储平台:local-1/aliyun-oss-1等 */
    private String payVoucherPlatform;
}
