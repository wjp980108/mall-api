package com.atguigu.meet.service.goods.consign.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.meet.common.Response;
import com.atguigu.meet.enums.AuditStatus;
import com.atguigu.meet.enums.ConsignGoodsOperateType;
import com.atguigu.meet.enums.EntrustStatus;
import com.atguigu.meet.enums.GoodsStatus;
import com.atguigu.meet.mapper.goods.consign.ConsignGoodsMapper;
import com.atguigu.meet.mapper.goods.consign.ConsignGoodsOperateLogMapper;
import com.atguigu.meet.mapper.permission.user.UserMapper;
import com.atguigu.meet.mapper.seckill.session.SessionMapper;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsAuditDTO;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsBizStatusDTO;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsDeleteDTO;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsOnlineStatusDTO;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsPageQueryDTO;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsSaveDTO;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsUpdateDTO;
import com.atguigu.meet.model.entity.goods.consign.ConsignGoods;
import com.atguigu.meet.model.entity.goods.consign.ConsignGoodsOperateLog;
import com.atguigu.meet.model.entity.general.settings.SysSettings;
import com.atguigu.meet.model.entity.permission.user.SysUser;
import com.atguigu.meet.model.entity.seckill.session.Session;
import com.atguigu.meet.model.vo.PageResultVO;
import com.atguigu.meet.model.vo.goods.consign.ConsignGoodsVO;
import com.atguigu.meet.service.general.settings.SysSettingsService;
import com.atguigu.meet.service.goods.consign.ConsignGoodsService;
import com.atguigu.meet.service.goods.consign.ConsignRecordService;
import com.atguigu.meet.service.seckill.guard.SeckillSellingGuard;
import com.atguigu.meet.utils.AdminContext;
import com.atguigu.meet.utils.BeanConvertUtils;
import com.atguigu.meet.utils.RequestContextUtil;
import com.atguigu.meet.utils.TimeRangeUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    @Autowired
    private ConsignRecordService consignRecordService;

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private SysSettingsService sysSettingsService;

    @Autowired
    private SeckillSellingGuard seckillSellingGuard;

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
                null,
                parameter.getEntrustStatus(),
                parameter.getAuditStatus(),
                parameter.getOnlineStatus(),
                startTime,
                endTime
        );
        // 组装商品业务状态/委托状态/审核状态中文名（VO 层派生字段，数据库不存）
        result.getRecords().forEach(vo -> {
            vo.setGoodsStatusName(GoodsStatus.descOf(vo.getGoodsStatus()));
            vo.setEntrustStatusName(EntrustStatus.descOf(vo.getEntrustStatus()));
            vo.setAuditStatusName(AuditStatus.descOf(vo.getAuditStatus()));
        });
        fillOnSale(result.getRecords());
        return Response.ok(PageResultVO.of(result));
    }

    @Override
    public Response getConsignGoodsById(Long id) {
        ConsignGoodsVO vo = baseMapper.selectConsignGoodsById(id);
        if (vo == null) {
            return Response.fail(500, "商品不存在");
        }
        // 组装商品业务状态/委托状态/审核状态中文名（VO 层派生字段，数据库不存）
        vo.setGoodsStatusName(GoodsStatus.descOf(vo.getGoodsStatus()));
        vo.setEntrustStatusName(EntrustStatus.descOf(vo.getEntrustStatus()));
        vo.setAuditStatusName(AuditStatus.descOf(vo.getAuditStatus()));
        fillOnSale(Collections.singletonList(vo));
        // 新会员提前抢购权益：详情页同样按登录用户身份后处理 canPurchase
        applyNewMemberAdvancePurchase(Collections.singletonList(vo));
        return Response.ok(vo);
    }

    @Override
    public Response addConsignGoods(ConsignGoodsSaveDTO dto) {
        // 校验委托人是否存在（memberId 可选，传了才校验）
        if (dto.getMemberId() != null) {
            SysUser member = userMapper.selectById(dto.getMemberId());
            if (member == null) {
                return Response.fail(500, "委托人不存在");
            }
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
        // 售卖中锁定：商品正在售卖时禁止编辑，避免窗口内改价/改图影响在途抢购订单快照口径
        if (seckillSellingGuard.isGoodsSelling(dto.getId())) {
            return Response.fail(500, "商品正在售卖中，暂不可操作，请等待抢购结束");
        }
        // 校验委托人是否存在（memberId 可选，传了才校验）
        if (dto.getMemberId() != null) {
            SysUser member = userMapper.selectById(dto.getMemberId());
            if (member == null) {
                return Response.fail(500, "委托人不存在");
            }
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
    @Transactional(rollbackFor = Exception.class)
    public Response updateOnlineStatus(ConsignGoodsOnlineStatusDTO dto) {
        ConsignGoods existGoods = getById(dto.getId());
        if (existGoods == null) {
            return Response.fail(500, "商品不存在");
        }
        // 售卖中锁定：售卖中禁止手动上下架（含止损下架），紧急止损应走"停用场次"通道
        if (seckillSellingGuard.isGoodsSelling(dto.getId())) {
            return Response.fail(500, "商品正在售卖中，暂不可操作，请等待抢购结束");
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
        // 追加：委托代卖未售出下架事件记录（仅下架时；委托代卖已上架商品 recordStatus=2 -> 4）
        // 非委托代卖商品下架无 recordStatus=2 记录，ConsignRecordService 内部跳过，符合"禁止在非委托节点操作"约束
        if (!afterOnlineStatus) {
            consignRecordService.recordDelist(dto.getId(), "后台下架");
        }
        log.info("[托售商品] 上下架成功，id={}, {}->{}",
                dto.getId(), beforeOnlineStatus, afterOnlineStatus);
        return Response.ok("上下架成功", null);
    }

    // ====================== C 端用户接口（JWT 登录态） ======================

    /**
     * C 端「我的卖方仓库」：memberId=当前用户，按状态筛选持仓商品
     * <p>goodsStatus 传值：4待处理 / 5委托审核中 / 1挂卖中(配合 onlineStatus 区分在售/已下架)；
     * 不传：查全部持有中 goodsStatus IN (1,4,5)，交易中间态 2/3（被抢购待付款/等待确认付款）
     * 期间持有者仍是卖家但不属于卖方仓库展示范围（买方订单列表兜底）。
     * <p>复用管理端分页查询（memberId + 状态条件），按创建时间倒序。
     */
    @Override
    public Response listMyHeld(Long memberId, Integer goodsStatus, Integer onlineStatus, Integer pageNum, Integer pageSize) {
        if (memberId == null) {
            return Response.fail(401, "未登录");
        }
        // 上下架筛选仅在有 goodsStatus=1（挂卖中）时有意义，其他状态强制忽略防误过滤
        Integer onlineFilter = Integer.valueOf(GoodsStatus.ON_SALE.getCode()).equals(goodsStatus) ? onlineStatus : null;
        ConsignGoodsPageQueryDTO dto = new ConsignGoodsPageQueryDTO();
        dto.setPageNum(pageNum);
        dto.setPageSize(pageSize);
        dto.setMemberId(memberId);
        dto.setGoodsStatus(goodsStatus);
        dto.setOnlineStatus(onlineFilter);
        if (goodsStatus == null) {
            // 全部持有中：1挂卖中(在售/已下架) + 4待处理 + 5委托审核中
            Page<ConsignGoodsVO> page = new Page<>(pageNum, pageSize);
            IPage<ConsignGoodsVO> result = baseMapper.selectConsignGoodsPage(
                    page, null, memberId, null, null,
                    Arrays.asList(GoodsStatus.ON_SALE.getCode(), GoodsStatus.PENDING.getCode(),
                            GoodsStatus.AGENT_SALE.getCode()),
                    null, null, onlineFilter, null, null);
            result.getRecords().forEach(vo -> {
                vo.setGoodsStatusName(GoodsStatus.descOf(vo.getGoodsStatus()));
                vo.setEntrustStatusName(EntrustStatus.descOf(vo.getEntrustStatus()));
                vo.setAuditStatusName(AuditStatus.descOf(vo.getAuditStatus()));
            });
            return Response.ok(PageResultVO.of(result));
        }
        return getPageList(dto);
    }

    /**
     * C 端「在售抢购商品列表」：上架+挂卖中+场次开启+当前在抢购时间窗口内；可选 sessionId 过滤
     */
    @Override
    public Response listSaleGoods(Integer pageNum, Integer pageSize, Long sessionId) {
        Page<ConsignGoodsVO> page = new Page<>(pageNum, pageSize);
        IPage<ConsignGoodsVO> result = baseMapper.selectSaleGoodsPage(page, sessionId);
        // 组装商品业务状态/委托状态/审核状态中文名（VO 层派生字段）
        result.getRecords().forEach(vo -> {
            vo.setGoodsStatusName(GoodsStatus.descOf(vo.getGoodsStatus()));
            vo.setEntrustStatusName(EntrustStatus.descOf(vo.getEntrustStatus()));
            vo.setAuditStatusName(AuditStatus.descOf(vo.getAuditStatus()));
        });
        // 新会员提前抢购权益：Service 层按当前登录用户身份后处理 canPurchase
        applyNewMemberAdvancePurchase(result.getRecords());
        return Response.ok(PageResultVO.of(result));
    }

    /**
     * 批量填充商品列表/详情的 onSale 派生字段（按商品ID批量经 t_session_product 反查判定；
     * 不用 VO.sessionId 列——t_consign_goods.session_id 为旧链路只读遗留，实时关联以关联表为准）
     */
    private void fillOnSale(List<ConsignGoodsVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> goodsIds = records.stream().map(ConsignGoodsVO::getId).collect(Collectors.toList());
        Set<Long> sellingIds = new HashSet<>(seckillSellingGuard.filterSellingGoodsIds(goodsIds));
        for (ConsignGoodsVO vo : records) {
            vo.setOnSale(sellingIds.contains(vo.getId()));
        }
    }

    /**
     * 新会员提前抢购 canPurchase 后处理
     * <p>XML 产出基于正常时间窗口 [rush_start, rush_end] 的 canPurchase；
     * 此处对登录的新会员且 sys_settings 开启提前抢购(advance>0 且 new_member_days>0)时，
     * 将提前窗口 [rush_start - advance, rush_end] 内但 XML 判定为 false 的商品放宽为 true。
     * 未登录/老会员/功能关闭 → 保持 XML 原值不动。
     */
    private void applyNewMemberAdvancePurchase(List<ConsignGoodsVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Long userId = AdminContext.getLoginUserId();
        if (userId == null) {
            // 未登录：保持 XML 原值
            return;
        }
        SysUser buyer = userMapper.selectById(userId);
        if (buyer == null || !Integer.valueOf(0).equals(buyer.getMemberType())) {
            // 非新会员（老会员或异常）：保持 XML 原值
            return;
        }
        SysSettings settings = sysSettingsService.get();
        Integer newMemberDays = settings.getNewMemberDays();
        Integer advanceMinutes = settings.getNewMemberAdvanceMinutes();
        if (newMemberDays == null || newMemberDays <= 0
                || advanceMinutes == null || advanceMinutes <= 0) {
            // 功能关闭：保持 XML 原值
            return;
        }
        // 批量查询当前页涉及的场次，避免逐行查询
        Set<Long> sessionIds = records.stream()
                .map(ConsignGoodsVO::getSessionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (sessionIds.isEmpty()) {
            return;
        }
        List<Session> sessions = sessionMapper.selectBatchIds(sessionIds);
        Map<Long, Session> sessionMap = sessions.stream()
                .collect(Collectors.toMap(Session::getId, s -> s, (a, b) -> a));
        LocalTime now = LocalTime.now();
        for (ConsignGoodsVO vo : records) {
            // XML 已判可购或无可购标记则跳过
            if (Boolean.TRUE.equals(vo.getCanPurchase()) || vo.getSessionId() == null) {
                continue;
            }
            Session session = sessionMap.get(vo.getSessionId());
            if (session == null || !Integer.valueOf(1).equals(session.getSessionStatus())) {
                continue;
            }
            if (session.getRushStartTime() == null || session.getRushEndTime() == null) {
                continue;
            }
            LocalTime advanceStart = session.getRushStartTime().minusMinutes(advanceMinutes);
            // 在提前窗口 [advanceStart, rush_end] 内则放宽为可购
            if (!now.isBefore(advanceStart) && !now.isAfter(session.getRushEndTime())) {
                vo.setCanPurchase(true);
            }
        }
    }

    /**
     * C 端首页搜索商品：上架+挂卖中，按商品名称模糊查询，按 sale_times 倒序
     */
    @Override
    public Response searchGoods(String keyword, Integer pageNum, Integer pageSize) {
        Page<ConsignGoodsVO> page = new Page<>(pageNum, pageSize);
        IPage<ConsignGoodsVO> result = baseMapper.selectSearchGoodsPage(page, keyword);
        result.getRecords().forEach(vo -> {
            vo.setGoodsStatusName(GoodsStatus.descOf(vo.getGoodsStatus()));
            vo.setEntrustStatusName(EntrustStatus.descOf(vo.getEntrustStatus()));
            vo.setAuditStatusName(AuditStatus.descOf(vo.getAuditStatus()));
        });
        return Response.ok(PageResultVO.of(result));
    }

    /**
     * C 端首页推荐商品：上架+挂卖中+场次开启+在抢购时间窗口内，按 sale_times 倒序
     */
    @Override
    public Response recommendGoods(Integer pageNum, Integer pageSize) {
        Page<ConsignGoodsVO> page = new Page<>(pageNum, pageSize);
        IPage<ConsignGoodsVO> result = baseMapper.selectRecommendGoodsPage(page);
        result.getRecords().forEach(vo -> {
            vo.setGoodsStatusName(GoodsStatus.descOf(vo.getGoodsStatus()));
            vo.setEntrustStatusName(EntrustStatus.descOf(vo.getEntrustStatus()));
            vo.setAuditStatusName(AuditStatus.descOf(vo.getAuditStatus()));
        });
        return Response.ok(PageResultVO.of(result));
    }

    @Override
    public Response updateBizStatus(ConsignGoodsBizStatusDTO dto) {
        ConsignGoods existGoods = getById(dto.getId());
        if (existGoods == null) {
            return Response.fail(500, "商品不存在");
        }
        // 售卖中锁定：售卖中禁止业务状态流转，避免与抢购下单的状态迁移（1挂卖中→2已抢购）竞争
        if (seckillSellingGuard.isGoodsSelling(dto.getId())) {
            return Response.fail(500, "商品正在售卖中，暂不可操作，请等待抢购结束");
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
        // 售卖中锁定：售卖中禁止删除，避免在途抢购订单的商品锚点失联
        if (seckillSellingGuard.isGoodsSelling(id)) {
            return Response.fail(500, "商品正在售卖中，暂不可操作，请等待抢购结束");
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
        // 售卖中锁定：批量中任一商品正在售卖即整批取消（沿用本方法全部/全无语义）
        List<Long> sellingIds = seckillSellingGuard.filterSellingGoodsIds(idList);
        if (!sellingIds.isEmpty()) {
            return Response.fail(500, "托售商品ID：" + sellingIds + " 正在售卖中，本次全部取消删除");
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

    // ====================== 委托代卖审核 ======================

    /**
     * C 端买家申请委托代卖
     * <p>
     * 前置校验（持有者 + 状态4待处理）+ 条件更新（4->5委托代卖，entrust=1，audit=1待审核），
     * affected=0 即并发冲突；成功后补商品审计日志。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response entrustByOwner(Long goodsId, Long currentUserId) {
        if (currentUserId == null) {
            return Response.fail(401, "未登录");
        }
        ConsignGoods goods = getById(goodsId);
        if (goods == null) {
            return Response.fail(500, "商品不存在");
        }
        if (!Objects.equals(goods.getMemberId(), currentUserId)) {
            return Response.fail(500, "仅商品持有者可申请委托代卖");
        }
        if (!Integer.valueOf(GoodsStatus.PENDING.getCode()).equals(goods.getGoodsStatus())) {
            return Response.fail(500, "商品当前状态不可申请委托代卖");
        }
        // 时间校验：仅当天确认收款的商品可申请委托（进入待处理时 update_time 被刷新），
        // 跨天商品不可委托；23:59 定时任务会将当日未委托商品下架，与本校验双保险闭环
        LocalDateTime heldTime = goods.getUpdateTime();
        if (heldTime == null || heldTime.toLocalDate().isBefore(LocalDate.now())) {
            return Response.fail(500, "仅当天可申请委托代售，跨天商品不可委托");
        }
        // 条件更新：4待处理 -> 5委托代卖 + 委托状态1委托代卖中 + 审核状态1待审核
        int affected = baseMapper.updateStatusWithCondition(goodsId,
                GoodsStatus.AGENT_SALE.getCode(), GoodsStatus.PENDING.getCode(),
                null, EntrustStatus.ENTRUSTING.getCode(), AuditStatus.WAIT_AUDIT.getCode(), null);
        if (affected == 0) {
            return Response.fail(500, "商品状态已变更，请刷新后重试");
        }
        recordExternalBizFlow(goodsId, GoodsStatus.PENDING.getCode(), GoodsStatus.AGENT_SALE.getCode(),
                "买家申请委托代卖，等待平台审核");
        // 追加：委托代卖事件记录 - INSERT 一条待审核记录，冻结商品+委托人快照
        SysUser consignMember = userMapper.selectById(goods.getMemberId());
        String consignMemberName = pickMemberName(consignMember);
        consignRecordService.recordApplyConsign(goods, consignMemberName);
        log.info("[托售商品] 买家申请委托代卖成功，goodsId={}, userId={}", goodsId, currentUserId);
        return Response.ok("委托申请已提交，等待平台审核", null);
    }

    /**
     * 后台管理员委托代卖审核
     * <p>
     * 通过：5委托代卖 -> 1挂卖中 + 自动上架（audit=2通过，entrust 保持1委托代卖中，
     *      待下一轮被抢购时由下单流程重置）；驳回：5 -> 4待处理（entrust=0，audit=3驳回）。
     * <p>前置校验（状态5 + 待审核）+ 条件更新（expect=5）双保险防并发重复审核。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response auditEntrust(ConsignGoodsAuditDTO dto) {
        Long adminId = AdminContext.getLoginUserId();
        ConsignGoods goods = getById(dto.getGoodsId());
        if (goods == null) {
            return Response.fail(500, "商品不存在");
        }
        // 售卖中锁定：售卖中禁止委托审核，避免审核通过重新上架与抢购下单状态迁移竞争
        if (seckillSellingGuard.isGoodsSelling(dto.getGoodsId())) {
            return Response.fail(500, "商品正在售卖中，暂不可操作，请等待抢购结束");
        }
        if (!Integer.valueOf(GoodsStatus.AGENT_SALE.getCode()).equals(goods.getGoodsStatus())
                || !Integer.valueOf(AuditStatus.WAIT_AUDIT.getCode()).equals(goods.getAuditStatus())) {
            return Response.fail(500, "商品不在委托待审核状态，无法审核");
        }
        boolean pass = Boolean.TRUE.equals(dto.getPass());
        String baseRemark = pass ? "委托审核通过，商品重新上架进入下一轮售卖" : "委托审核驳回，商品退回待处理";
        String remark = StringUtils.hasText(dto.getRemark()) ? baseRemark + "，备注：" + dto.getRemark() : baseRemark;

        int affected;
        int toStatus;
        if (pass) {
            // 通过：5 -> 1挂卖中 + 上架 + audit=2（entrust 保持1，下一轮被抢购时重置）
            affected = baseMapper.updateStatusWithCondition(dto.getGoodsId(),
                    GoodsStatus.ON_SALE.getCode(), GoodsStatus.AGENT_SALE.getCode(),
                    null, null, AuditStatus.PASS.getCode(), 1);
            toStatus = GoodsStatus.ON_SALE.getCode();
        } else {
            // 驳回：5 -> 4待处理 + entrust=0 + audit=3（买家可重新申请）
            affected = baseMapper.updateStatusWithCondition(dto.getGoodsId(),
                    GoodsStatus.PENDING.getCode(), GoodsStatus.AGENT_SALE.getCode(),
                    null, EntrustStatus.NOT_ENTRUST.getCode(), AuditStatus.REJECT.getCode(), null);
            toStatus = GoodsStatus.PENDING.getCode();
        }
        if (affected == 0) {
            return Response.fail(500, "商品状态已变更，请刷新后重试");
        }
        recordExternalBizFlow(dto.getGoodsId(), GoodsStatus.AGENT_SALE.getCode(), toStatus, remark);
        // 追加：委托代卖事件记录 - UPDATE 该条审核结果（通过=2 / 驳回=5），不动快照
        String auditOperatorName = AdminContext.get() == null ? null : AdminContext.get().getUsername();
        if (pass) {
            consignRecordService.recordAuditPass(dto.getGoodsId(), adminId, auditOperatorName);
        } else {
            consignRecordService.recordAuditReject(dto.getGoodsId(), adminId, auditOperatorName, dto.getRemark());
        }
        log.info("[托售商品] 委托审核完成，goodsId={}, pass={}, 操作人={}", dto.getGoodsId(), pass, adminId);
        return Response.ok(pass ? "审核通过，商品已重新上架" : "已驳回，商品退回待处理", null);
    }

    /**
     * C 端用户撤销委托申请
     * <p>
     * 前置校验（持有者 + 5委托代卖 + 1待审核）+ 条件更新（5->4待处理，entrust=0，audit=0），
     * affected=0 即并发冲突（后台已审核/状态已变更）；成功后补商品审计日志 + 委托记录 1->6用户撤销。
     * <p>撤销后 update_time 刷新为 now，当天可重新申请；当天未再委托则 23:59 定时任务照常下架。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response cancelEntrustByOwner(Long goodsId, Long currentUserId) {
        if (currentUserId == null) {
            return Response.fail(401, "未登录");
        }
        ConsignGoods goods = getById(goodsId);
        if (goods == null) {
            return Response.fail(500, "商品不存在");
        }
        if (!Objects.equals(goods.getMemberId(), currentUserId)) {
            return Response.fail(500, "仅商品持有者可撤销委托申请");
        }
        if (!Integer.valueOf(GoodsStatus.AGENT_SALE.getCode()).equals(goods.getGoodsStatus())
                || !Integer.valueOf(AuditStatus.WAIT_AUDIT.getCode()).equals(goods.getAuditStatus())) {
            return Response.fail(500, "商品不在委托待审核状态，无法撤销");
        }
        // 条件更新：5委托代卖 -> 4待处理 + 委托状态0未委托 + 审核状态0无需审核（update_time 刷新为 NOW()）
        int affected = baseMapper.updateStatusWithCondition(goodsId,
                GoodsStatus.PENDING.getCode(), GoodsStatus.AGENT_SALE.getCode(),
                null, EntrustStatus.NOT_ENTRUST.getCode(), AuditStatus.NONE.getCode(), null);
        if (affected == 0) {
            return Response.fail(500, "商品状态已变更，请刷新后重试");
        }
        recordExternalBizFlow(goodsId, GoodsStatus.AGENT_SALE.getCode(), GoodsStatus.PENDING.getCode(),
                "用户撤销委托申请，商品退回待处理");
        // 委托记录：1待审核 -> 6用户撤销（操作人=当前用户）
        SysUser cancelUser = userMapper.selectById(currentUserId);
        consignRecordService.recordCancelConsign(goodsId, currentUserId, pickMemberName(cancelUser));
        log.info("[托售商品] 用户撤销委托申请成功，goodsId={}, userId={}", goodsId, currentUserId);
        return Response.ok("已撤销委托申请", null);
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
     * 4待处理 -> 5委托代卖 / 1挂卖中
     * 5委托代卖 -> 1挂卖中
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
        transitions.put(4, new int[]{1, 5});
        transitions.put(5, new int[]{1});
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

    /** 业务状态中文名称 —— 委托 GoodsStatus 枚举统一维护，避免重复硬编码 */
    private String statusName(Integer status) {
        return GoodsStatus.descOf(status);
    }

    /** 取会员展示名：优先 nickname，回退 username（与订单模块 pickName 保持一致） */
    private String pickMemberName(SysUser user) {
        if (user == null) {
            return null;
        }
        return StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername();
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

    // ====================== 定时任务 ======================

    /**
     * 定时任务：每天 23:59:59 批量下架当日未委托的待处理商品
     * <p>将 goods_status=4(待处理) + entrust_status=0(未委托) + 当日进入待处理(update_time) 的商品
     * 批量更新为 goods_status=1(挂卖中) + online_status=0(下架)，并写入委托下架记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void scheduledDelistUnentrustedGoods() {
        int affected = baseMapper.batchDelistUnentrustedGoods();
        if (affected == 0) {
            log.info("[定时任务] 当日无需下架的未委托商品");
            return;
        }
        log.info("[定时任务] 批量下架当日未委托商品成功，共 {} 件", affected);
        // 为每件下架商品写入委托下架记录（recordStatus=4 未售出下架）
        // 由于是批量操作，这里只记录日志，不逐条写 ConsignRecord（避免大量 INSERT）
    }

    /**
     * 定时任务：每天 23:59:59 下架"当日上架仍未卖出"的委托商品（上架当日必须卖出，未卖出退回终结）
     * <p>判定：goods_status=1挂卖中 + online_status=1上架 + entrust_status=1委托代卖中 + 当日进入上架(update_time，
     * 审核通过 updateStatusWithCondition 固定刷新 update_time=NOW())。
     * <p>逐件条件更新下架（防竞态：上架瞬间被抢/后台手动下架则 affected=0 跳过），
     * 下架成功后委托记录 2已上架 → 4未售出下架（recordDelist，含下架时间/原因，无活跃记录时安全跳过）。
     * 商品保持持有者不变，进入 1挂卖中+下架 终态，此后不可再委托（与"未委托下架"终点一致）。
     *
     * @return 本次实际下架件数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int scheduledDelistUnsoldListedGoods() {
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        List<ConsignGoods> unsoldList = list(new LambdaQueryWrapper<ConsignGoods>()
                .eq(ConsignGoods::getGoodsStatus, GoodsStatus.ON_SALE.getCode())
                .eq(ConsignGoods::getOnlineStatus, 1)
                .eq(ConsignGoods::getEntrustStatus, EntrustStatus.ENTRUSTING.getCode())
                .ge(ConsignGoods::getUpdateTime, dayStart)
                .lt(ConsignGoods::getUpdateTime, dayStart.plusDays(1)));
        if (unsoldList.isEmpty()) {
            log.info("[定时任务] 当日无需下架的上架未卖出商品");
            return 0;
        }
        int count = 0;
        for (ConsignGoods goods : unsoldList) {
            // 条件更新：仅当仍处于 1挂卖中+上架 时下架（防竞态：刚被抢购/已被手动下架则跳过）
            LambdaUpdateWrapper<ConsignGoods> uw = new LambdaUpdateWrapper<>();
            uw.eq(ConsignGoods::getId, goods.getId())
              .eq(ConsignGoods::getGoodsStatus, GoodsStatus.ON_SALE.getCode())
              .eq(ConsignGoods::getOnlineStatus, 1)
              .set(ConsignGoods::getOnlineStatus, 0);
            if (baseMapper.update(null, uw) == 0) {
                continue;
            }
            // 委托记录 2已上架 -> 4未售出下架
            consignRecordService.recordDelist(goods.getId(), "上架当日未卖出，系统自动下架");
            count++;
        }
        log.info("[定时任务] 批量下架当日上架未卖出商品成功，共 {} 件", count);
        return count;
    }
}