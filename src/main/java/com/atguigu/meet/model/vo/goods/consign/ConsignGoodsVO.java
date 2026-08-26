package com.atguigu.meet.model.vo.goods.consign;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 抢购托售商品响应VO
 * <p>
 * 委托人信息通过 JOIN sys_user 实时获取，确保用户表数据更新后，列表/详情中的委托人信息同步更新。
 */
@Data
public class ConsignGoodsVO {
    private Long id;
    private String goodsName;
    private BigDecimal goodsPrice;
    private Long memberId;
    private Long sessionId;
    private String coverImg;
    /** 商品缩略图存储平台:local-1/aliyun-oss-1等 */
    private String coverImgPlatform;
    private String detailImg;
    /** 商品详情图存储平台:local-1/aliyun-oss-1等 */
    private String detailImgPlatform;
    private String goodsDetail;
    private Integer saleTimes;
    /** 商品业务状态 1挂卖中 2已抢购待付款 3等待确认付款 4待处理 5委托代卖 6委托发货 */
    private Integer goodsStatus;
    /** 上下架状态 false下架 true上架 */
    private Boolean onlineStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 委托人信息（JOIN sys_user 实时获取） */
    private ConsignorVO consignor;

    /** 场次名称（JOIN t_session 获取） */
    private String sessionName;

    /**
     * 委托人简要信息
     * 每次 JOIN sys_user 查询最新数据，用户表更新后自动同步
     */
    @Data
    public static class ConsignorVO {
        private Long id;
        private String username;
        private String nickname;
        private String phone;
        private String avatar;
        /** 委托人头像存储平台:local-1/aliyun-oss-1等 */
        private String avatarPlatform;
    }
}
