package com.atguigu.meet.service.seckill.guard;

import com.atguigu.meet.model.entity.general.settings.SysSettings;
import com.atguigu.meet.model.entity.seckill.session.Session;
import com.atguigu.meet.service.general.settings.SysSettingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * SeckillSellingGuard 售卖窗口判定回归（fix-new-member-advance-midnight-wrap）
 * <p>
 * 重点覆盖跨零点场次：全天场（00:00 开场）白天必须判定售卖中——
 * 修复前 LocalTime 回绕把售卖时段错误收缩到 23:30~23:59。
 */
@ExtendWith(MockitoExtension.class)
class SeckillSellingGuardTest {

    @Mock
    private SysSettingsService sysSettingsService;

    @InjectMocks
    private SeckillSellingGuard guard;

    /** 构造场次（状态与抢购窗口按用例给定） */
    private Session session(Integer status, LocalTime start, LocalTime end) {
        Session s = new Session();
        s.setId(1L);
        s.setSessionStatus(status);
        s.setRushStartTime(start);
        s.setRushEndTime(end);
        return s;
    }

    /** 双开关开启：newMemberDays=7、advance=30 */
    private void stubAdvanceOn() {
        SysSettings settings = new SysSettings();
        settings.setNewMemberDays(7);
        settings.setNewMemberAdvanceMinutes(30);
        when(sysSettingsService.get()).thenReturn(settings);
    }

    /** 固定时钟下判定场次售卖状态（CALLS_REAL_METHODS 保证 LocalTime 算术不被静态 mock 干扰） */
    private boolean sellingAt(Session s, LocalTime fixedNow) {
        try (MockedStatic<LocalTime> mockedClock = mockStatic(LocalTime.class, Mockito.CALLS_REAL_METHODS)) {
            mockedClock.when(LocalTime::now).thenReturn(fixedNow);
            return guard.isSessionSelling(s);
        }
    }

    @Test
    void allDaySession_sellingAtNoon_whenAdvanceOn() {
        stubAdvanceOn();
        Session s = session(1, LocalTime.of(0, 0), LocalTime.of(23, 59));
        // 修复前 12:00 因 00:00-30分回绕为 23:30 而误判非售卖（写锁全天失效）
        assertTrue(sellingAt(s, LocalTime.of(12, 0)));
    }

    @Test
    void allDaySession_sellingAtMidnight_whenAdvanceOn() {
        stubAdvanceOn();
        Session s = session(1, LocalTime.of(0, 0), LocalTime.of(23, 59));
        assertTrue(sellingAt(s, LocalTime.of(0, 0)));
    }

    @Test
    void allDaySession_notSellingAfterEnd() {
        stubAdvanceOn();
        Session s = session(1, LocalTime.of(0, 0), LocalTime.of(23, 59));
        assertFalse(sellingAt(s, LocalTime.of(23, 59, 1)));
    }

    @Test
    void disabledSession_notSelling_evenInWindow() {
        // status=0 早返回，不应触发任何设置读取
        Session s = session(0, LocalTime.of(0, 0), LocalTime.of(23, 59));
        assertFalse(sellingAt(s, LocalTime.of(12, 0)));
    }

    @Test
    void normalSession_lockTimingUnchanged() {
        stubAdvanceOn();
        Session s = session(1, LocalTime.of(18, 30), LocalTime.of(23, 30));
        // 提前窗口前：非售卖（防过度锁定）
        assertFalse(sellingAt(s, LocalTime.of(17, 59)));
        // 提前窗口起点整点：售卖
        assertTrue(sellingAt(s, LocalTime.of(18, 0)));
        // 结束后：非售卖
        assertFalse(sellingAt(s, LocalTime.of(23, 30, 1)));
    }

    @Test
    void allDaySession_sellingAtNoon_whenAdvanceOff() {
        // 双开关关闭时按正常窗口，全天场白天仍售卖（与 C 端老会员口径一致）
        SysSettings settings = new SysSettings();
        settings.setNewMemberDays(0);
        settings.setNewMemberAdvanceMinutes(30);
        when(sysSettingsService.get()).thenReturn(settings);

        Session s = session(1, LocalTime.of(0, 0), LocalTime.of(23, 59));
        assertTrue(sellingAt(s, LocalTime.of(12, 0)));
    }
}
