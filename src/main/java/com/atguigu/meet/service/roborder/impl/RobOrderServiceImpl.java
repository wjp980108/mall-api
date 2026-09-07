package com.atguigu.meet.service.roborder.impl;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.enums.PointsAccountType;
import com.atguigu.meet.enums.PointsBizType;
import com.atguigu.meet.enums.RobOrderOperateType;
import com.atguigu.meet.enums.RobOrderStatus;
import com.atguigu.meet.mapper.permission.user.UserMapper;
import com.atguigu.meet.mapper.roborder.RobOrderMapper;
import com.atguigu.meet.mapper.roborder.RobOrderOperateLogMapper;
import com.atguigu.meet.mapper.seckill.session.SessionMapper;
import com.atguigu.meet.mapper.seckill.sessionproduct.SessionProductMapper;
import com.atguigu.meet.model.dto.roborder.PlaceRobOrderDTO;
import com.atguigu.meet.model.dto.roborder.RobOrderPageQueryDTO;
import com.atguigu.meet.model.dto.roborder.RobOrderTransferDTO;
import com.atguigu.meet.model.entity.general.settings.SysSettings;
import com.atguigu.meet.model.entity.permission.user.AdminUser;
import com.atguigu.meet.model.entity.permission.user.SysUser;
import com.atguigu.meet.model.entity.roborder.RobOrder;
import com.atguigu.meet.model.entity.roborder.RobOrderOperateLog;
import com.atguigu.meet.model.entity.seckill.session.Session;
import com.atguigu.meet.model.entity.seckill.sessionproduct.SessionProduct;
import com.atguigu.meet.model.vo.PageResultVO;
import com.atguigu.meet.model.vo.roborder.RobOrderVO;
import com.atguigu.meet.model.vo.seckill.sessionproduct.SessionProductVO;
import com.atguigu.meet.service.general.settings.SysSettingsService;
import com.atguigu.meet.service.points.UserPointsService;
import com.atguigu.meet.service.roborder.RobOrderService;
import com.atguigu.meet.utils.AdminContext;
import com.atguigu.meet.utils.BeanConvertUtils;
import com.atguigu.meet.utils.OrderNoUtil;
import com.atguigu.meet.utils.TimeRangeUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 抢购订单 Service 实现
 * <p>
 * 库存扣减/回滚、订单状态条件更新、积分发放/冲回均在同一 {@code @Transactional} 内完成；
 * 金额全部在下单瞬间按 sys_settings 比例快照冻结，改比例不影响历史订单。
 */
@Service
@Slf4j
public class RobOrderServiceImpl implements RobOrderService {

    @Autowired
    private RobOrderMapper robOrderMapper;
    @Autowired
    private RobOrderOperateLogMapper operateLogMapper;
    @Autowired
    private SessionProductMapper sessionProductMapper;
    @Autowired
    private SessionMapper sessionMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private SysSettingsService sysSettingsService;
    @Autowired
    private UserPointsService userPointsService;

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    // ====================== C 端下单 ======================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response placeOrder(PlaceRobOrderDTO dto, Long currentUserId) {
        if (currentUserId == null) {
            return Response.fail(401, "请先登录");
        }
        SysUser buyer = userMapper.selectById(currentUserId);
        if (buyer == null) {
            return Response.fail(401, "用户不存在");
        }
        Integer quantity = dto.getQuantity() == null || dto.getQuantity() < 1 ? 1 : dto.getQuantity();

        // 1. 场次商品关联（库存锚点）
        SessionProduct sp = sessionProductMapper.selectById(dto.getSessionProductId());
        if (sp == null) {
            return Response.fail(500, "场次商品不存在或已下架");
        }
        SessionProductVO spVO = sessionProductMapper.selectSessionProductById(sp.getId());
        Session session = sessionMapper.selectById(sp.getSessionId());
        if (session == null || session.getSessionStatus() == null || session.getSessionStatus() != 1) {
            return Response.fail(500, "抢购场次未开启");
        }

        // 2. 时间窗口校验（含新会员提前抢购）
        SysSettings settings = sysSettingsService.get();
        Response timeCheck = checkRushWindow(session, buyer, settings);
        if (timeCheck != null) {
            return timeCheck;
        }

        // 3. 限购校验
        Response limitCheck = checkLimitRule(settings, buyer.getId(), session.getId());
        if (limitCheck != null) {
            return limitCheck;
        }

        // 4. 条件扣库存（原子防超卖）
        int affected = sessionProductMapper.deductStock(sp.getId(), quantity);
        if (affected == 0) {
            return Response.fail(500, "手慢了，库存不足");
        }

        // 5. 金额快照（利润池模型）
        BigDecimal unitPrice = spVO != null && spVO.getPrice() != null ? spVO.getPrice() : BigDecimal.ZERO;
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal profitRate = nz(settings.getOrderProfitRate());
        BigDecimal recommendRate = nz(settings.getRecommendRate());
        BigDecimal selfBuyRate = nz(settings.getSelfBuyRate());
        BigDecimal bonusRatio = nz(settings.getSelfBuyBonusRatio());
        BigDecimal couponRatio = nz(settings.getCouponRatio());

        BigDecimal profitAmount = percent(totalAmount, profitRate);
        BigDecimal recommendAmount = percent(totalAmount, recommendRate);
        BigDecimal selfBuyAmount = percent(totalAmount, selfBuyRate);
        BigDecimal selfBuyBonusAmount = percent(selfBuyAmount, bonusRatio);
        BigDecimal selfBuyCouponAmount = percent(selfBuyAmount, couponRatio);

        // 6. 买家 + 推荐人快照（无邀请人则推荐奖留利润池）
        SysUser inviter = buyer.getInviterId() != null
                ? userMapper.selectById(buyer.getInviterId()) : null;

        // 7. 建单
        RobOrder order = new RobOrder();
        order.setOrderNo(OrderNoUtil.generate(buyer.getId()));
        order.setSessionProductId(sp.getId());
        order.setSessionId(session.getId());
        order.setSessionName(session.getSessionName());
        order.setRushStartTime(session.getRushStartTime());
        order.setRushEndTime(session.getRushEndTime());
        order.setGoodsId(sp.getGoodsId());
        order.setGoodsName(spVO != null ? spVO.getGoodsName() : null);
        order.setGoodsSn(spVO != null ? spVO.getGoodsSn() : null);
        order.setGoodsThumb(spVO != null ? spVO.getGoodsThumb() : null);
        order.setGoodsThumbPlatform(spVO != null ? spVO.getGoodsThumbPlatform() : null);
        order.setUnitPrice(unitPrice);
        order.setQuantity(quantity);
        order.setTotalAmount(totalAmount);
        order.setProfitAmount(profitAmount);
        order.setRecommendAmount(recommendAmount);
        order.setSelfBuyAmount(selfBuyAmount);
        order.setSelfBuyBonusAmount(selfBuyBonusAmount);
        order.setSelfBuyCouponAmount(selfBuyCouponAmount);
        order.setBuyerId(buyer.getId());
        order.setBuyerName(pickName(buyer));
        order.setBuyerPhone(buyer.getPhone());
        order.setBuyerAvatar(buyer.getAvatar());
        order.setBuyerAvatarPlatform(buyer.getAvatarPlatform());
        if (inviter != null) {
            order.setInviterId(inviter.getId());
            order.setInviterName(pickName(inviter));
        }
        order.setOrderStatus(RobOrderStatus.NORMAL.getCode());
        robOrderMapper.insert(order);

        // 8. 积分发放（同事务，行锁）
        if (inviter != null && recommendAmount.signum() > 0) {
            userPointsService.credit(inviter.getId(), PointsAccountType.POINTS.getCode(),
                    recommendAmount, PointsBizType.RECOMMEND.getCode(),
                    order.getId(), order.getOrderNo(), "抢购订单推荐奖 " + order.getOrderNo());
        }
        if (selfBuyBonusAmount.signum() > 0) {
            userPointsService.credit(buyer.getId(), PointsAccountType.POINTS.getCode(),
                    selfBuyBonusAmount, PointsBizType.SELF_BUY.getCode(),
                    order.getId(), order.getOrderNo(), "抢购订单自购奖金 " + order.getOrderNo());
        }
        if (selfBuyCouponAmount.signum() > 0) {
            userPointsService.credit(buyer.getId(), PointsAccountType.COUPON.getCode(),
                    selfBuyCouponAmount, PointsBizType.COUPON.getCode(),
                    order.getId(), order.getOrderNo(), "抢购订单购物券 " + order.getOrderNo());
        }

        // 9. 审计日志
        writeLog(order.getId(), null, RobOrderStatus.NORMAL.getCode(),
                RobOrderOperateType.PLACE_ORDER, buyer.getId(), pickName(buyer), "用户抢购下单");

        log.info("[抢购下单] orderNo={} buyer={} total={} 库存扣减 spId={} qty={}",
                order.getOrderNo(), buyer.getId(), totalAmount, sp.getId(), quantity);
        return Response.ok("抢购成功", order.getOrderNo());
    }

    // ====================== 管理端取消订单 ======================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response cancelOrder(Long orderId) {
        RobOrder order = robOrderMapper.selectById(orderId);
        if (order == null) {
            return Response.fail(500, "订单不存在");
        }
        if (RobOrderStatus.CANCEL.getCode() == order.getOrderStatus()) {
            return Response.fail(500, "订单已取消，请勿重复操作");
        }

        // 条件置已取消（幂等）
        int affected = robOrderMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<RobOrder>()
                        .eq(RobOrder::getId, orderId)
                        .eq(RobOrder::getOrderStatus, RobOrderStatus.NORMAL.getCode())
                        .set(RobOrder::getOrderStatus, RobOrderStatus.CANCEL.getCode()));
        if (affected == 0) {
            return Response.fail(500, "订单状态已变更，取消失败");
        }

        // 回滚库存
        sessionProductMapper.addStock(order.getSessionProductId(), order.getQuantity());

        // 冲回积分（按订单当前快照；允许负余额）
        if (order.getInviterId() != null && nz(order.getRecommendAmount()).signum() > 0) {
            userPointsService.reverse(order.getInviterId(), PointsAccountType.POINTS.getCode(),
                    order.getRecommendAmount(), PointsBizType.RECOMMEND.getCode(),
                    order.getId(), order.getOrderNo(), "取消订单冲回推荐奖 " + order.getOrderNo());
        }
        if (nz(order.getSelfBuyBonusAmount()).signum() > 0) {
            userPointsService.reverse(order.getBuyerId(), PointsAccountType.POINTS.getCode(),
                    order.getSelfBuyBonusAmount(), PointsBizType.SELF_BUY.getCode(),
                    order.getId(), order.getOrderNo(), "取消订单冲回自购奖金 " + order.getOrderNo());
        }
        if (nz(order.getSelfBuyCouponAmount()).signum() > 0) {
            userPointsService.reverse(order.getBuyerId(), PointsAccountType.COUPON.getCode(),
                    order.getSelfBuyCouponAmount(), PointsBizType.COUPON.getCode(),
                    order.getId(), order.getOrderNo(), "取消订单冲回购物券 " + order.getOrderNo());
        }

        AdminUser admin = AdminContext.get();
        writeLog(order.getId(), RobOrderStatus.NORMAL.getCode(), RobOrderStatus.CANCEL.getCode(),
                RobOrderOperateType.CANCEL_ORDER,
                admin != null ? admin.getUserId() : null, admin != null ? admin.getUsername() : null,
                "取消订单，库存与积分已回滚");

        log.info("[抢购订单取消] orderNo={} operator={}", order.getOrderNo(),
                admin != null ? admin.getUsername() : "system");
        return Response.ok("订单已取消");
    }

    // ====================== 管理端转移订单 ======================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response transferOrder(RobOrderTransferDTO dto) {
        RobOrder order = robOrderMapper.selectById(dto.getOrderId());
        if (order == null) {
            return Response.fail(500, "订单不存在");
        }
        if (RobOrderStatus.CANCEL.getCode() == order.getOrderStatus()) {
            return Response.fail(500, "已取消订单不可转移");
        }
        SysUser newBuyer = userMapper.selectById(dto.getNewBuyerId());
        if (newBuyer == null || !"1".equals(newBuyer.getStatus())) {
            return Response.fail(500, "新买家不存在或已禁用");
        }
        if (newBuyer.getId().equals(order.getBuyerId())) {
            return Response.fail(500, "新买家与原买家相同，无需转移");
        }
        SysUser newInviter = newBuyer.getInviterId() != null ? userMapper.selectById(newBuyer.getInviterId()) : null;

        Long oldBuyerId = order.getBuyerId();
        Long oldInviterId = order.getInviterId();

        // 条件更新订单（仍为正常态才允许）：重写买家 + 推荐人快照，金额冻结不变
        int affected = robOrderMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<RobOrder>()
                        .eq(RobOrder::getId, order.getId())
                        .eq(RobOrder::getOrderStatus, RobOrderStatus.NORMAL.getCode())
                        .set(RobOrder::getBuyerId, newBuyer.getId())
                        .set(RobOrder::getBuyerName, pickName(newBuyer))
                        .set(RobOrder::getBuyerPhone, newBuyer.getPhone())
                        .set(RobOrder::getBuyerAvatar, newBuyer.getAvatar())
                        .set(RobOrder::getBuyerAvatarPlatform, newBuyer.getAvatarPlatform())
                        .set(RobOrder::getInviterId, newInviter != null ? newInviter.getId() : null)
                        .set(RobOrder::getInviterName, newInviter != null ? pickName(newInviter) : null));
        if (affected == 0) {
            return Response.fail(500, "订单状态已变更，转移失败");
        }

        // 积分划转：原受益人冲回
        if (oldInviterId != null && nz(order.getRecommendAmount()).signum() > 0) {
            userPointsService.reverse(oldInviterId, PointsAccountType.POINTS.getCode(),
                    order.getRecommendAmount(), PointsBizType.RECOMMEND.getCode(),
                    order.getId(), order.getOrderNo(), "订单转移冲回原推荐人推荐奖 " + order.getOrderNo());
        }
        if (nz(order.getSelfBuyBonusAmount()).signum() > 0) {
            userPointsService.reverse(oldBuyerId, PointsAccountType.POINTS.getCode(),
                    order.getSelfBuyBonusAmount(), PointsBizType.SELF_BUY.getCode(),
                    order.getId(), order.getOrderNo(), "订单转移冲回原买家自购奖金 " + order.getOrderNo());
        }
        if (nz(order.getSelfBuyCouponAmount()).signum() > 0) {
            userPointsService.reverse(oldBuyerId, PointsAccountType.COUPON.getCode(),
                    order.getSelfBuyCouponAmount(), PointsBizType.COUPON.getCode(),
                    order.getId(), order.getOrderNo(), "订单转移冲回原买家购物券 " + order.getOrderNo());
        }
        // 新受益人入账（金额冻结不变；新买家无邀请人则推荐奖留利润池不发）
        if (nz(order.getSelfBuyBonusAmount()).signum() > 0) {
            userPointsService.credit(newBuyer.getId(), PointsAccountType.POINTS.getCode(),
                    order.getSelfBuyBonusAmount(), PointsBizType.SELF_BUY.getCode(),
                    order.getId(), order.getOrderNo(), "订单转入获得自购奖金 " + order.getOrderNo());
        }
        if (nz(order.getSelfBuyCouponAmount()).signum() > 0) {
            userPointsService.credit(newBuyer.getId(), PointsAccountType.COUPON.getCode(),
                    order.getSelfBuyCouponAmount(), PointsBizType.COUPON.getCode(),
                    order.getId(), order.getOrderNo(), "订单转入获得购物券 " + order.getOrderNo());
        }
        if (newInviter != null && nz(order.getRecommendAmount()).signum() > 0) {
            userPointsService.credit(newInviter.getId(), PointsAccountType.POINTS.getCode(),
                    order.getRecommendAmount(), PointsBizType.RECOMMEND.getCode(),
                    order.getId(), order.getOrderNo(), "订单转入获得推荐奖 " + order.getOrderNo());
        }

        AdminUser admin = AdminContext.get();
        writeLog(order.getId(), RobOrderStatus.NORMAL.getCode(), RobOrderStatus.NORMAL.getCode(),
                RobOrderOperateType.TRANSFER_ORDER,
                admin != null ? admin.getUserId() : null, admin != null ? admin.getUsername() : null,
                "订单由买家[" + oldBuyerId + "]转移给买家[" + newBuyer.getId() + "]");

        log.info("[抢购订单转移] orderNo={} buyer: {}->{} inviter: {}->{} operator={}",
                order.getOrderNo(), oldBuyerId, newBuyer.getId(), oldInviterId,
                newInviter != null ? newInviter.getId() : null,
                admin != null ? admin.getUsername() : "system");
        return Response.ok("订单已转移");
    }

    // ====================== 列表 / 详情 / 可抢商品 ======================

    @Override
    public Response getPageList(RobOrderPageQueryDTO parameter) {
        LocalDateTime[] range = parseRange(parameter.getTimeRange());
        Page<RobOrderVO> page = new Page<>(parameter.getPageNum(), parameter.getPageSize());
        IPage<RobOrderVO> result = robOrderMapper.selectRobOrderPage(page,
                null,
                parameter.getSessionId(),
                StringUtils.hasText(parameter.getKeyword()) ? parameter.getKeyword().trim() : null,
                parameter.getAmount() != null ? parameter.getAmount().toPlainString() : null,
                parameter.getOrderStatus(),
                range != null ? range[0] : null,
                range != null ? range[1] : null);
        result.getRecords().forEach(vo -> vo.setOrderStatusName(RobOrderStatus.descOf(vo.getOrderStatus())));
        return Response.ok(PageResultVO.of(result));
    }

    /**
     * 将日期范围字符串列表（yyyy-MM-dd 起, yyyy-MM-dd 止）转为 [当天00:00:00, 当天23:59:59]；
     * 空范围返回 null。
     */
    private LocalDateTime[] parseRange(List<String> timeRange) {
        if (timeRange == null || timeRange.isEmpty()) {
            return null;
        }
        LocalDateTime start = TimeRangeUtils.toStartOfDay(timeRange.get(0));
        LocalDateTime end = timeRange.size() > 1 ? TimeRangeUtils.toEndOfDay(timeRange.get(1)) : null;
        if (start == null && end == null) {
            return null;
        }
        return new LocalDateTime[]{start, end};
    }

    @Override
    public Response listMyOrders(Long buyerId, Integer orderStatus, Integer pageNum, Integer pageSize) {
        if (buyerId == null) {
            return Response.fail(401, "请先登录");
        }
        Page<RobOrderVO> page = new Page<>(pageNum, pageSize);
        IPage<RobOrderVO> result = robOrderMapper.selectRobOrderPage(page,
                buyerId, null, null, null, orderStatus, null, null);
        result.getRecords().forEach(vo -> vo.setOrderStatusName(RobOrderStatus.descOf(vo.getOrderStatus())));
        return Response.ok(PageResultVO.of(result));
    }

    @Override
    public Response getDetailForUser(Long id, Long currentUserId) {
        if (currentUserId == null) {
            return Response.fail(401, "请先登录");
        }
        RobOrder order = robOrderMapper.selectById(id);
        if (order == null) {
            return Response.fail(500, "订单不存在");
        }
        if (!order.getBuyerId().equals(currentUserId)) {
            return Response.fail(403, "无权查看该订单");
        }
        RobOrderVO vo = new RobOrderVO();
        BeanConvertUtils.copyProperties(order, vo);
        vo.setOrderStatusName(RobOrderStatus.descOf(order.getOrderStatus()));
        return Response.ok(vo);
    }

    @Override
    public Response listSaleGoods(Long sessionId, Integer pageNum, Integer pageSize) {
        Page<SessionProductVO> page = new Page<>(pageNum, pageSize);
        IPage<SessionProductVO> result = sessionProductMapper.selectRobSaleGoodsPage(page, sessionId);
        return Response.ok(PageResultVO.of(result));
    }

    // ====================== 私有方法 ======================

    /**
     * 抢购时间窗口校验（含新会员提前进场）。返回 null 表示通过，否则返回失败 Response。
     */
    private Response checkRushWindow(Session session, SysUser buyer, SysSettings settings) {
        LocalTime start = session.getRushStartTime();
        LocalTime end = session.getRushEndTime();
        if (start == null || end == null) {
            return Response.fail(500, "场次抢购时间未配置");
        }
        LocalTime now = LocalTime.now();
        LocalTime effectiveStart = start;
        // 新会员（member_type=0）且开启提前抢购
        boolean isNewMember = buyer.getMemberType() != null && buyer.getMemberType() == 0;
        Integer advanceMin = settings.getNewMemberAdvanceMinutes();
        if (isNewMember && advanceMin != null && advanceMin > 0) {
            effectiveStart = start.minusMinutes(advanceMin);
        }
        if (now.isBefore(effectiveStart) || now.isAfter(end)) {
            return Response.fail(500, "不在抢购时间内");
        }
        return null;
    }

    /**
     * 限购规则校验（limit_rule: 0不限购 1同场次限购一次 2当天限购一次）。
     */
    private Response checkLimitRule(SysSettings settings, Long buyerId, Long sessionId) {
        Integer rule = settings.getLimitRule();
        if (rule == null || rule == 0) {
            return null;
        }
        if (rule == 1) {
            int count = robOrderMapper.countRushedByUserAndSession(buyerId, sessionId);
            if (count > 0) {
                return Response.fail(500, "同一场次仅限抢购一次");
            }
        } else if (rule == 2) {
            LocalDate today = LocalDate.now();
            int count = robOrderMapper.countRushedByUserAndDate(buyerId,
                    today.atStartOfDay(), today.atTime(23, 59, 59));
            if (count > 0) {
                return Response.fail(500, "当天仅限抢购一次");
            }
        }
        return null;
    }

    private void writeLog(Long orderId, Integer beforeStatus, Integer afterStatus,
                          RobOrderOperateType type, Long operateUserId, String operateUserName, String remark) {
        RobOrderOperateLog log = new RobOrderOperateLog();
        log.setOrderId(orderId);
        log.setBeforeStatus(beforeStatus);
        log.setAfterStatus(afterStatus);
        log.setOperateType(type.getCode());
        log.setOperateDesc(type.getDesc());
        log.setOperateUserId(operateUserId);
        log.setOperateUserName(operateUserName);
        log.setRemark(remark);
        operateLogMapper.insert(log);
    }

    /** 按百分比计算金额（rate 为百分数，如 20.00 表示 20%），保留两位小数四舍五入 */
    private BigDecimal percent(BigDecimal base, BigDecimal rate) {
        if (base == null || rate == null) {
            return BigDecimal.ZERO;
        }
        return base.multiply(rate).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private String pickName(SysUser u) {
        if (u == null) {
            return null;
        }
        return StringUtils.hasText(u.getNickname()) ? u.getNickname() : u.getUsername();
    }
}
