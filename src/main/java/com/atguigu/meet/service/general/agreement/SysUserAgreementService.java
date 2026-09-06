package com.atguigu.meet.service.general.agreement;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.general.agreement.AgreementSaveDTO;

/**
 * 用户协议 Service
 */
public interface SysUserAgreementService {

    /** 根据类型获取协议 */
    Response getByType(Integer type);

    /** 保存协议 */
    Response saveAgreement(AgreementSaveDTO dto);
}