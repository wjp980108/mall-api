package com.atguigu.meet.config;

import com.atguigu.meet.model.entity.permission.user.AdminUser;
import com.atguigu.meet.utils.AdminContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置
 * <p>
 * operate-log 线程池：订单/商品操作审计日志异步落库，
 * 使业务主事务不再为同步日志消耗数据库连接与等待时间（P0 性能改造）。
 * <p>
 * 关键决策：
 * - {@code setTaskDecorator}：AdminContext 基于 ThreadLocal 存储登录管理员，
 *   异步切换线程后无法读取；装饰器在提交任务的主线程捕获快照、在执行线程还原，
 *   保证日志仍能记录操作人（operate_user_id / operate_user_name）；
 * - 拒绝策略使用 {@link ThreadPoolExecutor#CallerRunsPolicy}：队列打满时降级回调用方线程同步写入，
 *   宁可拖慢业务也绝不丢弃审计记录（审计优先于性能）；
 * - {@code setWaitForTasksToCompleteOnShutdown(true)}：应用优雅停机时等待队列内剩余日志写完，停机不丢日志。
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("operateLogExecutor")
    public Executor operateLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(5000);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("order-log-");
        // 跨线程传递登录管理员快照（提交时取值 -> 执行时恢复 -> 执行后清理）
        executor.setTaskDecorator(runnable -> {
            AdminUser admin = AdminContext.get();
            return () -> {
                try {
                    if (admin != null) {
                        AdminContext.set(admin);
                    }
                    runnable.run();
                } finally {
                    AdminContext.remove();
                }
            };
        });
        // 队列满 -> 调用者线程执行（审计不丢失）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 优雅停机：等剩余日志任务执行完毕再关闭
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        executor.initialize();
        return executor;
    }
}

