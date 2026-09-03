package com.atguigu.meet.service.goods.consign.impl;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.enums.RecordStatus;
import com.atguigu.meet.model.dto.goods.consign.ConsignRecordPageQueryDTO;
import com.atguigu.meet.model.entity.goods.consign.ConsignGoods;
import com.atguigu.meet.model.entity.goods.consign.ConsignRecord;
import com.atguigu.meet.model.vo.PageResultVO;
import com.atguigu.meet.model.vo.goods.consign.ConsignRecordVO;
import com.atguigu.meet.mapper.goods.consign.ConsignRecordMapper;
import com.atguigu.meet.service.goods.consign.ConsignRecordService;
import com.atguigu.meet.utils.BeanConvertUtils;
import com.atguigu.meet.utils.TimeRangeUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 委托代卖事件全生命周期快照 Service 实现
 * <p>
 * 核心保障：
 * - 一次委托申请只 INSERT 一条；审核/卖出/下架均 UPDATE 该条，禁止新增第二条
 * - 快照字段（发起委托/成交/下架）写入后永不修改；UPDATE 使用 LambdaUpdateWrapper 只动对应阶段字段
 * - 查询历史记录按 (consignGoodsId, recordStatus) 定位，id desc 取最新一条
 * - UPDATE WHERE 附加 recordStatus 期望值双保险，防并发重复流转
 * - 记录缺失仅告警不抛异常，保证主表业务流程不受影响；真实 DB 异常随事务回滚（原子一致）
 */
@Service
@Slf4j
public class ConsignRecordServiceImpl extends ServiceImpl<ConsignRecordMapper, ConsignRecord> implements ConsignRecordService {

    // 委托记录状态常量：1待审核 2审核通过已上架 3已卖出 4未售出下架 5审核驳回
    private static final int STATUS_PENDING_AUDIT = 1;
    private static final int STATUS_ON_SHELF = 2;
    private static final int STATUS_SOLD = 3;
    private static final int STATUS_DELIST = 4;
    private static final int STATUS_REJECTED = 5;

    /**
     * 节点1 发起委托：INSERT 一条待审核记录，冻结商品+委托人快照
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordApplyConsign(ConsignGoods goods, String memberName) {
        if (goods == null || goods.getId() == null) {
            log.warn("[委托记录] 发起委托快照失败：商品或商品ID为空");
            return;
        }
        ConsignRecord record = new ConsignRecord();
        record.setConsignGoodsId(goods.getId());
        // 发起委托快照（冻结，永不更新）
        record.setMemberId(goods.getMemberId());
        record.setMemberName(memberName);
        record.setGoodsName(goods.getGoodsName());
        record.setGoodsPrice(goods.getGoodsPrice());
        record.setCoverImg(goods.getCoverImg());
        record.setSessionId(goods.getSessionId());
        // 生命周期状态
        record.setRecordStatus(STATUS_PENDING_AUDIT);
        record.setApplyTime(LocalDateTime.now());
        save(record);
        log.info("[委托记录] 发起委托快照已存档，recordId={}, consignGoodsId={}, memberId={}",
                record.getId(), goods.getId(), goods.getMemberId());
    }

    /**
     * 节点2 审核通过：UPDATE 该条 recordStatus=2 + 审核快照，不动其他快照
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordAuditPass(Long consignGoodsId, Long auditOperatorId, String auditOperatorName) {
        ConsignRecord record = findLatestByGoodsAndStatus(consignGoodsId, STATUS_PENDING_AUDIT);
        if (record == null) {
            log.warn("[委托记录] 审核通过更新跳过：未找到 consignGoodsId={} 下 recordStatus={} 的记录",
                    consignGoodsId, STATUS_PENDING_AUDIT);
            return;
        }
        LambdaUpdateWrapper<ConsignRecord> uw = new LambdaUpdateWrapper<>();
        uw.eq(ConsignRecord::getId, record.getId())
          .eq(ConsignRecord::getRecordStatus, STATUS_PENDING_AUDIT)  // 双保险：仍是待审核
          .set(ConsignRecord::getRecordStatus, STATUS_ON_SHELF)
          .set(ConsignRecord::getAuditTime, LocalDateTime.now())
          .set(ConsignRecord::getAuditOperatorId, auditOperatorId)
          .set(ConsignRecord::getAuditOperatorName, auditOperatorName);
        int affected = baseMapper.update(null, uw);
        if (affected == 0) {
            log.warn("[委托记录] 审核通过更新未命中(并发已变更)，recordId={}, consignGoodsId={}",
                    record.getId(), consignGoodsId);
            return;
        }
        log.info("[委托记录] 审核通过已记录，recordId={}, consignGoodsId={}, operator={}",
                record.getId(), consignGoodsId, auditOperatorName);
    }

    /**
     * 节点3 审核驳回：UPDATE 该条 recordStatus=5 + 驳回原因，不动其他快照
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordAuditReject(Long consignGoodsId, Long auditOperatorId, String auditOperatorName, String rejectReason) {
        ConsignRecord record = findLatestByGoodsAndStatus(consignGoodsId, STATUS_PENDING_AUDIT);
        if (record == null) {
            log.warn("[委托记录] 审核驳回更新跳过：未找到 consignGoodsId={} 下 recordStatus={} 的记录",
                    consignGoodsId, STATUS_PENDING_AUDIT);
            return;
        }
        LambdaUpdateWrapper<ConsignRecord> uw = new LambdaUpdateWrapper<>();
        uw.eq(ConsignRecord::getId, record.getId())
          .eq(ConsignRecord::getRecordStatus, STATUS_PENDING_AUDIT)  // 双保险：仍是待审核
          .set(ConsignRecord::getRecordStatus, STATUS_REJECTED)
          .set(ConsignRecord::getAuditTime, LocalDateTime.now())
          .set(ConsignRecord::getAuditOperatorId, auditOperatorId)
          .set(ConsignRecord::getAuditOperatorName, auditOperatorName)
          .set(ConsignRecord::getRejectReason, rejectReason);
        int affected = baseMapper.update(null, uw);
        if (affected == 0) {
            log.warn("[委托记录] 审核驳回更新未命中(并发已变更)，recordId={}, consignGoodsId={}",
                    record.getId(), consignGoodsId);
            return;
        }
        log.info("[委托记录] 审核驳回已记录，recordId={}, consignGoodsId={}, operator={}, rejectReason={}",
                record.getId(), consignGoodsId, auditOperatorName, rejectReason);
    }

    /**
     * 节点4 卖出成交：UPDATE 该条 recordStatus=3 + 成交快照（成交价/买家），不动其他快照
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordSold(Long consignGoodsId, BigDecimal soldPrice, Long buyerId, String buyerName, String buyerPhone) {
        ConsignRecord record = findLatestByGoodsAndStatus(consignGoodsId, STATUS_ON_SHELF);
        if (record == null) {
            log.warn("[委托记录] 卖出成交更新跳过：未找到 consignGoodsId={} 下 recordStatus={} 的记录",
                    consignGoodsId, STATUS_ON_SHELF);
            return;
        }
        LambdaUpdateWrapper<ConsignRecord> uw = new LambdaUpdateWrapper<>();
        uw.eq(ConsignRecord::getId, record.getId())
          .eq(ConsignRecord::getRecordStatus, STATUS_ON_SHELF)  // 双保险：仍是已上架
          .set(ConsignRecord::getRecordStatus, STATUS_SOLD)
          .set(ConsignRecord::getSoldTime, LocalDateTime.now())
          .set(ConsignRecord::getSoldPrice, soldPrice)
          .set(ConsignRecord::getBuyerId, buyerId)
          .set(ConsignRecord::getBuyerName, buyerName)
          .set(ConsignRecord::getBuyerPhone, buyerPhone);
        int affected = baseMapper.update(null, uw);
        if (affected == 0) {
            log.warn("[委托记录] 卖出成交更新未命中(并发已变更)，recordId={}, consignGoodsId={}",
                    record.getId(), consignGoodsId);
            return;
        }
        log.info("[委托记录] 卖出成交已记录，recordId={}, consignGoodsId={}, soldPrice={}, buyerId={}",
                record.getId(), consignGoodsId, soldPrice, buyerId);
    }

    /**
     * 节点5 未售出下架：UPDATE 该条 recordStatus=4 + 下架快照（下架时间/原因），不动其他快照
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordDelist(Long consignGoodsId, String delistReason) {
        ConsignRecord record = findLatestByGoodsAndStatus(consignGoodsId, STATUS_ON_SHELF);
        if (record == null) {
            // 非委托代卖商品下架（无 recordStatus=2 记录）不操作，符合"禁止在非委托节点操作"约束
            log.info("[委托记录] 下架记录跳过：consignGoodsId={} 无 recordStatus={} 的委托记录（非委托代卖下架）",
                    consignGoodsId, STATUS_ON_SHELF);
            return;
        }
        LambdaUpdateWrapper<ConsignRecord> uw = new LambdaUpdateWrapper<>();
        uw.eq(ConsignRecord::getId, record.getId())
          .eq(ConsignRecord::getRecordStatus, STATUS_ON_SHELF)  // 双保险：仍是已上架
          .set(ConsignRecord::getRecordStatus, STATUS_DELIST)
          .set(ConsignRecord::getDelistTime, LocalDateTime.now())
          .set(ConsignRecord::getDelistReason, delistReason);
        int affected = baseMapper.update(null, uw);
        if (affected == 0) {
            log.warn("[委托记录] 下架记录更新未命中(并发已变更)，recordId={}, consignGoodsId={}",
                    record.getId(), consignGoodsId);
            return;
        }
        log.info("[委托记录] 未售出下架已记录，recordId={}, consignGoodsId={}, delistReason={}",
                record.getId(), consignGoodsId, delistReason);
    }

    // ====================== 查询履历接口 ======================

    /**
     * 分页查询委托代卖事件记录
     * <p>支持按 商品ID/商品名模糊/委托人ID/买家ID/记录状态/申请时间范围 筛选，
     * 结果按申请时间倒序，每条记录组装 recordStatusName 中文名。
     */
    @Override
    public Response getPageList(ConsignRecordPageQueryDTO parameter) {
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
        Page<ConsignRecord> page = new Page<>(parameter.getPageNum(), parameter.getPageSize());
        LambdaQueryWrapper<ConsignRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(parameter.getConsignGoodsId() != null, ConsignRecord::getConsignGoodsId, parameter.getConsignGoodsId())
          .like(StringUtils.hasText(parameter.getGoodsName()), ConsignRecord::getGoodsName, parameter.getGoodsName())
          .eq(parameter.getMemberId() != null, ConsignRecord::getMemberId, parameter.getMemberId())
          .eq(parameter.getBuyerId() != null, ConsignRecord::getBuyerId, parameter.getBuyerId())
          .eq(parameter.getRecordStatus() != null, ConsignRecord::getRecordStatus, parameter.getRecordStatus())
          .ge(startTime != null, ConsignRecord::getApplyTime, startTime)
          .le(endTime != null, ConsignRecord::getApplyTime, endTime)
          .orderByDesc(ConsignRecord::getApplyTime);
        IPage<ConsignRecord> result = baseMapper.selectPage(page, qw);
        IPage<ConsignRecordVO> voPage = result.convert(this::toVO);
        return Response.ok(PageResultVO.of(voPage));
    }

    /**
     * 按商品ID查询委托履历列表
     * <p>返回该商品全部委托代卖事件记录，按申请时间倒序；
     * 同一商品多轮委托生成多条独立记录，历史完整保留。
     */
    @Override
    public Response listByConsignGoodsId(Long consignGoodsId) {
        if (consignGoodsId == null) {
            return Response.fail(400, "商品ID不能为空");
        }
        List<ConsignRecord> list = list(new LambdaQueryWrapper<ConsignRecord>()
                .eq(ConsignRecord::getConsignGoodsId, consignGoodsId)
                .orderByDesc(ConsignRecord::getApplyTime));
        List<ConsignRecordVO> voList = list.stream().map(this::toVO).collect(Collectors.toList());
        return Response.ok(voList);
    }

    /**
     * 根据记录ID查详情，组装 recordStatusName 中文名
     */
    @Override
    public Response getRecordById(Long id) {
        ConsignRecord record = getById(id);
        if (record == null) {
            return Response.fail(500, "委托记录不存在");
        }
        return Response.ok(toVO(record));
    }

    // ====================== C 端用户履历接口 ======================

    /**
     * C 端「我的委托记录」：作为委托人(memberId)查询，按 applyTime 倒序分页
     */
    @Override
    public Response listMyConsign(Long memberId, Integer pageNum, Integer pageSize) {
        if (memberId == null) {
            return Response.fail(401, "未登录");
        }
        Page<ConsignRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ConsignRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(ConsignRecord::getMemberId, memberId)
          .orderByDesc(ConsignRecord::getApplyTime);
        IPage<ConsignRecord> result = baseMapper.selectPage(page, qw);
        IPage<ConsignRecordVO> voPage = result.convert(this::toVO);
        return Response.ok(PageResultVO.of(voPage));
    }

    /**
     * C 端「我的买入记录」：作为买家(buyerId)查询已成交(recordStatus=3已卖出)，按 soldTime 倒序分页
     */
    @Override
    public Response listMyBought(Long buyerId, Integer pageNum, Integer pageSize) {
        if (buyerId == null) {
            return Response.fail(401, "未登录");
        }
        Page<ConsignRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ConsignRecord> qw = new LambdaQueryWrapper<>();
        qw.eq(ConsignRecord::getBuyerId, buyerId)
          .eq(ConsignRecord::getRecordStatus, STATUS_SOLD)
          .orderByDesc(ConsignRecord::getSoldTime);
        IPage<ConsignRecord> result = baseMapper.selectPage(page, qw);
        IPage<ConsignRecordVO> voPage = result.convert(this::toVO);
        return Response.ok(PageResultVO.of(voPage));
    }

    // ====================== 私有方法 ======================

    /**
     * 实体转 VO：复制快照字段 + 组装 recordStatusName 中文名
     */
    private ConsignRecordVO toVO(ConsignRecord record) {
        if (record == null) {
            return null;
        }
        ConsignRecordVO vo = new ConsignRecordVO();
        BeanConvertUtils.copyProperties(record, vo);
        vo.setRecordStatusName(RecordStatus.descOf(record.getRecordStatus()));
        return vo;
    }

    /**
     * 按 (consignGoodsId, recordStatus) 查最新一条记录
     * <p>同一商品多轮委托会产生多条记录，但同一 recordStatus 在任意时刻至多一条
     * （待审核/已上架为活动态，卖出/下架/驳回为终态），id desc 取最新兜底。
     */
    private ConsignRecord findLatestByGoodsAndStatus(Long consignGoodsId, Integer recordStatus) {
        if (consignGoodsId == null) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<ConsignRecord>()
                .eq(ConsignRecord::getConsignGoodsId, consignGoodsId)
                .eq(ConsignRecord::getRecordStatus, recordStatus)
                .orderByDesc(ConsignRecord::getId)
                .last("LIMIT 1"));
    }
}
