package com.atguigu.meet.controller.info.notice;

import com.atguigu.meet.annotation.RequirePermission;
import com.atguigu.meet.common.Response;
import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.model.dto.info.notice.NoticePageQueryDTO;
import com.atguigu.meet.model.dto.info.notice.NoticeSaveDTO;
import com.atguigu.meet.model.dto.info.notice.NoticeUpdateDTO;
import com.atguigu.meet.service.info.notice.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 公告管理接口
 */
@RestController
@RequestMapping("/notices")
@Validated
@Tag(name = "公告管理", description = "后台公告管理接口")
public class NoticeController {
    @Autowired
    private NoticeService noticeService;

    /** 公告分页列表 */
    @GetMapping
    @RequirePermission(PermissionConst.NOTICE_QUERY)
    @Operation(summary = "公告分页列表", description = "分页查询公告列表")
    public Response getPageList(@Valid NoticePageQueryDTO parameter) {
        return noticeService.getPageList(parameter);
    }

    /** 所有启用公告（C端展示/下拉用） */
    @GetMapping("/enabled")
    @RequirePermission(PermissionConst.NOTICE_QUERY)
    @Operation(summary = "所有启用公告", description = "获取所有启用的公告（C端展示/下拉用）")
    public Response getAllEnabledNotices(String position) {
        return noticeService.getAllEnabledNotices(position);
    }

    /** 根据ID查公告（含阅读次数） */
    @GetMapping("/{id}")
    @RequirePermission(PermissionConst.NOTICE_QUERY)
    @Operation(summary = "公告详情", description = "根据ID查询公告详情（含阅读次数）")
    public Response getNoticeById(@PathVariable Long id) {
        return noticeService.getNoticeById(id);
    }

    /** 新增公告 */
    @PostMapping
    @RequirePermission(PermissionConst.NOTICE_ADD)
    @Operation(summary = "新增公告", description = "创建新公告")
    public Response addNotice(@RequestBody @Valid NoticeSaveDTO dto) {
        return noticeService.addNotice(dto);
    }

    /** 修改公告 */
    @PutMapping
    @RequirePermission(PermissionConst.NOTICE_UPDATE)
    @Operation(summary = "修改公告", description = "更新公告信息")
    public Response updateNotice(@RequestBody @Valid NoticeUpdateDTO dto) {
        return noticeService.updateNotice(dto);
    }

    /** 删除公告（逻辑删除） */
    @DeleteMapping("/{id}")
    @RequirePermission(PermissionConst.NOTICE_DELETE)
    @Operation(summary = "删除公告", description = "逻辑删除公告")
    public Response deleteNotice(@PathVariable Long id) {
        return noticeService.deleteNotice(id);
    }
}