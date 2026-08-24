package com.atguigu.meet.service.order;

import com.atguigu.meet.enums.OrderOperateType;
import com.atguigu.meet.mapper.order.OrderOperateLogMapper;
import com.atguigu.meet.model.entity.order.OrderOperateLog;
import com.atguigu.meet.model.entity.permission.user.AdminUser;
import com.atguigu.meet.utils.AdminContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单操作日志写入服务
 * <p>
 * 关键点：使用 {@link Propagation#REQUIRES_NEW} 独立事务写入日志，
 * 保证无论上游业务事务提交 / 回滚，操作日志都被硬写落地，审计链不丢失。
 * <p>
 * 为什么要独立成一个 Service？
 * - Spring 的事务注解基于 AOP 代理，同个类内部的 private 方法自调用（this.xxx）
 *   不会走代理，REQUIRES_NEW 形同虚设；必须由外部 bean 调用才会开启新事务。
 */
@Service
public class OrderOperateLogService {

    @Autowired
    private OrderOperateLogMapper logMapper;

    /**
     * 写入订单操作审计日志（独立事务，必落地）
     *
     * @param orderId      订单ID
     * @param beforeStatus 操作前状态（可为null，如首次创建）
     * @param afterStatus  操作后状态
     * @param type         操作类型枚举
     * @param remark       操作备注（可为空）
     */
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
