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

        // 定点更新所有业务字段
        LambdaUpdateWrapper<SysSettings> uw = new LambdaUpdateWrapper<>();
        uw.eq(SysSettings::getId, ROW_ID)
                .set(SysSettings::getSiteName, dto.getSiteName())
                .set(SysSettings::getSiteLogo, dto.getSiteLogo())
                .set(SysSettings::getNewMemberDays, dto.getNewMemberDays())
                .set(SysSettings::getNewMemberAdvanceMinutes, dto.getNewMemberAdvanceMinutes())
                .set(SysSettings::getPreViewMinutes, dto.getPreViewMinutes())
                .set(SysSettings::getLimitRule, dto.getLimitRule())
                .set(SysSettings::getShareValidDays, dto.getShareValidDays())
                .set(SysSettings::getRecommendRate, dto.getRecommendRate())
                .set(SysSettings::getReferrerPurchaseDays, dto.getReferrerPurchaseDays())
                .set(SysSettings::getSelfBuyRate, dto.getSelfBuyRate())
                .set(SysSettings::getSelfBuyBonusRatio, dto.getSelfBuyBonusRatio())
                .set(SysSettings::getCouponRatio, dto.getCouponRatio())
                .set(SysSettings::getShowSelfBuyBonus, dto.getShowSelfBuyBonus())
                .set(SysSettings::getShowCoupon, dto.getShowCoupon())
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
}
