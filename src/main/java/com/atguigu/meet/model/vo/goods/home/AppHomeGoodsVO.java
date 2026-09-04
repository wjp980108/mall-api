package com.atguigu.meet.model.vo.goods.home;

import lombok.Data;

import java.math.BigDecimal;

/**
 * C 端首页商品 VO（推荐 / 搜索，基于 t_goods）
 * <p>仅暴露 C 端展示字段，不透出后台管理字段（createBy/updateBy 等）。
 */
@Data
public class AppHomeGoodsVO {

    /** 商品ID */
    private Long id;

    /** 商品名称 */
    private String goodsName;

    /** 商品种类名称 */
    private String categoryName;

    /** 商品缩略图URL */
    private String goodsThumb;

    /** 商品售价 */
    private BigDecimal price;

    /** 库存数量 */
    private Integer stock;

    /** 销量 */
    private Integer sales;
}
