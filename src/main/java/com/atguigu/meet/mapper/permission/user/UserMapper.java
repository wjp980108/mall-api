package com.atguigu.meet.mapper.permission.user;

import com.atguigu.meet.model.entity.permission.user.SysUser;
import com.atguigu.meet.model.vo.permission.user.UserOrderVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Description
 * @Date 2026-04-22 11:05
 */
public interface UserMapper extends BaseMapper<SysUser> {
    SysUser selectByAccount(String account);

    // 根据用户 id 查询用户+所有订单
    UserOrderVO getUserWithOrders(String phone);

    /**
     * 预演：查询本轮将被自动禁用的到期新会员ID（与 {@link #disableOneExpiredNewMember} 同条件，只读）
     * <p>管辖条件：新会员(member_type=0) + 状态正常(status=1) + 注册超过 cutoff
     * + (无管理角色 或 拥有会员角色 role_id=memberRoleId)
     *
     * @param cutoff              注册时间上限（当前时间 - new_member_days）
     * @param superAdminRoleId    超级管理员角色ID（PermissionConst.SUPER_ADMIN_ROLE_ID）
     * @param platformAdminRoleId 平台管理员角色ID（PermissionConst.PLATFORM_ADMIN_ROLE_ID）
     * @param memberRoleId        会员角色ID（PermissionConst.MEMBER_ROLE_ID）
     */
    List<Long> selectExpiredNewMemberIds(@Param("cutoff") LocalDateTime cutoff,
                                         @Param("superAdminRoleId") long superAdminRoleId,
                                         @Param("platformAdminRoleId") long platformAdminRoleId,
                                         @Param("memberRoleId") long memberRoleId);

    /**
     * 逐行条件禁用一个到期新会员（全部管辖条件在 SQL 内原子重查，防查更间隙状态漂移误禁）
     *
     * @return 影响行数：1=本次禁用成功；0=条件不命中（已被转老会员/已禁用）
     */
    int disableOneExpiredNewMember(@Param("userId") Long userId,
                                   @Param("cutoff") LocalDateTime cutoff,
                                   @Param("superAdminRoleId") long superAdminRoleId,
                                   @Param("platformAdminRoleId") long platformAdminRoleId,
                                   @Param("memberRoleId") long memberRoleId);
}