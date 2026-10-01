package com.atguigu.meet.service.permission.invite;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.exception.BusinessException;
import com.atguigu.meet.mapper.permission.invite.SysInviteCodeMapper;
import com.atguigu.meet.mapper.permission.invite.SysInviteRecordMapper;
import com.atguigu.meet.mapper.permission.user.UserMapper;
import com.atguigu.meet.model.entity.permission.invite.SysInviteCode;
import com.atguigu.meet.model.entity.permission.invite.SysInviteRecord;
import com.atguigu.meet.model.entity.permission.user.SysUser;
import com.atguigu.meet.model.vo.permission.invite.CompensateResultVO;
import com.atguigu.meet.service.permission.invite.impl.InviteCodeServiceImpl;
import com.atguigu.meet.service.permission.invite.impl.RedisInviteSeqGenerator;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * InviteCodeServiceImpl 单元测试
 * <p>
 * 覆盖：
 * 1) compensateMissingInviteCodes 存量补偿：已有邀请码用户被跳过、无码用户补生成；
 * 2) validateInviteCode 人数上限校验：0 表示不限制、正数仍限制；
 * 3) processInviteRecord 核销：0 不自动停用、正数满额自动停用。
 */
@ExtendWith(MockitoExtension.class)
class InviteCodeServiceImplTest {

    @Mock
    private SysInviteCodeMapper sysInviteCodeMapper;

    @Mock
    private SysInviteRecordMapper sysInviteRecordMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RedisInviteSeqGenerator seqGenerator;

    /**
     * 自注入代理：补偿场景下逐个调用 generateInviteCode 时，必须走 Spring 代理
     * 才能让 @Transactional 生效（this 直接调用不走代理）。@Lazy 避免启动期循环依赖。
     */
    @Mock
    private InviteCodeService self;

    @InjectMocks
    private InviteCodeServiceImpl inviteCodeService;

    /**
     * 纯 Mockito 环境无 MyBatis-Plus 启动流程，手动初始化实体列缓存，
     * 使 LambdaWrapper 可正常解析列名。
     */
    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, SysInviteCode.class);
        TableInfoHelper.initTableInfo(assistant, SysUser.class);
    }

    @Test
    void compensate_skipsUsersWithExistingCode_andGeneratesForMissing() {
        // 存量用户：u1 已有邀请码，u2 无邀请码
        SysUser u1 = new SysUser();
        u1.setId(1L);
        u1.setUsername("u1");
        SysUser u2 = new SysUser();
        u2.setId(2L);
        u2.setUsername("u2");
        when(userMapper.selectList(null)).thenReturn(Arrays.asList(u1, u2));

        // u1 已有码：generateInviteCode 幂等返回"邀请码已存在"，原码不变
        SysInviteCode existingCode = new SysInviteCode();
        existingCode.setInviterId(1L);
        existingCode.setInviteCode("OLDCODE1");
        when(self.generateInviteCode(1L))
                .thenReturn(Response.ok("邀请码已存在", existingCode));

        // u2 无码：generateInviteCode 新生成
        SysInviteCode newCode = new SysInviteCode();
        newCode.setInviterId(2L);
        newCode.setInviteCode("NEWCODE2");
        when(self.generateInviteCode(2L))
                .thenReturn(Response.ok("邀请码生成成功", newCode));

        // 调用补偿接口
        Response<CompensateResultVO> resp = inviteCodeService.compensateMissingInviteCodes();

        // 接口成功
        assertEquals(200, resp.getCode());
        CompensateResultVO result = resp.getData();
        // u2 新生成 1 个，u1 跳过 1 个，无失败
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getSkippedCount());
        assertTrue(result.getFailures().isEmpty());

        // u1 被幂等检查跳过（调用 generateInviteCode 但返回"已存在"，原邀请码不变）
        verify(self).generateInviteCode(1L);
        assertEquals("OLDCODE1", existingCode.getInviteCode());
        // u2 补生成新码
        verify(self).generateInviteCode(2L);
    }

    @Test
    void validateInviteCode_unlimited_doesNotRejectHighUsage() {
        SysInviteCode code = new SysInviteCode();
        code.setInviteCode("UNLIMITED");
        code.setMaxInviteNum(0);
        code.setUsedInviteNum(999);
        code.setStatus(0);
        when(sysInviteCodeMapper.selectOne(any())).thenReturn(code);

        SysInviteCode result = inviteCodeService.validateInviteCode("UNLIMITED");

        assertEquals(code, result);
    }

    @Test
    void validateInviteCode_limited_rejectsWhenQuotaReached() {
        SysInviteCode code = new SysInviteCode();
        code.setInviteCode("LIMITED");
        code.setMaxInviteNum(3);
        code.setUsedInviteNum(3);
        code.setStatus(0);
        when(sysInviteCodeMapper.selectOne(any())).thenReturn(code);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> inviteCodeService.validateInviteCode("LIMITED"));
        assertEquals("邀请码已达到最大邀请人数", ex.getMessage());
    }

    @Test
    void processInviteRecord_unlimited_doesNotSetStatusToFull() {
        SysInviteCode inviteCode = new SysInviteCode();
        inviteCode.setId(1L);
        inviteCode.setInviteCode("UNLIMITED");
        inviteCode.setInviterId(10L);
        inviteCode.setMaxInviteNum(0);
        inviteCode.setUsedInviteNum(5);
        inviteCode.setStatus(0);

        when(sysInviteCodeMapper.update(isNull(), any())).thenReturn(1);
        when(userMapper.update(isNull(), any())).thenReturn(0);

        inviteCodeService.processInviteRecord(inviteCode, 99L, "13800000000");

        // 已邀请流水已落库
        verify(sysInviteRecordMapper).insert(any(SysInviteRecord.class));

        // 更新 SQL 只设置 used_invite_num，不应包含 status=2
        ArgumentCaptor<LambdaUpdateWrapper<SysInviteCode>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(sysInviteCodeMapper).update(isNull(), captor.capture());
        String sqlSet = captor.getValue().getSqlSet();
        assertTrue(sqlSet.toLowerCase().contains("used_invite_num"));
        assertFalse(sqlSet.toLowerCase().contains("status"));
    }

    @Test
    void processInviteRecord_limited_setsStatusToFullWhenQuotaReached() {
        SysInviteCode inviteCode = new SysInviteCode();
        inviteCode.setId(2L);
        inviteCode.setInviteCode("LIMITED");
        inviteCode.setInviterId(10L);
        inviteCode.setMaxInviteNum(3);
        inviteCode.setUsedInviteNum(2);
        inviteCode.setStatus(0);

        when(sysInviteCodeMapper.update(isNull(), any())).thenReturn(1);
        when(userMapper.update(isNull(), any())).thenReturn(0);

        inviteCodeService.processInviteRecord(inviteCode, 100L, "13800000001");

        ArgumentCaptor<LambdaUpdateWrapper<SysInviteCode>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(sysInviteCodeMapper).update(isNull(), captor.capture());
        String sqlSet = captor.getValue().getSqlSet();
        assertTrue(sqlSet.toLowerCase().contains("used_invite_num"));
        assertTrue(sqlSet.toLowerCase().contains("status"));
    }

    /**
     * 自愈核心场景：Redis 丢号后首次插入撞唯一键，自动同步 DB MAX(seq)，
     * 第二次发号插入成功。用户仅提交 1 次即成功。
     */
    @Test
    void generateInviteCode_firstInsertDuplicate_syncAndRetrySucceeds() {
        // 用户尚无邀请码
        when(sysInviteCodeMapper.selectOne(any())).thenReturn(null);
        // 第一次发出 seq=1（必撞老数据），同步后第二次发出 seq=38
        when(seqGenerator.nextSeq()).thenReturn(1L, 38L);
        // 第一次插入撞唯一键，第二次成功
        when(sysInviteCodeMapper.insert(any(SysInviteCode.class)))
                .thenThrow(new DuplicateKeyException("Duplicate entry for key 'uk_seq'"))
                .thenReturn(1);

        Response<?> resp = inviteCodeService.generateInviteCode(100L);

        // 1 次提交即成功
        assertEquals(200, resp.getCode());
        // DB MAX -> Redis 同步恰好触发 1 次
        verify(seqGenerator, times(1)).syncSeqFromDb();
        // 最终插入的是同步后的新号
        ArgumentCaptor<SysInviteCode> captor = ArgumentCaptor.forClass(SysInviteCode.class);
        verify(sysInviteCodeMapper, times(2)).insert(captor.capture());
        SysInviteCode inserted = captor.getValue();
        assertEquals(38L, inserted.getSeq());
    }

    /**
     * 并发竞争场景：自愈后第二次发号仍撞键（被其他注册抢先），第三次成功。
     */
    @Test
    void generateInviteCode_secondAttemptAlsoDuplicate_thirdSucceeds() {
        when(sysInviteCodeMapper.selectOne(any())).thenReturn(null);
        when(seqGenerator.nextSeq()).thenReturn(1L, 2L, 39L);
        when(sysInviteCodeMapper.insert(any(SysInviteCode.class)))
                .thenThrow(new DuplicateKeyException("dup 1"))
                .thenThrow(new DuplicateKeyException("dup 2"))
                .thenReturn(1);

        Response<?> resp = inviteCodeService.generateInviteCode(100L);

        assertEquals(200, resp.getCode());
        // 前两次失败各触发一次同步
        verify(seqGenerator, times(2)).syncSeqFromDb();
    }

    /**
     * 重试 3 次仍失败：抛出带明确文案的业务异常，前两次失败触发同步。
     */
    @Test
    void generateInviteCode_threeAttemptsFail_throwsBusinessException() {
        when(sysInviteCodeMapper.selectOne(any())).thenReturn(null);
        when(seqGenerator.nextSeq()).thenReturn(1L, 2L, 3L);
        when(sysInviteCodeMapper.insert(any(SysInviteCode.class)))
                .thenThrow(new DuplicateKeyException("dup"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> inviteCodeService.generateInviteCode(100L));
        assertEquals("注册服务暂时不可用，请稍后重试", ex.getMessage());
        // 最后一次失败不再同步
        verify(seqGenerator, times(2)).syncSeqFromDb();
    }

    /**
     * Redis 连接失败：无法发号，转为明确业务提示，且不执行插入。
     */
    @Test
    void generateInviteCode_redisConnectionFails_throwsBusinessException() {
        when(sysInviteCodeMapper.selectOne(any())).thenReturn(null);
        when(seqGenerator.nextSeq())
                .thenThrow(new RedisConnectionFailureException("connection refused"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> inviteCodeService.generateInviteCode(100L));
        assertEquals("注册服务暂时不可用，请稍后重试", ex.getMessage());
        verify(sysInviteCodeMapper, never()).insert(any(SysInviteCode.class));
    }

    /**
     * Redis 命令超时：转为明确业务提示。
     */
    @Test
    void generateInviteCode_redisTimeout_throwsBusinessException() {
        when(sysInviteCodeMapper.selectOne(any())).thenReturn(null);
        when(seqGenerator.nextSeq())
                .thenThrow(new QueryTimeoutException("command timed out"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> inviteCodeService.generateInviteCode(100L));
        assertEquals("注册服务暂时不可用，请稍后重试", ex.getMessage());
    }
}
