package com.atguigu.meet.service.permission.user;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.permission.user.UserChangePasswordDTO;
import com.atguigu.meet.model.dto.permission.user.UserCreateDTO;
import com.atguigu.meet.model.dto.permission.user.UserDeleteDTO;
import com.atguigu.meet.model.dto.permission.user.UserPageQueryDTO;
import com.atguigu.meet.model.dto.permission.user.UserProfileUpdateDTO;
import com.atguigu.meet.model.dto.permission.user.UserStatusDTO;
import com.atguigu.meet.model.dto.permission.user.UserUpdateDTO;
import com.atguigu.meet.model.entity.permission.user.AdminUser;
import com.atguigu.meet.model.vo.permission.user.AdminUserPointsVO;
import com.atguigu.meet.model.vo.permission.user.UserRelationVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Description
 * @Date 2026-08-12 23:59
 */
public interface UserService {
    Response deleteUserByIds(UserDeleteDTO userDeleteDTO);

    /** 后台创建用户（角色由前端传入，事务保证用户与角色关联同时写入） */
    Response createUser(UserCreateDTO userCreateDTO);

    Response updateUser(UserUpdateDTO userUpdateDTO);

    /** 启用/禁用用户 */
    Response updateStatus(UserStatusDTO userStatusDTO);

    Response getUserByPhone(String phone, AdminUser loginAdmin);

    Response getList();

    Response getPageList(UserPageQueryDTO parameter);

    void exportUserToCsv(HttpServletResponse response);

    Response uploadUserAvatar(MultipartFile file, String platform);

    Response getUserWithOrders(String phone, AdminUser loginAdmin);

    /** 获取当前登录用户信息 */
    Response getCurrentUserInfo();

    /** 获取当前登录用户可访问的菜单树 */
    Response getCurrentUserMenus();

    /** 用户下拉选项列表（仅启用用户） */
    Response getUserOptions();

    /** 将指定用户手动转为老会员（member_type 0→1，兜底机制） */
    Response toOldMember(Long userId);

    /** 当前登录用户修改个人信息（未传字段不更新），返回更新后的最新用户信息 */
    Response updateCurrentUserInfo(UserProfileUpdateDTO dto);

    /** 当前登录用户修改密码（用户身份由 token 解析，前端仅需传新密码） */
    Response changePassword(UserChangePasswordDTO dto);

    /**
     * 查询用户上下级邀请关系（一级直邀，不递归）。
     * <p>上级为目标用户 inviterId 指向的人；inviterId 为 null 或上级为内置超管（userId/username 双判据）时 upline=null。
     * <p>下级为所有 inviter_id 等于目标 userId 的用户，仅一级不递归。
     *
     * @param userId 目标用户ID
     * @return 上级简化对象 + 直邀下级简化对象数组
     */
    Response<UserRelationVO> getRelations(Long userId);

    /**
     * 查询用户积分余额与流水分页（管理端只读核查专用）。
     * <p>余额走 {@code getBalanceReadOnly}（不调 initAccount，不产生写副作用）；
     * 流水复用 {@code pageFlow}（已带枚举中文名组装），与 C 端 /app/assets/points/flow 同口径。
     *
     * @param userId   目标用户ID
     * @param bizType  业务类型筛选（1推荐奖 2自购奖 3购物券奖 4积分对冲；null 查全部）
     * @param pageNum  分页页码
     * @param pageSize 每页条数
     * @return 流水分页（分页字段平铺最外层）+ 余额（balance 字段）
     */
    Response<AdminUserPointsVO> getPointsDetail(Long userId, Integer bizType, Integer pageNum, Integer pageSize);

    /*List<Map<String, Object>> mapList();

    List<Object> idList();*/
}