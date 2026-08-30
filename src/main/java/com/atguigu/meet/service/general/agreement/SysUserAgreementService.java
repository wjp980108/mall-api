package com.atguigu.meet.service.general.agreement;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.general.agreement.AgreementSaveDTO;

/**
 * 用户协议 Service（表内仅一条生效记录，固定 id=1）
 */
public interface SysUserAgreementService {

    /** 获取最新协议（后台编辑回显 / C端展示共用） */
    Response getLatest();

    /** 保存协议（首次创建，之后覆盖更新，永远只保留一份最新版本） */
    Response saveAgreement(AgreementSaveDTO dto);
}
