package com.atguigu.meet.service.general.agreement.impl;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.mapper.general.agreement.SysUserAgreementMapper;
import com.atguigu.meet.model.dto.general.agreement.AgreementSaveDTO;
import com.atguigu.meet.model.entity.general.agreement.SysUserAgreement;
import com.atguigu.meet.service.general.agreement.SysUserAgreementService;
import com.atguigu.meet.utils.AdminContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户协议 Service 实现
 */
@Service
@Slf4j
public class SysUserAgreementServiceImpl extends ServiceImpl<SysUserAgreementMapper, SysUserAgreement> implements SysUserAgreementService {

    @Override
    public Response getByType(Integer type) {
        if (type == null) {
            return Response.fail("协议类型不能为空");
        }
        LambdaQueryWrapper<SysUserAgreement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserAgreement::getType, type);
        SysUserAgreement agreement = getOne(wrapper);
        if (agreement == null) {
            agreement = new SysUserAgreement();
            agreement.setType(type);
        }
        return Response.ok(agreement);
    }

    @Override
    public Response saveAgreement(AgreementSaveDTO dto) {
        if (dto.getType() == null) {
            return Response.fail("协议类型不能为空");
        }
        String operator = AdminContext.get() != null ? AdminContext.get().getUsername() : null;
        LocalDateTime now = LocalDateTime.now();

        LambdaQueryWrapper<SysUserAgreement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserAgreement::getType, dto.getType());
        SysUserAgreement agreement = getOne(wrapper);
        if (agreement == null) {
            // 首次创建
            agreement = new SysUserAgreement();
            agreement.setType(dto.getType());
            agreement.setTitle(dto.getTitle());
            agreement.setContent(dto.getContent());
            agreement.setCreateBy(operator);
            agreement.setCreateTime(now);
            agreement.setUpdateBy(operator);
            agreement.setUpdateTime(now);
            save(agreement);
            log.info("[用户协议] 首次创建协议，类型={}, 操作人={}", dto.getType(), operator);
        } else {
            // 覆盖更新
            agreement.setTitle(dto.getTitle());
            agreement.setContent(dto.getContent());
            agreement.setUpdateBy(operator);
            agreement.setUpdateTime(now);
            updateById(agreement);
            log.info("[用户协议] 保存协议成功，类型={}, 操作人={}", dto.getType(), operator);
        }
        return Response.ok("保存成功", null);
    }
}