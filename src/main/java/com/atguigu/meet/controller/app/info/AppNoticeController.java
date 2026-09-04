package com.atguigu.meet.controller.app.info;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.service.info.notice.NoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * H5 公告接口
 * <p>
 * 不走 {@code @RequirePermission}（后台 RBAC），依赖 JWT 登录态。
 * 复用 {@link NoticeService} 已有方法，返回启用公告列表与详情。
 */
@RestController
@RequestMapping("/app/notice")
public class AppNoticeController {

    @Autowired
    private NoticeService noticeService;

    /**
     * 启用公告列表
     * （C 端首页展示，按 sort 倒序 + 创建时间倒序）
     */
    @GetMapping("/enabled")
    public Response listEnabled() {
        return noticeService.getAllEnabledNotices();
    }

    /**
     * 公告详情
     * （含阅读次数聚合）
     */
    @GetMapping("/{id}")
    public Response getDetail(@PathVariable Long id) {
        return noticeService.getNoticeById(id);
    }
}
