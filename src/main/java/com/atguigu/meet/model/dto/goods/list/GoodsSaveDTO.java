package com.atguigu.meet.model.dto.goods.list;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品新增DTO
 * <p>
 * 入参强校验：
 * - 非空校验（@NotBlank / @NotNull）
 * - 数字类型与范围校验（@DecimalMin / @Min）
 * - 防 XSS：通过 @Pattern 拒绝包含 < > 的输入
 */
@Data
@Schema(description = "商品新增参数")
public class GoodsSaveDTO {

    /** 商品名称 */
    @Schema(description = "商品名称", example = "iPhone 15 Pro", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "商品名称不能为空")
    @Size(max = 255, message = "商品名称长度不能超过255")
    @Pattern(regexp = "^[^<>]*$", message = "商品名称不能包含 < 或 > 字符")
    private String goodsName;

    /** 商品种类名称（暂时可传可不传） */
    @Schema(description = "商品种类", example = "手机")
    @Size(max = 128, message = "商品种类长度不能超过128")
    @Pattern(regexp = "^[^<>]*$", message = "商品种类不能包含 < 或 > 字符")
    private String categoryName;

    /** 商品货号/编码，唯一（可选，空则后端自动生成） */
    @Schema(description = "商品货号/编码", example = "IP15PRO001")
    @Size(max = 64, message = "商品货号长度不能超过64")
    @Pattern(regexp = "^[^<>]*$", message = "商品货号不能包含 < 或 > 字符")
    private String goodsSn;

    /** 商品缩略图URL */
    @Schema(description = "商品缩略图URL", example = "https://example.com/thumb.jpg")
    @Size(max = 512, message = "缩略图URL长度不能超过512")
    @Pattern(regexp = "^[^<>]*$", message = "缩略图URL不能包含 < 或 > 字符")
    private String goodsThumb;

    /** 商品缩略图存储平台:local-1/aliyun-oss-1等 */
    @Schema(description = "缩略图存储平台", example = "aliyun-oss-1")
    private String goodsThumbPlatform;

    /** 商品售价 */
    @Schema(description = "商品售价", example = "9999.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "商品售价不能为空")
    @DecimalMin(value = "0.00", message = "商品售价不能为负数")
    private BigDecimal price;

    /** 库存数量 */
    @Schema(description = "库存数量", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "库存数量不能为空")
    @Min(value = 0, message = "库存数量不能为负数")
    private Integer stock;

    /** 销量（不传默认 0） */
    @Schema(description = "销量", example = "0")
    @Min(value = 0, message = "销量不能为负数")
    private Integer sales;

    /** 商品状态 false=下架 true=已上架（不传默认 false 下架） */
    @Schema(description = "商品状态", example = "false")
    private Boolean status;
}