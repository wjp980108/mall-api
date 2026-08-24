package com.atguigu.meet.controller.permission.menu;

import com.atguigu.meet.annotation.RequirePermission;
import com.atguigu.meet.common.Response;
import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.model.dto.permission.menu.MenuPageQueryDTO;
import com.atguigu.meet.model.dto.permission.menu.MenuSaveDTO;
import com.atguigu.meet.model.dto.permission.menu.MenuStatusDTO;
import com.atguigu.meet.model.dto.permission.menu.MenuUpdateDTO;
import com.atguigu.meet.service.permission.menu.MenuService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 菜单管理接口
 */
@RestController
@RequestMapping("/menus")
@Validated
public class MenuController {
    @Autowired
    private MenuService menuService;

    /** 菜单树形列表 */
    @GetMapping("/tree")
    @RequirePermission(PermissionConst.MENU_QUERY)
    public Response getMenuTree(@RequestParam(required = false) String name) {
        return menuService.getMenuTree(name);
    }

    /** 菜单平铺分页列表 */
    @GetMapping
    @RequirePermission(PermissionConst.MENU_QUERY)
    public Response getPageList(@Valid MenuPageQueryDTO parameter) {
        return menuService.getPageList(parameter);
    }

    /** 所有菜单（平铺，角色分配菜单用） */
    @GetMapping("/all")
    @RequirePermission(PermissionConst.MENU_QUERY)
    public Response getAllMenus(@RequestParam(required = false) Boolean status) {
        return menuService.getAllMenus(status);
    }

    /** 根据ID查菜单 */
    @GetMapping("/{id}")
    @RequirePermission(PermissionConst.MENU_QUERY)
    public Response getMenuById(@PathVariable Long id) {
        return menuService.getMenuById(id);
    }

    /** 新增菜单 */
    @PostMapping
    @RequirePermission(PermissionConst.MENU_ADD)
    public Response addMenu(@RequestBody @Valid MenuSaveDTO dto) {
        return menuService.addMenu(dto);
    }

    /** 修改菜单 */
    @PutMapping
    @RequirePermission(PermissionConst.MENU_UPDATE)
    public Response updateMenu(@RequestBody @Valid MenuUpdateDTO dto) {
        return menuService.updateMenu(dto);
    }

    /** 删除菜单（递归删除子菜单） */
    @DeleteMapping("/{id}")
    @RequirePermission(PermissionConst.MENU_DELETE)
    public Response deleteMenu(@PathVariable Long id) {
        return menuService.deleteMenu(id);
    }

    /** 启用/禁用菜单 */
    @PatchMapping("/status")
    @RequirePermission(PermissionConst.MENU_STATUS)
    public Response updateStatus(@RequestBody @Valid MenuStatusDTO dto) {
        return menuService.updateStatus(dto);
    }
}