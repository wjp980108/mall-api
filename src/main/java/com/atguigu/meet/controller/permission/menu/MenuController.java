package com.atguigu.meet.controller.permission.menu;

import com.atguigu.meet.annotation.RequirePermission;
import com.atguigu.meet.common.Response;
import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.model.dto.permission.menu.MenuPageQueryDTO;
import com.atguigu.meet.model.dto.permission.menu.MenuSaveDTO;
import com.atguigu.meet.model.dto.permission.menu.MenuStatusDTO;
import com.atguigu.meet.model.dto.permission.menu.MenuUpdateDTO;
import com.atguigu.meet.model.vo.permission.menu.MenuVO;
import com.atguigu.meet.service.permission.menu.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "菜单管理", description = "菜单CRUD及树形结构管理")
public class MenuController {
    @Autowired
    private MenuService menuService;

    /** 菜单树形列表 */
    @GetMapping("/tree")
    @Operation(summary = "菜单树形列表", description = "获取菜单树形结构")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MenuVO.class)))
    public Response<MenuVO> getMenuTree(@RequestParam(required = false) String name,
                                @RequestParam(required = false) Boolean status) {
        return menuService.getMenuTree(name, status);
    }

    /** 菜单平铺分页列表 */
    @GetMapping
    @Operation(summary = "菜单分页列表", description = "分页查询菜单列表（平铺）")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MenuVO.class)))
    public Response<MenuVO> getPageList(@Valid MenuPageQueryDTO parameter) {
        return menuService.getPageList(parameter);
    }

    /** 所有菜单（平铺，角色分配菜单用） */
    @GetMapping("/all")
    @Operation(summary = "所有菜单", description = "获取所有菜单（平铺，角色分配菜单用）")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MenuVO.class)))
    public Response<MenuVO> getAllMenus(@RequestParam(required = false) Boolean status) {
        return menuService.getAllMenus(status);
    }

    /** 根据ID查菜单 */
    @GetMapping("/{id}")
    @Operation(summary = "菜单详情", description = "根据ID查询菜单详情")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MenuVO.class)))
    public Response<MenuVO> getMenuById(@PathVariable Long id) {
        return menuService.getMenuById(id);
    }

    /** 新增菜单 */
    @PostMapping
    @RequirePermission(PermissionConst.MENU_ADD)
    @Operation(summary = "新增菜单", description = "创建新菜单")
    public Response<Void> addMenu(@RequestBody @Valid MenuSaveDTO dto) {
        return menuService.addMenu(dto);
    }

    /** 修改菜单 */
    @PutMapping
    @RequirePermission(PermissionConst.MENU_UPDATE)
    @Operation(summary = "修改菜单", description = "更新菜单信息")
    public Response<Void> updateMenu(@RequestBody @Valid MenuUpdateDTO dto) {
        return menuService.updateMenu(dto);
    }

    /** 删除菜单（递归删除子菜单） */
    @DeleteMapping("/{id}")
    @RequirePermission(PermissionConst.MENU_DELETE)
    @Operation(summary = "删除菜单", description = "删除菜单（递归删除子菜单）")
    public Response<Void> deleteMenu(@PathVariable Long id) {
        return menuService.deleteMenu(id);
    }

    /** 启用/禁用菜单 */
    @PatchMapping("/status")
    @RequirePermission(PermissionConst.MENU_STATUS)
    @Operation(summary = "更新菜单状态", description = "启用或禁用菜单")
    public Response<Void> updateStatus(@RequestBody @Valid MenuStatusDTO dto) {
        return menuService.updateStatus(dto);
    }
}