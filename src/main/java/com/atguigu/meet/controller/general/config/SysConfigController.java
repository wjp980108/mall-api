package com.atguigu.meet.controller.general.config;

import com.atguigu.meet.annotation.RequirePermission;
import com.atguigu.meet.common.Response;
import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.model.dto.general.config.SysConfigGroupSaveDTO;
import com.atguigu.meet.service.general.config.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统动态配置接口
 * <p>
 * 前端 Tab 页按 config_group（base/member/pay/email）查询；
 * 点击【确定】一次性提交整个分组下全部配置项（全量覆盖保存）。
 * 权限标识使用 {@link PermissionConst} 中预留的 sys:config:xxx。
 */
@RestController
@RequestMapping("/configs")
@Validated
@Tag(name = "系统配置管理", description = "系统动态配置管理接口")
public class SysConfigController {

    @Autowired
    private SysConfigService sysConfigService;

    /**
     * 分组配置列表（Tab 页传 config_group，按 sort 升序返回）
     */
    @GetMapping
    @RequirePermission(PermissionConst.SYS_CONFIG_QUERY)
    @Operation(summary = "查询分组配置", description = "按分组查询配置列表")
    public Response listByGroup(@RequestParam("configGroup")
                                @NotBlank(message = "配置分组不能为空") String configGroup) {
        return sysConfigService.getGroupConfigs(configGroup);
    }

    /**
     * 分组全量保存（提交整个分组下全部配置项数组，禁止单条保存）
     */
    @PutMapping
    @RequirePermission(PermissionConst.SYS_CONFIG_UPDATE)
    @Operation(summary = "保存分组配置", description = "全量保存分组配置")
    public Response saveGroup(@RequestBody @Valid SysConfigGroupSaveDTO dto) {
        return sysConfigService.saveGroup(dto);
    }
}