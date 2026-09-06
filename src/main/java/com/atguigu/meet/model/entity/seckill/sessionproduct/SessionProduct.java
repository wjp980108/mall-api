package com.atguigu.meet.model.entity.seckill.sessionproduct;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 场次商品关联实体（对应 t_session_product）
 * <p>
 * 场次与抢购商品多对多关联：每行即「场次X - 商品Y - 库存Z」，
 * 库存行级挂在关联上，抢购时按本表 stock 扣减，与 t_goods.stock（商品自身库存）互不干扰。
 */
@Data
@TableName("t_session_product")
@Schema(description = "场次商品关联数据")
public class SessionProduct extends Model<SessionProduct> {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /** 场次ID，关联t_session.id */
    @Schema(description = "场次ID")
    private Long sessionId;

    /** 抢购商品ID，关联t_goods.id */
    @Schema(description = "抢购商品ID")
    private Long goodsId;

    /** 该场次该商品的抢购库存 */
    @Schema(description = "该场次该商品的抢购库存")
    private Integer stock;

    /** 排序号（场次内商品展示顺序） */
    @Schema(description = "排序号")
    private Integer sort;

    /** 逻辑删除 0未删 1已删 */
    @JsonIgnore
    @TableLogic
    private Integer isDeleted = 0;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
