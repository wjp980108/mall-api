package com.atguigu.meet.controller.admin.home;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.vo.admin.home.HomeStatsVO;
import com.atguigu.meet.service.admin.home.HomeStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台首页接口
 * <p>
 * 只读聚合查询，仅要求登录态（JWT 有效、用户状态正常），不做按钮权限校验，
 * 与既有后台查询接口（列表/分页/详情/回显）口径一致。
 */
@RestController
@RequestMapping("/admin/home")
@Tag(name = "后台首页", description = "后台首页聚合统计")
public class AdminHomeController {

    @Autowired
    private HomeStatsService homeStatsService;

    /**
     * 首页统计：会员总数、今日新增会员、文件总数。
     */
    @GetMapping("/stats")
    @Operation(summary = "首页统计", description = "获取会员总数/今日新增与文件总数")
    public Response<HomeStatsVO> getHomeStats() {
        return homeStatsService.getHomeStats();
    }
}
