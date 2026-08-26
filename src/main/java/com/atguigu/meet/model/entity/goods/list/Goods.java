package com.atguigu.meet.model.entity.goods.list;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.atguigu.meet.config.jackson.Integer01ToBooleanSerializer;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品实体（商品列表模块）
 */
@Data
@TableName("t_goods")
public class Goods extends Model<Goods> {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品名称 */
    private String goodsName;

    /** 商品种类名称 */
    private String categoryName;

    /** 商品货号/编码，唯一 */
    private String goodsSn;

    /** 商品缩略图URL */
    private String goodsThumb;

    /** 商品缩略图存储平台:local-1/aliyun-oss-1等 */
    private String goodsThumbPlatform;

    /** 商品售价 */
    private BigDecimal price;

    /** 库存数量 */
    private Integer stock;

    /** 销量 */
    private Integer sales;

    /** 商品状态 0=下架 1=已上架 */
    @JsonSerialize(using = Integer01ToBooleanSerializer.class)
    private Integer status = 0;

    /** 逻辑删除 0未删 1已删 */
    @JsonIgnore
    @TableLogic
    private Integer isDeleted = 0;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 创建人ID(管理员id) */
    private Long createBy;

    /** 更新人ID */
    private Long updateBy;
}
