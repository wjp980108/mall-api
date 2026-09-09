package com.atguigu.meet.service.seckill.guard;

import com.atguigu.meet.mapper.seckill.session.SessionMapper;
import com.atguigu.meet.mapper.seckill.sessionproduct.SessionProductMapper;
import com.atguigu.meet.model.entity.general.settings.SysSettings;
import com.atguigu.meet.model.entity.seckill.session.Session;
import com.atguigu.meet.model.entity.seckill.sessionproduct.SessionProduct;
import com.atguigu.meet.service.general.settings.SysSettingsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 抢购售卖中守卫（lock-crud-during-seckill-sale）
 * <p>
 * 统一"售卖中"判定口径，供场次/场次商品关联/托售商品三处管理端写链路复用：
 * <pre>
 * isSelling = session_status = 1
 *          AND now ∈ [effectiveStart, rush_end_time]   （每日时段）
 * effectiveStart = rush_start_time - newMemberAdvanceMinutes
 *                  （仅当 sys_settings 双开关 newMemberDays>0 且 newMemberAdvanceMinutes>0 时提前）
 * </pre>
 * 含新会员提前窗口：提前窗口内新会员已可下单扣减库存，管理端写锁定同步生效，与 C 端最早可下单时刻对齐。
 * <p>
 * 判定不依赖请求者身份（管理端守卫取 C 端最早可下单时刻）；rush 时间未配置的场次视为非售卖。
 */
@Component
public class SeckillSellingGuard {

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private SessionProductMapper sessionProductMapper;

    @Autowired
    private SysSettingsService sysSettingsService;

    /**
     * 场次维度售卖判定（按场次实体）
     *
     * @param session 场次（可为 null，判非售卖）
     */
    public boolean isSessionSelling(Session session) {
        if (session == null || !Integer.valueOf(1).equals(session.getSessionStatus())) {
            return false;
        }
        return isSellingAt(session, LocalTime.now(), loadAdvanceMinutes());
    }

    /**
     * 场次维度售卖判定（按场次ID）
     */
    public boolean isSessionSelling(Long sessionId) {
        return sessionId != null && isSessionSelling(sessionMapper.selectById(sessionId));
    }

    /**
     * 商品维度售卖判定：商品存在于任一售卖中场次的关联（t_session_product）中即视为售卖中
     */
    public boolean isGoodsSelling(Long goodsId) {
        return goodsId != null && !filterSellingGoodsIds(Collections.singletonList(goodsId)).isEmpty();
    }

    /**
     * 批量过滤：返回给定商品中处于售卖中的 goodsId（一次反查关联 + 批量取场次 + 一次取设置）
     *
     * @param goodsIds 商品ID列表
     * @return 命中售卖中的 goodsId 集合（无序）
     */
    public List<Long> filterSellingGoodsIds(List<Long> goodsIds) {
        if (goodsIds == null || goodsIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<SessionProduct> relations = sessionProductMapper.selectList(
                new LambdaQueryWrapper<SessionProduct>().in(SessionProduct::getGoodsId, goodsIds));
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> sessionIds = new HashSet<>();
        for (SessionProduct sp : relations) {
            if (sp.getSessionId() != null) {
                sessionIds.add(sp.getSessionId());
            }
        }
        Set<Long> sellingSessions = filterSellingSessionIds(sessionIds);
        Set<Long> selling = new HashSet<>();
        for (SessionProduct sp : relations) {
            if (sellingSessions.contains(sp.getSessionId())) {
                selling.add(sp.getGoodsId());
            }
        }
        return new ArrayList<>(selling);
    }

    /**
     * 批量判定：返回给定场次中处于售卖中的 sessionId（一次批量取场次 + 一次取设置）
     */
    public Set<Long> filterSellingSessionIds(Collection<Long> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<Session> sessions = sessionMapper.selectBatchIds(sessionIds);
        Integer advanceMinutes = loadAdvanceMinutes();
        LocalTime now = LocalTime.now();
        Set<Long> selling = new HashSet<>();
        for (Session session : sessions) {
            if (Integer.valueOf(1).equals(session.getSessionStatus())
                    && isSellingAt(session, now, advanceMinutes)) {
                selling.add(session.getId());
            }
        }
        return selling;
    }

    /**
     * 批量填充场次列表的 onSale 派生字段（一次取设置，逐场次判定）
     */
    public void fillOnSale(List<Session> sessions) {
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        Integer advanceMinutes = loadAdvanceMinutes();
        LocalTime now = LocalTime.now();
        for (Session session : sessions) {
            boolean selling = Integer.valueOf(1).equals(session.getSessionStatus())
                    && isSellingAt(session, now, advanceMinutes);
            session.setOnSale(selling);
        }
    }

    /**
     * 新会员提前分钟数：双开关（newMemberDays>0 且 newMemberAdvanceMinutes>0）才生效，否则 null（不提前）
     */
    private Integer loadAdvanceMinutes() {
        SysSettings settings = sysSettingsService.get();
        if (settings == null) {
            return null;
        }
        Integer days = settings.getNewMemberDays();
        Integer advance = settings.getNewMemberAdvanceMinutes();
        return (days != null && days > 0 && advance != null && advance > 0) ? advance : null;
    }

    /**
     * 售卖窗口判定：now ∈ [effectiveStart, rushEnd]（每日时段，不含场次启用状态，由调用方先行判断）
     */
    private boolean isSellingAt(Session session, LocalTime now, Integer advanceMinutes) {
        LocalTime start = session.getRushStartTime();
        LocalTime end = session.getRushEndTime();
        if (start == null || end == null) {
            return false;
        }
        LocalTime effectiveStart = advanceMinutes != null ? start.minusMinutes(advanceMinutes) : start;
        return !now.isBefore(effectiveStart) && !now.isAfter(end);
    }
}
