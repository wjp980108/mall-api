package com.atguigu.meet.model.vo.roborder;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * H5 抢购商品详情VO（基于场次商品关联 t_session_product）
 * <p>
 * 商品详细信息 JOIN t_consign_goods 实时获取；抢购下单信息含下单锚点 sessionProductId、
 * 抢购库存、场次时间窗口、当前用户可抢状态与限购命中情况，
 * 派生字段口径与 {@code RobOrderService.placeOrder} 下单校验保持一致。
 */
@Data
@Schema(description = "抢购商品详情")
public class RobGoodsDetailVO {

    // ====================== 下单锚点（场次商品关联 t_session_product） ======================

    @Schema(description = "场次商品关联ID（抢购下单入参 sessionProductId）")
    private Long sessionProductId;

    @Schema(description = "场次ID")
    private Long sessionId;

    @Schema(description = "场次名称")
    private String sessionName;

    @Schema(description = "场次状态 1开启 0关闭")
    private Integer sessionStatus;

    @Schema(description = "场次是否开启")
    private Boolean sessionOpen;

    @Schema(description = "该场次该商品的抢购库存")
    private Integer stock;

    @Schema(description = "是否售罄（抢购库存<=0）")
    private Boolean soldOut;

    // ====================== 商品详细信息（t_consign_goods） ======================

    @Schema(description = "商品ID")
    private Long goodsId;

    @Schema(description = "商品名称")
    private String goodsName;

    @Schema(description = "商品售价")
    private BigDecimal price;

    @Schema(description = "商品封面图URL")
    private String coverImg;

    @Schema(description = "商品封面图存储平台")
    private String coverImgPlatform;

    @Schema(description = "商品详情图URL")
    private String detailImg;

    @Schema(description = "商品详情图存储平台")
    private String detailImgPlatform;

    @Schema(description = "商品详情富文本")
    private String goodsDetail;

    @Schema(description = "已售出次数")
    private Integer saleTimes;

    @Schema(description = "商品业务状态 1挂卖中 2已抢购待付款 3等待确认付款 4待处理 5委托代卖")
    private Integer goodsStatus;

    @Schema(description = "上下架状态 0下架 1上架")
    private Integer onlineStatus;

    @Schema(description = "商品是否可售（上架且业务状态为挂卖中/委托代卖）")
    private Boolean goodsOnline;

    // ====================== 抢购下单信息 ======================

    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "每日抢购开始时间")
    private LocalTime rushStartTime;

    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "每日抢购结束时间")
    private LocalTime rushEndTime;

    @Schema(description = "当前是否可抢购（严格版：场次开启+商品可售+在抢购时间窗口内(新会员双开关开启含提前进场)+库存>0+未命中限购，五项全满足才为true；false 时可按 sessionOpen/goodsOnline/soldOut/hasRushed 定位原因）")
    private Boolean canPurchase;

    @Schema(description = "限购规则 0不限购 1同场次限购一次 2当天限购一次")
    private Integer limitRule;

    @Schema(description = "限购规则描述")
    private String limitRuleName;

    @Schema(description = "当前登录用户是否已命中限购（未登录或不限购时为false）")
    private Boolean hasRushed;
}
