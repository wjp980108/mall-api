package com.atguigu.meet.service.permission.invite;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.entity.permission.invite.SysInviteCode;
import com.atguigu.meet.model.entity.permission.invite.SysInviteRecord;

import java.util.List;

/**
 * 邀请码服务接口
 */
public interface InviteCodeService {

    /**
     * 生成邀请码（1个用户只能生成1个，已存在则直接返回）
     */
    Response generateInviteCode(Long userId);

    /**
     * 查询我的邀请码
     */
    Response getMyInviteCode(Long userId);

    /**
     * 查询用户邀请码字符串（纯读，无生成副作用；无码返回 null，供用户信息接口回填）
     */
    String getInviteCodeByUserId(Long userId);

    /**
     * 查询邀请明细流水
     */
    Response getInviteRecords(Long inviterId);

    /**
     * 校验邀请码有效性（供注册时调用）
     * 返回邀请码实体，无效则抛 BusinessException
     */
    SysInviteCode validateInviteCode(String inviteCode);

    /**
     * 注册成功后处理邀请流水（供注册时调用）
     */
    void processInviteRecord(SysInviteCode inviteCode, Long inviteeId, String inviteePhone);
}
