package com.atguigu.meet.model.entity.permission.user;

import lombok.Data;

import java.util.Set;

/**
 * @Description
 * @Date 2026-06-03 16:39
 */
@Data
public class AdminUser {
    private Long userId;
    private String phone;  // 管理员手机号
    private String username;

    /**
     * 是否为系统内置超级管理员（true：userId+username 已与 DB 实时核对一致）。
     * <p>
     * 该字段由 {@code JwtAuthenticationFilter} 在每次请求从数据库加载时填入，
     * 是权限放行的最终可信依据，不依赖 JWT claims 中的 username（防止 token 伪造）。
     */
    private boolean builtinSuperAdmin;

    /**
     * 用户权限集合（从Redis/DB加载，存入ThreadLocal上下文）
     */
    private Set<String> permissions;

    /**
     * 用户角色编码集合（用于判断超级管理员等特殊角色）
     */
    private Set<String> roleCodes;
}