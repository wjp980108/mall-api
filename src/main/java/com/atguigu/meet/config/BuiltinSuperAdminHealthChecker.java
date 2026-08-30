package com.atguigu.meet.config;

import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.mapper.permission.user.UserMapper;
import com.atguigu.meet.model.entity.permission.user.SysUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 内置超级管理员账户健康校验器（Fail-Fast）。
 * <p>
 *   在 Spring Boot 应用启动完成、所有 Bean 就绪后 立即运行（order = HIGHEST_PRECEDENCE，优先于其他初始化）。
 *   强制校验数据库中 id = {@link PermissionConst#SUPER_ADMIN_USER_ID} = 1 的那条记录必须满足：
 *   <ol>
 *     <li>必须存在（不能被删除，也不能没初始化）</li>
 *     <li>username 精确 == "{@value PermissionConst#SUPER_ADMIN_USERNAME}"（大小写敏感，不能被篡改）</li>
 *     <li>status == "1"（启用状态，不能被禁用）</li>
 *     <li>is_deleted == 0（未逻辑删除）</li>
 *   </ol>
 *   只要任意一条不满足，就抛出 RuntimeException 阻止应用启动，Fail-Fast 原则：
 *   在发现系统核心数据被篡改 / 初始化不完整的情况下，宁可服务起不来，也绝不能带着风险运行。
 * </p>
 *
 * @author ruanbaozhong
 * @since 2026/08/30
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class BuiltinSuperAdminHealthChecker implements ApplicationRunner {

    @Autowired
    private UserMapper userMapper;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.warn("[内置超管健康校验] 开始校验 id = {} 的内置超级管理员账户完整性...", PermissionConst.SUPER_ADMIN_USER_ID);
        try {
            final Long expectedId = PermissionConst.SUPER_ADMIN_USER_ID;
            final String expectedUsername = PermissionConst.SUPER_ADMIN_USERNAME;

            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.select(SysUser::getId, SysUser::getUsername, SysUser::getStatus, SysUser::getIsDeleted)
                    .eq(SysUser::getId, expectedId);
            SysUser user = userMapper.selectOne(wrapper);

            // ① 记录必须存在
            if (user == null) {
                String msg = String.format(
                        "[内置超管健康校验] ❌ 失败：sys_user 中 id=%d 的记录不存在。" +
                                "请执行 rbac_data.sql 中的 INSERT IGNORE INTO sys_user (id, username, ...) VALUES (%d, '%s', ...) 初始化内置超级管理员。",
                        expectedId, expectedId, expectedUsername);
                log.error(msg);
                throw new IllegalStateException(msg);
            }

            // ② username 必须精确匹配（大小写敏感）
            if (!expectedUsername.equals(user.getUsername())) {
                String msg = String.format(
                        "[内置超管健康校验] ❌ 失败：id=%d 的记录 username = '%s'，但期望值是 '%s'（大小写敏感，数据库记录已被篡改！请立即核查）。",
                        expectedId, user.getUsername(), expectedUsername);
                log.error(msg);
                throw new IllegalStateException(msg);
            }

            // ③ status 必须启用
            if (!"1".equals(user.getStatus())) {
                String msg = String.format(
                        "[内置超管健康校验] ❌ 失败：内置超级管理员(id=%d, username='%s') 当前 status='%s'，必须是 '1'（启用）。系统管理员账户不能被禁用。",
                        expectedId, expectedUsername, user.getStatus());
                log.error(msg);
                throw new IllegalStateException(msg);
            }

            // ④ is_deleted 必须为 0（未逻辑删除）
            if (user.getIsDeleted() == null || user.getIsDeleted() != 0) {
                String msg = String.format(
                        "[内置超管健康校验] ❌ 失败：内置超级管理员(id=%d, username='%s') is_deleted=%s，必须是 0（不得逻辑删除）。",
                        expectedId, expectedUsername, user.getIsDeleted());
                log.error(msg);
                throw new IllegalStateException(msg);
            }

            // ⑤ 双重保护：再用 Java 精确 equals 一次（防御 DB COLLATE 的大小写不敏感让 wrapper.eq 返回了变体）
            if (!PermissionConst.SUPER_ADMIN_USERNAME.equals(user.getUsername())
                    || !PermissionConst.isReservedSuperAdminName(user.getUsername())) {
                String msg = String.format(
                        "[内置超管健康校验] ❌ 失败：Java 层二次比对不通过，username = '%s' 与常量不匹配。",
                        user.getUsername());
                log.error(msg);
                throw new IllegalStateException(msg);
            }

            log.info("[内置超管健康校验] ✅ 通过：id={}, username='{}', status={}, is_deleted=0，内置超级管理员账户完整可信。",
                    user.getId(), user.getUsername(), user.getStatus());

        } catch (IllegalStateException e) {
            // 已经输出了详细错误，直接 rethrow 阻止启动
            throw e;
        } catch (Exception e) {
            String msg = String.format(
                    "[内置超管健康校验] ❌ 异常（DB 不可达？）：%s。如果数据库尚未初始化，请先执行 rbac_schema.sql + rbac_data.sql。",
                    e.getMessage());
            log.error(msg, e);
            throw new IllegalStateException(msg, e);
        }
    }
}
