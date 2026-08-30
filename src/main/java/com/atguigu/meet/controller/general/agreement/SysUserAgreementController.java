package com.atguigu.meet.controller.general.agreement;

import com.atguigu.meet.annotation.RequirePermission;
import com.atguigu.meet.common.Response;
import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.model.dto.general.agreement.AgreementSaveDTO;
import com.atguigu.meet.service.general.agreement.SysUserAgreementService;
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
public class SysUserAgreementController {

    @Autowired
    private SysUserAgreementService agreementService;

    /** 获取最新协议（编辑回显） */
    @GetMapping
    @RequirePermission(PermissionConst.AGREEMENT_QUERY)
    public Response getLatest() {
        return agreementService.getLatest();
    }

    /** 保存协议（仅保留一份最新版本） */
    @PutMapping
    @RequirePermission(PermissionConst.AGREEMENT_UPDATE)
    public Response save(@RequestBody @Valid AgreementSaveDTO dto) {
        return agreementService.saveAgreement(dto);
    }
}
