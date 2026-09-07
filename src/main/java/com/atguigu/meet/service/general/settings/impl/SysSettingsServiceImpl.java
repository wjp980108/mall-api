package com.atguigu.meet.service.general.settings.impl;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.mapper.general.settings.SysSettingsMapper;
import com.atguigu.meet.model.dto.general.settings.SysSettingsUpdateDTO;
import com.atguigu.meet.model.entity.general.settings.SysSettings;
import com.atguigu.meet.service.general.settings.SysSettingsService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 系统设置 Service 实现
 * <p>
 * 单行表(全局固定 id=1), 读: selectById(1); 写: LambdaUpdateWrapper 定点更新,
 * 避免实体 null 覆盖 DB 默认值 (项目 Lesson Learned: updateById 跳过 NULL 字段
 * 但实体内联默认值会被覆盖, 单行表用定点更新更安全).
 */
@Service
public class SysSettingsServiceImpl extends ServiceImpl<SysSettingsMapper, SysSettings>
        implements SysSettingsService {

    /** 单行表固定主键 */
    private static final long ROW_ID = 1L;

    @Override
    public SysSettings get() {
        return getById(ROW_ID);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response update(SysSettingsUpdateDTO dto) {
        // 业务校验: 自购奖金占比 + 购物券占比 = 100
        BigDecimal sum = dto.getSelfBuyBonusRatio().add(dto.getCouponRatio());
        if (sum.compareTo(BigDecimal.valueOf(100)) != 0) {
            return Response.fail(500, "自购奖金占比与购物券占比合计须为100, 当前=" + sum);
        }

        // 业务校验: 奖励总和不得超过利润池（推荐奖比例 + 自购奖励比例 <= 订单利润比例）
        // 所有奖励(推荐奖/自购奖)都从「订单金额×订单利润比例%」的利润池里出，超池会导致佣金发放失败
        BigDecimal rewardSum = nz(dto.getRecommendRate()).add(nz(dto.getSelfBuyRate()));
        if (rewardSum.compareTo(nz(dto.getOrderProfitRate())) > 0) {
            return Response.fail(500, "推荐奖比例与自购奖励比例之和(" + rewardSum
                    + "%)不能超过订单利润比例(" + nz(dto.getOrderProfitRate())
                    + "%)，否则奖励超出利润池会发放失败");
        }

        // 定点更新所有业务字段
        LambdaUpdateWrapper<SysSettings> uw = new LambdaUpdateWrapper<>();
        uw.eq(SysSettings::getId, ROW_ID)
                .set(SysSettings::getSiteName, dto.getSiteName())
                .set(SysSettings::getSiteLogo, dto.getSiteLogo())
                .set(SysSettings::getNewMemberDays, dto.getNewMemberDays())
                .set(SysSettings::getNewMemberAdvanceMinutes, dto.getNewMemberAdvanceMinutes())
                // pre_view_minutes / share_valid_days / referrer_purchase_days 三预留字段写入路径切断（design D7）
                // DTO/Entity 保留为预留位，未来启用时加回 .set() 三行 + 前端输入框即可
                .set(SysSettings::getLimitRule, dto.getLimitRule())
                .set(SysSettings::getRecommendRate, dto.getRecommendRate())
                .set(SysSettings::getSelfBuyRate, dto.getSelfBuyRate())
                .set(SysSettings::getSelfBuyBonusRatio, dto.getSelfBuyBonusRatio())
                .set(SysSettings::getCouponRatio, dto.getCouponRatio())
                // 展示开关未传时默认 1(显示), 防止全量覆盖把 NULL 写进 NOT NULL 列
                .set(SysSettings::getShowSelfBuyBonus, dto.getShowSelfBuyBonus() != null ? dto.getShowSelfBuyBonus() : 1)
                .set(SysSettings::getShowCoupon, dto.getShowCoupon() != null ? dto.getShowCoupon() : 1)
                .set(SysSettings::getOrderProfitRate, dto.getOrderProfitRate())
                .set(SysSettings::getPosterBgImage, dto.getPosterBgImage());
        update(uw);

        return Response.ok("保存成功", null);
    }

    @Override
    public SysSettings getPublic() {
        // 直接返回整行即可, C 端只取需要的字段(siteName/siteLogo 等), 内部比例字段即使返回也无安全问题
        return getById(ROW_ID);
    }

    /** null 安全的 BigDecimal（null 按 0 处理） */
    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
