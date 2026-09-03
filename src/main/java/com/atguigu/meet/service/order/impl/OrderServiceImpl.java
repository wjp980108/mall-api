package com.atguigu.meet.service.order.impl;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.constant.OrderConstants;
import com.atguigu.meet.enums.OrderOperateType;
import com.atguigu.meet.enums.OrderStatus;
import com.atguigu.meet.mapper.goods.consign.ConsignGoodsMapper;
import com.atguigu.meet.mapper.order.OrderMapper;
import com.atguigu.meet.mapper.permission.user.UserMapper;
import com.atguigu.meet.mapper.seckill.session.SessionMapper;
import com.atguigu.meet.model.dto.order.AllOrderQueryDTO;
import com.atguigu.meet.model.dto.order.OrderOperateDTO;
import com.atguigu.meet.model.dto.order.PlaceOrderDTO;
import com.atguigu.meet.model.dto.order.UploadVoucherDTO;
import com.atguigu.meet.model.entity.goods.consign.ConsignGoods;
import com.atguigu.meet.model.entity.order.Order;
import com.atguigu.meet.model.entity.permission.user.SysUser;
import com.atguigu.meet.model.entity.seckill.session.Session;
import com.atguigu.meet.model.entity.user.UserAddress;
import com.atguigu.meet.model.vo.PageResultVO;
import com.atguigu.meet.model.vo.order.OrderVO;
import com.atguigu.meet.service.goods.consign.ConsignGoodsService;
import com.atguigu.meet.service.goods.consign.ConsignRecordService;
import com.atguigu.meet.service.order.OrderOperateLogService;
import com.atguigu.meet.service.order.OrderService;
import com.atguigu.meet.service.user.UserAddressService;
import com.atguigu.meet.utils.AdminContext;
import com.atguigu.meet.utils.BeanConvertUtils;
import com.atguigu.meet.utils.OrderNoUtil;
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
import java.time.LocalTime;
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
 *       手机号原文输出（不做脱敏）。</li>
 * </ol>
 */
@Service
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    private OrderOperateLogService orderOperateLogService;

    @Autowired
    private ConsignGoodsMapper consignGoodsMapper;

    @Autowired
    private ConsignGoodsService consignGoodsService;

    @Autowired
    private ConsignRecordService consignRecordService;

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserAddressService userAddressService;

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
                null,
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
        // 组装订单状态中文名（VO 层派生字段，数据库不存）
        result.getRecords().forEach(vo -> vo.setOrderStatusName(OrderStatus.descOf(vo.getOrderStatus())));
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
          .set(Order::getPayVoucherUrl, dto.getPayVoucherUrl())
          .set(Order::getPayVoucherPlatform, dto.getPayVoucherPlatform());
        int affected = baseMapper.update(null, uw);
        if (affected == 0) {
            return Response.fail(500, "订单状态已变更，请刷新后重试");
        }

        // 日志 REQUIRES_NEW：无论本事务后续是否回滚，都记录"上传凭证"操作
        orderOperateLogService.writeOperateLog(dto.getId(), beforeStatus, afterStatus,
                OrderOperateType.UPLOAD_VOUCHER, null);

        // 商品联动：已抢购待付款(2) -> 等待确认付款(3)，消除原「管理端手工补状态」缺口
        advanceConsignGoodsToWaitConfirmSafely(existOrder);

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

        // 商品联动：goods_status -> 4待处理(买家持有), member_id -> buyer, 委托/审核状态重置, sale_times SQL 层自增
        promoteConsignGoodsToPendingSafely(existOrder, beforeStatus);

        // 追加：委托代卖卖出成交事件记录（买家确认收款成为新持有人 = 卖出瞬间）
        // 查 consignGoodsId 下 recordStatus=2 的记录，UPDATE recordStatus=3 + 成交价/买家快照；不新增；不动其他快照
        consignRecordService.recordSold(existOrder.getGoodsId(), existOrder.getRushPrice(),
                existOrder.getBuyerId(), existOrder.getBuyerName(), existOrder.getBuyerPhone());

        log.info("[订单管理] 确认收款成功，orderId={}, {}->{}->{}",
                dto.getId(), beforeStatus, step1After, step2After);
        return Response.ok("确认收款成功", null);
    }

    // ====================== 私有方法：商品状态联动（并发安全版） ======================

    /**
     * 上传支付凭证成功：托售商品推进到 3 等待确认付款（条件更新 + 期望状态匹配）
     * <p>
     * 期望商品状态为 2（已抢购待付款），仅匹配时推进为 3；
     * 状态不匹配仅告警不阻断（商品可能已被管理端手工流转或处于并发保护中）。
     */
    private void advanceConsignGoodsToWaitConfirmSafely(Order order) {
        if (order.getGoodsId() == null) {
            return;
        }
        Integer expectGoodsStatus = 2; // 已抢购待付款
        int affected = consignGoodsMapper.updateStatusWithCondition(
                order.getGoodsId(),
                3,                     // 目标：3 等待确认付款
                expectGoodsStatus,
                null,                  // 委托人不变
                null, null, null       // 委托/审核/上下架状态不变
        );
        if (affected == 0) {
            log.warn("[订单管理] 商品并发保护：托售商品状态不匹配预期(2)，未自动推进，orderId={}, goodsId={}",
                    order.getId(), order.getGoodsId());
            return;
        }
        consignGoodsService.recordExternalBizFlow(order.getGoodsId(), expectGoodsStatus, 3,
                "上传支付凭证自动流转，订单号：" + order.getOrderNo());
    }

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
                order.getSellerId(),  // 委托人回滚至订单快照 seller_id（防二次销售商品委托人错乱）
                null, null, null      // 委托/审核/上下架状态不变
        );
        if (affected == 0) {
            log.warn("[订单管理] 商品并发保护：托售商品状态不匹配预期({})，可能已被其他流程处理，orderId={}, goodsId={}",
                    expectGoodsStatus, order.getId(), order.getGoodsId());
            return;
        }
        consignGoodsService.recordExternalBizFlow(order.getGoodsId(), expectGoodsStatus, 1,
                "取消/删除订单回滚商品，订单号：" + order.getOrderNo());
        log.info("[订单管理] 订单商品状态回滚完成，orderId={}, goodsId={}, status({})->1, memberId->{}",
                order.getId(), order.getGoodsId(), expectGoodsStatus, order.getSellerId());
    }

    /**
     * 确认收款成功：托售商品交由买家处理（3等待确认付款 -> 4待处理，条件更新 + SQL 层 sale_times 自增）
     * <p>
     * 期望商品状态为 3（等待确认付款），仅匹配时推进为 4；
     * 委托人变更为本轮买家（买家成为商品持有者，后续可主动申请委托代卖）；
     * 委托/审核状态重置为 0（新持有周期开始）；
     * sale_times 使用 UPDATE SET = +1 原子操作，杜绝 Java 层丢失更新。
     * <p>
     * 后续闭环：买家在 C 端申请委托代卖（4->5委托代卖,待审核）->
     * 平台管理员审核通过（5->1挂卖中+上架）进入下一轮抢购。
     */
    private void promoteConsignGoodsToPendingSafely(Order order, Integer orderBeforeStatus) {
        if (order.getGoodsId() == null) {
            return;
        }
        // 订单确认收款前是 PAID(2)，对应商品状态应为 3（等待确认付款）
        Integer expectGoodsStatus = 3;
        int affected = consignGoodsMapper.updateStatusWithCondition(
                order.getGoodsId(),
                OrderConstants.GOODS_STATUS_PENDING,   // 目标：4 待处理（买家持有）
                expectGoodsStatus,
                order.getBuyerId(),                    // 新委托人 = 本轮订单买家
                0,                                     // 委托状态重置：0未委托
                0,                                     // 审核状态重置：0无需审核
                null
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
        consignGoodsService.recordExternalBizFlow(order.getGoodsId(), expectGoodsStatus,
                OrderConstants.GOODS_STATUS_PENDING,
                "确认收款商品交由买家处理(待处理,可申请委托代卖)(sale_times+1)，订单号：" + order.getOrderNo());
        log.info("[订单管理] 订单商品交由买家处理完成，orderId={}, goodsId={}, status->4, memberId->{}",
                order.getId(), order.getGoodsId(), order.getBuyerId());
    }

    // ====================== C 端用户接口 ======================

    /**
     * C 端用户抢购下单
     * <p>
     * 流程：登录校验 → 商品状态/上架校验 → 场次开启+时间窗口 → 限购 → 收货地址归属
     *      → 商品状态条件更新(1挂卖中->2已抢购待付款，affected=0 即并发冲突)
     *      → 建单(订单号+买卖家快照+成交价+pay_deadline) → 商品/订单审计日志
     * <p>
     * 并发保障：商品状态条件更新与建单同处一个事务，事务回滚则商品状态一并回滚，
     *          避免出现「商品已占用但订单未建」的脏数据。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response placeOrder(PlaceOrderDTO dto, Long currentUserId) {
        if (currentUserId == null) {
            return Response.fail(401, "未登录");
        }
        // 1. 查商品 + 状态校验（必须挂卖中+已上架）
        ConsignGoods goods = consignGoodsMapper.selectById(dto.getGoodsId());
        if (goods == null) {
            return Response.fail(500, "商品不存在");
        }
        if (!Objects.equals(goods.getGoodsStatus(), OrderConstants.GOODS_STATUS_ON_SALE)) {
            return Response.fail(500, "商品已被抢购或不可抢");
        }
        if (!Integer.valueOf(1).equals(goods.getOnlineStatus())) {
            return Response.fail(500, "商品未上架");
        }
        // 2. 查场次 + 时间窗口校验（非抢购时段禁止下单）
        Session session = sessionMapper.selectById(goods.getSessionId());
        if (session == null || !Integer.valueOf(1).equals(session.getSessionStatus())) {
            return Response.fail(500, "抢购场次未开启或不存在");
        }
        LocalTime now = LocalTime.now();
        if (session.getRushStartTime() == null || session.getRushEndTime() == null
                || now.isBefore(session.getRushStartTime()) || now.isAfter(session.getRushEndTime())) {
            return Response.fail(500, "非抢购时段，无法下单");
        }
        // 3. 限购校验：t_order 无 session_id，JOIN t_consign_goods 统计该场次有效抢购次数（排除已取消）
        int maxBuy = session.getMaxBuyCount() == null ? 1 : session.getMaxBuyCount();
        int rushed = baseMapper.countRushedByUserAndSession(currentUserId, session.getId());
        if (rushed >= maxBuy) {
            return Response.fail(500, "已达本场次抢购上限(" + maxBuy + "次)");
        }
        // 4. 收货地址（归属校验，防越权使用他人地址）
        UserAddress addr = userAddressService.getByIdForOrder(dto.getAddressId(), currentUserId);
        if (addr == null) {
            return Response.fail(500, "收货地址不存在");
        }
        // 5. 买卖家信息快照（下单时从 sys_user 取，避免后续用户改名影响历史订单）
        SysUser buyer = userMapper.selectById(currentUserId);
        SysUser seller = userMapper.selectById(goods.getMemberId());
        if (buyer == null) {
            return Response.fail(500, "买家信息不存在");
        }
        // 6. 商品状态条件更新 1挂卖中 -> 2已抢购待付款（affected=0 即被并发抢走）
        //    同时重置委托/审核状态为 0：新一轮抢购周期开始，上轮委托审核结果清零
        int affected = consignGoodsMapper.updateStatusWithCondition(
                goods.getId(),
                OrderConstants.GOODS_STATUS_RUSHED_WAIT_PAY,
                OrderConstants.GOODS_STATUS_ON_SALE,
                null,
                0, 0, null);
        if (affected == 0) {
            return Response.fail(500, "商品已被抢购，请重试");
        }
        // 7. 建单
        Order order = new Order();
        order.setOrderNo(OrderNoUtil.generate(currentUserId));
        order.setGoodsId(goods.getId());
        order.setGoodsName(goods.getGoodsName());
        order.setSellerId(goods.getMemberId());
        order.setSellerName(pickName(seller));
        order.setSellerPhone(seller == null ? null : seller.getPhone());
        order.setBuyerId(currentUserId);
        order.setBuyerName(pickName(buyer));
        order.setBuyerPhone(buyer.getPhone());
        order.setRushPrice(goods.getGoodsPrice());
        order.setReceiveAddress(addr.getAddress());
        order.setOrderStatus(OrderStatus.WAIT_PAY.getCode());
        order.setPayDeadline(LocalDateTime.now().plusMinutes(OrderConstants.PAY_TIMEOUT_MINUTES));
        save(order);

        // 8. 审计日志（@Async REQUIRES_NEW，操作人自动取 AdminContext；商品侧跨模块日志保持链路完整）
        consignGoodsService.recordExternalBizFlow(goods.getId(),
                OrderConstants.GOODS_STATUS_ON_SALE, OrderConstants.GOODS_STATUS_RUSHED_WAIT_PAY,
                "用户抢购下单，订单号：" + order.getOrderNo());
        orderOperateLogService.writeOperateLog(order.getId(), null, OrderStatus.WAIT_PAY.getCode(),
                OrderOperateType.PLACE_ORDER, "用户抢购下单");

        log.info("[订单管理] 抢购下单成功，orderNo={}, orderId={}, goodsId={}, buyerId={}",
                order.getOrderNo(), order.getId(), goods.getId(), currentUserId);
        return Response.ok("下单成功", order.getOrderNo());
    }

    /** 取用户展示名：优先 nickname，回退 username */
    private String pickName(SysUser u) {
        if (u == null) {
            return null;
        }
        return StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername();
    }

    /**
     * C 端用户取消订单
     * <p>
     * 归属校验（order.buyerId == currentUserId）后复用 {@link #cancelOrder} 核心逻辑：
     * 状态条件更新 + 商品回滚 + 审计日志。防越权：用户只能取消自己的订单。
     * <p>
     * 事务由本方法 {@code @Transactional} 保证（this 调用 cancelOrder 跳过其代理注解，
     * 但本方法事务覆盖整个调用链，状态条件更新与商品回滚仍原子一致）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response cancelOrderByUser(OrderOperateDTO dto, Long currentUserId) {
        if (currentUserId == null) {
            return Response.fail(401, "未登录");
        }
        Order order = getById(dto.getId());
        if (order == null || !Objects.equals(order.getBuyerId(), currentUserId)) {
            return Response.fail(500, "订单不存在或无权操作");
        }
        return cancelOrder(dto);
    }

    /**
     * C 端用户上传支付凭证
     * <p>
     * 归属校验后复用 {@link #uploadVoucher} 核心逻辑：状态条件更新 1待付款->2已付款 +
     * 商品推进 2->3等待确认 + 审计日志。防越权：用户只能给自己的订单上传凭证。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response uploadVoucherByUser(UploadVoucherDTO dto, Long currentUserId) {
        if (currentUserId == null) {
            return Response.fail(401, "未登录");
        }
        Order order = getById(dto.getId());
        if (order == null || !Objects.equals(order.getBuyerId(), currentUserId)) {
            return Response.fail(500, "订单不存在或无权操作");
        }
        return uploadVoucher(dto);
    }

    /**
     * C 端「我的订单」：按 buyerId 分页，可选 orderStatus 筛选
     */
    @Override
    public Response listMyOrders(Long buyerId, Integer orderStatus, Integer pageNum, Integer pageSize) {
        if (buyerId == null) {
            return Response.fail(401, "未登录");
        }
        Page<OrderVO> page = new Page<>(pageNum, pageSize);
        IPage<OrderVO> result = baseMapper.selectOrderPage(
                page, buyerId, null, null, null, null, null, null, orderStatus, null, null);
        result.getRecords().forEach(vo -> vo.setOrderStatusName(OrderStatus.descOf(vo.getOrderStatus())));
        return Response.ok(PageResultVO.of(result));
    }

    /**
     * C 端订单详情：校验订单归属当前买家，防越权
     */
    @Override
    public Response getOrderDetailForUser(Long id, Long currentUserId) {
        if (currentUserId == null) {
            return Response.fail(401, "未登录");
        }
        Order order = getById(id);
        if (order == null) {
            return Response.fail(500, "订单不存在");
        }
        if (!Objects.equals(order.getBuyerId(), currentUserId)) {
            return Response.fail(403, "无权查看该订单");
        }
        OrderVO vo = new OrderVO();
        BeanConvertUtils.copyProperties(order, vo);
        vo.setOrderStatusName(OrderStatus.descOf(order.getOrderStatus()));
        return Response.ok(vo);
    }
}
