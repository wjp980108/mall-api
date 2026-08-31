package com.atguigu.meet.service.general.config;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.general.config.SysConfigGroupSaveDTO;

/**
 * 系统动态配置 Service
 */
public interface SysConfigService {

    /**
     * 按分组查询配置列表（sort 升序，Redis 缓存优先）
     *
     * @param configGroup 配置分组标识: base/member/pay/email
     */
    Response getGroupConfigs(String configGroup);

    /**
     * 分组全量保存（前端一次性提交整个分组下全部配置项，存在则更新值，不存在则新增；
     * 更新成功后删除分组 Redis 缓存并写入变更日志）
     */
    Response saveGroup(SysConfigGroupSaveDTO dto);
}
