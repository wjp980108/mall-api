package com.atguigu.meet.service.permission.invite.impl;

import com.atguigu.meet.mapper.permission.invite.SysInviteCodeMapper;
import com.atguigu.meet.utils.InviteCodeUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 邀请码序列号生成器（分布式发号器）
 * <p>
 * 基于 Redis <b>INCR</b> 原子自增实现，进程间唯一、单调递增，
 * 与 {@link InviteCodeUtil#encode(long)} 配合实现 <b>seq ↔ 邀请码</b> 一一对应。
 *
 * <h3>设计要点</h3>
 * <ul>
 *   <li>Redis Key: {@code seq:invite_code}，首次 INCR 时由 Redis 自动初始化为 1</li>
 *   <li>容量上限 = 54^8 - 1 ≈ 7.23 万亿（{@link InviteCodeUtil#maxSeq()}），超过则拒绝服务</li>
 *   <li>事务说明：Redis 操作不参与 DB 事务，若 DB 插入回滚会"浪费"一个 seq，可接受</li>
 *   <li>启动兜底：见 {@link #syncSeqFromDbOnStartup()}</li>
 * </ul>
 *
 * <h3>Redis 重启/丢号兜底</h3>
 * <p>
 * 若 Redis 重启或丢号导致 {@code seq:invite_code} 不存在/被重置，单纯依赖 INCR 会从 1 重新发号，
 * 必然撞上 DB 中已有的 invite_code 唯一索引。本类通过 {@link #syncSeqFromDbOnStartup()}
 * 在应用启动时扫描 {@code sys_invite_code} 表的 MAX(seq) 并同步到 Redis，确保下次 INCR 一定大于 DB 已有值。
 * <p>
 * 同步采用 Lua 脚本做<b>原子化 compare-and-set</b>：仅当 Redis 当前值 &lt; DB 最大值时才覆盖，
 * 避免多实例并发启动时互相覆盖、或 Redis 已领先时被回退。
 */
@Component
@Slf4j
public class RedisInviteSeqGenerator {

    /** Redis 中存储邀请码序列号的 Key */
    private static final String SEQ_KEY = "seq:invite_code";

    /**
     * Lua 脚本：原子化比较并设置
     * <p>逻辑：若 Redis 当前值不存在或小于 DB 最大值，则覆盖为 DB 最大值；否则不动
     * <ul>
     *   <li>返回 1：已同步</li>
     *   <li>返回 0：Redis 已领先或相等，无需同步</li>
     * </ul>
     */
    private static final DefaultRedisScript<Long> SYNC_SCRIPT;

    static {
        SYNC_SCRIPT = new DefaultRedisScript<>();
        SYNC_SCRIPT.setScriptText(
                "local current = redis.call('GET', KEYS[1]) " +
                "local dbMax = tonumber(ARGV[1]) " +
                "local currentNum = tonumber(current) " +
                "if (not currentNum) or (currentNum < dbMax) then " +
                "    redis.call('SET', KEYS[1], ARGV[1]) " +
                "    return 1 " +
                "else " +
                "    return 0 " +
                "end"
        );
        SYNC_SCRIPT.setResultType(Long.class);
    }

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private SysInviteCodeMapper sysInviteCodeMapper;

    /**
     * 应用启动时自动同步 DB 最大 seq → Redis
     * <p>兜底场景：Redis 重启/丢号后从 1 重新发号会导致碰撞，
     * 此方法从 DB 取 MAX(seq) 同步到 Redis，确保下次 INCR 一定大于 DB 已存在的 seq。
     * <p>同步失败不阻塞应用启动（Redis 可能未就绪），首次发号时若 Redis 异常会自然暴露。
     * <p>但若属于<b>数据库 schema 缺失</b>（例如 sys_invite_code 缺少 seq 列），会打印 ERROR
     * 并给出明确迁移指引，避免首次发号时才暴露模糊的 SQL 错误。
     */
    @PostConstruct
    public void syncSeqFromDbOnStartup() {
        try {
            syncSeqFromDb();
        } catch (Exception e) {
            if (isMissingSeqColumn(e)) {
                log.error(
                        "Redis seq 启动同步失败：sys_invite_code 表缺少 seq 列，请执行 rbac.sql 中的" +
                        "【0.1.1 存量库迁移】三步脚本（ADD COLUMN → 回填 seq → 加 NOT NULL/UNIQUE）。" +
                        " 错误详情: {}",
                        extractRootMsg(e)
                );
            } else {
                log.warn("Redis seq 启动同步失败（Redis 不可用？），将在首次发号时报错: {}", extractRootMsg(e));
            }
        }
    }

    /**
     * 手动触发：DB 最大 seq → Redis 原子同步
     * <p>供运维或管理端调用，例如 Redis 主备切换后主动同步
     */
    public void syncSeqFromDb() {
        Long dbMax = sysInviteCodeMapper.selectMaxSeq();
        if (dbMax == null) {
            log.info("Redis seq 启动同步：sys_invite_code 表为空，跳过（首次发号将从 seq=1 开始）");
            return;
        }

        Long result = redisTemplate.execute(
                SYNC_SCRIPT,
                Collections.singletonList(SEQ_KEY),
                String.valueOf(dbMax)
        );
        if (result != null && result == 1L) {
            log.info("Redis seq 启动同步：DB 最大 seq={}, Redis 已同步到该值", dbMax);
        } else {
            log.info("Redis seq 启动同步：DB 最大 seq={}, Redis 已领先或相等，无需同步", dbMax);
        }
    }

    /**
     * 获取下一个全局自增序列号
     * <p>首次调用时 Redis 自动初始化为 1，之后单调递增
     *
     * @return 全局唯一的自增序列号
     * @throws IllegalStateException Redis 异常 或 序列号超过邀请码容量上限
     */
    public long nextSeq() {
        try {
            Long seq = redisTemplate.opsForValue().increment(SEQ_KEY);
            if (seq == null) {
                throw new IllegalStateException("Redis INCR 返回 null，疑似连接异常");
            }
            // 容量校验：超过 54^8 - 1 则拒绝服务，避免编码溢出
            if (seq > InviteCodeUtil.maxSeq()) {
                throw new IllegalStateException(String.format(
                        "邀请码序列号已达容量上限 %d，当前 seq=%d", InviteCodeUtil.maxSeq(), seq));
            }
            return seq;
        } catch (Exception e) {
            // 兜底：若异常根因是 DB 缺少 seq 列（例如之前启动同步被静默吞掉、且 Redis 也失败走到 DB 兜底）
            // 给出清晰的行动指引
            if (isMissingSeqColumn(e)) {
                throw new IllegalStateException(
                        "生成邀请码失败：sys_invite_code 表缺少 seq 列，" +
                        "请执行 rbac.sql 中的【0.1.1 存量库迁移】三步脚本后再试。", e);
            }
            throw e;
        }
    }

    // ======================== 辅助方法 ========================

    /**
     * 判断异常链是否为 "Unknown column 'seq' in 'field list'" 类 schema 缺失错误
     */
    private static boolean isMissingSeqColumn(Throwable t) {
        Throwable cur = t;
        while (cur != null) {
            String msg = cur.getMessage();
            if (msg != null && msg.contains("Unknown column") && msg.contains("'seq'")) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }

    /**
     * 提取异常链最底层的异常消息，避免打印一堆包装层
     */
    private static String extractRootMsg(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName() + ": " + root.getMessage();
    }
}
