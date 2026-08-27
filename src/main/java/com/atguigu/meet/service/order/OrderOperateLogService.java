package com.atguigu.meet.service.order;

import com.atguigu.meet.enums.OrderOperateType;
import com.atguigu.meet.mapper.order.OrderOperateLogMapper;
import com.atguigu.meet.model.entity.order.OrderOperateLog;
import com.atguigu.meet.model.entity.permission.user.AdminUser;
import com.atguigu.meet.utils.AdminContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单操作日志写入服务
 * <p>
 * 关键点：
 * 1. 使用 {@link Propagation#REQUIRES_NEW} 独立事务写入日志，
 *    保证无论上游业务事务提交 / 回滚，操作日志都被硬写落地，审计链不丢失。
 * 2. {@code @Async("operateLogExecutor")} 异步执行（P0 性能改造）：
 *    调用方线程立即返回，不再占用主业务数据库连接等待日志插入；
 *    拒绝策略 CallerRunsPolicy + 优雅停机等待兜底，审计记录不丢。
 * <p>
 * 为什么要独立成一个 Service？
 * - Spring 的 AOP 注解基于代理：同个类内部的 this 自调用不走代理，
 *   REQUIRES_NEW / @Async 都会失效；必须由外部 bean 调用才生效。
 */
@Service
public class OrderOperateLogService {

    @Autowired
    private OrderOperateLogMapper logMapper;

    /**
     * 写入订单操作审计日志（异步 + 独立事务，必落地）
     *
     * @param orderId      订单ID
     * @param beforeStatus 操作前状态（可为null，如首次创建）
     * @param afterStatus  操作后状态
     * @param type         操作类型枚举
     * @param remark       操作备注（可为空）
     */
    @Async("operateLogExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void writeOperateLog(Long orderId, Integer beforeStatus, Integer afterStatus,
                                OrderOperateType type, String remark) {
        OrderOperateLog logEntity = new OrderOperateLog();
        logEntity.setOrderId(orderId);
        logEntity.setBeforeStatus(beforeStatus);
        logEntity.setAfterStatus(afterStatus);
        logEntity.setOperateType(type.getCode());
        logEntity.setOperateDesc(type.getDesc());
        AdminUser admin = AdminContext.get();
        if (admin != null) {
            logEntity.setOperateUserId(admin.getUserId());
            logEntity.setOperateUserName(admin.getUsername() != null ? admin.getUsername() : admin.getPhone());
        }
        logEntity.setRemark(remark);
        logMapper.insert(logEntity);
    }
}
