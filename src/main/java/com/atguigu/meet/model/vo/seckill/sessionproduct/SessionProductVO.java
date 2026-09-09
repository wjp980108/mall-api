package com.atguigu.meet.model.vo.seckill.sessionproduct;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 场次商品关联VO（含场次名称 + 抢购商品信息，列表/详情/库存共用）
 */
@Data
@Schema(description = "场次商品关联数据")
public class SessionProductVO {

    @Schema(description = "关联ID")
    private Long id;

    @Schema(description = "场次ID")
    private Long sessionId;

    @Schema(description = "场次名称")
    private String sessionName;

    @Schema(description = "抢购商品ID")
    private Long goodsId;

    @Schema(description = "商品名称")
    private String goodsName;

    @Schema(description = "商品种类名称")
    private String categoryName;

    @Schema(description = "商品货号")
    private String goodsSn;

    @Schema(description = "商品缩略图URL")
    private String goodsThumb;

    @Schema(description = "商品缩略图存储平台")
    private String goodsThumbPlatform;

    @Schema(description = "商品售价")
    private BigDecimal price;

    /** 商品自身库存：寄售商品(t_consign_goods)无独立库存列，恒为 null，保留字段兼容前端 */
    @Schema(description = "商品自身库存(寄售商品无此概念，恒为null)")
    private Integer goodsStock;

    /** 该场次该商品的抢购库存（t_session_product.stock） */
    @Schema(description = "该场次该商品的抢购库存")
    private Integer stock;

    @Schema(description = "排序号")
    private Integer sort;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 是否售卖中（接口派生）：所属场次启用且当前时间在其抢购时间窗口内（含新会员提前窗口），售卖中时本关联禁止增删改 */
    @Schema(description = "是否售卖中(接口派生，售卖中禁止修改/删除)")
    private Boolean onSale;
}
