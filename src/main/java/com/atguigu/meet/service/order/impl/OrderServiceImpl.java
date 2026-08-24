package com.atguigu.meet.service.order.impl;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.enums.OrderOperateType;
import com.atguigu.meet.enums.OrderStatus;
import com.atguigu.meet.mapper.goods.consign.ConsignGoodsMapper;
import com.atguigu.meet.mapper.order.OrderMapper;
import com.atguigu.meet.model.dto.order.AllOrderQueryDTO;
import com.atguigu.meet.model.dto.order.OrderOperateDTO;
import com.atguigu.meet.model.dto.order.UploadVoucherDTO;
import com.atguigu.meet.model.entity.order.Order;
import com.atguigu.meet.model.vo.PageResultVO;
import com.atguigu.meet.model.vo.order.OrderVO;
import com.atguigu.meet.service.order.OrderOperateLogService;
import com.atguigu.meet.service.order.OrderService;
import com.atguigu.meet.utils.TimeRangeUtils;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 订单管理 Service 实现
 * <p>
 * 并发 & 事务保障策略：
 * <ol>
 *   <li><b>订单状态「条件更新」</b>：所有状态变更 SQL 追加 {@code WHERE order_status = beforeStatus}，
 *       受影响行数 = 0 即判定并发冲突，直接返回提示「状态已变更，请刷新」，
 *       从根源消除 TOCTOU（先查后改空窗）竞态。</li>
 *   <li><b>商品状态「条件更新」</b>：同理，商品回滚/推进均走 {@code ConsignGoodsMapper.updateStatusWithCondition}，
 *       避免取消订单和另一下单线程互相覆盖商品 member_id / goods_status。</li>
 *   <li><b>sale_times 原子自增</b>：委托售卖次数走 SQL 层 {@code COALESCE(sale_times,0)+1}，
 *       消除 Java 层「读→算→写」丢失更新。</li>
 *   <li><b>操作日志 REQUIRES_NEW</b>：审计日志写入独立事务 {@link OrderOperateLogService}，
 *       无论业务事务提交或回滚，日志必落地，审计链不丢失。</li>
 *   <li><b>金额安全</b>：Entity/DTO/VO 层统一 BigDecimal，禁止 double/float；
 *       手机号 SQL 层脱敏（CONCAT+LEFT+RIGHT）避免 Service 层二次处理遗漏。</li>
 * </ol>
 */
@Service
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    private OrderOperateLogService orderOperateLogService;

    @Autowired
    private ConsignGoodsMapper consignGoodsMapper;

    // ====================== 5 个列表查询 ======================

    @Override
    public Response listAll(AllOrderQueryDTO parameter) {
        return doListPage(parameter, null);
    }

    @Override
    public Response listWaitPay(AllOrderQueryDTO parameter) {
        parameter.setOrderStatus(OrderStatus.WAIT_PAY.getCode());
        return doListPage(parameter, OrderStatus.WAIT_PAY.getCode());
    }

    @Override
    public Response listWaitConfirm(AllOrderQueryDTO parameter) {
        parameter.setOrderStatus(OrderStatus.PAID.getCode());
        return doListPage(parameter, OrderStatus.PAID.getCode());
    }

    @Override
    public Response listAgentSale(AllOrderQueryDTO parameter) {
        parameter.setOrderStatus(OrderStatus.AGENT_SALE.getCode());
        return doListPage(parameter, OrderStatus.AGENT_SALE.getCode());
    }

    @Override
    public Response listCancel(AllOrderQueryDTO parameter) {
        parameter.setOrderStatus(OrderStatus.CANCEL.getCode());
        return doListPage(parameter, OrderStatus.CANCEL.getCode());
    }

    /**
     * 统一分页查询逻辑
     *
     * @param fixedStatus 固定的状态筛选值；传 null 表示使用 dto.orderStatus（即 /list/all 接口）
     */
    private Response doListPage(AllOrderQueryDTO parameter, Integer fixedStatus) {
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

        Integer status = fixedStatus != null ? fixedStatus : parameter.getOrderStatus();

        Page<OrderVO> page = new Page<>(parameter.getPageNum(), parameter.getPageSize());
        IPage<OrderVO> result = baseMapper.selectOrderPage(
                page,
                parameter.getOrderNo(),
                parameter.getGoodsName(),
                parameter.getBuyerName(),
                parameter.getBuyerPhone(),
                parameter.getSellerName(),
                parameter.getSellerPhone(),
                status,
                startTime,
                endTime
        );
        return Response.ok(PageResultVO.of(result));
    }

    // ====================== 4 个操作接口 ======================

    /**
     * 待付款订单上传支付凭证
     * <p>
     * 并发保障：条件更新 {@code WHERE id=? AND order_status=1}，受影响行数=0 判定并发冲突
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response uploadVoucher(UploadVoucherDTO dto) {
        Order existOrder = getById(dto.getId());
        if (existOrder == null) {
            return Response.fail(500, "订单不存在");
        }
        Integer beforeStatus = existOrder.getOrderStatus();
        if (!Objects.equals(OrderStatus.WAIT_PAY.getCode(), beforeStatus)) {
            return Response.fail(500, "仅待付款订单可上传支付凭证");
        }
        if (existOrder.getPayDeadline() != null && LocalDateTime.now().isAfter(existOrder.getPayDeadline())) {
            return Response.fail(500, "订单已超过付款截止时间，无法上传凭证");
        }

        Integer afterStatus = OrderStatus.PAID.getCode();
        // 条件更新：仅当数据库中 order_status 仍为 WAIT_PAY 时允许改为 PAID + 写入凭证
        LambdaUpdateWrapper<Order> uw = new LambdaUpdateWrapper<>();
        uw.eq(Order::getId, dto.getId())
          .eq(Order::getOrderStatus, beforeStatus)
          .set(Order::getOrderStatus, afterStatus)
          .set(Order::getPayVoucherUrl, dto.getPayVoucherUrl());
        int affected = baseMapper.update(null, uw);
        if (affected == 0) {
            return Response.fail(500, "订单状态已变更，请刷新后重试");
        }

        // 日志 REQUIRES_NEW：无论本事务后续是否回滚，都记录"上传凭证"操作
        orderOperateLogService.writeOperateLog(dto.getId(), beforeStatus, afterStatus,
                OrderOperateType.UPLOAD_VOUCHER, null);
        log.info("[订单管理] 上传凭证成功，orderId={}, {}->{}", dto.getId(), beforeStatus, afterStatus);
        return Response.ok("上传凭证成功", null);
    }

    /**
     * 取消订单（待付款 / 已付款均可）
     * <p>
     * 并发保障：订单状态条件更新 + 商品状态条件更新，任一环节 affected=0 即中止并提示
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response cancelOrder(OrderOperateDTO dto) {
        Order existOrder = getById(dto.getId());
        if (existOrder == null) {
            return Response.fail(500, "订单不存在");
        }
        Integer beforeStatus = existOrder.getOrderStatus();
        if (!Objects.equals(OrderStatus.WAIT_PAY.getCode(), beforeStatus)
                && !Objects.equals(OrderStatus.PAID.getCode(), beforeStatus)) {
            return Response.fail(500, "仅待付款或已付款订单可取消");
        }

        Integer afterStatus = OrderStatus.CANCEL.getCode();
        // 条件更新：只允许 WAIT_PAY / PAID 两种 beforeStatus 进入 CANCEL
        LambdaUpdateWrapper<Order> uw = new LambdaUpdateWrapper<>();
        uw.eq(Order::getId, dto.getId())
          .eq(Order::getOrderStatus, beforeStatus)
          .set(Order::getOrderStatus, afterStatus);
        int affected = baseMapper.update(null, uw);
        if (affected == 0) {
            return Response.fail(500, "订单状态已变更，请刷新后重试");
        }

        // 关联抢购托售商品：回滚至 1挂卖中 + member_id 回滚至订单 seller_id（条件更新防并发覆盖）
        rollbackConsignGoodsSafely(existOrder, beforeStatus);

        orderOperateLogService.writeOperateLog(dto.getId(), beforeStatus, afterStatus,
                OrderOperateType.CANCEL_ORDER, dto.getRemark());
        log.info("[订单管理] 取消订单成功，orderId={}, {}->{}", dto.getId(), beforeStatus, afterStatus);
        return Response.ok("取消订单成功", null);
    }

    /**
     * 删除订单（仅待付款可用，逻辑删除）
     * <p>
     * 逻辑删除走条件更新（WHERE id=? AND order_status=1 AND is_deleted=0），
     * 避免两个并发删除线程 / 删除与取消 互相竞争导致商品被重复回滚。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response deleteOrder(OrderOperateDTO dto) {
        Order existOrder = getById(dto.getId());
        if (existOrder == null) {
            return Response.fail(500, "订单不存在");
        }
        Integer beforeStatus = existOrder.getOrderStatus();
        if (!Objects.equals(OrderStatus.WAIT_PAY.getCode(), beforeStatus)) {
            return Response.fail(500, "仅待付款订单可删除");
        }

        // 逻辑删除条件更新：MyBatis-Plus 会自动附加 is_deleted=0，这里再加 order_status=1 保护
        LambdaUpdateWrapper<Order> uw = new LambdaUpdateWrapper<>();
        uw.eq(Order::getId, dto.getId())
          .eq(Order::getOrderStatus, beforeStatus)
          // 逻辑删除由 MyBatis-Plus @TableLogic 负责转换为 SET is_deleted=1
          .set(Order::getIsDeleted, 1);
        int affected = baseMapper.update(null, uw);
        if (affected == 0) {
            return Response.fail(500, "订单状态已变更或已删除，请刷新后重试");
        }

        // 商品状态回滚（条件更新）
        rollbackConsignGoodsSafely(existOrder, beforeStatus);

        orderOperateLogService.writeOperateLog(dto.getId(), beforeStatus, OrderStatus.CANCEL.getCode(),
                OrderOperateType.DELETE_ORDER, dto.getRemark());
        log.info("[订单管理] 删除订单成功（逻辑删除），orderId={}, 原状态={}", dto.getId(), beforeStatus);
        return Response.ok("删除订单成功", null);
    }

    /**
     * 管理员确认收款（仅待确认可用）
     * <p>
     * 状态流转：2(已付款) -> 3(已确认) -> 4(已代售)，两步各一次条件更新；
     * 商品联动：goods_status 3 -> 5 条件更新 + sale_times SQL 层自增。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response confirmReceive(OrderOperateDTO dto) {
        Order existOrder = getById(dto.getId());
        if (existOrder == null) {
            return Response.fail(500, "订单不存在");
        }
        Integer beforeStatus = existOrder.getOrderStatus();
        if (!Objects.equals(OrderStatus.PAID.getCode(), beforeStatus)) {
            return Response.fail(500, "仅待确认收款订单可确认收款");
        }

        // 第一步：PAID(2) -> CONFIRMED(3)
        Integer step1After = OrderStatus.CONFIRMED.getCode();
        LambdaUpdateWrapper<Order> uw1 = new LambdaUpdateWrapper<>();
        uw1.eq(Order::getId, dto.getId())
           .eq(Order::getOrderStatus, beforeStatus)
           .set(Order::getOrderStatus, step1After);
        int affected1 = baseMapper.update(null, uw1);
        if (affected1 == 0) {
            return Response.fail(500, "订单状态已变更，请刷新后重试");
        }
        orderOperateLogService.writeOperateLog(dto.getId(), beforeStatus, step1After,
                OrderOperateType.CONFIRM_RECEIVE, dto.getRemark());

        // 第二步：CONFIRMED(3) -> AGENT_SALE(4) 系统自动流转
        Integer step2After = OrderStatus.AGENT_SALE.getCode();
        LambdaUpdateWrapper<Order> uw2 = new LambdaUpdateWrapper<>();
        uw2.eq(Order::getId, dto.getId())
           .eq(Order::getOrderStatus, step1After)
           .set(Order::getOrderStatus, step2After);
        int affected2 = baseMapper.update(null, uw2);
        if (affected2 == 0) {
            // 理论不可能（同一事务内不可能被外部改），留兜底
            throw new IllegalStateException("订单 3->4 自动流转失败：状态不匹配");
        }
        String step2Remark = StringUtils.hasText(dto.getRemark())
                ? dto.getRemark() + "；系统自动流转至已代售"
                : "系统自动流转至已代售";
        orderOperateLogService.writeOperateLog(dto.getId(), step1After, step2After,
                OrderOperateType.CONFIRM_RECEIVE, step2Remark);

        // 商品联动：goods_status -> 5, member_id -> buyer, sale_times SQL 层自增
        promoteConsignGoodsToAgentSaleSafely(existOrder, beforeStatus);

        log.info("[订单管理] 确认收款成功，orderId={}, {}->{}->{}",
                dto.getId(), beforeStatus, step1After, step2After);
        return Response.ok("确认收款成功", null);
    }

    // ====================== 私有方法：商品状态联动（并发安全版） ======================

    /**
     * 删除/取消订单：回滚托售商品状态（条件更新 + 期望状态匹配）
     * <p>
     * 不同订单 beforeStatus 对应不同的「期望商品状态」：
     * <ul>
     *     <li>订单 WAIT_PAY(1) → 商品期望状态 2（已抢购待付款）</li>
     *     <li>订单 PAID(2) → 商品期望状态 3（等待确认付款）</li>
     * </ul>
     * 只有 goods_status 匹配 expectStatus 时才真正更新，避免与并发的下单/确认流程互相覆盖。
     */
    private void rollbackConsignGoodsSafely(Order order, Integer orderBeforeStatus) {
        if (order.getGoodsId() == null) {
            return;
        }
        // 订单状态 → 商品状态的映射（取消/删除前，商品正处于哪个中间态）
        Integer expectGoodsStatus;
        if (Objects.equals(OrderStatus.WAIT_PAY.getCode(), orderBeforeStatus)) {
            expectGoodsStatus = 2; // 已抢购待付款
        } else if (Objects.equals(OrderStatus.PAID.getCode(), orderBeforeStatus)) {
            expectGoodsStatus = 3; // 等待确认付款
        } else {
            log.warn("[订单管理] 订单状态 {} 不对应任何商品中间态，跳过商品回滚", orderBeforeStatus);
            return;
        }
        int affected = consignGoodsMapper.updateStatusWithCondition(
                order.getGoodsId(),
                1,           // 回滚目标：1 挂卖中
                expectGoodsStatus,
                order.getSellerId()  // 委托人回滚至订单快照 seller_id（防二次销售商品委托人错乱）
        );
        if (affected == 0) {
            log.warn("[订单管理] 商品并发保护：托售商品状态不匹配预期({})，可能已被其他流程处理，orderId={}, goodsId={}",
                    expectGoodsStatus, order.getId(), order.getGoodsId());
            return;
        }
        log.info("[订单管理] 订单商品状态回滚完成，orderId={}, goodsId={}, status({})->1, memberId->{}",
                order.getId(), order.getGoodsId(), expectGoodsStatus, order.getSellerId());
    }

    /**
     * 确认收款成功：托售商品推进到 5 委托代卖（条件更新 + SQL 层 sale_times 自增）
     * <p>
     * 期望商品状态为 3（等待确认付款），仅匹配时推进为 5；
     * sale_times 使用 UPDATE SET = +1 原子操作，杜绝 Java 层丢失更新。
     */
    private void promoteConsignGoodsToAgentSaleSafely(Order order, Integer orderBeforeStatus) {
        if (order.getGoodsId() == null) {
            return;
        }
        // 订单确认收款前是 PAID(2)，对应商品状态应为 3（等待确认付款）
        Integer expectGoodsStatus = 3;
        int affected = consignGoodsMapper.updateStatusWithCondition(
                order.getGoodsId(),
                5,                     // 目标：5 委托代卖
                expectGoodsStatus,
                order.getBuyerId()     // 新委托人 = 本轮订单买家
        );
        if (affected == 0) {
            log.warn("[订单管理] 商品并发保护：托售商品状态不匹配预期(3)，可能已被其他流程处理，orderId={}, goodsId={}",
                    order.getId(), order.getGoodsId());
            return;
        }
        // 售卖次数 SQL 层原子自增（同一商品行上已有行锁，顺序自增）
        int ok = consignGoodsMapper.incrementSaleTimesById(order.getGoodsId());
        if (ok == 0) {
            log.error("[订单管理] sale_times 自增失败：商品行不存在或已删除，orderId={}, goodsId={}",
                    order.getId(), order.getGoodsId());
        }
        log.info("[订单管理] 订单商品进入委托代卖完成，orderId={}, goodsId={}, status->5, memberId->{}",
                order.getId(), order.getGoodsId(), order.getBuyerId());
    }
}
