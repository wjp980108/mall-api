package com.atguigu.meet.service.permission.user;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.mapper.permission.role.SysRoleMapper;
import com.atguigu.meet.mapper.permission.user.UserMapper;
import com.atguigu.meet.mapper.permission.userRole.SysUserRoleMapper;
import com.atguigu.meet.model.dto.permission.user.UserCreateDTO;
import com.atguigu.meet.model.entity.permission.user.SysUser;
import com.atguigu.meet.model.entity.permission.userRole.SysUserRole;
import com.atguigu.meet.service.permission.invite.InviteCodeService;
import com.atguigu.meet.service.permission.user.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserServiceImpl.createUser 原子性测试
 * <p>
 * 验证场景：后台创建用户时，为新用户生成邀请码失败 → 异常向上抛出，由外层
 * {@code @Transactional(rollbackFor=Exception.class)} 触发整体回滚，
 * 用户落库与角色绑定虽已发生，但会随事务回滚而不持久化，杜绝半成品。
 * <p>
 * 说明：事务回滚由 Spring AOP 在容器层保证，纯 Mockito 单元测试通过断言
 * 「异常向上抛出 + 关键写入已被触发」验证触发条件；接口返回失败由全局异常
 * 处理器将该异常转换为 500 响应。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private SysUserRoleMapper sysUserRoleMapper;

    @Mock
    private SysRoleMapper sysRoleMapper;

    @Mock
    private InviteCodeService inviteCodeService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void injectBaseMapper() {
        // Mockito @InjectMocks 默认不注入 ServiceImpl 父类的 baseMapper 字段，
        // 而 getOne 内部依赖 getBaseMapper()。手动反射注入，否则会抛
        // "baseMapper can not be null"。
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    @Test
    void createUser_inviteCodeGenerationFails_rollsBackAll() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("newUser2");
        dto.setPhone("13800000002");
        dto.setPassword("pwd456");
        dto.setRoleIds(Collections.singletonList(5L));

        // 用户名/手机号查询均返回空 → 未占用。
        // ServiceImpl.getOne 在不同 mybatis-plus 版本可能走 selectList 或 selectOne，
        // 两者都 stub 并用 lenient 修饰，避免 strict stubbing 误报。
        lenient().when(userMapper.selectList(any())).thenReturn(Collections.emptyList());
        lenient().when(userMapper.selectOne(any())).thenReturn(null);
        // 角色 ID=5 有效且启用（selectCount 返回与 roleIds.size() 相等）
        when(sysRoleMapper.selectCount(any())).thenReturn(1L);
        // 密码加密
        when(passwordEncoder.encode("pwd456")).thenReturn("encodedPwd");
        // 用户落库时回填自增 ID，供后续 generateInviteCode(user.getId()) 使用
        when(userMapper.insert(any(SysUser.class))).thenAnswer(inv -> {
            SysUser u = inv.getArgument(0);
            u.setId(200L);
            return 1;
        });
        // 邀请码生成失败 → 抛异常触发事务回滚
        doThrow(new RuntimeException("redis down"))
                .when(inviteCodeService).generateInviteCode(200L);

        // 调用 createUser：异常应向上抛出（@Transactional 据此标记回滚）
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.createUser(dto));
        assertEquals("redis down", ex.getMessage());

        // 断言：异常抛出前已发生的写入会随事务回滚而不持久化
        // 1) sys_user 已写入（事务回滚后不留痕）
        verify(userMapper).insert(any(SysUser.class));
        // 2) sys_user_role 已写入（事务回滚后不留痕）
        verify(sysUserRoleMapper).insert(any(SysUserRole.class));
    }

    /**
     * 成功路径回归：createUser 所有环节（用户落库、角色绑定、新用户自己的邀请码
     * 生成）均成功 → 接口返回 200，sys_user 与 sys_invite_code 对同一新用户
     * 同时可见，无半成品。
     * <p>
     * 单元测试层面通过 verify 关键写入被触发 + Response 成功 来覆盖；
     * 真实「同时落库」由外层事务提交保证。
     */
    @Test
    void createUser_allStepsSucceed_userAndInviteCodeBothPersisted() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setUsername("newUser4");
        dto.setPhone("13800000004");
        dto.setPassword("pwd999");
        dto.setRoleIds(Collections.singletonList(5L));

        lenient().when(userMapper.selectList(any())).thenReturn(Collections.emptyList());
        lenient().when(userMapper.selectOne(any())).thenReturn(null);
        when(sysRoleMapper.selectCount(any())).thenReturn(1L);
        when(passwordEncoder.encode("pwd999")).thenReturn("encodedPwd");
        when(userMapper.insert(any(SysUser.class))).thenAnswer(inv -> {
            SysUser u = inv.getArgument(0);
            u.setId(400L);
            return 1;
        });
        // 邀请码生成成功
        when(inviteCodeService.generateInviteCode(400L))
                .thenReturn(Response.ok("生成邀请码成功", null));

        Response<?> resp = userService.createUser(dto);

        // 接口返回成功
        assertEquals(200, resp.getCode());
        // 用户落库已触发（sys_user 写入，事务提交后与 sys_invite_code 同时可见）
        verify(userMapper).insert(any(SysUser.class));
        // 角色绑定已触发（sys_user_role 写入）
        verify(sysUserRoleMapper).insert(any(SysUserRole.class));
        // 邀请码生成已触发（与 sys_user 同一事务，对同一新用户同时可见，无半成品）
        verify(inviteCodeService).generateInviteCode(400L);
    }
}
