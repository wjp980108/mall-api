package com.atguigu.meet.model.entity.general.settings;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 系统设置 (sys_settings)
 * <p>
 * 单行表(固定字段), 与 sys_config(key-value 动态表) 并存:
 * - sys_config 负责灵活扩展的配置项(站点名称/备案号/CDN/支付方式等)
 * - sys_settings 负责固定结构/一次性读写的业务设置(会员权益/抢单规则/推广海报等)
 * <p>
 * 全局固定 id=1, 初始数据由 SQL INSERT IGNORE 写入, 后续只做 UPDATE。
 */
@Data
@TableName("sys_settings")
@Schema(description = "系统设置")
public class SysSettings extends Model<SysSettings> {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    // ========== 站点设置 ==========
    /** 站点名称 */
    @Schema(description = "站点名称")
    private String siteName;

    /** 站点Logo图片URL */
    @Schema(description = "站点Logo图片URL")
    private String siteLogo;

    // ========== 会员权益 ==========
    /** 新会员身份有效期(天), 注册后多少天内属于新会员 */
    @Schema(description = "新会员身份有效期(天)")
    private Integer newMemberDays;

    /** 新会员提前抢购(分钟), 设0表示关闭提前抢购权益 */
    @Schema(description = "新会员提前抢购(分钟)")
    private Integer newMemberAdvanceMinutes;

    // ========== 抢单规则 ==========
    /** 可提前查看抢购商品(分钟), 0表示开抢时才展示商品 */
    @Schema(description = "可提前查看抢购商品(分钟)")
    private Integer preViewMinutes;

    /** 会员限购规则: 0不限购 1同一场次限购一次 2当天限购一次 */
    @Schema(description = "会员限购规则")
    private Integer limitRule;

    /** 分享资格有效期(天), 老会员最近多少天内需成功邀请新会员, 0关闭 */
    @Schema(description = "分享资格有效期(天)")
    private Integer shareValidDays;

    /** 推荐奖比例(%), 直属会员成功下单时按订单金额计算 */
    @Schema(description = "推荐奖比例(%)")
    private BigDecimal recommendRate;

    /** 推荐人购买有效期(天), 0不限; 大于0时从推荐人最后一次下单向后计算 */
    @Schema(description = "推荐人购买有效期(天)")
    private Integer referrerPurchaseDays;

    /** 自购奖励比例(%), 按会员本人订单金额计算 */
    @Schema(description = "自购奖励比例(%)")
    private BigDecimal selfBuyRate;

    /** 自购奖金占比(%), 自购奖金与购物券占比合计须为100 */
    @Schema(description = "自购奖金占比(%)")
    private BigDecimal selfBuyBonusRatio;

    /** 购物券占比(%), 自购奖金与购物券占比合计须为100 */
    @Schema(description = "购物券占比(%)")
    private BigDecimal couponRatio;

    /** 前端显示自购奖: 0否 1是 */
    @Schema(description = "前端显示自购奖")
    private Integer showSelfBuyBonus;

    /** 前端显示购物券: 0否 1是 */
    @Schema(description = "前端显示购物券")
    private Integer showCoupon;

    /** 订单利润比例(%), 订单创建时按订单金额计算并保存 */
    @Schema(description = "订单利润比例(%)")
    private BigDecimal orderProfitRate;

    // ========== 推广海报 ==========
    /** 推广海报背景图URL */
    @Schema(description = "推广海报背景图URL")
    private String posterBgImage;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
