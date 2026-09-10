package com.atguigu.meet.service.permission.userRole;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.permission.userRole.UserAssignRoleDTO;

import java.util.List;

/**
 * 用户-角色关联 Service
 */
public interface UserRoleService {

    /** 查询用户已分配的角色ID列表 */
    Response getUserRoleIds(Long userId);

    /** 给用户分配角色（全量覆盖） */
    Response assignRoles(UserAssignRoleDTO dto);

    /**
     * 同步用户角色绑定（编辑用户 / 独立分配接口共用的统一入口）。
     * <p>语义：
     * <ul>
     *   <li>roleIds 为 null：不调用本方法，调用方负责保持原角色不变；</li>
     *   <li>roleIds 为空列表：清空该用户全部角色；</li>
     *   <li>roleIds 非空：去重 + 校验全部存在且启用后全量覆盖。</li>
     * </ul>
     * 内部完成：去重 -> 校验角色有效性 -> 删除旧关联 -> 插入新关联 -> 失效权限缓存。
     *
     * @param userId  用户ID
     * @param roleIds 目标角色ID列表（非 null；空列表表示清空）
     * @return 操作结果
     */
    Response syncUserRoles(Long userId, List<Long> roleIds);
}