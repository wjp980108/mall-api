package com.atguigu.meet.service.goods.consign.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.meet.common.Response;
import com.atguigu.meet.enums.ConsignGoodsOperateType;
import com.atguigu.meet.mapper.goods.consign.ConsignGoodsMapper;
import com.atguigu.meet.mapper.goods.consign.ConsignGoodsOperateLogMapper;
import com.atguigu.meet.mapper.permission.user.UserMapper;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsBizStatusDTO;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsDeleteDTO;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsOnlineStatusDTO;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsPageQueryDTO;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsSaveDTO;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsUpdateDTO;
import com.atguigu.meet.model.entity.goods.consign.ConsignGoods;
import com.atguigu.meet.model.entity.goods.consign.ConsignGoodsOperateLog;
import com.atguigu.meet.model.entity.permission.user.SysUser;
import com.atguigu.meet.model.vo.PageResultVO;
import com.atguigu.meet.model.vo.goods.consign.ConsignGoodsVO;
import com.atguigu.meet.service.goods.consign.ConsignGoodsService;
import com.atguigu.meet.utils.AdminContext;
import com.atguigu.meet.utils.BeanConvertUtils;
import com.atguigu.meet.utils.RequestContextUtil;
import com.atguigu.meet.utils.TimeRangeUtils;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 抢购托售商品 Service 实现
 * <p>
 * 核心保障：
 * - 委托人信息实时同步：列表/详情通过 JOIN sys_user 获取最新委托人数据，用户表更新后自动同步
 * - 业务状态流转校验：仅允许合法的前后状态迁移
 * - 上下架独立控制：online_status 与 goods_status 分离
 * - 软删除：@TableLogic 逻辑删除
 * - 入参强校验：DTO @Valid + XSS 防护
 * - 操作日志：新增/编辑/删除/上下架/业务状态流转 均写入 t_consign_goods_operate_log
 */
@Service
@Slf4j
public class ConsignGoodsServiceImpl extends ServiceImpl<ConsignGoodsMapper, ConsignGoods> implements ConsignGoodsService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ConsignGoodsOperateLogMapper consignGoodsOperateLogMapper;

    @Override
    public Response getPageList(ConsignGoodsPageQueryDTO parameter) {
        // 解析时间范围：timeRange[0] -> 当天 00:00:00，timeRange[1] -> 当天 23:59:59
        LocalDateTime startTime = null;
        LocalDateTime endTime = null;
        List<String> timeRange = parameter.getTimeRange();
        if (timeRange != null && !timeRange.isEmpty()) {
            if (timeRange.size() >= 1) {
                startTime = TimeRangeUtils.toStartOfDay(timeRange.get(0));
            }
            if (timeRange.size() >= 2) {
                endTime = TimeRangeUtils.toEndOfDay(timeRange.get(1));
            }
        }

        Page<ConsignGoodsVO> page = new Page<>(parameter.getPageNum(), parameter.getPageSize());
        IPage<ConsignGoodsVO> result = baseMapper.selectConsignGoodsPage(
                page,
                parameter.getGoodsName(),
                parameter.getMemberId(),
                parameter.getSessionId(),
                parameter.getGoodsStatus(),
                parameter.getOnlineStatus(),
                startTime,
                endTime
        );
        return Response.ok(PageResultVO.of(result));
    }

    @Override
    public Response getConsignGoodsById(Long id) {
        ConsignGoodsVO vo = baseMapper.selectConsignGoodsById(id);
        if (vo == null) {
            return Response.fail(500, "商品不存在");
        }
        return Response.ok(vo);
    }

    @Override
    public Response addConsignGoods(ConsignGoodsSaveDTO dto) {
        // 校验委托人是否存在
        SysUser member = userMapper.selectById(dto.getMemberId());
        if (member == null) {
            return Response.fail(500, "委托人不存在");
        }
        // 图片平台条件校验：传了图片URL就必须传存储平台，避免后续 NPE 导致 500
        if (StringUtils.hasText(dto.getCoverImg()) && !StringUtils.hasText(dto.getCoverImgPlatform())) {
            return Response.fail(400, "商品缩略图存储平台不能为空");
        }
        if (StringUtils.hasText(dto.getDetailImg()) && !StringUtils.hasText(dto.getDetailImgPlatform())) {
            return Response.fail(400, "商品详情图存储平台不能为空");
        }
        ConsignGoods goods = new ConsignGoods();
        BeanConvertUtils.copyProperties(dto, goods);
        // 兜底 XSS 防护：转义字符串字段
        goods.setGoodsName(escape(goods.getGoodsName()));
        goods.setCoverImg(escape(goods.getCoverImg()));
        goods.setDetailImg(escape(goods.getDetailImg()));
        // 默认值：业务状态不传则 1 挂卖中，上下架不传则 0 下架
        if (goods.getGoodsStatus() == null) {
            goods.setGoodsStatus(1);
        }
        if (goods.getOnlineStatus() == null) {
            goods.setOnlineStatus(0);
        }
        goods.setSaleTimes(0);
        save(goods);
        // 记录操作日志：before=null, after=dto, changedFields=空(新增), remark=新增托售商品
        saveOperateLog(goods.getId(), ConsignGoodsOperateType.ADD, null, dto,
                Collections.emptyList(), "新增托售商品", null, null);
        log.info("[托售商品] 新增成功，id={}, goodsName={}, memberId={}",
                goods.getId(), goods.getGoodsName(), goods.getMemberId());
        return Response.ok("新增成功", null);
    }

    @Override
    public Response updateConsignGoods(ConsignGoodsUpdateDTO dto) {
        ConsignGoods existGoods = getById(dto.getId());
        if (existGoods == null) {
            return Response.fail(500, "商品不存在");
        }
        // 校验委托人是否存在
        SysUser member = userMapper.selectById(dto.getMemberId());
        if (member == null) {
            return Response.fail(500, "委托人不存在");
        }
        // 图片平台条件校验：传了图片URL就必须传存储平台，避免后续 NPE 导致 500
        if (StringUtils.hasText(dto.getCoverImg()) && !StringUtils.hasText(dto.getCoverImgPlatform())) {
            return Response.fail(400, "商品缩略图存储平台不能为空");
        }
        if (StringUtils.hasText(dto.getDetailImg()) && !StringUtils.hasText(dto.getDetailImgPlatform())) {
            return Response.fail(400, "商品详情图存储平台不能为空");
        }
        ConsignGoods goods = new ConsignGoods();
        BeanConvertUtils.copyProperties(dto, goods);
        // 兜底 XSS 防护：转义字符串字段
        goods.setGoodsName(escape(goods.getGoodsName()));
        goods.setCoverImg(escape(goods.getCoverImg()));
        goods.setDetailImg(escape(goods.getDetailImg()));
        updateById(goods);
        // 记录操作日志：before=修改前快照, after=dto, changedFields=dto 非空字段, remark=编辑托售商品基础信息
        saveOperateLog(dto.getId(), ConsignGoodsOperateType.EDIT, existGoods, dto,
                calcChangedFields(dto), "编辑托售商品基础信息", null, null);
        log.info("[托售商品] 修改成功，id={}", dto.getId());
        return Response.ok("修改成功", null);
    }

    @Override
    public Response updateOnlineStatus(ConsignGoodsOnlineStatusDTO dto) {
        ConsignGoods existGoods = getById(dto.getId());
        if (existGoods == null) {
            return Response.fail(500, "商品不存在");
        }
        ConsignGoods goods = new ConsignGoods();
        goods.setId(dto.getId());
        goods.setOnlineStatus(Boolean.TRUE.equals(dto.getOnlineStatus()) ? 1 : 0);
        updateById(goods);
        // 记录操作日志：4=上下架，before/after 仅记录 onlineStatus 字段；按目标状态选枚举值
        boolean beforeOnlineStatus = existGoods.getOnlineStatus() == 1;
        boolean afterOnlineStatus = Boolean.TRUE.equals(dto.getOnlineStatus());
        ConsignGoodsOperateType type = afterOnlineStatus
                ? ConsignGoodsOperateType.SHELF_ON
                : ConsignGoodsOperateType.SHELF_OFF;
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("onlineStatus", beforeOnlineStatus);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("onlineStatus", afterOnlineStatus);
        saveOperateLog(dto.getId(), type, before, after,
                Collections.singletonList("onlineStatus"), type.getDefaultDesc(), null, null);
        log.info("[托售商品] 上下架成功，id={}, {}->{}",
                dto.getId(), beforeOnlineStatus, afterOnlineStatus);
        return Response.ok("上下架成功", null);
    }

    @Override
    public Response updateBizStatus(ConsignGoodsBizStatusDTO dto) {
        ConsignGoods existGoods = getById(dto.getId());
        if (existGoods == null) {
            return Response.fail(500, "商品不存在");
        }
        Integer fromStatus = existGoods.getGoodsStatus();
        Integer toStatus = dto.getGoodsStatus();
        // 业务状态流转校验
        if (!isValidTransition(fromStatus, toStatus)) {
            return Response.fail(500, String.format("业务状态不允许从[%s]流转到[%s]",
                    statusName(fromStatus), statusName(toStatus)));
        }
        // 条件更新：WHERE id=? AND goods_status=fromStatus，闭合「先查后改」并发空窗
        LambdaUpdateWrapper<ConsignGoods> uw = new LambdaUpdateWrapper<>();
        uw.eq(ConsignGoods::getId, dto.getId())
          .eq(ConsignGoods::getGoodsStatus, fromStatus)
          .set(ConsignGoods::getGoodsStatus, toStatus);
        int affected = baseMapper.update(null, uw);
        if (affected == 0) {
            return Response.fail(500, "业务状态已变更，请刷新后重试");
        }
        // 记录操作日志：5=业务状态流转，动态 desc 由枚举方法 buildBizFlowDesc 拼装
        String bizDesc = ConsignGoodsOperateType.BIZ_FLOW
                .buildBizFlowDesc(statusName(fromStatus), statusName(toStatus));
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("goodsStatus", fromStatus);
        before.put("goodsStatusName", statusName(fromStatus));
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("goodsStatus", toStatus);
        after.put("goodsStatusName", statusName(toStatus));
        saveOperateLog(dto.getId(), ConsignGoodsOperateType.BIZ_FLOW, bizDesc, before, after,
                Collections.singletonList("goodsStatus"), bizDesc, fromStatus, toStatus);
        log.info("[托售商品] 业务状态流转成功，id={}, {}->{}",
                dto.getId(), statusName(fromStatus), statusName(toStatus));
        return Response.ok("业务状态流转成功", null);
    }

    @Override
    public Response deleteConsignGoods(Long id) {
        ConsignGoods existGoods = getById(id);
        if (existGoods == null) {
            return Response.fail(500, "商品不存在");
        }
        removeById(id);
        // 记录操作日志：before=删除前快照, after=null, changedFields=null, remark=删除托售商品
        saveOperateLog(id, ConsignGoodsOperateType.DELETE, existGoods, null, null,
                "删除托售商品(逻辑删除)", null, null);
        log.info("[托售商品] 删除成功（逻辑删除），id={}", id);
        return Response.ok("删除成功", null);
    }

    @Override
    public Response deleteConsignGoodsBatch(ConsignGoodsDeleteDTO dto) {
        List<Long> idList = Arrays.asList(dto.getIds());
        List<ConsignGoods> existList = listByIds(idList);
        Set<Long> existIdSet = existList.stream().map(ConsignGoods::getId).collect(Collectors.toSet());
        List<Long> notExistIds = idList.stream()
                .filter(id -> !existIdSet.contains(id))
                .collect(Collectors.toList());
        if (!notExistIds.isEmpty()) {
            return Response.fail(500, "托售商品ID：" + notExistIds + " 不存在，本次全部取消删除");
        }
        // 逻辑删除
        removeByIds(idList);
        // 记录操作日志（每个商品一条删除日志，带删除前快照）
        Map<Long, ConsignGoods> existMap = existList.stream()
                .collect(Collectors.toMap(ConsignGoods::getId, g -> g));
        for (Long id : idList) {
            saveOperateLog(id, ConsignGoodsOperateType.DELETE, existMap.get(id), null, null,
                    "批量删除托售商品(逻辑删除)", null, null);
        }
        log.info("[托售商品] 批量删除成功（逻辑删除），ids={}", idList);
        return Response.ok("成功删除" + idList.size() + "个托售商品", null);
    }

    @Override
    public void recordExternalBizFlow(Long goodsId, Integer fromStatus, Integer toStatus, String remark) {
        String bizDesc = ConsignGoodsOperateType.BIZ_FLOW
                .buildBizFlowDesc(statusName(fromStatus), statusName(toStatus));
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("goodsStatus", fromStatus);
        before.put("goodsStatusName", statusName(fromStatus));
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("goodsStatus", toStatus);
        after.put("goodsStatusName", statusName(toStatus));
        saveOperateLog(goodsId, ConsignGoodsOperateType.BIZ_FLOW, bizDesc, before, after,
                Collections.singletonList("goodsStatus"), remark, fromStatus, toStatus);
    }

    // ====================== 私有方法 ======================

    /**
     * 记录托售商品操作日志（使用枚举默认 desc）
     * <p>
     * content JSON 结构统一为: {"before":{...},"after":{...},"changedFields":["xxx"],"remark":"..."}
     * 同时补充 operate_desc/ip/user_agent 物理列，便于列表展示与审计溯源。
     *
     * @param consignGoodsId 托售商品ID
     * @param type           操作类型枚举（提供 operate_type 与默认 operate_desc）
     * @param before         变更前快照
     * @param after          变更后快照
     * @param changedFields  本次修改字段名集合
     * @param remark         日志备注
     * @param fromStatus     业务流转前状态（仅type=BIZ_FLOW有效，其他传null）
     * @param toStatus       业务流转后状态（仅type=BIZ_FLOW有效，其他传null）
     */
    private void saveOperateLog(Long consignGoodsId, ConsignGoodsOperateType type,
                                Object before, Object after,
                                List<String> changedFields, String remark,
                                Integer fromStatus, Integer toStatus) {
        saveOperateLog(consignGoodsId, type, null, before, after,
                changedFields, remark, fromStatus, toStatus);
    }

    /**
     * 记录托售商品操作日志（允许覆盖默认 desc，仅 BIZ_FLOW 用动态描述时需要）
     *
     * @param consignGoodsId 托售商品ID
     * @param type           操作类型枚举
     * @param descOverride   覆盖默认 desc；传 null 则使用 type.getDefaultDesc()
     * @param before         变更前快照
     * @param after          变更后快照
     * @param changedFields  本次修改字段名集合
     * @param remark         日志备注
     * @param fromStatus     业务流转前状态
     * @param toStatus       业务流转后状态
     */
    private void saveOperateLog(Long consignGoodsId, ConsignGoodsOperateType type, String descOverride,
                                Object before, Object after,
                                List<String> changedFields, String remark,
                                Integer fromStatus, Integer toStatus) {
        ConsignGoodsOperateLog log = new ConsignGoodsOperateLog();
        log.setConsignGoodsId(consignGoodsId);
        log.setAdminId(AdminContext.getLoginUserId());
        log.setOperateType(type.getCode());
        log.setOperateDesc(descOverride != null ? descOverride : type.getDefaultDesc());
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setIp(RequestContextUtil.getClientIp());
        log.setUserAgent(RequestContextUtil.getUserAgent());
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("before", before);
        content.put("after", after);
        content.put("changedFields", changedFields);
        content.put("remark", remark);
        log.setContent(JSON.toJSONString(content));
        consignGoodsOperateLogMapper.insert(log);
    }

    /**
     * 计算 dto 中所有非 null 的可读字段名（用于编辑场景的 changedFields）
     */
    private List<String> calcChangedFields(Object dto) {
        if (dto == null) {
            return Collections.emptyList();
        }
        BeanWrapper wrapper = new BeanWrapperImpl(dto);
        List<String> result = new ArrayList<>();
        for (PropertyDescriptor pd : wrapper.getPropertyDescriptors()) {
            String name = pd.getName();
            if ("class".equals(name)) {
                continue;
            }
            if (wrapper.isReadableProperty(name) && wrapper.getPropertyValue(name) != null) {
                result.add(name);
            }
        }
        return result;
    }

    /**
     * 业务状态流转合法性校验
     * <p>
     * 合法流转：
     * 1挂卖中 -> 2已抢购待付款 / 4待处理 / 5委托代卖
     * 2已抢购待付款 -> 3等待确认付款 / 4待处理
     * 3等待确认付款 -> 4待处理 / 5委托代卖
     * 4待处理 -> 5委托代卖 / 6委托发货 / 1挂卖中
     * 5委托代卖 -> 6委托发货 / 1挂卖中
     * 6委托发货 -> 1挂卖中（发货完成后重新挂卖）
     * 同状态不允许流转
     */
    private boolean isValidTransition(Integer from, Integer to) {
        if (from == null || to == null || from.equals(to)) {
            return false;
        }
        // 使用邻接表定义合法流转
        Map<Integer, int[]> transitions = new LinkedHashMap<>();
        transitions.put(1, new int[]{2, 4, 5});
        transitions.put(2, new int[]{3, 4});
        transitions.put(3, new int[]{4, 5});
        transitions.put(4, new int[]{1, 5, 6});
        transitions.put(5, new int[]{1, 6});
        transitions.put(6, new int[]{1});
        int[] allowed = transitions.get(from);
        if (allowed == null) {
            return false;
        }
        for (int s : allowed) {
            if (s == to) {
                return true;
            }
        }
        return false;
    }

    /** 业务状态中文名称 */
    private String statusName(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 1: return "挂卖中";
            case 2: return "已抢购待付款";
            case 3: return "等待确认付款";
            case 4: return "待处理";
            case 5: return "委托代卖";
            case 6: return "委托发货";
            default: return "未知";
        }
    }

    /**
     * XSS 兜底防护：转义 HTML 特殊字符
     */
    private String escape(String input) {
        if (!StringUtils.hasText(input)) {
            return input;
        }
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
