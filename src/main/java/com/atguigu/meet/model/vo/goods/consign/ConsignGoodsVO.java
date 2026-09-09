package com.atguigu.meet.model.vo.goods.consign;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 抢购托售商品响应VO
 * <p>
 * 委托人信息通过 JOIN sys_user 实时获取，确保用户表数据更新后，列表/详情中的委托人信息同步更新。
 */
@Data
@Schema(description = "托售商品响应数据")
public class ConsignGoodsVO {
    @Schema(description = "商品ID")
    private Long id;
    @Schema(description = "商品名称")
    private String goodsName;
    @Schema(description = "商品价格")
    private BigDecimal goodsPrice;
    @Schema(description = "会员ID")
    private Long memberId;
    @Schema(description = "场次ID")
    private Long sessionId;
    @Schema(description = "封面图URL")
    private String coverImg;
    /** 商品缩略图存储平台:local-1/aliyun-oss-1等 */
    @Schema(description = "封面图存储平台")
    private String coverImgPlatform;
    @Schema(description = "详情图URL")
    private String detailImg;
    /** 商品详情图存储平台:local-1/aliyun-oss-1等 */
    @Schema(description = "详情图存储平台")
    private String detailImgPlatform;
    @Schema(description = "商品详情")
    private String goodsDetail;
    @Schema(description = "售出次数")
    private Integer saleTimes;
    /** 商品业务状态 1挂卖中 2已抢购待付款 3等待确认付款 4待处理 5委托代卖
     * @see com.atguigu.meet.enums.GoodsStatus
     */
    @Schema(description = "商品业务状态 1挂卖中 2已抢购待付款 3等待确认付款 4待处理 5委托代卖")
    private Integer goodsStatus;

    /** 商品业务状态中文名（由 Service 层通过枚举组装） */
    @Schema(description = "商品业务状态中文名")
    private String goodsStatusName;

    /** 委托状态 0未委托 1委托代卖中 @see com.atguigu.meet.enums.EntrustStatus */
    @Schema(description = "委托状态 0未委托 1委托代卖中")
    private Integer entrustStatus;
    /** 委托状态中文名（由 Service 层通过枚举组装） */
    @Schema(description = "委托状态中文名")
    private String entrustStatusName;

    /** 审核状态 0无需审核 1待审核 2审核通过 3审核驳回 @see com.atguigu.meet.enums.AuditStatus */
    @Schema(description = "审核状态 0无需审核 1待审核 2审核通过 3审核驳回")
    private Integer auditStatus;
    /** 审核状态中文名（由 Service 层通过枚举组装） */
    @Schema(description = "审核状态中文名")
    private String auditStatusName;

    /** 上下架状态 false下架 true上架 */
    @Schema(description = "上下架状态")
    private Boolean onlineStatus;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 委托人信息（JOIN sys_user 实时获取） */
    @Schema(description = "委托人信息")
    private ConsignorVO consignor;

    /** 场次名称（JOIN t_session 获取） */
    @Schema(description = "场次名称")
    private String sessionName;

    /** 是否可购买（场次开启且当前时间在抢购时间窗口内） */
    @Schema(description = "是否可购买")
    private Boolean canPurchase;

    /** 是否售卖中（接口派生）：商品存在于任一售卖中场次的关联中，售卖中时禁止编辑/删除/下架/状态流转/委托审核 */
    @Schema(description = "是否售卖中(接口派生，售卖中禁止写操作)")
    private Boolean onSale;

    /**
     * 委托人简要信息
     * 每次 JOIN sys_user 查询最新数据，用户表更新后自动同步
     */
    @Data
    @Schema(description = "委托人信息")
    public static class ConsignorVO {
        @Schema(description = "委托人ID")
        private Long id;
        @Schema(description = "用户名")
        private String username;
        @Schema(description = "昵称")
        private String nickname;
        @Schema(description = "手机号")
        private String phone;
        @Schema(description = "头像URL")
        private String avatar;
        /** 委托人头像存储平台:local-1/aliyun-oss-1等 */
        @Schema(description = "头像存储平台")
        private String avatarPlatform;
    }
}