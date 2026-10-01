package com.atguigu.meet.service.permission.invite.impl;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.exception.BusinessException;
import com.atguigu.meet.mapper.permission.invite.SysInviteCodeMapper;
import com.atguigu.meet.mapper.permission.invite.SysInviteRecordMapper;
import com.atguigu.meet.mapper.permission.user.UserMapper;
import com.atguigu.meet.model.entity.permission.invite.SysInviteCode;
import com.atguigu.meet.model.entity.permission.invite.SysInviteRecord;
import com.atguigu.meet.model.entity.permission.user.SysUser;
import com.atguigu.meet.model.vo.permission.invite.CompensateResultVO;
import com.atguigu.meet.service.permission.invite.InviteCodeService;
import com.atguigu.meet.utils.InviteCodeUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 邀请码服务实现
 */
@Service
@Slf4j
public class InviteCodeServiceImpl implements InviteCodeService {

    /**
     * 邀请码生成最大尝试次数：插入撞唯一键时，自动同步 DB MAX(seq) 后重试。
     */
    private static final int MAX_GENERATE_ATTEMPTS = 3;

    @Autowired
    private SysInviteCodeMapper sysInviteCodeMapper;

    @Autowired
    private SysInviteRecordMapper sysInviteRecordMapper;

    @Autowired
    private RedisInviteSeqGenerator seqGenerator;

    @Autowired
    private UserMapper userMapper;

    /**
     * 自注入代理：补偿场景下逐个调用 generateInviteCode 时，必须走 Spring 代理
     * 才能让 @Transactional 生效（this 直接调用不走代理）。@Lazy 避免启动期循环依赖。
     */
    @Autowired
    @Lazy
    private InviteCodeService self;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response generateInviteCode(Long userId) {
        // 1. 检查是否已生成过邀请码（1人1码）
        LambdaQueryWrapper<SysInviteCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysInviteCode::getInviterId, userId);
        SysInviteCode existing = sysInviteCodeMapper.selectOne(wrapper);
        if (existing != null) {
            return Response.ok("邀请码已存在", existing);
        }

        // 2. 发号 -> 编码 -> 插入。
        //    若 Redis 丢号导致 seq 落后于 DB，插入会撞 uk_seq/uk_invite_code；
        //    此时在同一事务内自动同步 DB MAX(seq) 到 Redis 后重新发号重试（MySQL/InnoDB
        //    单条语句失败不会中止事务），最多 MAX_GENERATE_ATTEMPTS 次。
        for (int attempt = 1; attempt <= MAX_GENERATE_ATTEMPTS; attempt++) {
            long seq;
            try {
                seq = seqGenerator.nextSeq();
            } catch (RedisConnectionFailureException | QueryTimeoutException | RedisSystemException e) {
                // Redis 不可达/命令超时：无法发号，转为明确业务提示（外层事务回滚）
                log.warn("[邀请码] Redis发号不可用，userId={}, error={}", userId, e.getMessage());
                throw new BusinessException("注册服务暂时不可用，请稍后重试");
            }

            String code = InviteCodeUtil.encode(seq);

            SysInviteCode inviteCode = new SysInviteCode();
            inviteCode.setSeq(seq);
            inviteCode.setInviteCode(code);
            inviteCode.setInviterId(userId);
            inviteCode.setStatus(0);
            inviteCode.setMaxInviteNum(0);
            inviteCode.setUsedInviteNum(0);
            try {
                sysInviteCodeMapper.insert(inviteCode);
            } catch (DuplicateKeyException e) {
                if (attempt < MAX_GENERATE_ATTEMPTS) {
                    log.warn("[邀请码] 发号撞唯一键(attempt={})，同步DB MAX(seq)后重试，userId={}", attempt, userId);
                    seqGenerator.syncSeqFromDb();
                    continue;
                }
                log.error("[邀请码] 发号撞唯一键，重试{}次仍失败，userId={}", MAX_GENERATE_ATTEMPTS, userId);
                throw new BusinessException("注册服务暂时不可用，请稍后重试");
            }
            return Response.ok("邀请码生成成功", inviteCode);
        }

        // 理论不可达：循环中每次失败均 continue 或 throw
        throw new BusinessException("注册服务暂时不可用，请稍后重试");
    }

    /**
     * 存量补偿实现：
     * <ul>
     *   <li>扫描全部 sys_user（不在此处精确过滤无码用户，复用 generateInviteCode
     *       的「1 人 1 码」幂等检查，避免并发窗口期遗漏）</li>
     *   <li>逐个通过 {@code self.generateInviteCode(userId)} 调用走代理，确保
     *       {@code @Transactional(REQUIRED)} 生效；无外层事务时每次独立提交，
     *       单个失败只回滚该用户的操作，不影响其他用户</li>
     *   <li>根据返回 msg 区分「新生成」与「已存在被跳过」；异常捕获进失败列表</li>
     * </ul>
     * 本方法不加 @Transactional：避免把所有补偿并入一个事务，与 design 决策 2 相悖。
     */
    @Override
    public Response<CompensateResultVO> compensateMissingInviteCodes() {
        List<SysUser> allUsers = userMapper.selectList(null);
        int successCount = 0;
        int skippedCount = 0;
        List<CompensateResultVO.Failure> failures = new ArrayList<>();

        for (SysUser user : allUsers) {
            Long userId = user.getId();
            try {
                Response<?> result = self.generateInviteCode(userId);
                // generateInviteCode 幂等：已有码返回 msg="邀请码已存在"
                if (result != null && "邀请码已存在".equals(result.getMsg())) {
                    skippedCount++;
                } else {
                    successCount++;
                }
            } catch (Exception e) {
                log.warn("[邀请码补偿] userId={} 补生成失败，跳过该用户: {}", userId, e.getMessage());
                failures.add(new CompensateResultVO.Failure(userId, user.getUsername(), e.getMessage()));
                // 跳过单个用户继续下一个
            }
        }

        log.info("[邀请码补偿] 完成：新增 {}，跳过 {}，失败 {}",
                successCount, skippedCount, failures.size());
        return Response.ok("补偿完成",
                new CompensateResultVO(successCount, skippedCount, failures));
    }

    @Override
    public Response getMyInviteCode(Long userId) {
        LambdaQueryWrapper<SysInviteCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysInviteCode::getInviterId, userId);
        SysInviteCode inviteCode = sysInviteCodeMapper.selectOne(wrapper);
        if (inviteCode == null) {
            return Response.fail(404, "尚未生成邀请码");
        }
        return Response.ok(inviteCode);
    }

    @Override
    public String getInviteCodeByUserId(Long userId) {
        LambdaQueryWrapper<SysInviteCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysInviteCode::getInviterId, userId);
        SysInviteCode inviteCode = sysInviteCodeMapper.selectOne(wrapper);
        return inviteCode != null ? inviteCode.getInviteCode() : null;
    }

    @Override
    public Response getInviteRecords(Long inviterId) {
        LambdaQueryWrapper<SysInviteRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysInviteRecord::getInviterId, inviterId)
                .orderByDesc(SysInviteRecord::getCreateTime);
        List<SysInviteRecord> records = sysInviteRecordMapper.selectList(wrapper);
        return Response.ok(records);
    }

    @Override
    public SysInviteCode validateInviteCode(String inviteCode) {
        LambdaQueryWrapper<SysInviteCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysInviteCode::getInviteCode, inviteCode);
        SysInviteCode code = sysInviteCodeMapper.selectOne(wrapper);
        if (code == null) {
            throw new BusinessException("邀请码不存在");
        }
        if (code.getStatus() == 1) {
            throw new BusinessException("邀请码已被手动失效");
        }
        if (code.getStatus() == 2) {
            throw new BusinessException("邀请码名额已满");
        }
        if (code.getExpireTime() != null && code.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("邀请码已过期");
        }
        // 0 表示不限制，跳过人数上限校验
        if (code.getMaxInviteNum() != null
                && code.getMaxInviteNum() > 0
                && code.getUsedInviteNum() >= code.getMaxInviteNum()) {
            throw new BusinessException("邀请码已达到最大邀请人数");
        }
        return code;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processInviteRecord(SysInviteCode inviteCode, Long inviteeId, String inviteePhone) {
        // 1. 插入邀请明细流水
        SysInviteRecord record = new SysInviteRecord();
        record.setInviteCode(inviteCode.getInviteCode());
        record.setInviterId(inviteCode.getInviterId());
        record.setInviteeId(inviteeId);
        record.setInviteePhone(inviteePhone);
        record.setStatus(1); // 已注册
        sysInviteRecordMapper.insert(record);

        // 2. 更新邀请码已邀请人数（乐观锁防并发）
        int newUsedNum = inviteCode.getUsedInviteNum() + 1;
        LambdaUpdateWrapper<SysInviteCode> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(SysInviteCode::getId, inviteCode.getId())
                .eq(SysInviteCode::getUsedInviteNum, inviteCode.getUsedInviteNum())
                .set(SysInviteCode::getUsedInviteNum, newUsedNum);

        // 名额满则自动停用；0 表示不限制，跳过停用
        if (inviteCode.getMaxInviteNum() != null
                && inviteCode.getMaxInviteNum() > 0
                && newUsedNum >= inviteCode.getMaxInviteNum()) {
            updateWrapper.set(SysInviteCode::getStatus, 2);
        }

        int updated = sysInviteCodeMapper.update(null, updateWrapper);
        if (updated == 0) {
            throw new BusinessException("邀请码核销失败，请重试");
        }

        // 3. 邀请成功 → 邀请人自动转为老会员（member_type 0→1，条件更新幂等）
        LambdaUpdateWrapper<SysUser> memberUpdate = new LambdaUpdateWrapper<>();
        memberUpdate.eq(SysUser::getId, inviteCode.getInviterId())
                .eq(SysUser::getMemberType, 0)
                .set(SysUser::getMemberType, 1);
        int memberUpdated = userMapper.update(null, memberUpdate);
        if (memberUpdated > 0) {
            log.info("[邀请转老会员] 邀请人 userId={} 已从新会员转为老会员", inviteCode.getInviterId());
        }
    }
}
