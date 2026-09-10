package com.atguigu.meet.service.permission.invite;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.mapper.permission.invite.SysInviteCodeMapper;
import com.atguigu.meet.mapper.permission.invite.SysInviteRecordMapper;
import com.atguigu.meet.mapper.permission.user.UserMapper;
import com.atguigu.meet.model.entity.permission.invite.SysInviteCode;
import com.atguigu.meet.model.entity.permission.user.SysUser;
import com.atguigu.meet.model.vo.permission.invite.CompensateResultVO;
import com.atguigu.meet.service.permission.invite.impl.InviteCodeServiceImpl;
import com.atguigu.meet.service.permission.invite.impl.RedisInviteSeqGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * InviteCodeServiceImpl.compensateMissingInviteCodes 存量补偿测试
 * <p>
 * 验证 spec「存量无邀请码用户必须一次性补偿生成」：
 * 已有邀请码的用户被跳过、原邀请码保持不变；无邀请码的用户补生成新码。
 * <p>
 * 通过 self 代理 mock 控制 generateInviteCode 的返回，断言 success/skip 计数
 * 与各用户的调用路径。
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
     * 补偿方法通过 self 走代理调用 generateInviteCode（确保 @Transactional 生效）。
     * 测试中用 mock 控制其返回，模拟「已有码 / 新生成」两条路径。
     */
    @Mock
    private InviteCodeService self;

    @InjectMocks
    private InviteCodeServiceImpl inviteCodeService;

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
}
