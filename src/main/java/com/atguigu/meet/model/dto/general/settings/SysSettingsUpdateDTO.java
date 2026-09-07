package com.atguigu.meet.model.dto.general.settings;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 系统设置更新 DTO
 * <p>
 * 单行表全量更新, 所有业务字段都必填 (不含 id/create_time/update_time/is_deleted).
 * 自购奖金占比 + 购物券占比 = 100 的校验在 Service 层做.
 */
@Data
@Schema(description = "系统设置更新请求")
public class SysSettingsUpdateDTO {

    // ========== 站点设置 ==========
    @Schema(description = "站点名称")
    @Size(max = 128, message = "站点名称最长128字符")
    private String siteName;

    @Schema(description = "站点Logo图片URL")
    @Size(max = 512, message = "站点Logo URL最长512字符")
    private String siteLogo;

    // ========== 会员权益 ==========
    @Schema(description = "新会员身份有效期(天)")
    @Min(value = 0, message = "新会员有效期不能为负")
    private Integer newMemberDays;

    @Schema(description = "新会员提前抢购(分钟)")
    @Min(value = 0, message = "提前抢购分钟数不能为负")
    private Integer newMemberAdvanceMinutes;

    // ========== 抢单规则 ==========
    @Schema(description = "可提前查看抢购商品(分钟)")
    @Min(value = 0, message = "提前查看分钟数不能为负")
    private Integer preViewMinutes;

    @Schema(description = "会员限购规则: 0不限购 1同一场次限购一次 2当天限购一次")
    @Min(value = 0, message = "限购规则非法")
    @Max(value = 2, message = "限购规则非法")
    private Integer limitRule;

    @Schema(description = "分享资格有效期(天)")
    @Min(value = 0, message = "分享资格有效期不能为负")
    private Integer shareValidDays;

    @Schema(description = "推荐奖比例(%)")
    @Min(value = 0, message = "推荐奖比例不能为负")
    private BigDecimal recommendRate;

    @Schema(description = "推荐人购买有效期(天)")
    @Min(value = 0, message = "推荐人购买有效期不能为负")
    private Integer referrerPurchaseDays;

    @Schema(description = "自购奖励比例(%)")
    @Min(value = 0, message = "自购奖励比例不能为负")
    private BigDecimal selfBuyRate;

    @Schema(description = "自购奖金占比(%)")
    @Min(value = 0, message = "自购奖金占比不能为负")
    @Max(value = 100, message = "自购奖金占比不能超过100")
    private BigDecimal selfBuyBonusRatio;

    @Schema(description = "购物券占比(%)")
    @Min(value = 0, message = "购物券占比不能为负")
    @Max(value = 100, message = "购物券占比不能超过100")
    private BigDecimal couponRatio;

    @Schema(description = "前端显示自购奖: 0否 1是")
    @Min(value = 0, message = "showSelfBuyBonus 只能 0 或 1")
    @Max(value = 1, message = "showSelfBuyBonus 只能 0 或 1")
    private Integer showSelfBuyBonus;

    @Schema(description = "前端显示购物券: 0否 1是")
    @Min(value = 0, message = "showCoupon 只能 0 或 1")
    @Max(value = 1, message = "showCoupon 只能 0 或 1")
    private Integer showCoupon;

    @Schema(description = "订单利润比例(%)")
    @Min(value = 0, message = "订单利润比例不能为负")
    private BigDecimal orderProfitRate;

    // ========== 推广海报 ==========
    @Schema(description = "推广海报背景图URL")
    @Size(max = 512, message = "海报背景图URL最长512字符")
    private String posterBgImage;
}
