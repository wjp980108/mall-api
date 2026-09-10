package com.atguigu.meet.controller.general.agreement;

import com.atguigu.meet.annotation.RequirePermission;
import com.atguigu.meet.common.Response;
import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.model.dto.general.agreement.AgreementSaveDTO;
import com.atguigu.meet.model.entity.general.agreement.SysUserAgreement;
import com.atguigu.meet.service.general.agreement.SysUserAgreementService;
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
 * 用户协议管理接口
 */
@RestController
@RequestMapping("/agreement")
@Validated
@Tag(name = "用户协议管理", description = "后台用户协议管理接口")
public class SysUserAgreementController {

    @Autowired
    private SysUserAgreementService agreementService;

    /** 根据类型获取协议 */
    @GetMapping
    @Operation(summary = "根据类型获取协议", description = "协议类型：1-用户协议 2-隐私协议 3-委托协议 4-分销说明")
    @ApiResponse(responseCode = "200", description = "成功", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SysUserAgreement.class)))
    public Response<SysUserAgreement> getByType(Integer type) {
        return agreementService.getByType(type);
    }

    /** 保存协议 */
    @PutMapping
    @RequirePermission(PermissionConst.AGREEMENT_UPDATE)
    @Operation(summary = "保存协议", description = "保存用户协议（根据type区分协议类型）")
    public Response<Void> save(@RequestBody @Valid AgreementSaveDTO dto) {
        return agreementService.saveAgreement(dto);
    }
}