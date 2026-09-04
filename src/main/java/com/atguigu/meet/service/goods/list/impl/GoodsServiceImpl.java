package com.atguigu.meet.service.goods.list.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.meet.common.Response;
import com.atguigu.meet.enums.GoodsOperateType;
import com.atguigu.meet.mapper.goods.list.GoodsMapper;
import com.atguigu.meet.mapper.goods.list.GoodsOperateLogMapper;
import com.atguigu.meet.model.dto.goods.list.GoodsDeleteDTO;
import com.atguigu.meet.model.dto.goods.list.GoodsPageQueryDTO;
import com.atguigu.meet.model.dto.goods.list.GoodsSaveDTO;
import com.atguigu.meet.model.dto.goods.list.GoodsStatusDTO;
import com.atguigu.meet.model.dto.goods.list.GoodsUpdateDTO;
import com.atguigu.meet.model.entity.goods.list.Goods;
import com.atguigu.meet.model.entity.goods.list.GoodsOperateLog;
import com.atguigu.meet.model.vo.PageResultVO;
import com.atguigu.meet.model.vo.goods.home.AppHomeGoodsVO;
import com.atguigu.meet.service.goods.list.GoodsService;
import com.atguigu.meet.utils.AdminContext;
import com.atguigu.meet.utils.BeanConvertUtils;
import com.atguigu.meet.utils.GoodsSnUtil;
import com.atguigu.meet.utils.RequestContextUtil;
import com.atguigu.meet.utils.TimeRangeUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

/**
 * 商品管理 Service 实现（商品列表模块）
 * <p>
 * 核心保障：
 * - goods_sn 全局唯一：DB 唯一索引 + Service 层预校验
 * - price ≥ 0、stock ≥ 0：DTO 层 @DecimalMin / @Min 校验
 * - 软删除：@TableLogic 逻辑删除，查询默认过滤已删除
 * - 入参强校验：DTO @Valid + XSS 防护（@Pattern 拒绝 < > + 落库前 HtmlUtils.htmlEscape 兜底）
 * - 操作日志：新增/编辑/删除/上下架 均写入 t_goods_operate_log
 */
@Service
@Slf4j
public class GoodsServiceImpl extends ServiceImpl<GoodsMapper, Goods> implements GoodsService {

    @Autowired
    private GoodsOperateLogMapper goodsOperateLogMapper;

    @Override
    public Response getPageList(GoodsPageQueryDTO parameter) {
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(parameter.getGoodsName())) {
            wrapper.like(Goods::getGoodsName, parameter.getGoodsName());
        }
        if (StringUtils.hasText(parameter.getCategoryName())) {
            wrapper.like(Goods::getCategoryName, parameter.getCategoryName());
        }
        if (StringUtils.hasText(parameter.getGoodsSn())) {
            wrapper.eq(Goods::getGoodsSn, parameter.getGoodsSn());
        }
        if (parameter.getStatus() != null) {
            wrapper.eq(Goods::getStatus, parameter.getStatus());
        }
        // 解析时间范围：timeRange[0] -> 当天 00:00:00，timeRange[1] -> 当天 23:59:59
        List<String> timeRange = parameter.getTimeRange();
        if (timeRange != null && !timeRange.isEmpty()) {
            if (timeRange.size() >= 1) {
                LocalDateTime startTime = TimeRangeUtils.toStartOfDay(timeRange.get(0));
                if (startTime != null) {
                    wrapper.ge(Goods::getCreateTime, startTime);
                }
            }
            if (timeRange.size() >= 2) {
                LocalDateTime endTime = TimeRangeUtils.toEndOfDay(timeRange.get(1));
                if (endTime != null) {
                    wrapper.le(Goods::getCreateTime, endTime);
                }
            }
        }
        wrapper.orderByDesc(Goods::getCreateTime);

        IPage<Goods> page = new Page<>(parameter.getPageNum(), parameter.getPageSize());
        IPage<Goods> result = page(page, wrapper);
        return Response.ok(PageResultVO.of(result));
    }

    @Override
    public Response getGoodsById(Long id) {
        Goods goods = getById(id);
        if (goods == null) {
            return Response.fail(500, "商品不存在");
        }
        return Response.ok(goods);
    }

    @Override
    public Response addGoods(GoodsSaveDTO dto) {
        // 1. 商品货号处理：前端未传则后端自动生成，传了则走唯一性预校验
        String goodsSn = dto.getGoodsSn();
        if (!StringUtils.hasText(goodsSn)) {
            goodsSn = generateUniqueGoodsSn();
            dto.setGoodsSn(goodsSn);
        } else if (existsByGoodsSn(goodsSn, null)) {
            return Response.fail(500, "商品货号已存在");
        }
        // 图片平台条件校验：传了缩略图URL就必须传存储平台，避免后续 NPE 导致 500
        if (StringUtils.hasText(dto.getGoodsThumb()) && !StringUtils.hasText(dto.getGoodsThumbPlatform())) {
            return Response.fail(400, "商品缩略图存储平台不能为空");
        }
        Goods goods = new Goods();
        BeanConvertUtils.copyProperties(dto, goods);
        // 兜底 XSS 防护：转义字符串字段
        goods.setGoodsName(escape(goods.getGoodsName()));
        goods.setCategoryName(escape(goods.getCategoryName()));
        goods.setGoodsSn(escape(goods.getGoodsSn()));
        goods.setGoodsThumb(escape(goods.getGoodsThumb()));
        goods.setCreateBy(AdminContext.getLoginUserId());
        // status 不传则默认 0 下架
        if (goods.getStatus() == null) {
            goods.setStatus(0);
        }
        // sales 不传则默认 0
        if (goods.getSales() == null) {
            goods.setSales(0);
        }
        save(goods);

        // 记录操作日志：before=null, after=dto, changedFields=空(新增), remark=新增商品
        saveOperateLog(goods.getId(), GoodsOperateType.ADD, null, dto, Collections.emptyList(), "新增商品");
        log.info("[商品管理] 新增商品成功，id={}, goodsSn={}, 操作人={}",
                goods.getId(), goods.getGoodsSn(), goods.getCreateBy());
        return Response.ok("新增商品成功", null);
    }

    @Override
    public Response updateGoods(GoodsUpdateDTO dto) {
        Goods existGoods = getById(dto.getId());
        if (existGoods == null) {
            return Response.fail(500, "商品不存在");
        }
        // 货号唯一性预校验（排除自身）
        if (existsByGoodsSn(dto.getGoodsSn(), dto.getId())) {
            return Response.fail(500, "商品货号已存在");
        }
        // 图片平台条件校验：传了缩略图URL就必须传存储平台，避免后续 NPE 导致 500
        if (StringUtils.hasText(dto.getGoodsThumb()) && !StringUtils.hasText(dto.getGoodsThumbPlatform())) {
            return Response.fail(400, "商品缩略图存储平台不能为空");
        }
        Goods goods = new Goods();
        BeanConvertUtils.copyProperties(dto, goods);
        // 兜底 XSS 防护：转义字符串字段
        goods.setGoodsName(escape(goods.getGoodsName()));
        goods.setCategoryName(escape(goods.getCategoryName()));
        goods.setGoodsSn(escape(goods.getGoodsSn()));
        goods.setGoodsThumb(escape(goods.getGoodsThumb()));
        goods.setUpdateBy(AdminContext.getLoginUserId());
        updateById(goods);

        // 记录操作日志：before=修改前快照, after=dto, changedFields=dto 非空字段, remark=编辑商品基础信息
        saveOperateLog(dto.getId(), GoodsOperateType.EDIT, existGoods, dto, calcChangedFields(dto), "编辑商品基础信息");
        log.info("[商品管理] 修改商品成功，id={}, 操作人={}", dto.getId(), goods.getUpdateBy());
        return Response.ok("修改商品成功", null);
    }

    @Override
    public Response updateStatus(GoodsStatusDTO dto) {
        Goods existGoods = getById(dto.getId());
        if (existGoods == null) {
            return Response.fail(500, "商品不存在");
        }
        Goods goods = new Goods();
        goods.setId(dto.getId());
        goods.setStatus(Boolean.TRUE.equals(dto.getStatus()) ? 1 : 0);
        goods.setUpdateBy(AdminContext.getLoginUserId());
        updateById(goods);

        // 记录操作日志：4=上下架，before/after 仅记录 status 字段；按目标状态选枚举值
        boolean beforeStatus = existGoods.getStatus() == 1;
        boolean afterStatus = Boolean.TRUE.equals(dto.getStatus());
        GoodsOperateType type = afterStatus ? GoodsOperateType.SHELF_ON : GoodsOperateType.SHELF_OFF;
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("status", beforeStatus);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("status", afterStatus);
        saveOperateLog(dto.getId(), type, before, after,
                Collections.singletonList("status"), type.getDefaultDesc());
        log.info("[商品管理] 商品上下架成功，id={}, {}->{}，操作人={}",
                dto.getId(), beforeStatus, afterStatus, goods.getUpdateBy());
        return Response.ok("商品上下架成功", null);
    }

    @Override
    public Response deleteGoods(Long id) {
        Goods existGoods = getById(id);
        if (existGoods == null) {
            return Response.fail(500, "商品不存在");
        }
        // 逻辑删除（@TableLogic 注解生效，自动追加 is_deleted 条件并将 is_deleted 置 1）
        removeById(id);

        // 记录操作日志：before=删除前快照, after=null, changedFields=null, remark=删除商品
        saveOperateLog(id, GoodsOperateType.DELETE, existGoods, null, null, "删除商品(逻辑删除)");
        log.info("[商品管理] 删除商品成功（逻辑删除），id={}", id);
        return Response.ok("删除商品成功", null);
    }

    @Override
    public Response deleteGoodsBatch(GoodsDeleteDTO dto) {
        List<Long> idList = Arrays.asList(dto.getIds());
        List<Goods> existList = listByIds(idList);
        Set<Long> existIdSet = existList.stream().map(Goods::getId).collect(Collectors.toSet());
        List<Long> notExistIds = idList.stream()
                .filter(id -> !existIdSet.contains(id))
                .collect(Collectors.toList());
        if (!notExistIds.isEmpty()) {
            return Response.fail(500, "商品ID：" + notExistIds + " 不存在，本次全部取消删除");
        }
        // 逻辑删除
        removeByIds(idList);
        // 记录操作日志（每个商品一条删除日志，带删除前快照）
        Map<Long, Goods> existMap = existList.stream()
                .collect(Collectors.toMap(Goods::getId, g -> g));
        for (Long id : idList) {
            saveOperateLog(id, GoodsOperateType.DELETE, existMap.get(id), null, null, "批量删除商品(逻辑删除)");
        }
        log.info("[商品管理] 批量删除成功（逻辑删除），ids={}", idList);
        return Response.ok("成功删除" + idList.size() + "个商品", null);
    }

    // ====================== C 端首页接口（基于 t_goods） ======================

    @Override
    public Response recommendGoods(Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Goods> wrapper = homeBaseWrapper()
                .orderByDesc(Goods::getSales)
                .orderByDesc(Goods::getId);
        IPage<Goods> result = page(new Page<>(pageNum, pageSize), wrapper);
        return Response.ok(PageResultVO.of(toHomeVOPage(result)));
    }

    @Override
    public Response searchGoods(String keyword, Integer pageNum, Integer pageSize) {
        LambdaQueryWrapper<Goods> wrapper = homeBaseWrapper();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Goods::getGoodsName, keyword.trim());
        }
        wrapper.orderByDesc(Goods::getSales)
                .orderByDesc(Goods::getId);
        IPage<Goods> result = page(new Page<>(pageNum, pageSize), wrapper);
        return Response.ok(PageResultVO.of(toHomeVOPage(result)));
    }

    // ====================== 私有方法 ======================

    /**
     * C 端首页查询基础条件：仅取展示列 + 已上架(status=1)；is_deleted 由 @TableLogic 自动过滤
     */
    private LambdaQueryWrapper<Goods> homeBaseWrapper() {
        return new LambdaQueryWrapper<Goods>()
                .select(Goods::getId, Goods::getGoodsName, Goods::getCategoryName,
                        Goods::getGoodsThumb, Goods::getPrice, Goods::getStock, Goods::getSales)
                .eq(Goods::getStatus, 1);
    }

    /**
     * Goods 分页结果转 C 端首页 VO 分页（仅暴露 C 端展示字段，不透出后台管理字段）
     */
    private Page<AppHomeGoodsVO> toHomeVOPage(IPage<Goods> result) {
        Page<AppHomeGoodsVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(goods -> {
            AppHomeGoodsVO vo = new AppHomeGoodsVO();
            BeanConvertUtils.copyProperties(goods, vo);
            return vo;
        }).collect(Collectors.toList()));
        return voPage;
    }

    /**
     * 货号唯一性校验（排除指定商品ID，新增时传 null）
     */
    private boolean existsByGoodsSn(String goodsSn, Long excludeId) {
        LambdaQueryWrapper<Goods> wrapper = new LambdaQueryWrapper<Goods>()
                .eq(Goods::getGoodsSn, goodsSn);
        if (excludeId != null) {
            wrapper.ne(Goods::getId, excludeId);
        }
        return count(wrapper) > 0;
    }

    /**
     * 生成唯一的商品货号（基于雪花算法，无需查库）
     * <p>
     * 雪花算法在正确配置 workerId/dataCenterId 的前提下 100% 不重复；
     * 极端场景（workerId 误配、时钟严重回拨）由数据库 uk_goods_sn 唯一索引做最终兜底。
     * </p>
     *
     * @return 唯一的 goodsSn
     */
    private String generateUniqueGoodsSn() {
        return GoodsSnUtil.generate();
    }

    /**
     * 记录商品操作日志（使用枚举默认 desc）
     * <p>
     * content JSON 结构统一为: {"before":{...},"after":{...},"changedFields":["xxx"],"remark":"..."}
     * 同时补充 operate_desc/ip/user_agent 物理列，便于列表展示与审计溯源。
     *
     * @param goodsId       商品ID
     * @param type          操作类型枚举（提供 operate_type 与默认 operate_desc）
     * @param before        变更前快照
     * @param after         变更后快照
     * @param changedFields 本次修改字段名集合
     * @param remark        日志备注
     */
    private void saveOperateLog(Long goodsId, GoodsOperateType type,
                                Object before, Object after,
                                List<String> changedFields, String remark) {
        saveOperateLog(goodsId, type, null, before, after, changedFields, remark);
    }

    /**
     * 记录商品操作日志（允许覆盖默认 desc）
     *
     * @param goodsId       商品ID
     * @param type          操作类型枚举
     * @param descOverride  覆盖默认 desc；传 null 则使用 type.getDefaultDesc()
     * @param before        变更前快照
     * @param after         变更后快照
     * @param changedFields 本次修改字段名集合
     * @param remark        日志备注
     */
    private void saveOperateLog(Long goodsId, GoodsOperateType type, String descOverride,
                                Object before, Object after,
                                List<String> changedFields, String remark) {
        GoodsOperateLog log = new GoodsOperateLog();
        log.setGoodsId(goodsId);
        log.setAdminId(AdminContext.getLoginUserId());
        log.setOperateType(type.getCode());
        log.setOperateDesc(descOverride != null ? descOverride : type.getDefaultDesc());
        log.setIp(RequestContextUtil.getClientIp());
        log.setUserAgent(RequestContextUtil.getUserAgent());
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("before", before);
        content.put("after", after);
        content.put("changedFields", changedFields);
        content.put("remark", remark);
        log.setContent(JSON.toJSONString(content));
        goodsOperateLogMapper.insert(log);
    }

    /**
     * 计算 dto 中所有非 null 的可读字段名（用于编辑场景的 changedFields）
     * <p>
     * 反向利用 Spring BeanWrapper 的 PropertyDescriptors，过滤掉 class 与 null 值，
     * 得到本次用户显式传入的字段名集合。
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
     * XSS 兜底防护：转义 HTML 特殊字符。
     * DTO 层 @Pattern 已拒绝 < >，此处再转义双引号/单引号/&，防止绕过。
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
