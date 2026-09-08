package com.atguigu.meet.service.permission.invite.impl;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.exception.BusinessException;
import com.atguigu.meet.mapper.permission.invite.SysInviteCodeMapper;
import com.atguigu.meet.mapper.permission.invite.SysInviteRecordMapper;
import com.atguigu.meet.mapper.permission.user.UserMapper;
import com.atguigu.meet.model.entity.permission.invite.SysInviteCode;
import com.atguigu.meet.model.entity.permission.invite.SysInviteRecord;
import com.atguigu.meet.model.entity.permission.user.SysUser;
import com.atguigu.meet.service.permission.invite.InviteCodeService;
import com.atguigu.meet.utils.InviteCodeUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 邀请码服务实现
 */
@Service
@Slf4j
public class InviteCodeServiceImpl implements InviteCodeService {

    @Autowired
    private SysInviteCodeMapper sysInviteCodeMapper;

    @Autowired
    private SysInviteRecordMapper sysInviteRecordMapper;

    @Autowired
    private RedisInviteSeqGenerator seqGenerator;

    @Autowired
    private UserMapper userMapper;

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

        // 2. Redis 全局自增 seq → 54 进制编码为 8 位邀请码（数学上一一对应，无碰撞，无需重试）
        long seq = seqGenerator.nextSeq();
        String code = InviteCodeUtil.encode(seq);

        // 3. 插入邀请码表
        SysInviteCode inviteCode = new SysInviteCode();
        inviteCode.setSeq(seq);
        inviteCode.setInviteCode(code);
        inviteCode.setInviterId(userId);
        inviteCode.setStatus(0);
        inviteCode.setMaxInviteNum(10);
        inviteCode.setUsedInviteNum(0);
        sysInviteCodeMapper.insert(inviteCode);

        return Response.ok("邀请码生成成功", inviteCode);
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
        if (code.getUsedInviteNum() >= code.getMaxInviteNum()) {
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

        // 名额满则自动停用
        if (newUsedNum >= inviteCode.getMaxInviteNum()) {
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
