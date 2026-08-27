package com.atguigu.meet.service.permission.user.impl;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.exception.BusinessException;
import com.atguigu.meet.mapper.permission.menu.SysMenuMapper;
import com.atguigu.meet.mapper.permission.role.SysRoleMapper;
import com.atguigu.meet.mapper.permission.user.UserMapper;
import com.atguigu.meet.mapper.permission.userRole.SysUserRoleMapper;
import com.atguigu.meet.model.dto.permission.user.UserCreateDTO;
import com.atguigu.meet.model.dto.permission.user.UserDeleteDTO;
import com.atguigu.meet.model.dto.permission.user.UserPageQueryDTO;
import com.atguigu.meet.model.dto.permission.user.UserStatusDTO;
import com.atguigu.meet.model.dto.permission.user.UserUpdateDTO;
import com.atguigu.meet.model.entity.permission.menu.SysMenu;
import com.atguigu.meet.model.entity.permission.role.SysRole;
import com.atguigu.meet.model.entity.permission.user.AdminUser;
import com.atguigu.meet.model.entity.permission.user.SysUser;
import com.atguigu.meet.model.entity.permission.userRole.SysUserRole;
import com.atguigu.meet.model.vo.OptionVO;
import com.atguigu.meet.model.vo.PageResultVO;
import com.atguigu.meet.model.vo.permission.menu.MenuVO;
import com.atguigu.meet.model.vo.permission.role.RoleVO;
import com.atguigu.meet.model.vo.permission.user.UserOrderVO;
import com.atguigu.meet.model.vo.permission.user.UserVO;
import com.atguigu.meet.service.auth.PermissionCacheService;
import com.atguigu.meet.service.file.FileService;
import com.atguigu.meet.service.permission.user.UserService;
import com.atguigu.meet.utils.AdminContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import com.atguigu.meet.utils.BeanConvertUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 * @Description
 * @Date 2026-08-12 23:59
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, SysUser> implements UserService {
    @Autowired
    private FileService fileService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private SysMenuMapper sysMenuMapper;

    @Autowired
    private PermissionCacheService permissionCacheService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class) // 所有异常都回滚，保证原子性
    public Response deleteUserByIds(UserDeleteDTO userDeleteDTO) {
        List<Long> idList = Arrays.asList(userDeleteDTO.getUserIds());
        List<SysUser> dbUserList = listByIds(idList);
        Set<Long> existIdSet = dbUserList.stream()
                .map(SysUser::getId)
                .collect(Collectors.toSet());
        List<Long> notExistIds = idList.stream()
                .filter(id -> !existIdSet.contains(id))
                .collect(Collectors.toList());
        if (!notExistIds.isEmpty()) {
            return Response.fail(500, "用户ID：" + notExistIds + " 不存在，本次全部取消删除");
        }

        removeByIds(idList);

        return Response.ok("成功删除" + idList.size() + "个用户", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response createUser(UserCreateDTO userCreateDTO) {
        // 1. 校验用户名是否已存在
        LambdaQueryWrapper<SysUser> usernameWrapper = new LambdaQueryWrapper<>();
        usernameWrapper.eq(SysUser::getUsername, userCreateDTO.getUsername());
        SysUser existUsername = getOne(usernameWrapper);
        if (existUsername != null) {
            return Response.fail(500, "用户名已存在");
        }

        // 2. 校验手机号是否已存在
        LambdaQueryWrapper<SysUser> phoneWrapper = new LambdaQueryWrapper<>();
        phoneWrapper.eq(SysUser::getPhone, userCreateDTO.getPhone());
        SysUser existPhone = getOne(phoneWrapper);
        if (existPhone != null) {
            return Response.fail(500, "手机号已存在");
        }

        // 3. 校验角色ID有效性（存在且启用）
        List<Long> roleIds = userCreateDTO.getRoleIds();
        if (roleIds != null && !roleIds.isEmpty()) {
            LambdaQueryWrapper<SysRole> roleWrapper = new LambdaQueryWrapper<>();
            roleWrapper.in(SysRole::getId, roleIds).eq(SysRole::getStatus, 1);
            long validCount = sysRoleMapper.selectCount(roleWrapper);
            if (validCount != roleIds.size()) {
                return Response.fail(500, "包含无效或已禁用的角色ID");
            }
        }

        // 4. 创建用户（加密密码，密码为空时使用默认密码 123456）
        SysUser user = new SysUser();
        String rawPassword = (userCreateDTO.getPassword() == null || userCreateDTO.getPassword().isEmpty())
                ? "123456"
                : userCreateDTO.getPassword();
        String encodePwd = passwordEncoder.encode(rawPassword);
        BeanConvertUtils.copyProperties(userCreateDTO, user);
        user.setPassword(encodePwd);
        userMapper.insert(user);

        // 5. 分配角色（写入 sys_user_role 关联）
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long roleId : roleIds) {
                SysUserRole userRole = new SysUserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(roleId);
                sysUserRoleMapper.insert(userRole);
            }
        }

        log.info("[用户管理] 创建用户成功，userId={}, roleIds={}", user.getId(), roleIds);
        return Response.ok("创建用户成功", null);
    }

    @Override
    public Response updateUser(UserUpdateDTO userUpdateDTO) {
        Long userId = userUpdateDTO.getId();
        LambdaQueryWrapper<SysUser> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(SysUser::getId, userId);
        SysUser existUser = getOne(lambdaQueryWrapper);
        if (existUser == null) {
            return Response.fail(500, "用户不存在");
        }
        // 只复制 DTO 中非 null 的字段到 existUser，避免覆盖数据库原有值
        // 注：password 字段已在 UserUpdateDTO.setPassword 中强制置 null，此接口禁止修改密码
        BeanConvertUtils.copyProperties(userUpdateDTO, existUser, getNullPropertyNames(userUpdateDTO));
        userMapper.updateById(existUser);
        return Response.ok("更新用户信息成功", null);
    }

    @Override
    @com.atguigu.meet.annotation.ForbidOperateSelf(message = "不允许禁用当前登录账号", idField = "userId")
    public Response updateStatus(UserStatusDTO userStatusDTO) {
        Long userId = userStatusDTO.getUserId();
        SysUser existUser = userMapper.selectById(userId);
        if (existUser == null) {
            return Response.fail(500, "用户不存在");
        }
        SysUser user = new SysUser();
        user.setId(userId);
        user.setStatus(Boolean.TRUE.equals(userStatusDTO.getStatus()) ? "1" : "0");
        userMapper.updateById(user);

        // 用户状态变更后失效自己的权限缓存
        permissionCacheService.invalidateUserPermissions(userId);
        log.info("[用户管理] 用户启停成功，userId={}, {}->{}，操作人={}",
                userId, "1".equals(existUser.getStatus()), userStatusDTO.getStatus(), AdminContext.getLoginUserId());
        return Response.ok("用户启停成功", null);
    }

    /**
     * 获取对象中值为 null 的属性名数组，配合 BeanConvertUtils.copyProperties 忽略 null 值字段
     */
    private static String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();
        Set<String> emptyNames = new HashSet<>();
        for (java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) {
                emptyNames.add(pd.getName());
            }
        }
        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }

    @Override
    public Response getUserByPhone(String phone, AdminUser loginAdmin) {
        if (loginAdmin != null && !loginAdmin.getPhone().equals(phone)) {
            throw new BusinessException("无权限查询其他用户信息");
        }
        LambdaQueryWrapper<SysUser> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(SysUser::getPhone, phone);
        SysUser existUser = userMapper.selectOne(lambdaQueryWrapper);
        if (existUser == null) {
            return Response.fail(500, "用户不存在");
        }
        UserVO userVO = new UserVO();
        BeanConvertUtils.copyProperties(existUser, userVO);
        return Response.ok("查询用户成功", userVO);
    }

    @Override
    public Response getList() {
        LambdaQueryWrapper<SysUser> wrapper = Wrappers.lambdaQuery(SysUser.class);
        List<SysUser> userList = userMapper.selectList(wrapper);
        return Response.ok(userList);
    }

    @Override
    public Response getPageList(UserPageQueryDTO parameter) {
        LambdaQueryWrapper<SysUser> lambdaQueryWrapper = new LambdaQueryWrapper<>();

        if (parameter.getAge() != null) {
            lambdaQueryWrapper.lt(SysUser::getAge, parameter.getAge());
        }

        if (parameter.getUsername() != null && StringUtils.hasText(parameter.getUsername())) {
            lambdaQueryWrapper.like(SysUser::getUsername, parameter.getUsername());
        }
        if (parameter.getPhone() != null && StringUtils.hasText(parameter.getPhone())) {
            lambdaQueryWrapper.like(SysUser::getPhone, parameter.getPhone());
        }

        IPage<SysUser> page = new Page<>(parameter.getPageNum(), parameter.getPageSize());
        IPage<SysUser> result = page(page, lambdaQueryWrapper);

        List<SysUser> records = result.getRecords();
        List<UserVO> voList = new ArrayList<>(records.size());
        if (!records.isEmpty()) {
            // ========== 一次性批量查询本页所有用户的角色信息，避免 N+1 ==========
            List<Long> userIds = records.stream().map(SysUser::getId).collect(Collectors.toList());

            // 1. 批量查用户-角色关联（sys_user_role），按 userId 分组
            LambdaQueryWrapper<SysUserRole> urWrapper = Wrappers.lambdaQuery(SysUserRole.class);
            urWrapper.in(SysUserRole::getUserId, userIds);
            List<SysUserRole> userRoleList = sysUserRoleMapper.selectList(urWrapper);
            // Map<userId, List<roleId>>
            java.util.Map<Long, List<Long>> userRoleIdsMap = userRoleList.stream()
                    .collect(Collectors.groupingBy(
                            SysUserRole::getUserId,
                            Collectors.mapping(SysUserRole::getRoleId, Collectors.toList())
                    ));

            // 2. 批量查角色表（sys_role），roleId -> RoleVO（含 id/roleName/roleCode/status）
            java.util.Map<Long, RoleVO> roleIdToVOMap = new java.util.HashMap<>();
            if (!userRoleList.isEmpty()) {
                Set<Long> roleIds = userRoleList.stream()
                        .map(SysUserRole::getRoleId)
                        .collect(Collectors.toSet());
                LambdaQueryWrapper<SysRole> roleWrapper = Wrappers.lambdaQuery(SysRole.class);
                roleWrapper.in(SysRole::getId, roleIds);
                List<SysRole> roles = sysRoleMapper.selectList(roleWrapper);
                roleIdToVOMap = roles.stream()
                        .collect(Collectors.toMap(
                                SysRole::getId,
                                r -> {
                                    RoleVO rv = new RoleVO();
                                    rv.setId(r.getId());
                                    rv.setRoleName(r.getRoleName());
                                    rv.setRoleCode(r.getRoleCode());
                                    rv.setStatus(r.getStatus() != null && r.getStatus() == 1);
                                    return rv;
                                },
                                (a, b) -> a));
            }

            // 3. 组装 UserVO（含角色 ID 列表 + 角色名称拼接 + 角色完整信息列表）
            final java.util.Map<Long, RoleVO> finalRoleIdToVOMap = roleIdToVOMap;
            for (SysUser user : records) {
                UserVO vo = new UserVO();
                BeanConvertUtils.copyProperties(user, vo);
                List<Long> roleIds = userRoleIdsMap.getOrDefault(user.getId(), Collections.emptyList());
                vo.setRoleIds(roleIds);
                if (!roleIds.isEmpty()) {
                    List<RoleVO> userRoles = roleIds.stream()
                            .map(finalRoleIdToVOMap::get)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    vo.setRoles(userRoles);
                    String roleNames = userRoles.stream()
                            .map(RoleVO::getRoleName)
                            .collect(Collectors.joining(","));
                    vo.setRoleNames(roleNames);
                }
                voList.add(vo);
            }
        }

        // 手动构建分页结果（注意泛型从 SysUser 变为 UserVO，复用 total/pages 等元信息）
        PageResultVO<UserVO> pageVO = new PageResultVO<>();
        pageVO.setList(voList);
        pageVO.setTotal(result.getTotal());
        pageVO.setPages(result.getPages());
        pageVO.setCurrent(result.getCurrent());
        pageVO.setSize(result.getSize());
        return Response.ok(pageVO);
    }

    @Override
    public void exportUserToCsv(HttpServletResponse response) {
// 1. 设置响应头，让浏览器下载文件
        response.setContentType("text/csv");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename=user_list.csv");

        try (
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8));
                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("ID", "用户名", "昵称", "邮箱", "手机号", "性别", "状态"))
        ) {
            // 2. 构建查询条件：只导出未删除的用户
            LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
//            wrapper.eq(SysUser::getIsDeleted, 0);

            // 3. 流式查询 + 边读边写
            this.baseMapper.selectList(wrapper, context -> {
                SysUser user = context.getResultObject();
                try {
                    // 把当前这条数据写入 CSV
                    csvPrinter.printRecord(
                            user.getId(),
                            user.getUsername(),
                            user.getNickname(),
                            user.getEmail(),
                            user.getPhone(),
                            user.getGender(),
                            user.getStatus()
                    );
                } catch (IOException e) {
                    throw new RuntimeException("写入CSV失败", e);
                }
            });

            csvPrinter.flush();
        } catch (Exception e) {
            throw new RuntimeException("导出用户数据失败", e);
        }
    }

    @Override
    public Response uploadUserAvatar(MultipartFile file, String platform) {
        Long userId = AdminContext.getLoginUserId();
        if (userId == null) {
            return Response.fail(401, "未登录");
        }
        // platform 默认值由 FileService.upload 内部处理(为空则 local-1)
        try {
            Response resUpload = fileService.upload(file, "avatar", platform);
            if (resUpload.getCode() == 500) return resUpload;
            String url = (String) resUpload.getData();
            // 上传时传给 fileService 的 platform 即为最终入库平台(fileService 内部已做默认处理)
            String finalPlatform = (platform == null || platform.isBlank()) ? "local-1" : platform;
            UserUpdateDTO userUpdateDTO = new UserUpdateDTO();
            userUpdateDTO.setId(userId);
            userUpdateDTO.setAvatar(url);
            userUpdateDTO.setAvatarPlatform(finalPlatform);
            updateUser(userUpdateDTO);
            return Response.ok("头像上传并更新成功", url);
        } catch (RuntimeException e) {
            throw new BusinessException(e.getMessage());
//            return Response.fail(500, e.getMessage());
        }
    }

    @Override
    public Response getUserWithOrders(String phone, AdminUser loginAdmin) {
        if (loginAdmin != null && !loginAdmin.getPhone().equals(phone)) {
            throw new BusinessException("无权限查询其他用户信息");
        }
        try {
            UserOrderVO userOrderVO = userMapper.getUserWithOrders(phone);
            return Response.ok(userOrderVO);
        } catch (RuntimeException e) {
            throw new BusinessException(e.getMessage());
        }
    }

    @Override
    public Response getCurrentUserInfo() {
        AdminUser currentUser = AdminContext.get();
        if (currentUser == null) {
            return Response.fail(401, "未登录");
        }
        SysUser user = userMapper.selectById(currentUser.getUserId());
        if (user == null) {
            return Response.fail(404, "用户不存在");
        }
        UserVO userVO = new UserVO();
        BeanConvertUtils.copyProperties(user, userVO);
        userVO.setPermissions(currentUser.getPermissions());
        return Response.ok(userVO);
    }

    @Override
    public Response getCurrentUserMenus() {
        AdminUser currentUser = AdminContext.get();
        if (currentUser == null) {
            return Response.fail(401, "未登录");
        }
        Long userId = currentUser.getUserId();
        Set<String> userPermissions = currentUser.getPermissions();

        Set<String> roleCodes = currentUser.getRoleCodes();
        boolean isSuperAdmin = roleCodes != null && roleCodes.contains(PermissionConst.ROLE_SUPER_ADMIN);

        // 查询所有启用的目录与菜单（不含按钮权限 type=2），确保包含父级菜单保证树形结构完整
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getStatus, 1)
                .eq(SysMenu::getIsDeleted, 0)
                .ne(SysMenu::getType, 2)
                .orderByAsc(SysMenu::getSort);
        List<SysMenu> allMenus = sysMenuMapper.selectList(wrapper);
        log.info("[菜单] 查询所有菜单，userId={}, 菜单数={}", userId, allMenus.size());

        if (isSuperAdmin) {
            List<MenuVO> menuTree = buildMenuTree(allMenus, 0L);
            return Response.ok(menuTree);
        }

        // 获取用户有权限的菜单ID集合（通过角色-菜单关联）
        List<SysMenu> authorizedMenus = sysMenuMapper.selectMenusByUserId(userId);
        Set<Long> authorizedMenuIds = authorizedMenus.stream()
                .map(SysMenu::getId)
                .collect(Collectors.toSet());
        log.info("[菜单] 用户授权菜单ID，userId={}, menuIds={}", userId, authorizedMenuIds);

        // 构建完整菜单树后按权限过滤
        List<MenuVO> menuTree = filterMenuTree(buildMenuTree(allMenus, 0L), authorizedMenuIds, userPermissions);
        log.info("[菜单] 普通用户菜单树，userId={}, menuTree={}", userId, menuTree);
        return Response.ok(menuTree);
    }

    @Override
    public Response getUserOptions() {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getStatus, "1");
        // 过滤超级管理员角色
        List<Long> superAdminUserIds = getSuperAdminUserIds();
        if (!superAdminUserIds.isEmpty()) {
            wrapper.notIn(SysUser::getId, superAdminUserIds);
        }
        wrapper.orderByAsc(SysUser::getId);
        List<SysUser> users = list(wrapper);

        List<OptionVO<Long>> options = new ArrayList<>(users.size());
        for (SysUser u : users) {
            String label = "用户账号: " + u.getPhone() + "; 用户 ID: " + u.getId();
            options.add(new OptionVO<>(label, u.getId()));
        }
        return Response.ok(options);
    }

    /**
     * 查询拥有超级管理员角色的用户ID集合
     */
    private List<Long> getSuperAdminUserIds() {
        LambdaQueryWrapper<SysRole> roleWrapper = Wrappers.lambdaQuery(SysRole.class)
                .eq(SysRole::getRoleCode, PermissionConst.ROLE_SUPER_ADMIN);
        List<Long> superRoleIds = sysRoleMapper.selectList(roleWrapper).stream()
                .map(SysRole::getId)
                .collect(Collectors.toList());
        if (superRoleIds.isEmpty()) {
            return Collections.emptyList();
        }
        return sysUserRoleMapper.selectList(
                        Wrappers.lambdaQuery(SysUserRole.class).in(SysUserRole::getRoleId, superRoleIds))
                .stream()
                .map(SysUserRole::getUserId)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<MenuVO> buildMenuTree(List<SysMenu> allMenus, Long parentId) {
        return allMenus.stream()
                .filter(m -> parentId.equals(m.getParentId()))
                .map(m -> {
                    MenuVO vo = new MenuVO();
                    BeanConvertUtils.copyProperties(m, vo);
                    vo.setChildren(buildMenuTree(allMenus, m.getId()));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 按权限过滤菜单树
     * <ul>
     *   <li>type=0 目录：有子菜单保留时自身才保留</li>
     *   <li>type=1 菜单：必须在 authorizedMenuIds 中才保留</li>
     *   <li>type=2 按钮：必须在 userPermissions 中才保留</li>
     * </ul>
     */
    private List<MenuVO> filterMenuTree(List<MenuVO> tree, Set<Long> authorizedMenuIds, Set<String> userPermissions) {
        List<MenuVO> result = new ArrayList<>();
        for (MenuVO menu : tree) {
            if (menu.getType() == 2) {
                // 按钮：通过 perm 字段匹配（当前无按钮级权限时不返回）
                if (userPermissions != null && userPermissions.contains(menu.getPerm())) {
                    result.add(menu);
                }
            } else if (menu.getType() == 1) {
                // 菜单：必须在用户授权菜单ID中
                if (authorizedMenuIds.contains(menu.getId())) {
                    List<MenuVO> filteredChildren = filterMenuTree(menu.getChildren(), authorizedMenuIds, userPermissions);
                    menu.setChildren(filteredChildren);
                    result.add(menu);
                }
            } else {
                // 目录(type=0)：子菜单有保留则自身保留
                List<MenuVO> filteredChildren = filterMenuTree(menu.getChildren(), authorizedMenuIds, userPermissions);
                menu.setChildren(filteredChildren);
                if (!filteredChildren.isEmpty()) {
                    result.add(menu);
                }
            }
        }
        return result;
    }

    /*public Response exportAllUser() {
        LambdaQueryWrapper<SysUser> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        userMapper.selectList(lambdaQueryWrapper, new ResultHandler<SysUser>() {
            @Override
            public void handleResult(ResultContext<? extends SysUser> resultContext) {
                SysUser user = resultContext.getResultObject();
                if (resultContext.getResultCount() >= 100) {
                    resultContext.stop();
                }
            }
        });
    }*/

   /* @Override
    public List<Map<String, Object>> mapList() {
        LambdaQueryWrapper<SysUser> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(SysUser::getAge, 10);
        List<Map<String, Object>> mapList = listMaps(lambdaQueryWrapper);
        return mapList;
    }

    @Override
    public List<Object> idList() {
        LambdaQueryWrapper<SysUser> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(SysUser::getAge, 10);
        return listObjs(lambdaQueryWrapper, (obj) -> String.valueOf(obj));
    }*/
}