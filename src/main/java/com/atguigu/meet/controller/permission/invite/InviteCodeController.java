package com.atguigu.meet.controller.permission.invite;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.service.permission.invite.InviteCodeService;
import com.atguigu.meet.utils.AdminContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 邀请码相关接口
 */
@RestController
@RequestMapping("/invite")
@Tag(name = "邀请码管理", description = "邀请码生成与查询")
public class InviteCodeController {

    @Autowired
    private InviteCodeService inviteCodeService;

    /**
     * 生成我的邀请码（1人1码，已存在则直接返回）
     */
    @PostMapping("/code/generate")
    @Operation(summary = "生成邀请码", description = "生成我的邀请码（1人1码，已存在则直接返回）")
    public Response generateInviteCode() {
        Long userId = AdminContext.getLoginUserId();
        return inviteCodeService.generateInviteCode(userId);
    }

    /**
     * 查询我的邀请码
     */
    @GetMapping("/code/mine")
    @Operation(summary = "查询我的邀请码", description = "查询当前用户的邀请码")
    public Response getMyInviteCode() {
        Long userId = AdminContext.getLoginUserId();
        return inviteCodeService.getMyInviteCode(userId);
    }

    /**
     * 查询我的邀请明细流水
     */
    @GetMapping("/records")
    @Operation(summary = "查询邀请记录", description = "查询我的邀请明细流水")
    public Response getInviteRecords() {
        Long userId = AdminContext.getLoginUserId();
        return inviteCodeService.getInviteRecords(userId);
    }
}