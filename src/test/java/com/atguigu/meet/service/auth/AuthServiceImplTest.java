package com.atguigu.meet.service.auth;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.mapper.permission.user.UserMapper;
import com.atguigu.meet.mapper.permission.userRole.SysUserRoleMapper;
import com.atguigu.meet.model.dto.auth.AuthRegisterDTO;
import com.atguigu.meet.model.entity.permission.invite.SysInviteCode;
import com.atguigu.meet.model.entity.permission.user.SysUser;
import com.atguigu.meet.model.entity.permission.userRole.SysUserRole;
import com.atguigu.meet.service.auth.impl.AuthServiceImpl;
import com.atguigu.meet.service.permission.invite.InviteCodeService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AuthServiceImpl.register 原子性测试
 * <p>
 * 验证场景：为新用户生成邀请码失败时，异常向上抛出，由外层
 * {@code @Transactional(rollbackFor=Exception.class)} 触发整体回滚。
 * 用户落库、角色绑定、邀请流水处理虽已被调用，但会随事务回滚而不持久化，
 * 杜绝「有用户无邀请码」的半成品。
 * <p>
 * 说明：事务回滚行为由 Spring AOP 在容器层保证，纯 Mockito 单元测试通过
 * 断言「异常向上抛出 + 关键写入已被触发」来验证触发条件；接口返回失败
 * 由全局异常处理器将该异常转换为 500 响应（见全局异常处理链路）。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private SysUserRoleMapper sysUserRoleMapper;

    @Mock
    private InviteCodeService inviteCodeService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void injectBaseMapper() {
        // Mockito @InjectMocks 默认不注入 ServiceImpl 父类的 baseMapper 字段，
        // 而 getOne 内部依赖 getBaseMapper()。手动反射注入，否则会抛
        // "baseMapper can not be null"。
        ReflectionTestUtils.setField(authService, "baseMapper", userMapper);
    }

    @Test
    void register_inviteCodeGenerationFails_rollsBackAll() {
        // H5 场景：用户填写邀请码 ABC123
        AuthRegisterDTO dto = new AuthRegisterDTO();
        dto.setUsername("newUser1");
        dto.setPhone("13800000001");
        dto.setPassword("pwd123");
        dto.setInviteCode("ABC123");

        SysInviteCode inviteCode = new SysInviteCode();
        inviteCode.setInviterId(99L);

        // 用户名/手机号查询均返回空 → 未占用。
        // ServiceImpl.getOne 在不同 mybatis-plus 版本可能走 selectList 或 selectOne，
        // 两者都 stub 并用 lenient 修饰，避免 strict stubbing 误报。
        lenient().when(userMapper.selectList(any())).thenReturn(Collections.emptyList());
        lenient().when(userMapper.selectOne(any())).thenReturn(null);
        // 邀请码校验通过
        when(inviteCodeService.validateInviteCode("ABC123")).thenReturn(inviteCode);
        // 密码加密
        when(passwordEncoder.encode("pwd123")).thenReturn("encodedPwd");
        // 用户落库时回填自增 ID，供后续 generateInviteCode(user.getId()) 使用
        when(userMapper.insert(any(SysUser.class))).thenAnswer(inv -> {
            SysUser u = inv.getArgument(0);
            u.setId(100L);
            return 1;
        });
        // 邀请码生成失败 → 抛异常触发事务回滚
        doThrow(new RuntimeException("redis down"))
                .when(inviteCodeService).generateInviteCode(100L);

        // 调用 register：异常应向上抛出（@Transactional 据此标记回滚）
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.register(dto));
        assertEquals("redis down", ex.getMessage());

        // 断言：异常抛出前已发生的写入会随事务回滚而不持久化
        // 1) sys_user 已写入（事务回滚后不留痕）
        verify(userMapper).insert(any(SysUser.class));
        // 2) sys_user_role 已写入（事务回滚后不留痕）
        verify(sysUserRoleMapper).insert(any(SysUserRole.class));
        // 3) H5 场景：邀请流水处理已触发，其内部 sys_invite_record.insert
        //    同样并入外层事务，会随之回滚（原邀请码已用名额不会增加）
        verify(inviteCodeService).processInviteRecord(eq(inviteCode), eq(100L), eq("13800000001"));
    }

    /**
     * 成功路径回归：register 所有环节（用户落库、角色绑定、邀请流水核销、
     * 新用户自己的邀请码生成）均成功 → 接口返回 200，sys_user 与
     * sys_invite_code 对同一新用户同时可见，无半成品。
     * <p>
     * 单元测试层面通过 verify 关键写入被触发 + Response 成功 来覆盖；
     * 真实「同时落库」由外层事务提交保证。
     */
    @Test
    void register_allStepsSucceed_userAndInviteCodeBothPersisted() {
        // H5 场景：用户填写邀请码 XYZ9876
        AuthRegisterDTO dto = new AuthRegisterDTO();
        dto.setUsername("newUser3");
        dto.setPhone("13800000003");
        dto.setPassword("pwd789");
        dto.setInviteCode("XYZ9876");

        SysInviteCode inviteCode = new SysInviteCode();
        inviteCode.setInviterId(99L);

        lenient().when(userMapper.selectList(any())).thenReturn(Collections.emptyList());
        lenient().when(userMapper.selectOne(any())).thenReturn(null);
        when(inviteCodeService.validateInviteCode("XYZ9876")).thenReturn(inviteCode);
        when(passwordEncoder.encode("pwd789")).thenReturn("encodedPwd");
        when(userMapper.insert(any(SysUser.class))).thenAnswer(inv -> {
            SysUser u = inv.getArgument(0);
            u.setId(300L);
            return 1;
        });
        // 邀请码生成成功
        when(inviteCodeService.generateInviteCode(300L))
                .thenReturn(Response.ok("生成邀请码成功", null));

        Response<?> resp = authService.register(dto);

        // 接口返回成功
        assertEquals(200, resp.getCode());
        // 用户落库已触发（sys_user 写入，事务提交后与 sys_invite_code 同时可见）
        verify(userMapper).insert(any(SysUser.class));
        // 角色绑定已触发（sys_user_role 写入）
        verify(sysUserRoleMapper).insert(any(SysUserRole.class));
        // H5 场景：邀请流水处理已触发
        verify(inviteCodeService).processInviteRecord(eq(inviteCode), eq(300L), eq("13800000003"));
        // 邀请码生成已触发（与 sys_user 同一事务，对同一新用户同时可见，无半成品）
        verify(inviteCodeService).generateInviteCode(300L);
    }
}
