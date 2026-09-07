package com.atguigu.meet.task;

import com.atguigu.meet.mapper.permission.user.UserMapper;
import com.atguigu.meet.model.entity.general.settings.SysSettings;
import com.atguigu.meet.model.entity.permission.user.SysUser;
import com.atguigu.meet.service.general.settings.SysSettingsService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 会员定时任务
 * <p>依赖应用入口的 @EnableScheduling 开启调度（与 OrderTask 相同机制）。
 */
@Component
@Slf4j
public class MemberTask {

    @Autowired
    private SysSettingsService sysSettingsService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 新会员到期未邀请自动禁用
     * <p>每小时执行：读取 sys_settings.new_member_days，>0 时扫描
     * member_type=0(新会员) 且 status=1(正常) 且 create_time + N 天 <= 当前时间
     * 的用户，置 status=0(禁用)。new_member_days=0 表示功能关闭，不执行任何禁用。
     * <p>条件更新幂等：失败本轮回滚，下一轮重试；已禁用用户因 status=1 条件不命中不会重复处理。
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void disableExpiredNewMembers() {
        try {
            SysSettings settings = sysSettingsService.get();
            Integer newMemberDays = settings.getNewMemberDays();
            if (newMemberDays == null || newMemberDays <= 0) {
                // 功能关闭，不禁用任何人
                return;
            }
            LocalDateTime cutoff = LocalDateTime.now().minusDays(newMemberDays);
            LambdaUpdateWrapper<SysUser> uw = new LambdaUpdateWrapper<>();
            uw.eq(SysUser::getMemberType, 0)
                    .eq(SysUser::getStatus, "1")
                    .le(SysUser::getCreateTime, cutoff)
                    .set(SysUser::getStatus, "0");
            int count = userMapper.update(null, uw);
            if (count > 0) {
                log.info("[定时任务] 新会员到期未邀请自动禁用完成，共 {} 人", count);
            }
        } catch (Exception e) {
            log.error("[定时任务] 新会员到期禁用任务失败，等待下一轮重试", e);
        }
    }
}
