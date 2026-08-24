package com.atguigu.meet.service.permission.menu;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.permission.menu.MenuPageQueryDTO;
import com.atguigu.meet.model.dto.permission.menu.MenuSaveDTO;
import com.atguigu.meet.model.dto.permission.menu.MenuStatusDTO;
import com.atguigu.meet.model.dto.permission.menu.MenuUpdateDTO;
import com.atguigu.meet.model.entity.permission.menu.SysMenu;

import java.util.List;

/**
 * 菜单管理 Service
 */
public interface MenuService {

    /** 菜单树形列表（支持按名称模糊查询、按状态过滤） */
    Response getMenuTree(String name, Boolean status);

    /** 菜单平铺分页列表 */
    Response getPageList(MenuPageQueryDTO parameter);

    /** 根据ID查菜单 */
    Response getMenuById(Long id);

    /** 新增菜单 */
    Response addMenu(MenuSaveDTO dto);

    /** 修改菜单 */
    Response updateMenu(MenuUpdateDTO dto);

    /** 删除菜单（递归删除子菜单） */
    Response deleteMenu(Long id);

    /** 启用/禁用菜单 */
    Response updateStatus(MenuStatusDTO dto);

    /** 获取所有菜单（平铺，供角色分配菜单时使用） */
    Response getAllMenus(Boolean status);
}