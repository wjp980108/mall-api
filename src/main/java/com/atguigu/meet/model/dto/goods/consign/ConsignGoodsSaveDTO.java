package com.atguigu.meet.model.dto.goods.consign;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 抢购托售商品新增DTO
 * <p>
 * 入参强校验：非空 / 数字范围 / 防 XSS（@Pattern 拒绝包含 < > 的输入）
 */
@Data
@Schema(description = "抢购托售商品新增参数")
public class ConsignGoodsSaveDTO {

    /** 抢购区商品名称 */
    @Schema(description = "商品名称", example = "iPhone 15 Pro", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "商品名称不能为空")
    @Size(max = 255, message = "商品名称长度不能超过255")
    @Pattern(regexp = "^[^<>]*$", message = "商品名称不能包含 < 或 > 字符")
    private String goodsName;

    /** 抢购区商品价格 */
    @Schema(description = "商品价格", example = "9999.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.00", message = "商品价格不能为负数")
    private BigDecimal goodsPrice;

    /** 本轮委托人ID，关联 sys_user.id */
    @Schema(description = "委托人ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "委托人ID不能为空")
    @Min(value = 1, message = "委托人ID不合法")
    private Long memberId;

    /** 所属场次ID，关联 t_session.id */
    @Schema(description = "场次ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "场次ID不能为空")
    @Min(value = 1, message = "场次ID不合法")
    private Long sessionId;

    /** 商品缩略图URL */
    @Schema(description = "商品缩略图URL", example = "https://example.com/cover.jpg")
    @Size(max = 500, message = "缩略图URL长度不能超过500")
    @Pattern(regexp = "^[^<>]*$", message = "缩略图URL不能包含 < 或 > 字符")
    private String coverImg;

    /** 商品缩略图存储平台:local-1/aliyun-oss-1等 */
    @Schema(description = "缩略图存储平台", example = "aliyun-oss-1")
    private String coverImgPlatform;

    /** 商品详情图URL */
    @Schema(description = "商品详情图URL", example = "https://example.com/detail.jpg")
    @Size(max = 500, message = "详情图URL长度不能超过500")
    @Pattern(regexp = "^[^<>]*$", message = "详情图URL不能包含 < 或 > 字符")
    private String detailImg;

    /** 商品详情图存储平台:local-1/aliyun-oss-1等 */
    @Schema(description = "详情图存储平台", example = "aliyun-oss-1")
    private String detailImgPlatform;

    /** 商品详情富文本 */
    @Schema(description = "商品详情富文本", example = "<p>商品详情</p>")
    private String goodsDetail;

    /**
     * 商品业务状态
     * 1挂卖中 2已抢购待付款 3等待确认付款 4待处理 5委托代卖
     * 不传默认 1 挂卖中
     */
    @Schema(description = "商品业务状态：1挂卖中 2已抢购待付款 3等待确认付款 4待处理 5委托代卖", example = "1", allowableValues = {"1", "2", "3", "4", "5"})
    @Min(value = 1, message = "业务状态取值范围 1-5")
    @Max(value = 5, message = "业务状态取值范围 1-5")
    private Integer goodsStatus;

    /** 上下架状态 false下架 true上架（不传默认 false 下架） */
    @Schema(description = "上下架状态", example = "false")
    private Boolean onlineStatus;
}