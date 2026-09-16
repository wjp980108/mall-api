package com.atguigu.meet.service.permission.user;

import com.atguigu.meet.enums.UserOperateType;
import com.atguigu.meet.mapper.permission.user.UserOperateLogMapper;
import com.atguigu.meet.model.entity.permission.user.AdminUser;
import com.atguigu.meet.model.entity.permission.user.UserOperateLog;
import com.atguigu.meet.utils.AdminContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户操作审计日志写入服务
 * <p>
 * 关键点：
 * 1. 使用 {@link Propagation#REQUIRES_NEW} 独立事务写入日志，
 *    保证无论上游业务事务提交 / 回滚，操作日志都被硬写落地，审计链不丢失。
 * 2. {@code @Async("operateLogExecutor")} 异步执行：
 *    调用方线程立即返回，不占用主业务数据库连接；拒绝策略 CallerRunsPolicy 兜底不丢记录。
 * 3. 与订单模块 {@code OrderOperateLogService} 的差异：方法内 try-catch 兜底——
 *    CallerRuns 模式下任务在调用方线程执行，若日志插入异常向上传播会连带回滚主业务，
 *    违反"审计写入不阻断业务"；此处捕获后仅记录错误日志。
 * <p>
 * 为什么要独立成一个 Service？
 * - Spring 的 AOP 注解基于代理：同个类内部的 this 自调用不走代理，
 *   REQUIRES_NEW / @Async 都会失效；必须由外部 bean 调用才生效。
 */
@Slf4j
@Service
public class UserOperateLogService {

    /** 定时任务等系统自动操作的固定操作人名称快照（operate_user_id 存 NULL） */
    public static final String SYSTEM_OPERATOR_NAME = "SYSTEM";

    /** remark 字段最大长度，与 sys_user_operate_log.remark VARCHAR(512) 对齐 */
    private static final int REMARK_MAX_LENGTH = 512;

    @Autowired
    private UserOperateLogMapper logMapper;

    /**
     * 写入管理端用户操作审计日志（异步 + 独立事务，必落地）
     * <p>操作人从 {@link AdminContext} 获取（当前登录管理员）。
     *
     * @param userId       目标用户ID
     * @param type         操作类型枚举
     * @param beforeStatus 操作前账号状态（非状态类操作传null）
     * @param afterStatus  操作后账号状态（非状态类操作传null）
     * @param remark       操作备注（关键入参JSON摘要，超长自动截断，可为空）
     */
    @Async("operateLogExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void writeOperateLog(Long userId, UserOperateType type, Integer beforeStatus,
                                Integer afterStatus, String remark) {
        try {
            insertLog(userId, type, beforeStatus, afterStatus, remark, null, null);
        } catch (Exception e) {
            // 审计失败不阻断业务：仅记录错误日志
            log.error("[用户操作日志] 审计日志写入失败, userId={}, type={}", userId, type, e);
        }
    }

    /**
     * 写入定时任务等系统自动操作的审计日志（异步 + 独立事务，必落地）
     * <p>操作人固定为 SYSTEM：operate_user_id=NULL、operate_user_name='SYSTEM'。
     *
     * @param userId       目标用户ID
     * @param type         操作类型枚举
     * @param beforeStatus 操作前账号状态（非状态类操作传null）
     * @param afterStatus  操作后账号状态（非状态类操作传null）
     * @param remark       操作备注（超长自动截断，可为空）
     */
    @Async("operateLogExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void writeSystemLog(Long userId, UserOperateType type, Integer beforeStatus,
                               Integer afterStatus, String remark) {
        try {
            insertLog(userId, type, beforeStatus, afterStatus, remark, null, SYSTEM_OPERATOR_NAME);
        } catch (Exception e) {
            log.error("[用户操作日志] 系统操作审计日志写入失败, userId={}, type={}", userId, type, e);
        }
    }

    /**
     * 组装并插入日志实体
     * <p>operatorId/operatorName 均为 null 时视为管理端调用，从 AdminContext 取当前登录管理员；
     * operatorName 非空（SYSTEM）时直接使用，表示系统自动操作。
     */
    private void insertLog(Long userId, UserOperateType type, Integer beforeStatus, Integer afterStatus,
                           String remark, Long operatorId, String operatorName) {
        if (operatorName == null) {
            AdminUser admin = AdminContext.get();
            if (admin != null) {
                operatorId = admin.getUserId();
                operatorName = admin.getUsername() != null ? admin.getUsername() : admin.getPhone();
            }
        }
        UserOperateLog entity = new UserOperateLog();
        entity.setUserId(userId);
        entity.setBeforeStatus(beforeStatus);
        entity.setAfterStatus(afterStatus);
        entity.setOperateType(type.getCode());
        entity.setOperateDesc(type.getDesc());
        entity.setOperateUserId(operatorId);
        entity.setOperateUserName(operatorName);
        entity.setRemark(truncate(remark));
        logMapper.insert(entity);
    }

    /** remark 超长截断，防止入库报错（审计宁可截断不可失败） */
    private String truncate(String remark) {
        if (remark == null || remark.length() <= REMARK_MAX_LENGTH) {
            return remark;
        }
        return remark.substring(0, REMARK_MAX_LENGTH);
    }
}
