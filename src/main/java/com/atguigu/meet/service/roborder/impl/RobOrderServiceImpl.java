package com.atguigu.meet.service.roborder.impl;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.enums.PointsAccountType;
import com.atguigu.meet.enums.PointsBizType;
import com.atguigu.meet.enums.RobOrderOperateType;
import com.atguigu.meet.enums.RobOrderStatus;
import com.atguigu.meet.enums.RobOrderUserIdentity;
import com.atguigu.meet.mapper.goods.consign.ConsignGoodsMapper;
import com.atguigu.meet.mapper.permission.user.UserMapper;
import com.atguigu.meet.mapper.roborder.RobOrderMapper;
import com.atguigu.meet.mapper.roborder.RobOrderOperateLogMapper;
import com.atguigu.meet.mapper.seckill.session.SessionMapper;
import com.atguigu.meet.mapper.seckill.sessionproduct.SessionProductMapper;
import com.atguigu.meet.model.dto.roborder.PlaceRobOrderDTO;
import com.atguigu.meet.model.dto.roborder.RobOrderCancelDTO;
import com.atguigu.meet.model.dto.roborder.RobOrderPageQueryDTO;
import com.atguigu.meet.model.dto.roborder.RobOrderTransferDTO;
import com.atguigu.meet.model.dto.points.PointsReverseItem;
import com.atguigu.meet.model.entity.general.settings.SysSettings;
import com.atguigu.meet.model.entity.goods.consign.ConsignGoods;
import com.atguigu.meet.model.entity.permission.user.AdminUser;
import com.atguigu.meet.model.entity.permission.user.SysUser;
import com.atguigu.meet.model.entity.roborder.RobOrder;
import com.atguigu.meet.model.entity.roborder.RobOrderOperateLog;
import com.atguigu.meet.model.entity.seckill.session.Session;
import com.atguigu.meet.model.entity.seckill.sessionproduct.SessionProduct;
import com.atguigu.meet.model.vo.PageResultVO;
import com.atguigu.meet.model.vo.roborder.InsufficientUserVO;
import com.atguigu.meet.model.vo.roborder.PointsInsufficientVO;
import com.atguigu.meet.model.vo.roborder.RobGoodsDetailVO;
import com.atguigu.meet.model.vo.roborder.RobOrderVO;
import com.atguigu.meet.model.vo.seckill.sessionproduct.SessionProductVO;
import com.atguigu.meet.service.general.settings.SysSettingsService;
import com.atguigu.meet.service.points.UserPointsService;
import com.atguigu.meet.service.roborder.RobOrderService;
import com.atguigu.meet.service.user.UserAddressService;
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
import java.util.ArrayList;
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
    private ConsignGoodsMapper consignGoodsMapper;
    @Autowired
    private SessionMapper sessionMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private SysSettingsService sysSettingsService;
    @Autowired
    private UserPointsService userPointsService;
    @Autowired
    private UserAddressService userAddressService;

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
        // 商品可售校验：与可抢商品列表 SQL 同口径（上架 + 挂卖中/委托代卖），防绕过列表对已下架/非可售商品下单
        ConsignGoods goods = consignGoodsMapper.selectById(sp.getGoodsId());
        if (goods == null
                || !Integer.valueOf(1).equals(goods.getOnlineStatus())
                || goods.getGoodsStatus() == null
                || (goods.getGoodsStatus() != 1 && goods.getGoodsStatus() != 5)) {
            return Response.fail(500, "商品已下架或不可抢");
        }

        // 2. 时间窗口校验（含新会员提前抢购）
        SysSettings settings = sysSettingsService.get();
        if (settings == null) {
            return Response.fail(500, "系统设置未配置，请联系管理员");
        }
        Response timeCheck = checkRushWindow(session, buyer, settings);
        if (timeCheck != null) {
            return timeCheck;
        }

        // 3. 限购校验
        Response limitCheck = checkLimitRule(settings, buyer.getId(), session.getId());
        if (limitCheck != null) {
            return limitCheck;
        }

        // 3.5 收货地址归属校验（必须在扣库存之前：地址无效时库存一行都不动，防越权使用他人地址）
        com.atguigu.meet.model.entity.user.UserAddress receiver =
                userAddressService.getByIdForOrder(dto.getAddressId(), currentUserId);
        if (receiver == null) {
            return Response.fail(500, "收货地址不存在");
        }

        // 4. 库存校验 + 条件扣库存（原子防超卖）：列表不过滤库存，售罄商品在此明确提示
        if (sp.getStock() == null || sp.getStock() < quantity) {
            return Response.fail(500, "商品库存不足");
        }
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
        // 收货信息快照（只冻结文字，不存 address_id；地址事后改/删不影响历史订单）
        order.setReceiverName(receiver.getReceiverName());
        order.setReceiverPhone(receiver.getReceiverPhone());
        order.setReceiveAddress(receiver.getAddress());
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
    public Response cancelOrder(RobOrderCancelDTO dto) {
        Long orderId = dto.getOrderId();
        RobOrder order = robOrderMapper.selectById(orderId);
        if (order == null) {
            return Response.fail(500, "订单不存在");
        }
        if (RobOrderStatus.CANCEL.getCode() == order.getOrderStatus()) {
            return Response.fail(500, "订单已取消，请勿重复操作");
        }

        // 积分不足预检（先于一切写操作）：未确认且不足时返回提示等待二次确认，事务内无任何变更
        boolean confirmed = Boolean.TRUE.equals(dto.getConfirmInsufficient());
        PointsInsufficientVO insufficient =
                checkInsufficient(order.getInviterId(), order.getBuyerId(), order);
        if (insufficient != null && !confirmed) {
            return Response.ok("用户积分不足，请确认是否继续", insufficient);
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
        String remark = insufficient != null
                ? "取消订单，库存与积分已回滚；积分不足经确认强制执行（负余额）"
                : "取消订单，库存与积分已回滚";
        writeLog(order.getId(), RobOrderStatus.NORMAL.getCode(), RobOrderStatus.CANCEL.getCode(),
                RobOrderOperateType.CANCEL_ORDER,
                admin != null ? admin.getUserId() : null, admin != null ? admin.getUsername() : null,
                remark);

        log.info("[抢购订单取消] orderNo={} operator={}", order.getOrderNo(),
                admin != null ? admin.getUsername() : "system");
        return Response.ok("订单已取消", null);
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

        // 积分不足预检（先于一切写操作）：未确认且不足时返回提示等待二次确认，事务内无任何变更
        boolean confirmed = Boolean.TRUE.equals(dto.getConfirmInsufficient());
        PointsInsufficientVO insufficient = checkInsufficient(oldInviterId, oldBuyerId, order);
        if (insufficient != null && !confirmed) {
            return Response.ok("用户积分不足，请确认是否继续", insufficient);
        }

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
        String transferRemark = "订单由买家[" + oldBuyerId + "]转移给买家[" + newBuyer.getId() + "]"
                + (insufficient != null ? "；积分不足经确认强制执行（负余额）" : "");
        writeLog(order.getId(), RobOrderStatus.NORMAL.getCode(), RobOrderStatus.NORMAL.getCode(),
                RobOrderOperateType.TRANSFER_ORDER,
                admin != null ? admin.getUserId() : null, admin != null ? admin.getUsername() : null,
                transferRemark);

        log.info("[抢购订单转移] orderNo={} buyer: {}->{} inviter: {}->{} operator={}",
                order.getOrderNo(), oldBuyerId, newBuyer.getId(), oldInviterId,
                newInviter != null ? newInviter.getId() : null,
                admin != null ? admin.getUsername() : "system");
        return Response.ok("订单已转移", null);
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

    @Override
    public Response getSaleGoodsDetail(Long sessionProductId, Long currentUserId) {
        RobGoodsDetailVO vo = sessionProductMapper.selectRobGoodsDetailById(sessionProductId);
        if (vo == null) {
            return Response.fail(500, "商品不存在或已下架");
        }

        // 1. 状态标志（口径与 placeOrder 下单校验一致）
        boolean sessionOpen = vo.getSessionStatus() != null && vo.getSessionStatus() == 1;
        vo.setSessionOpen(sessionOpen);
        boolean goodsOnline = vo.getOnlineStatus() != null && vo.getOnlineStatus() == 1
                && vo.getGoodsStatus() != null && (vo.getGoodsStatus() == 1 || vo.getGoodsStatus() == 5);
        vo.setGoodsOnline(goodsOnline);
        vo.setSoldOut(vo.getStock() == null || vo.getStock() <= 0);

        // 2. 系统设置：限购规则 + 新会员提前进场双开关（与 checkRushWindow/checkLimitRule 同口径）
        SysSettings settings = sysSettingsService.get();
        Integer limitRule = settings != null ? settings.getLimitRule() : null;
        vo.setLimitRule(limitRule);
        vo.setLimitRuleName(limitRuleName(limitRule));

        // 3. 抢购时间窗口（新会员需 newMemberDays>0 且 advanceMin>0 双开关开启才提前；未登录按普通窗口）
        boolean inWindow = false;
        boolean isNewMember = false;
        if (vo.getRushStartTime() != null && vo.getRushEndTime() != null) {
            LocalTime effectiveStart = vo.getRushStartTime();
            if (currentUserId != null) {
                SysUser buyer = userMapper.selectById(currentUserId);
                isNewMember = buyer != null && buyer.getMemberType() != null && buyer.getMemberType() == 0;
                if (isNewMember && settings != null) {
                    Integer newMemberDays = settings.getNewMemberDays();
                    Integer advanceMin = settings.getNewMemberAdvanceMinutes();
                    if (newMemberDays != null && newMemberDays > 0
                            && advanceMin != null && advanceMin > 0) {
                        effectiveStart = vo.getRushStartTime().minusMinutes(advanceMin);
                    }
                }
            }
            LocalTime now = LocalTime.now();
            inWindow = !now.isBefore(effectiveStart) && !now.isAfter(vo.getRushEndTime());
        }

        // 4. 限购命中（未登录或不限购视为未命中，点击下单仍走登录校验）
        boolean hasRushed = false;
        if (currentUserId != null && limitRule != null) {
            if (limitRule == 1) {
                hasRushed = robOrderMapper.countRushedByUserAndSession(currentUserId, vo.getSessionId()) > 0;
            } else if (limitRule == 2) {
                LocalDate today = LocalDate.now();
                hasRushed = robOrderMapper.countRushedByUserAndDate(currentUserId,
                        today.atStartOfDay(), today.atTime(23, 59, 59)) > 0;
            }
        }
        vo.setHasRushed(hasRushed);

        // 5. 严格版可购标志：场次开启 + 商品可售 + 时间窗口 + 有库存 + 未命中限购，五项全满足才可购
        vo.setCanPurchase(sessionOpen && goodsOnline && inWindow && !vo.getSoldOut() && !hasRushed);

        return Response.ok(vo);
    }

    // ====================== 私有方法 ======================

    /**
     * 冲回余额预检（提示性检查，无锁读）：按订单快照组装本单可用积分冲回项
     * （推荐人-推荐奖、买家-自购奖金，均 POINTS 账户；购物券不参与预检，照常冲回允许负余额），
     * 全部充足返回 null；任一用户不足返回逐用户信息 VO（含订单身份与不足标志）。
     */
    private PointsInsufficientVO checkInsufficient(Long inviterId, Long buyerId, RobOrder order) {
        List<PointsReverseItem> items = new ArrayList<>();
        if (inviterId != null && nz(order.getRecommendAmount()).signum() > 0) {
            items.add(new PointsReverseItem(inviterId, PointsAccountType.POINTS.getCode(),
                    order.getRecommendAmount()));
        }
        if (nz(order.getSelfBuyBonusAmount()).signum() > 0) {
            items.add(new PointsReverseItem(buyerId, PointsAccountType.POINTS.getCode(),
                    order.getSelfBuyBonusAmount()));
        }
        PointsInsufficientVO vo = userPointsService.checkReverseBalance(items);
        if (vo != null && vo.getUsers() != null) {
            // 身份为抢购订单业务口径，按订单快照回填（与 D1 一致：积分服务不感知订单语境）
            for (InsufficientUserVO u : vo.getUsers()) {
                RobOrderUserIdentity identity = u.getUserId() != null && u.getUserId().equals(inviterId)
                        ? RobOrderUserIdentity.INVITER : RobOrderUserIdentity.BUYER;
                u.setIdentityType(identity.getCode());
                u.setIdentityName(identity.getDesc());
            }
        }
        return vo;
    }

    /** 限购规则描述（limit_rule: 0不限购 1同场次限购一次 2当天限购一次） */
    private String limitRuleName(Integer limitRule) {
        if (limitRule == null) {
            return "不限购";
        }
        switch (limitRule) {
            case 1:
                return "同一场次仅限抢购一次";
            case 2:
                return "当天仅限抢购一次";
            default:
                return "不限购";
        }
    }

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
        // 新会员提前进场：需同时开启新会员权益(newMemberDays>0)与提前抢购(advanceMin>0)，且用户为新会员(member_type=0)
        boolean isNewMember = buyer.getMemberType() != null && buyer.getMemberType() == 0;
        Integer newMemberDays = settings.getNewMemberDays();
        Integer advanceMin = settings.getNewMemberAdvanceMinutes();
        boolean newMemberAdvance = isNewMember
                && newMemberDays != null && newMemberDays > 0
                && advanceMin != null && advanceMin > 0;
        if (newMemberAdvance) {
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