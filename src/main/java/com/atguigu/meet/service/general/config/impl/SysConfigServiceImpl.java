package com.atguigu.meet.service.general.config.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.meet.common.Response;
import com.atguigu.meet.constant.ConfigKeyConst;
import com.atguigu.meet.mapper.general.config.SysConfigMapper;
import com.atguigu.meet.model.dto.general.config.SysConfigGroupSaveDTO;
import com.atguigu.meet.model.dto.general.config.SysConfigItemSaveDTO;
import com.atguigu.meet.model.entity.general.config.SysConfig;
import com.atguigu.meet.model.entity.general.config.SysConfigLog;
import com.atguigu.meet.model.vo.general.config.SysConfigVO;
import com.atguigu.meet.service.general.config.SysConfigLogService;
import com.atguigu.meet.service.general.config.SysConfigService;
import com.atguigu.meet.utils.SysConfigUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 系统动态配置 Service 实现
 * <p>
 * 保存流程（分组全量覆盖）：
 * 1. 后端强校验：提交内 configKey 不重复；number 必须为合法数字且禁止负数（业务正数键禁止0）；
 *    boolean 仅识别 true/false；json 必须可解析（复选框/键值表格统一 JSON 存储，禁用逗号分割字符串）；
 * 2. 按 config_group + config_key 唯一索引判断：存在 -> 更新（LambdaUpdateWrapper 定向更新，
 *    规避实体默认值覆盖坑）；不存在 -> 新增；并发冲突由 uk_group_key 唯一索引兜底；
 * 3. 事务提交成功后：立刻删除该分组 Redis 缓存（防旧值回填）+ 异步写 sys_config_log 变更日志。
 */
@Service
@Slf4j
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    /** 合法值类型 */
    private static final Set<String> VALUE_TYPES = Set.of("string", "number", "boolean", "json");

    /** 业务上必须为正数的配置键（禁止负数与 0；income_rate 比例允许 0，不在此列） */
    private static final Set<String> POSITIVE_NUMBER_KEYS = Set.of(
            ConfigKeyConst.MEMBER_ORDER_LIMIT,      // 会员下单限制数量
            ConfigKeyConst.MEMBER_ROB_ORDER_NUM,    // 会员提前抢购订单数量限制
            ConfigKeyConst.ORDER_LIMIT_TIME);       // 订单超时时间（秒）

    @Autowired
    private SysConfigUtil sysConfigUtil;

    @Autowired
    private SysConfigLogService sysConfigLogService;

    @Override
    public Response getGroupConfigs(String configGroup) {
        if (!StringUtils.hasText(configGroup)) {
            return Response.fail(500, "配置分组不能为空");
        }
        List<SysConfigVO> list = sysConfigUtil.getGroupConfigList(configGroup.trim());
        return Response.ok(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response saveGroup(SysConfigGroupSaveDTO dto) {
        String group = dto.getConfigGroup().trim();
        List<SysConfigItemSaveDTO> items = dto.getItems();

        // 1. 提交内 configKey 重复校验
        Set<String> submitKeys = new HashSet<>();
        for (SysConfigItemSaveDTO item : items) {
            String key = item.getConfigKey().trim();
            if (!submitKeys.add(key)) {
                return Response.fail(500, "配置键在提交中重复: " + key);
            }
        }

        // 2. 逐条后端强校验（不可只依赖前端校验）
        for (SysConfigItemSaveDTO item : items) {
            String err = validateItem(item);
            if (err != null) {
                return Response.fail(500, err);
            }
        }

        // 3. 加载该分组现有记录，按唯一键比对
        List<SysConfig> existList = list(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigGroup, group));
        Map<String, SysConfig> existMap = existList.stream()
                .collect(Collectors.toMap(SysConfig::getConfigKey, e -> e, (a, b) -> a));

        // 新增记录用的分组展示名：优先取提交值，其次沿用库内已有记录
        String groupName = StringUtils.hasText(dto.getConfigGroupName())
                ? dto.getConfigGroupName().trim()
                : (existList.isEmpty() ? group : existList.get(0).getConfigGroupName());

        // 4. upsert + 收集变更明细
        List<SysConfigLog> changes = new ArrayList<>();
        int fallbackSort = 0;
        for (SysConfigItemSaveDTO item : items) {
            String key = item.getConfigKey().trim();
            String value = item.getConfigValue();
            String title = item.getConfigTitle().trim();
            String valueType = item.getValueType().trim();
            Integer sort = item.getSort() != null ? item.getSort() : fallbackSort;
            fallbackSort++;

            SysConfig exist = existMap.get(key);
            if (exist == null) {
                // 不存在 -> 新增
                SysConfig entity = new SysConfig();
                entity.setConfigGroup(group);
                entity.setConfigGroupName(groupName);
                entity.setConfigKey(key);
                entity.setConfigTitle(title);
                entity.setConfigValue(value);
                entity.setValueType(valueType);
                entity.setSort(sort);
                entity.setRemark(item.getRemark());
                try {
                    save(entity);
                } catch (DuplicateKeyException e) {
                    // 并发保存下唯一索引兜底：已存在则转更新
                    updateByKey(group, key, title, value, sort, item.getRemark());
                }
                changes.add(buildLog(group, key, null, value));
            } else if (!Objects.equals(exist.getConfigValue(), value)) {
                // 存在 -> 定向更新（仅展示字段 + 值，避开实体默认值覆盖坑）
                updateByKey(group, key, title, value, sort, item.getRemark());
                changes.add(buildLog(group, key, exist.getConfigValue(), value));
            }
        }

        // 5. 事务提交成功后：删缓存（必做，防旧值）+ 写变更日志
        final List<SysConfigLog> finalChanges = changes;
        afterCommit(() -> {
            sysConfigUtil.evictGroupCache(group);
            sysConfigLogService.writeLogs(finalChanges);
        });

        log.info("[系统配置] 分组 {} 全量保存完成，提交 {} 项，变更 {} 项", group, items.size(), changes.size());
        return Response.ok("保存成功", null);
    }

    // ====================== 私有方法 ======================

    /** 按 group+key 定向更新值与展示字段 */
    private void updateByKey(String group, String key, String title, String value, Integer sort, String remark) {
        LambdaUpdateWrapper<SysConfig> uw = new LambdaUpdateWrapper<>();
        uw.eq(SysConfig::getConfigGroup, group)
                .eq(SysConfig::getConfigKey, key)
                .set(SysConfig::getConfigTitle, title)
                .set(SysConfig::getConfigValue, value)
                .set(SysConfig::getSort, sort)
                .set(SysConfig::getRemark, remark);
        update(uw);
    }

    /**
     * 单条配置后端校验
     *
     * @return 错误消息；null 表示通过
     */
    private String validateItem(SysConfigItemSaveDTO item) {
        String key = item.getConfigKey().trim();
        String valueType = item.getValueType() == null ? "" : item.getValueType().trim();
        String value = item.getConfigValue();

        if (!VALUE_TYPES.contains(valueType)) {
            return key + ": 值类型非法(" + valueType + ")，仅支持 string/number/boolean/json";
        }
        if (value == null) {
            value = "";
        }
        switch (valueType) {
            case "number":
                try {
                    BigDecimal num = new BigDecimal(value.trim());
                    if (num.compareTo(BigDecimal.ZERO) < 0) {
                        return key + ": 数字配置禁止负数";
                    }
                    if (POSITIVE_NUMBER_KEYS.contains(key) && num.compareTo(BigDecimal.ZERO) == 0) {
                        return key + ": 该业务配置必须为正数";
                    }
                } catch (NumberFormatException e) {
                    return key + ": 值类型为 number，必须传入合法数字";
                }
                break;
            case "boolean":
                if (!"true".equalsIgnoreCase(value.trim()) && !"false".equalsIgnoreCase(value.trim())) {
                    return key + ": 值类型为 boolean，只能传入 true/false";
                }
                break;
            case "json":
                if (!StringUtils.hasText(value)) {
                    return key + ": json 类型配置值不能为空";
                }
                try {
                    JSON.parse(value.trim());
                } catch (Exception e) {
                    return key + ": json 格式不合法，须为标准 JSON 数组或对象字符串";
                }
                break;
            default:
                // string 无附加校验
                break;
        }
        return null;
    }

    private SysConfigLog buildLog(String group, String key, String oldValue, String newValue) {
        SysConfigLog logEntity = new SysConfigLog();
        logEntity.setConfigGroup(group);
        logEntity.setConfigKey(key);
        logEntity.setOldValue(oldValue);
        logEntity.setNewValue(newValue);
        return logEntity;
    }

    /** 事务提交成功后执行回调；无事务上下文时立即执行 */
    private void afterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }
}
