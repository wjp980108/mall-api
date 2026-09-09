package com.atguigu.meet.controller.app.general.settings;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.vo.general.settings.SysSettingsPublicVO;
import com.atguigu.meet.service.general.settings.SysSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端公开系统设置查询接口
 * <p>
 * 公开访问(application.yml public-paths 白名单 /app/settings):
 * 首页/登录页等展示站点名称、Logo、推广海报等, 无需登录态.
 */
@RestController
@RequestMapping("/app/settings")
@Tag(name = "H5系统设置", description = "H5端公开系统设置查询接口")
public class AppSysSettingsController {

    @Autowired
    private SysSettingsService sysSettingsService;

    /** 公开查询站点设置 */
    @GetMapping
    @Operation(summary = "公开查询站点设置", description = "只返回站点名称、Logo两个展示字段")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SysSettingsPublicVO.class)))
    public Response<SysSettingsPublicVO> getPublic() {
        return Response.ok(sysSettingsService.getPublic());
    }
}
