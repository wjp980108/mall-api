package com.atguigu.meet.controller.permission.invite;

import com.atguigu.meet.annotation.RequirePermission;
import com.atguigu.meet.common.Response;
import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.model.vo.permission.invite.CompensateResultVO;
import com.atguigu.meet.service.permission.invite.InviteCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 邀请码运维接口（受限权限）
 * <p>
 * 与 {@link InviteCodeController}（普通用户接口）分离，路径前缀
 * {@code /admin/invite-code}，仅运维角色可见，需配套在 sys_menu 中初始化
 * 对应按钮权限（{@link PermissionConst#INVITE_CODE_COMPENSATE}）。
 */
@RestController
@RequestMapping("/admin/invite-code")
@Tag(name = "邀请码运维", description = "邀请码存量补偿等受限运维接口")
public class AdminInviteCodeController {

    @Autowired
    private InviteCodeService inviteCodeService;

    /**
     * 存量补偿：扫描 sys_user 中无对应 sys_invite_code 的用户，逐个补生成邀请码。
     * <p>
     * 接口幂等，多次调用只补尚未生成的那批；已有码用户自动跳过。
     * 单个用户失败只回滚该用户的操作，记录失败列表返回，不影响其他用户。
     */
    @PostMapping("/compensate")
    @RequirePermission(PermissionConst.INVITE_CODE_COMPENSATE)
    @Operation(summary = "邀请码存量补偿", description = "扫描无邀请码用户并补生成（一次性运维能力，幂等）")
    public Response<CompensateResultVO> compensate() {
        return inviteCodeService.compensateMissingInviteCodes();
    }
}
