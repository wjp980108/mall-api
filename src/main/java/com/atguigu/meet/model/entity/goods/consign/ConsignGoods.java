package com.atguigu.meet.model.entity.goods.consign;

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
 * 抢购托售商品实体（对应 t_consign_goods）
 * <p>
 * 业务状态流转：1挂卖中 -> 2已抢购待付款 -> 3等待确认付款 -> 4待处理 -> 5委托代卖 -> 6委托发货
 * 上下架：online_status 0下架 1上架
 */
@Data
@TableName("t_consign_goods")
public class ConsignGoods extends Model<ConsignGoods> {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 抢购区商品名称 */
    private String goodsName;

    /** 抢购区商品价格 */
    private BigDecimal goodsPrice;

    /** 本轮委托人ID，关联 sys_user.id */
    private Long memberId;

    /** 所属场次ID，关联 t_session.id */
    private Long sessionId;

    /** 商品缩略图URL */
    private String coverImg;

    /** 商品缩略图存储平台:local-1/aliyun-oss-1等 */
    private String coverImgPlatform;

    /** 商品详情图URL */
    private String detailImg;

    /** 商品详情图存储平台:local-1/aliyun-oss-1等 */
    private String detailImgPlatform;

    /** 商品详情富文本 */
    private String goodsDetail;

    /** 委托售卖次数（入库默认由 DB 列 DEFAULT 0 提供，禁止实例默认值：避免部分字段 updateById 时被静默清零） */
    private Integer saleTimes;

    /**
     * 商品业务状态
     * 1挂卖中 2已抢购待付款 3等待确认付款 4待处理 5委托代卖 6委托发货
     */
    private Integer goodsStatus;

    /** 上下架状态 0下架 1上架（同上，禁止实例默认值；新增时由 Service 显式兜底） */
    @JsonSerialize(using = Integer01ToBooleanSerializer.class)
    private Integer onlineStatus;

    /** 逻辑删除 0正常 1删除 */
    @JsonIgnore
    @TableLogic
    private Integer isDeleted = 0;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
