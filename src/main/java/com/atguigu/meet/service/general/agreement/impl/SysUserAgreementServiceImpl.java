package com.atguigu.meet.service.general.agreement.impl;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.mapper.general.agreement.SysUserAgreementMapper;
import com.atguigu.meet.model.dto.general.agreement.AgreementSaveDTO;
import com.atguigu.meet.model.entity.general.agreement.SysUserAgreement;
import com.atguigu.meet.service.general.agreement.SysUserAgreementService;
import com.atguigu.meet.utils.AdminContext;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户协议 Service 实现
 * <p>单例行设计：固定 id={@link SysUserAgreement#SINGLETON_ID}，
 * 首次保存执行插入，之后全部转为覆盖更新，天然保证表内永远只有一条生效数据。
 */
@Service
@Slf4j
public class SysUserAgreementServiceImpl extends ServiceImpl<SysUserAgreementMapper, SysUserAgreement> implements SysUserAgreementService {

    @Override
    public Response getLatest() {
        SysUserAgreement agreement = getById(SysUserAgreement.SINGLETON_ID);
        // 未初始化时返回空对象，前端编辑页可直接填写（不视为错误）
        if (agreement == null) {
            agreement = new SysUserAgreement();
        }
        return Response.ok(agreement);
    }

    @Override
    public Response saveAgreement(AgreementSaveDTO dto) {
        String operator = AdminContext.get() != null ? AdminContext.get().getUsername() : null;
        LocalDateTime now = LocalDateTime.now();

        SysUserAgreement agreement = getById(SysUserAgreement.SINGLETON_ID);
        if (agreement == null) {
            // 首次创建（固定 id=1；若并发重复插入会触发主键冲突，兜底保证单例行）
            agreement = new SysUserAgreement();
            agreement.setId(SysUserAgreement.SINGLETON_ID);
            agreement.setTitle(dto.getTitle());
            agreement.setContent(dto.getContent());
            agreement.setCreateBy(operator);
            agreement.setCreateTime(now);
            agreement.setUpdateBy(operator);
            agreement.setUpdateTime(now);
            save(agreement);
            log.info("[用户协议] 首次创建协议，操作人={}", operator);
        } else {
            // 覆盖更新（updateById 跳过 null 字段，create_by/create_time 不受影响）
            agreement.setTitle(dto.getTitle());
            agreement.setContent(dto.getContent());
            agreement.setUpdateBy(operator);
            agreement.setUpdateTime(now);
            updateById(agreement);
            log.info("[用户协议] 保存协议成功，操作人={}", operator);
        }
        return Response.ok("保存成功", null);
    }
}
