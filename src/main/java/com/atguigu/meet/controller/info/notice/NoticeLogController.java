package com.atguigu.meet.controller.info.notice;

import com.atguigu.meet.annotation.RequirePermission;
import com.atguigu.meet.common.Response;
import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.model.dto.info.notice.NoticeLogPageQueryDTO;
import com.atguigu.meet.model.entity.info.notice.NoticeLog;
import com.atguigu.meet.service.info.notice.NoticeLogService;
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
 * 公告阅读日志接口（管理端：阅读记录与统计）
 */
@RestController
@RequestMapping("/notice-logs")
@Validated
@Tag(name = "公告阅读日志", description = "公告阅读记录与统计管理")
public class NoticeLogController {
    @Autowired
    private NoticeLogService noticeLogService;

    /** 阅读日志分页列表（可按公告/用户筛选） */
    @GetMapping
    @RequirePermission(PermissionConst.NOTICE_LOG_QUERY)
    @Operation(summary = "阅读日志分页列表", description = "分页查询阅读日志（可按公告/用户筛选）")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = NoticeLog.class)))
    public Response<NoticeLog> getPageList(@Valid NoticeLogPageQueryDTO parameter) {
        return noticeLogService.getPageList(parameter);
    }

    /** 根据公告ID查询读者阅读记录列表 */
    @GetMapping("/by-notice/{noticeId}")
    @RequirePermission(PermissionConst.NOTICE_LOG_QUERY)
    @Operation(summary = "查询读者记录", description = "根据公告ID查询读者阅读记录列表")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = NoticeLog.class)))
    public Response<NoticeLog> getReadersByNoticeId(@PathVariable Long noticeId) {
        return noticeLogService.getReadersByNoticeId(noticeId);
    }

    /** 根据公告ID查询阅读次数 */
    @GetMapping("/count/{noticeId}")
    @RequirePermission(PermissionConst.NOTICE_LOG_QUERY)
    @Operation(summary = "查询阅读次数", description = "根据公告ID查询阅读次数")
    public Response<Long> getReadCount(@PathVariable Long noticeId) {
        return noticeLogService.getReadCount(noticeId);
    }
}