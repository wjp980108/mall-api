package com.atguigu.meet.config;

import com.atguigu.meet.constant.PermissionConst;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 内置超级管理员 userId 判定组件。
 * <p>
 *   【方案升级：启动时动态缓存 DB id → 硬编码常量 + Fail-Fast 启动校验】
 *   为什么使用硬编码常量而不是「启动时查 DB 缓存 id」？
 *   因为后者存在致命攻击面：如果攻击者通过数据库直连：
 *     ① 把原 admin(id=常量) 删掉；② 重新 INSERT 一条 username='admin' 的新记录（新自增主键）
 *   应用重启后「查 DB 缓存 id」就会把那条伪造账户的 id 缓存为「合法内置超管 id」，攻击者获得全权限。
 *   而硬编码常量方案下，只要 id != PermissionConst.SUPER_ADMIN_USER_ID，数据库中哪怕有 100 条 username='admin' 都不可能被识别为内置超管。
 * </p>
 * <p>
 *   配合 {@link BuiltinSuperAdminHealthChecker} Fail-Fast 启动校验：
 *   DB 中 id = {@link PermissionConst#SUPER_ADMIN_USER_ID} 的记录必须存在、username 精确 == "admin"、未禁用、未删除，
 *   不满足直接阻止应用启动，从根源避免「数据被篡改了还在运行」的风险。
 * </p>
 *
 * @author ruanbaozhong
 * @since 2026/08/30
 */
@Component
@Slf4j
public class BuiltinSuperAdminIdCache {

    /**
     * 获取内置超级管理员的固定 userId（硬编码常量，永久不变，零查询开销）
     *
     * @return 恒等于 {@link PermissionConst#SUPER_ADMIN_USER_ID}
     */
    public Long get() {
        return PermissionConst.SUPER_ADMIN_USER_ID;
    }

    /**
     * 判断给定 userId 是否为内置超级管理员（纯常量比较，零 DB 开销）
     *
     * @param userId 要判断的 userId，可为 null
     * @return true = 是内置超管
     */
    public boolean isBuiltinAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        return userId.longValue() == PermissionConst.SUPER_ADMIN_USER_ID;
    }
}
