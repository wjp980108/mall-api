package com.atguigu.meet.utils;

import java.time.LocalTime;

/**
 * 抢购时间窗口判定工具类
 * <p>
 * 统一 C 端购买链路（详情 canPurchase、下单门禁、老链路列表后处理）与
 * 管理端售卖中守卫（SeckillSellingGuard）的每日抢购窗口口径。
 * <ul>
 *   <li>正常窗口为闭区间 [start, end]（边界整点可购，沿用 {@code !isBefore/!isAfter} 语义）</li>
 *   <li>含提前量时窗口为 [start - advanceMinutes, end]</li>
 * </ul>
 * <p>
 * <b>跨零点语义：</b>{@link LocalTime} 只有时分、没有日期，当 start 早于 00:00 之后 advanceMinutes
 * 分钟时（如全天场 00:00 开场、提前 30 分钟），{@code start.minusMinutes(n)} 会回绕到前一日
 * （00:00 - 30分 = 23:30）。该提前量的前半夜段属于前一日，对当日判定无意义，因此当日有效下界
 * 退化为 00:00，即当天 {@code 00:00 <= now <= end} 均属窗口内；全天场因此新老会员窗口重合。
 * <p>
 * 本类是无状态纯函数，<b>不感知会员身份与系统开关</b>：是否新会员（member_type=0）、
 * newMemberDays/advance 双开关是否开启由各调用方先行判断，资格不满足时传 {@code null} 或
 * 非正提前量调用本类。调用方须自行保证 start/end/now 非空（时间未配置的防御留在业务层）。
 * <p>
 * 结束时间跨次日零点的场次（end &lt; start，如 22:00~次日02:00）不在支持范围，属存量限制。
 */
public final class RushWindowUtils {

    private RushWindowUtils() {
    }

    /**
     * 判断 now 是否处于正常抢购窗口 [start, end]（闭区间）
     *
     * @param start 场次每日开抢时间（非空）
     * @param end   场次每日结束时间（非空）
     * @param now   当前时间（非空）
     * @return true 表示在窗口内（含两端整点）
     */
    public static boolean inWindow(LocalTime start, LocalTime end, LocalTime now) {
        return !now.isBefore(start) && !now.isAfter(end);
    }

    /**
     * 判断 now 是否处于含新会员提前量的抢购窗口 [start - advanceMinutes, end]（闭区间）
     * <p>
     * advanceMinutes 为 null 或 &lt;=0 时等同正常窗口；提前量跨零点回绕时当日下界退化为 00:00。
     *
     * @param start          场次每日开抢时间（非空）
     * @param end            场次每日结束时间（非空）
     * @param now            当前时间（非空）
     * @param advanceMinutes 新会员提前分钟数（null/非正表示不提前）
     * @return true 表示在窗口内（含两端整点）
     */
    public static boolean inWindow(LocalTime start, LocalTime end, LocalTime now, Integer advanceMinutes) {
        if (advanceMinutes == null || advanceMinutes <= 0) {
            return inWindow(start, end, now);
        }
        LocalTime effectiveStart = start.minusMinutes(advanceMinutes);
        if (effectiveStart.isAfter(start)) {
            // 减法结果反而晚于 start：LocalTime 回绕跨入前一日，当日不再校验下界，仅保留上界
            return !now.isAfter(end);
        }
        return !now.isBefore(effectiveStart) && !now.isAfter(end);
    }
}
