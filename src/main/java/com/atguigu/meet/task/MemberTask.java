package com.atguigu.meet.task;

import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.enums.UserOperateType;
import com.atguigu.meet.mapper.permission.user.UserMapper;
import com.atguigu.meet.model.entity.general.settings.SysSettings;
import com.atguigu.meet.service.general.settings.SysSettingsService;
import com.atguigu.meet.service.permission.user.UserOperateLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

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

    @Autowired
    private UserOperateLogService userOperateLogService;

    /**
     * 新会员到期未邀请自动禁用
     * <p>每小时执行：读取 sys_settings.new_member_days，>0 时扫描
     * member_type=0(新会员) 且 status=1(正常) 且 create_time + N 天 <= 当前时间
     * 的用户，置 status=0(禁用)。new_member_days=0 表示功能关闭，不执行任何禁用。
     * <p>管辖范围（见 openspec change add-user-operate-log / member-lifecycle spec）：
     * 仅管辖"无管理角色 或 拥有会员角色(role_id=3)"的用户；
     * 仅拥有超级管理员/平台管理员角色的账号豁免，防止后台运营账号被误禁。
     * <p>条件更新幂等：失败本轮回滚，下一轮重试；已禁用/已转老会员用户条件不命中不会重复处理。
     * <p>审计：每个被禁用用户落一条 sys_user_operate_log（操作人=SYSTEM，
     * 操作类型=SYSTEM_AUTO_DISABLE，before=1/after=0），日志失败不阻断任务。
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
            // 预演：先取本轮管辖命中的用户ID（只读），供逐行禁用与审计落日志使用
            List<Long> expiredIds = userMapper.selectExpiredNewMemberIds(cutoff,
                    PermissionConst.SUPER_ADMIN_ROLE_ID,
                    PermissionConst.PLATFORM_ADMIN_ROLE_ID,
                    PermissionConst.MEMBER_ROLE_ID);
            int disabled = 0;
            for (Long userId : expiredIds) {
                // 逐行条件更新：全部管辖条件在 UPDATE 内原子重查，
                // 查更间隙被转老会员/已禁用的用户不命中，禁用数与审计日志严格一一对应
                int rows = userMapper.disableOneExpiredNewMember(userId, cutoff,
                        PermissionConst.SUPER_ADMIN_ROLE_ID,
                        PermissionConst.PLATFORM_ADMIN_ROLE_ID,
                        PermissionConst.MEMBER_ROLE_ID);
                if (rows > 0) {
                    disabled++;
                    // 审计：系统自动禁用落用户操作日志（独立事务异步，失败不阻断任务）
                    userOperateLogService.writeSystemLog(userId, UserOperateType.SYSTEM_AUTO_DISABLE,
                            1, 0, "新会员注册超过" + newMemberDays + "天未转化，系统自动禁用");
                }
            }
            if (disabled > 0) {
                log.info("[定时任务] 新会员到期未邀请自动禁用完成，共 {} 人", disabled);
            }
        } catch (Exception e) {
            log.error("[定时任务] 新会员到期禁用任务失败，等待下一轮重试", e);
        }
    }
}
