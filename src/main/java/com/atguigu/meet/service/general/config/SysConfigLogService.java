package com.atguigu.meet.service.general.config;

import com.atguigu.meet.mapper.general.config.SysConfigLogMapper;
import com.atguigu.meet.model.entity.general.config.SysConfigLog;
import com.atguigu.meet.model.entity.permission.user.AdminUser;
import com.atguigu.meet.utils.AdminContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 系统配置变更日志写入服务
 * <p>
 * 参考 {@code OrderOperateLogService}：
 * 1. {@code REQUIRES_NEW} 独立事务，日志硬写落地；
 * 2. {@code @Async("operateLogExecutor")} 异步执行，调用方线程立即返回；
 *    operateLogExecutor 的 TaskDecorator 会把 AdminContext 快照带到异步线程，操作人信息不丢。
 * <p>
 * 注意：调用方需在主事务提交后调用（afterCommit），保证日志只记录真正落库成功的变更。
 */
@Service
@Slf4j
public class SysConfigLogService {

    @Autowired
    private SysConfigLogMapper logMapper;

    /**
     * 批量写入配置变更日志（异步 + 独立事务）
     *
     * @param logs 变更明细（group/key/oldValue/newValue 已填充，操作人在此补充）
     */
    @Async("operateLogExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void writeLogs(List<SysConfigLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return;
        }
        AdminUser admin = AdminContext.get();
        if (admin != null) {
            for (SysConfigLog logEntity : logs) {
                logEntity.setOperatorId(admin.getUserId());
                logEntity.setOperatorName(admin.getUsername() != null ? admin.getUsername() : admin.getPhone());
            }
        }
        for (SysConfigLog logEntity : logs) {
            logMapper.insert(logEntity);
        }
        log.info("[系统配置] 变更日志写入 {} 条，操作人={}", logs.size(),
                admin != null ? admin.getUsername() : "unknown");
    }
}
