package com.atguigu.meet.model.dto.info.banner;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 轮播图新增DTO
 */
@Data
@Schema(description = "轮播图新增参数")
public class BannerSaveDTO {

    @Schema(description = "轮播图地址", example = "https://example.com/banner.jpg", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "轮播图地址不能为空")
    private String imgUrl;

    /** 轮播图存储平台:local-1/aliyun-oss-1等 */
    @Schema(description = "轮播图存储平台", example = "aliyun-oss-1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "轮播图存储平台不能为空")
    private String imgUrlPlatform;

    /** 轮播位置：home=首页 seckill=抢购 */
    @Schema(description = "轮播位置", example = "home", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"home", "seckill"})
    @NotBlank(message = "轮播位置不能为空")
    private String position;

    /** 权重，越大越靠前 */
    @Schema(description = "权重", example = "100")
    private Integer sort;

    /** 跳转url */
    @Schema(description = "跳转链接", example = "https://example.com/detail/1")
    private String linkValue;

    /** 状态：false-禁用，true-启用 */
    @Schema(description = "状态", example = "true")
    private Boolean status;
}