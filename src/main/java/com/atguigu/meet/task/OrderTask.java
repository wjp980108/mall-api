package com.atguigu.meet.task;

import com.atguigu.meet.service.order.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 订单定时任务
 * <p>依赖应用入口的 @EnableScheduling 开启调度（与 ConsignGoodsTask 相同机制）。
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderService orderService;

    /**
     * 自动取消超时未付款订单
     * <p>每分钟执行：扫描 pay_deadline 已过且仍为 1待付款 的订单，系统自动取消
     * （订单 -> 5已取消，商品回滚 1挂卖中），审计日志记录 TIMEOUT_CANCEL。
     * Service 层整体事务，失败则本轮全部回滚、下一轮幂等重试。
     */
    @Scheduled(cron = "0 * * * * ?")
    public void autoCancelTimeoutOrders() {
        try {
            int count = orderService.autoCancelTimeoutOrders();
            if (count > 0) {
                log.info("[定时任务] 超时订单自动取消完成，共 {} 笔", count);
            }
        } catch (Exception e) {
            log.error("[定时任务] 超时订单自动取消失败，等待下一轮重试", e);
        }
    }
}
