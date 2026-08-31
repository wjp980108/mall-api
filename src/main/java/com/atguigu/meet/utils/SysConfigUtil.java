package com.atguigu.meet.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.meet.constant.ConfigKeyConst;
import com.atguigu.meet.model.entity.general.config.SysConfig;
import com.atguigu.meet.model.vo.general.config.SysConfigVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.atguigu.meet.mapper.general.config.SysConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统配置统一读取工具类（Spring Bean，业务模块直接注入使用）
 * <p>
 * 读取链路：Redis 缓存(key = sys_config:group:{group}) 优先 -> miss 回查数据库并回填缓存。
 * 缓存无 TTL：项目启动时 SysConfigCacheLoader 全量预加载；保存配置后由服务层删除对应分组缓存，
 * 下次读取自动回填（规避"改了配置一直读旧值"的坑）。
 * <p>
 * 对外方法（configValue 统一存字符串，按 valueType 转换为业务类型）：
 * <pre>
 *   String        getConfigStr(group, key)
 *   Integer       getConfigInt(group, key)
 *   Boolean       getConfigBool(group, key)
 *   List&lt;String&gt;  getConfigList(group, key)   // json 数组，多选框
 *   Map&lt;String,String&gt; getConfigMap(group, key) // json 对象，键值表格
 * </pre>
 * 配置不存在或解析失败时返回 null / 空集合并打印错误日志（读取路径不抛异常，避免拖垮业务）。
 */
@Component
@Slf4j
public class SysConfigUtil {

    /** 分组缓存 key 前缀，完整 key = sys_config:group:{configGroup} */
    public static final String CACHE_KEY_PREFIX = "sys_config:group:";

    @Autowired
    private SysConfigMapper sysConfigMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    // ==================== 对外类型转换读取 ====================

    /** 获取字符串配置，不存在返回 null */
    public String getConfigStr(String group, String key) {
        SysConfigVO vo = findItem(group, key);
        return vo == null ? null : vo.getConfigValue();
    }

    /** 获取整型配置，不存在或非合法数字返回 null */
    public Integer getConfigInt(String group, String key) {
        String value = getConfigStr(group, key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim()).intValueExact();
        } catch (Exception e) {
            log.error("[系统配置] number 配置解析失败 group={}, key={}, value={}", group, key, value, e);
            return null;
        }
    }

    /** 获取小数配置，不存在或非合法数字返回 null */
    public BigDecimal getConfigDecimal(String group, String key) {
        String value = getConfigStr(group, key);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (Exception e) {
            log.error("[系统配置] number 配置解析失败 group={}, key={}, value={}", group, key, value, e);
            return null;
        }
    }

    /** 获取布尔配置，仅识别 true/false，不存在或非法值返回 null */
    public Boolean getConfigBool(String group, String key) {
        String value = getConfigStr(group, key);
        if (value == null) {
            return null;
        }
        if ("true".equalsIgnoreCase(value.trim())) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value.trim())) {
            return Boolean.FALSE;
        }
        log.error("[系统配置] boolean 配置解析失败 group={}, key={}, value={}", group, key, value);
        return null;
    }

    /** 获取 JSON 数组配置（多选复选框），不存在或解析失败返回空集合 */
    public List<String> getConfigList(String group, String key) {
        String value = getConfigStr(group, key);
        if (value == null || value.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<String> list = JSON.parseObject(value.trim(), new TypeReference<List<String>>() {});
            return list == null ? Collections.emptyList() : list;
        } catch (Exception e) {
            log.error("[系统配置] json 数组配置解析失败 group={}, key={}, value={}", group, key, value, e);
            return Collections.emptyList();
        }
    }

    /** 获取 JSON 对象配置（键值表格），不存在或解析失败返回空 Map */
    public Map<String, String> getConfigMap(String group, String key) {
        String value = getConfigStr(group, key);
        if (value == null || value.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            Map<String, String> map = JSON.parseObject(value.trim(), new TypeReference<Map<String, String>>() {});
            return map == null ? Collections.emptyMap() : map;
        } catch (Exception e) {
            log.error("[系统配置] json 对象配置解析失败 group={}, key={}, value={}", group, key, value, e);
            return Collections.emptyMap();
        }
    }

    // ==================== 具名快捷读取（sys_config 固定配置，key 见 ConfigKeyConst） ====================

    // -------- 会员配置 (member) --------

    /** 会员下单限制数量（正数） */
    public Integer getMemberOrderLimit() {
        return getConfigInt(ConfigKeyConst.GROUP_MEMBER, ConfigKeyConst.MEMBER_ORDER_LIMIT);
    }

    /** 会员提前抢购订单数量限制（正数） */
    public Integer getMemberRobOrderNum() {
        return getConfigInt(ConfigKeyConst.GROUP_MEMBER, ConfigKeyConst.MEMBER_ROB_ORDER_NUM);
    }

    // -------- 基础配置 (base) --------

    /** 网站站点名称 */
    public String getSiteName() {
        return getConfigStr(ConfigKeyConst.GROUP_BASE, ConfigKeyConst.SITE_NAME);
    }

    /** 网站ICP备案号 */
    public String getSiteBeian() {
        return getConfigStr(ConfigKeyConst.GROUP_BASE, ConfigKeyConst.SITE_BEIAN);
    }

    /** 静态资源CDN访问地址 */
    public String getSiteCdnUrl() {
        return getConfigStr(ConfigKeyConst.GROUP_BASE, ConfigKeyConst.SITE_CDN_URL);
    }

    /** 系统当前版本号 */
    public String getSiteVersion() {
        return getConfigStr(ConfigKeyConst.GROUP_BASE, ConfigKeyConst.SITE_VERSION);
    }

    /** 系统时区配置 */
    public String getSiteTimezone() {
        return getConfigStr(ConfigKeyConst.GROUP_BASE, ConfigKeyConst.SITE_TIMEZONE);
    }

    /** 黑名单禁止访问IP列表（多个换行分隔 -> 拆分为集合） */
    public List<String> getSiteForbiddenIps() {
        String value = getConfigStr(ConfigKeyConst.GROUP_BASE, ConfigKeyConst.SITE_FORBIDDEN_IP);
        if (value == null || value.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split("\\r?\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /** 前后台语言配置（json 键值对: backend/frontend -> zh-cn 等） */
    public Map<String, String> getSiteLanguages() {
        return getConfigMap(ConfigKeyConst.GROUP_BASE, ConfigKeyConst.SITE_LANGUAGES);
    }

    /** 登录后默认跳转后台页面 */
    public String getSiteFixedPage() {
        return getConfigStr(ConfigKeyConst.GROUP_BASE, ConfigKeyConst.SITE_FIXED_PAGE);
    }

    // -------- 支付配置 (pay) --------

    /** 启用的支付方式列表（多选: 余额、微信、支付宝） */
    public List<String> getPayTypes() {
        return getConfigList(ConfigKeyConst.GROUP_PAY, ConfigKeyConst.PAY_TYPE);
    }

    /** 未支付订单超时自动关闭时间（秒，正数） */
    public Integer getOrderLimitTimeSeconds() {
        return getConfigInt(ConfigKeyConst.GROUP_PAY, ConfigKeyConst.ORDER_LIMIT_TIME);
    }

    /** 是否开启主动查询支付状态 */
    public Boolean isOpenAdapayQuery() {
        return getConfigBool(ConfigKeyConst.GROUP_PAY, ConfigKeyConst.OPEN_ADAPAY_QUERY);
    }

    /** 勾选的工作日列表（json 数组: 周一~周日） */
    public List<String> getWorkDays() {
        return getConfigList(ConfigKeyConst.GROUP_PAY, ConfigKeyConst.WORK_DAY);
    }

    /** 收益积分百分比比例（允许 0） */
    public BigDecimal getIncomeRate() {
        return getConfigDecimal(ConfigKeyConst.GROUP_PAY, ConfigKeyConst.INCOME_RATE);
    }

    // ==================== 分组缓存读写 ====================

    /**
     * 读取整个分组的配置列表（按 sort 升序，缓存优先，miss 回查数据库并回填）
     *
     * @param group 配置分组标识
     */
    public List<SysConfigVO> getGroupConfigList(String group) {
        if (group == null || group.isBlank()) {
            return Collections.emptyList();
        }
        String cacheKey = CACHE_KEY_PREFIX + group;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return JSON.parseArray(cached, SysConfigVO.class);
            }
        } catch (Exception e) {
            log.error("[系统配置] 读取 Redis 缓存失败，降级查库 cacheKey={}", cacheKey, e);
        }
        // 缓存失效 -> 查库 -> 回填
        List<SysConfigVO> list = loadGroupFromDb(group);
        try {
            redisTemplate.opsForValue().set(cacheKey, JSON.toJSONString(list));
        } catch (Exception e) {
            log.error("[系统配置] 回填 Redis 缓存失败 cacheKey={}", cacheKey, e);
        }
        return list;
    }

    /**
     * 从数据库加载指定分组并写入缓存（启动预加载 / 缓存回填共用）
     */
    public void loadGroupCache(String group) {
        List<SysConfigVO> list = loadGroupFromDb(group);
        redisTemplate.opsForValue().set(CACHE_KEY_PREFIX + group, JSON.toJSONString(list));
    }

    /**
     * 全量加载所有分组的配置进缓存（项目启动时调用）
     *
     * @return 加载的分组数量
     */
    public int loadAllGroupsCache() {
        List<SysConfig> all = sysConfigMapper.selectList(null);
        Map<String, List<SysConfig>> byGroup = all.stream()
                .collect(Collectors.groupingBy(SysConfig::getConfigGroup));
        byGroup.forEach((group, configs) -> {
            List<SysConfigVO> voList = configs.stream()
                    .sorted((a, b) -> Integer.compare(
                            a.getSort() == null ? 0 : a.getSort(),
                            b.getSort() == null ? 0 : b.getSort()))
                    .map(this::toVO)
                    .collect(Collectors.toList());
            redisTemplate.opsForValue().set(CACHE_KEY_PREFIX + group, JSON.toJSONString(voList));
        });
        return byGroup.size();
    }

    /**
     * 删除指定分组的缓存（保存更新成功后必须调用，下次读取自动回填新值）
     */
    public void evictGroupCache(String group) {
        try {
            redisTemplate.delete(CACHE_KEY_PREFIX + group);
        } catch (Exception e) {
            log.error("[系统配置] 删除 Redis 缓存失败 cacheKey={}", CACHE_KEY_PREFIX + group, e);
        }
    }

    // ==================== 私有方法 ====================

    /** 从单个分组缓存 JSON 中定位某个配置项 */
    private SysConfigVO findItem(String group, String key) {
        List<SysConfigVO> list = getGroupConfigList(group);
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return list.stream()
                .filter(vo -> key.equals(vo.getConfigKey()))
                .findFirst()
                .orElse(null);
    }

    /** 查询指定分组配置（sort 升序）并转 VO */
    private List<SysConfigVO> loadGroupFromDb(String group) {
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysConfig::getConfigGroup, group);
        wrapper.orderByAsc(SysConfig::getSort);
        return sysConfigMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    private SysConfigVO toVO(SysConfig entity) {
        SysConfigVO vo = new SysConfigVO();
        vo.setId(entity.getId());
        vo.setConfigGroup(entity.getConfigGroup());
        vo.setConfigGroupName(entity.getConfigGroupName());
        vo.setConfigKey(entity.getConfigKey());
        vo.setConfigTitle(entity.getConfigTitle());
        vo.setConfigValue(entity.getConfigValue());
        vo.setValueType(entity.getValueType());
        vo.setSort(entity.getSort());
        vo.setRemark(entity.getRemark());
        return vo;
    }
}
