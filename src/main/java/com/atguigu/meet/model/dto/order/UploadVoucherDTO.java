package com.atguigu.meet.model.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 待付款订单上传支付凭证 DTO
 */
@Data
@Schema(description = "订单上传支付凭证参数")
public class UploadVoucherDTO {

    @Schema(description = "订单ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "订单ID不能为空")
    private Long id;

    /** 支付凭证图片地址 */
    @Schema(description = "支付凭证图片URL", example = "https://example.com/voucher.jpg", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "支付凭证不能为空")
    private String payVoucherUrl;

    /** 支付凭证存储平台:local-1/aliyun-oss-1等 */
    @Schema(description = "支付凭证存储平台", example = "aliyun-oss-1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "支付凭证存储平台不能为空")
    private String payVoucherPlatform;
}