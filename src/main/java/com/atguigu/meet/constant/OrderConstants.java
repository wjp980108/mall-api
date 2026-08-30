package com.atguigu.meet.constant;

/**
 * 订单模块常量
 */
public final class OrderConstants {

    private OrderConstants() {}

    /** 待付款超时时间（分钟）：下单后 N 分钟未上传凭证视为超时 */
    public static final int PAY_TIMEOUT_MINUTES = 30;

    /** 托售商品业务状态：1挂卖中 */
    public static final int GOODS_STATUS_ON_SALE = 1;
    /** 托售商品业务状态：2已抢购待付款 */
    public static final int GOODS_STATUS_RUSHED_WAIT_PAY = 2;
    /** 托售商品业务状态：3等待确认付款 */
    public static final int GOODS_STATUS_WAIT_CONFIRM = 3;
    /** 托售商品业务状态：4待处理（买家持有，可申请委托代卖） */
    public static final int GOODS_STATUS_PENDING = 4;
    /** 托售商品业务状态：5委托代卖（买家已申请，待平台审核） */
    public static final int GOODS_STATUS_AGENT_SALE = 5;
}
