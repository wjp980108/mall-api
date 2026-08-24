package com.atguigu.meet.service.permission.menu.impl;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.mapper.permission.menu.SysMenuMapper;
import com.atguigu.meet.model.dto.permission.menu.MenuPageQueryDTO;
import com.atguigu.meet.model.dto.permission.menu.MenuSaveDTO;
import com.atguigu.meet.model.dto.permission.menu.MenuStatusDTO;
import com.atguigu.meet.model.dto.permission.menu.MenuUpdateDTO;
import com.atguigu.meet.model.entity.permission.menu.SysMenu;
import com.atguigu.meet.model.vo.PageResultVO;
import com.atguigu.meet.model.vo.permission.menu.MenuVO;
import com.atguigu.meet.service.auth.PermissionCacheService;
import com.atguigu.meet.service.permission.menu.MenuService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import com.atguigu.meet.utils.BeanConvertUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单管理 Service 实现
 */
@Service
@Slf4j
public class MenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements MenuService {

    @Autowired
    private PermissionCacheService permissionCacheService;

    @Override
    public Response getMenuTree(String name) {
        List<SysMenu> allMenus;

        if (StringUtils.hasText(name)) {
            // 按名称模糊查询，同时保留匹配菜单的父级以维持树形结构
            LambdaQueryWrapper<SysMenu> matchWrapper = new LambdaQueryWrapper<>();
            matchWrapper.like(SysMenu::getName, name)
                    .orderByAsc(SysMenu::getSort);
            List<SysMenu> matchedMenus = list(matchWrapper);

            if (matchedMenus.isEmpty()) {
                return Response.ok(new ArrayList<>());
            }

            // 收集所有匹配菜单及其祖先ID
            List<Long> neededIds = new ArrayList<>();
            for (SysMenu menu : matchedMenus) {
                neededIds.add(menu.getId());
                collectAncestorIds(menu.getParentId(), neededIds);
            }

            LambdaQueryWrapper<SysMenu> treeWrapper = new LambdaQueryWrapper<>();
            treeWrapper.in(SysMenu::getId, neededIds)
                    .orderByAsc(SysMenu::getSort);
            allMenus = list(treeWrapper);
        } else {
            allMenus = list();
        }

        List<MenuVO> tree = buildMenuTree(allMenus, 0L);
        return Response.ok(tree);
    }

    @Override
    public Response getPageList(MenuPageQueryDTO parameter) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(parameter.getName())) {
            wrapper.like(SysMenu::getName, parameter.getName());
        }
        if (parameter.getType() != null) {
            wrapper.eq(SysMenu::getType, parameter.getType());
        }
        if (parameter.getStatus() != null) {
            wrapper.eq(SysMenu::getStatus, parameter.getStatus());
        }
        wrapper.orderByAsc(SysMenu::getSort);

        IPage<SysMenu> page = new Page<>(parameter.getPageNum(), parameter.getPageSize());
        IPage<SysMenu> result = page(page, wrapper);
        return Response.ok(PageResultVO.of(result));
    }

    @Override
    public Response getMenuById(Long id) {
        SysMenu menu = getById(id);
        if (menu == null) {
            return Response.fail(500, "菜单不存在");
        }
        MenuVO vo = new MenuVO();
        BeanConvertUtils.copyProperties(menu, vo);
        return Response.ok(vo);
    }

    @Override
    public Response addMenu(MenuSaveDTO dto) {
        SysMenu menu = new SysMenu();
        BeanConvertUtils.copyProperties(dto, menu);
        save(menu);
        return Response.ok("新增菜单成功", null);
    }

    @Override
    public Response updateMenu(MenuUpdateDTO dto) {
        SysMenu existMenu = getById(dto.getId());
        if (existMenu == null) {
            return Response.fail(500, "菜单不存在");
        }
        SysMenu menu = new SysMenu();
        BeanConvertUtils.copyProperties(dto, menu);
        updateById(menu);
        return Response.ok("修改菜单成功", null);
    }

    @Override
    public Response deleteMenu(Long id) {
        SysMenu menu = getById(id);
        if (menu == null) {
            return Response.fail(500, "菜单不存在");
        }
        // 递归收集所有子菜单ID
        List<Long> idsToDelete = new ArrayList<>();
        collectChildIds(id, idsToDelete);
        idsToDelete.add(id);
        removeByIds(idsToDelete);
        return Response.ok("成功删除" + idsToDelete.size() + "个菜单/按钮", null);
    }

    @Override
    public Response updateStatus(MenuStatusDTO dto) {
        SysMenu existMenu = getById(dto.getId());
        if (existMenu == null) {
            return Response.fail(500, "菜单不存在");
        }
        SysMenu menu = new SysMenu();
        menu.setId(dto.getId());
        menu.setStatus(Boolean.TRUE.equals(dto.getStatus()) ? 1 : 0);
        updateById(menu);

        // 菜单状态变更可能影响所有用户可见/可访问菜单，清除全部权限缓存
        permissionCacheService.invalidateAllPermissions();
        log.info("[菜单管理] 菜单启停成功，menuId={}, {}->{}", dto.getId(), existMenu.getStatus() == 1, dto.getStatus());
        return Response.ok("菜单启停成功", null);
    }

    @Override
    public Response getAllMenus(Boolean status) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(SysMenu::getStatus, Boolean.TRUE.equals(status) ? 1 : 0);
        }
        wrapper.orderByAsc(SysMenu::getSort);
        List<SysMenu> menus = list(wrapper);
        List<MenuVO> voList = menus.stream().map(m -> {
            MenuVO vo = new MenuVO();
            BeanConvertUtils.copyProperties(m, vo);
            return vo;
        }).collect(Collectors.toList());
        return Response.ok(voList);
    }

    // ====================== 私有方法 ======================

    /**
     * 递归构建菜单树
     */
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
     * 递归收集所有子菜单ID
     */
    private void collectChildIds(Long parentId, List<Long> ids) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getParentId, parentId);
        List<SysMenu> children = list(wrapper);
        for (SysMenu child : children) {
            ids.add(child.getId());
            collectChildIds(child.getId(), ids);
        }
    }

    /**
     * 递归收集祖先菜单ID（用于按名称搜索时保持树结构完整）
     */
    private void collectAncestorIds(Long parentId, List<Long> ids) {
        if (parentId == null || parentId == 0L) {
            return;
        }
        if (!ids.contains(parentId)) {
            ids.add(parentId);
            SysMenu parent = getById(parentId);
            if (parent != null) {
                collectAncestorIds(parent.getParentId(), ids);
            }
        }
    }
}