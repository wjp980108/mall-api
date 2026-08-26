package com.atguigu.meet.model.dto.goods.list;

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
public class GoodsSaveDTO {

    /** 商品名称 */
    @NotBlank(message = "商品名称不能为空")
    @Size(max = 255, message = "商品名称长度不能超过255")
    @Pattern(regexp = "^[^<>]*$", message = "商品名称不能包含 < 或 > 字符")
    private String goodsName;

    /** 商品种类名称（暂时可传可不传） */
    @Size(max = 128, message = "商品种类长度不能超过128")
    @Pattern(regexp = "^[^<>]*$", message = "商品种类不能包含 < 或 > 字符")
    private String categoryName;

    /** 商品货号/编码，唯一（可选，空则后端自动生成） */
    @Size(max = 64, message = "商品货号长度不能超过64")
    @Pattern(regexp = "^[^<>]*$", message = "商品货号不能包含 < 或 > 字符")
    private String goodsSn;

    /** 商品缩略图URL */
    @Size(max = 512, message = "缩略图URL长度不能超过512")
    @Pattern(regexp = "^[^<>]*$", message = "缩略图URL不能包含 < 或 > 字符")
    private String goodsThumb;

    /** 商品缩略图存储平台:local-1/aliyun-oss-1等 */
    private String goodsThumbPlatform;

    /** 商品售价 */
    @NotNull(message = "商品售价不能为空")
    @DecimalMin(value = "0.00", message = "商品售价不能为负数")
    private BigDecimal price;

    /** 库存数量 */
    @NotNull(message = "库存数量不能为空")
    @Min(value = 0, message = "库存数量不能为负数")
    private Integer stock;

    /** 商品状态 false=下架 true=已上架（不传默认 false 下架） */
    private Boolean status;
}
