package com.atguigu.meet.controller.app.seckill;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.service.seckill.session.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * H5 抢购场次接口
 * <p>
 * 不走 {@code @RequirePermission}（后台 RBAC），依赖 JWT 登录态。
 * 返回所有启用场次完整信息（含抢购时间窗口、背景图、进场控制等），供 C 端首页展示。
 */
@RestController
@RequestMapping("/app/session")
public class AppSessionController {

    @Autowired
    private SessionService sessionService;

    /**
     * 启用场次列表
     * （C 端首页展示，按 sort 升序 + 创建时间倒序，返回完整字段含时间窗口/背景图）
     */
    @GetMapping("/enabled")
    public Response listEnabled() {
        return sessionService.getAllEnabledSessions();
    }
}
