package com.atguigu.meet.controller.seckill.session;

import com.atguigu.meet.annotation.RequirePermission;
import com.atguigu.meet.common.Response;
import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.model.dto.seckill.session.SessionPageQueryDTO;
import com.atguigu.meet.model.dto.seckill.session.SessionSaveDTO;
import com.atguigu.meet.model.dto.seckill.session.SessionStatusDTO;
import com.atguigu.meet.model.dto.seckill.session.SessionUpdateDTO;
import com.atguigu.meet.service.file.FileService;
import com.atguigu.meet.service.seckill.session.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 抢购时间设置
 */
@RestController
@RequestMapping("/sessions")
@Validated
@Tag(name = "抢购场次管理", description = "抢购场次CRUD及背景图管理")
public class SessionController {
    @Autowired
    private SessionService sessionService;

    @Autowired
    private FileService fileService;

    /**
     * 上传场次背景图
     * 内部调用通用上传接口(bizType=sessionBg)，使用独立的场次背景图上传权限
     */
    @PostMapping("/bgImg")
    @RequirePermission(PermissionConst.SESSION_BG_UPLOAD)
    @Operation(summary = "上传场次背景图", description = "上传抢购场次背景图")
    public Response uploadBgImg(@RequestParam("file") MultipartFile file,
                                @RequestParam(value = "platform", required = false) String platform) {
        try {
            return fileService.upload(file, "sessionBg", platform);
        } catch (RuntimeException e) {
            return Response.fail(500, e.getMessage());
        }
    }

    /** 分页列表 */
    @GetMapping
    @RequirePermission(PermissionConst.SESSION_QUERY)
    @Operation(summary = "场次分页列表", description = "分页查询抢购场次列表")
    public Response getPageList(@Valid SessionPageQueryDTO parameter) {
        return sessionService.getPageList(parameter);
    }

    /** 场次下拉选项列表（仅启用场次） */
    @GetMapping("/options")
    @RequirePermission(PermissionConst.SESSION_QUERY)
    @Operation(summary = "场次下拉选项", description = "获取启用的场次下拉选项列表")
    public Response getSessionOptions() {
        return sessionService.getSessionOptions();
    }

    /** 根据ID查场次 */
    @GetMapping("/{id}")
    @RequirePermission(PermissionConst.SESSION_QUERY)
    @Operation(summary = "场次详情", description = "根据ID查询场次详情")
    public Response getSessionById(@PathVariable Long id) {
        return sessionService.getSessionById(id);
    }

    /** 新增场次 */
    @PostMapping
    @RequirePermission(PermissionConst.SESSION_ADD)
    @Operation(summary = "新增场次", description = "创建新抢购场次")
    public Response addSession(@RequestBody @Valid SessionSaveDTO dto) {
        return sessionService.addSession(dto);
    }

    /** 修改场次 */
    @PutMapping
    @RequirePermission(PermissionConst.SESSION_UPDATE)
    @Operation(summary = "修改场次", description = "更新场次信息")
    public Response updateSession(@RequestBody @Valid SessionUpdateDTO dto) {
        return sessionService.updateSession(dto);
    }

    /** 删除场次（逻辑删除） */
    @DeleteMapping("/{id}")
    @RequirePermission(PermissionConst.SESSION_DELETE)
    @Operation(summary = "删除场次", description = "逻辑删除抢购场次")
    public Response deleteSession(@PathVariable Long id) {
        return sessionService.deleteSession(id);
    }

    /** 场次启用/禁用 */
    @PatchMapping("/status")
    @RequirePermission(PermissionConst.SESSION_STATUS)
    @Operation(summary = "更新场次状态", description = "启用或禁用抢购场次")
    public Response updateStatus(@RequestBody @Valid SessionStatusDTO dto) {
        return sessionService.updateStatus(dto);
    }
}