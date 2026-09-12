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

    /** 当前登录用户修改密码（账号须与当前登录用户一致，防越权） */
    Response changePassword(UserChangePasswordDTO dto);

    /*List<Map<String, Object>> mapList();

    List<Object> idList();*/
}