package com.atguigu.meet.controller.general.settings;

import com.atguigu.meet.annotation.RequirePermission;
import com.atguigu.meet.common.Response;
import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.model.dto.general.settings.SysSettingsUpdateDTO;
import com.atguigu.meet.model.entity.general.settings.SysSettings;
import com.atguigu.meet.service.general.settings.SysSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统设置管理接口
 * <p>
 * 单行表, 只有查询 + 更新两个接口.
 * 权限标识 sys:settings:query / sys:settings:update, 菜单挂在"常规管理"下.
 */
@RestController
@RequestMapping("/settings")
@Validated
@Tag(name = "系统设置管理", description = "系统设置管理接口")
public class SysSettingsController {

    @Autowired
    private SysSettingsService sysSettingsService;

    /**
     * 查询系统设置(单行)
     */
    @GetMapping
    @RequirePermission(PermissionConst.SYS_SETTINGS_QUERY)
    @Operation(summary = "查询系统设置", description = "查询系统设置(单行回显)")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SysSettings.class)))
    public Response<SysSettings> get() {
        return Response.ok(sysSettingsService.get());
    }

    /**
     * 公开查询系统设置(无需 token, 后台登录页等展示站点信息用)
     */
    @GetMapping("/public")
    @Operation(summary = "公开查询系统设置", description = "无需登录, 返回站点名称、Logo等前端展示字段")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SysSettings.class)))
    public Response<SysSettings> getPublic() {
        return Response.ok(sysSettingsService.getPublic());
    }

    /**
     * 更新系统设置(全量覆盖)
     */
    @PutMapping
    @RequirePermission(PermissionConst.SYS_SETTINGS_UPDATE)
    @Operation(summary = "更新系统设置", description = "全量更新系统设置")
    public Response<Void> update(@RequestBody @Valid SysSettingsUpdateDTO dto) {
        return sysSettingsService.update(dto);
    }
}