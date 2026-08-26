package com.atguigu.meet.model.dto.info.banner;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 轮播图新增DTO
 */
@Data
public class BannerSaveDTO {

    @NotBlank(message = "轮播图地址不能为空")
    private String imgUrl;

    /** 轮播图存储平台:local-1/aliyun-oss-1等 */
    private String imgUrlPlatform;

    /** 轮播位置：home=首页 seckill=抢购 */
    @NotBlank(message = "轮播位置不能为空")
    private String position;

    /** 权重，越大越靠前 */
    private Integer sort;

    /** 跳转url */
    private String linkValue;

    /** 状态：false-禁用，true-启用 */
    private Boolean status;
}