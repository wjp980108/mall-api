package com.atguigu.meet.service.general.settings;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.general.settings.SysSettingsUpdateDTO;
import com.atguigu.meet.model.entity.general.settings.SysSettings;
import com.atguigu.meet.model.vo.general.settings.SysSettingsPublicVO;

/**
 * 系统设置 Service
 * <p>单行表, 全局固定 id=1, 只有 get + update 两个业务方法.
 */
public interface SysSettingsService {

    /**
     * 查询系统设置(单行, id=1)
     */
    SysSettings get();

    /**
     * 更新系统设置(LambdaUpdateWrapper 定点更新, 防实体 null 覆盖默认值)
     */
    Response update(SysSettingsUpdateDTO dto);

    /**
     * C 端公开查询(只返回站点名称/Logo 展示字段, 脱敏内部业务参数)
     */
    SysSettingsPublicVO getPublic();
}
