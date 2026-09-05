package com.atguigu.meet.controller.app.general.agreement;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.service.general.agreement.SysUserAgreementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户协议接口
 * <p>公开访问（application.yml public-paths 白名单 /app/agreement）：
 * 用户注册前即需查看用户注册协议，此时尚无登录态。
 */
@RestController
@RequestMapping("/app/agreement")
@Tag(name = "H5用户协议", description = "H5端用户协议查询接口")
public class AppUserAgreementController {

    @Autowired
    private SysUserAgreementService agreementService;

    /** 获取最新用户协议 */
    @GetMapping
    @Operation(summary = "获取最新协议", description = "获取最新版本的用户协议")
    public Response getLatest() {
        return agreementService.getLatest();
    }
}