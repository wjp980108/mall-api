package com.atguigu.meet.config;

import com.atguigu.meet.utils.SysConfigUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 系统配置缓存预加载器
 * <p>
 * 项目启动完成后将 sys_config 全部分组配置一次性加载进 Redis
 * （key = sys_config:group:{group}，不设 TTL）。
 * 作用：
 * 1. 规避"缓存永久过期，服务重启后缓存丢失"——启动即预热，重启后立即重建；
 * 2. 运行期缓存 miss 也会按分组回填（SysConfigUtil 内兜底），双保险；
 * 3. 加载失败不阻止启动（仅记录错误，读取路径有回查数据库兜底），
 *    与 BuiltinSuperAdminHealthChecker 的 Fail-Fast 语义区分：配置数据非安全底座。
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@Slf4j
public class SysConfigCacheLoader implements ApplicationRunner {

    @Autowired
    private SysConfigUtil sysConfigUtil;

    @Override
    public void run(ApplicationArguments args) {
        try {
            int groups = sysConfigUtil.loadAllGroupsCache();
            log.info("[系统配置] 启动预加载完成，共 {} 个分组配置已写入 Redis 缓存", groups);
        } catch (Exception e) {
            log.error("[系统配置] 启动预加载失败（Redis/DB 不可达？）。运行期读取时将回查数据库并回填缓存。", e);
        }
    }
}
