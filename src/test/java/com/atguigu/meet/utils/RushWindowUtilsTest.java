package com.atguigu.meet.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RushWindowUtils 纯函数单测：跨零点回绕修复（fix-new-member-advance-midnight-wrap）
 */
class RushWindowUtilsTest {

    @Nested
    @DisplayName("正常窗口两参版本（闭区间）")
    class NormalWindow {

        @Test
        void 区间内与两端整点为true() {
            LocalTime start = LocalTime.of(18, 30);
            LocalTime end = LocalTime.of(23, 30);
            assertTrue(RushWindowUtils.inWindow(start, end, LocalTime.of(18, 30)));
            assertTrue(RushWindowUtils.inWindow(start, end, LocalTime.of(20, 0)));
            assertTrue(RushWindowUtils.inWindow(start, end, LocalTime.of(23, 30)));
        }

        @Test
        void 区间外为false() {
            LocalTime start = LocalTime.of(18, 30);
            LocalTime end = LocalTime.of(23, 30);
            assertFalse(RushWindowUtils.inWindow(start, end, LocalTime.of(18, 29, 59)));
            assertFalse(RushWindowUtils.inWindow(start, end, LocalTime.of(23, 30, 1)));
        }
    }

    @Nested
    @DisplayName("非跨零点场次提前窗口行为不变")
    class NormalAdvance {

        private final LocalTime start = LocalTime.of(18, 30);
        private final LocalTime end = LocalTime.of(23, 30);
        private final int advance = 30;

        @Test
        void 提前半小时进入窗口() {
            assertFalse(RushWindowUtils.inWindow(start, end, LocalTime.of(17, 59), advance));
            assertTrue(RushWindowUtils.inWindow(start, end, LocalTime.of(18, 0), advance));
            assertTrue(RushWindowUtils.inWindow(start, end, LocalTime.of(18, 0, 1), advance));
        }

        @Test
        void 提前不影响白天与结束边界() {
            assertFalse(RushWindowUtils.inWindow(start, end, LocalTime.of(12, 0), advance));
            assertTrue(RushWindowUtils.inWindow(start, end, LocalTime.of(23, 30), advance));
            assertFalse(RushWindowUtils.inWindow(start, end, LocalTime.of(23, 30, 1), advance));
        }

        @Test
        void advance为null或0等同正常窗口() {
            // 18:29 在提前窗口内本应为 true，但不提前时必须 false
            assertFalse(RushWindowUtils.inWindow(start, end, LocalTime.of(18, 29), null));
            assertFalse(RushWindowUtils.inWindow(start, end, LocalTime.of(18, 29), 0));
            assertFalse(RushWindowUtils.inWindow(start, end, LocalTime.of(18, 29), -30));
            assertTrue(RushWindowUtils.inWindow(start, end, LocalTime.of(18, 30), null));
        }
    }

    @Nested
    @DisplayName("跨零点场次：当日下界退化为 00:00")
    class MidnightWrap {

        @Test
        @DisplayName("全天场 00:00~23:59 + 提前30：00:00/12:00/23:59 均 true")
        void allDaySession_newMemberAnyBusinessTime() {
            LocalTime start = LocalTime.of(0, 0);
            LocalTime end = LocalTime.of(23, 59);
            int advance = 30;
            assertTrue(RushWindowUtils.inWindow(start, end, LocalTime.of(0, 0), advance));
            assertTrue(RushWindowUtils.inWindow(start, end, LocalTime.of(0, 0, 1), advance));
            assertTrue(RushWindowUtils.inWindow(start, end, LocalTime.of(12, 0), advance));
            assertTrue(RushWindowUtils.inWindow(start, end, LocalTime.of(23, 59), advance));
            // 结束后仍为 false
            assertFalse(RushWindowUtils.inWindow(start, end, LocalTime.of(23, 59, 1), advance));
        }

        @Test
        @DisplayName("全天场老会员（不提前）同口径全天 true")
        void allDaySession_oldMember() {
            LocalTime start = LocalTime.of(0, 0);
            LocalTime end = LocalTime.of(23, 59);
            assertTrue(RushWindowUtils.inWindow(start, end, LocalTime.of(12, 0)));
            assertTrue(RushWindowUtils.inWindow(start, end, LocalTime.of(0, 0)));
            assertFalse(RushWindowUtils.inWindow(start, end, LocalTime.of(23, 59, 1)));
        }

        @Test
        @DisplayName("00:10 开场 + 提前30：00:05 true、当天 23:00 true、end 后 false")
        void afterMidnightStart_wrapsToPreviousDay() {
            LocalTime start = LocalTime.of(0, 10);
            LocalTime end = LocalTime.of(23, 59);
            int advance = 30;
            assertTrue(RushWindowUtils.inWindow(start, end, LocalTime.of(0, 5), advance));
            assertTrue(RushWindowUtils.inWindow(start, end, LocalTime.of(0, 0), advance));
            assertTrue(RushWindowUtils.inWindow(start, end, LocalTime.of(23, 0), advance));
            assertFalse(RushWindowUtils.inWindow(start, end, LocalTime.of(23, 59, 1), advance));
        }

        @Test
        @DisplayName("修复前误判的 23:30 回绕点不再收缩当日窗口（全天场）")
        void wrappedEffectiveStart_doesNotShrinkWindow() {
            // 旧实现 effectiveStart=23:30 会把 12:00 判出窗口；修复后必须在窗口内
            LocalTime start = LocalTime.of(0, 0);
            LocalTime end = LocalTime.of(23, 59);
            assertTrue(RushWindowUtils.inWindow(start, end, LocalTime.of(23, 29, 59), 30));
        }
    }
}
