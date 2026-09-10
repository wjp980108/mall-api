package com.atguigu.meet.service.permission.invite;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.entity.permission.invite.SysInviteCode;
import com.atguigu.meet.model.entity.permission.invite.SysInviteRecord;
import com.atguigu.meet.model.vo.permission.invite.CompensateResultVO;

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

    /**
     * 存量补偿：扫描 sys_user 中存在但 sys_invite_code 中无对应邀请码的用户，
     * 逐个调用 {@link #generateInviteCode(Long)}（依赖其「1 人 1 码」幂等检查），
     * 失败时跳过单个用户、记录失败列表返回。
     * <p>
     * 不并入任何用户创建事务：每个用户的 generateInviteCode 走自身独立事务
     * （{@code @Transactional(REQUIRED)}，无外层事务时独立提交），
     * 单个失败只回滚该用户的操作，不影响其他用户。
     * <p>
     * 接口幂等：多次调用只补尚未生成邀请码的那批，已有码用户自动跳过。
     */
    Response<CompensateResultVO> compensateMissingInviteCodes();
}
