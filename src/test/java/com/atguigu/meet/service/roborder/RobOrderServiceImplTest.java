package com.atguigu.meet.service.roborder;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.enums.RobOrderStatus;
import com.atguigu.meet.mapper.goods.consign.ConsignGoodsMapper;
import com.atguigu.meet.mapper.permission.user.UserMapper;
import com.atguigu.meet.mapper.roborder.RobOrderMapper;
import com.atguigu.meet.mapper.roborder.RobOrderOperateLogMapper;
import com.atguigu.meet.mapper.seckill.session.SessionMapper;
import com.atguigu.meet.mapper.seckill.sessionproduct.SessionProductMapper;
import com.atguigu.meet.model.dto.roborder.PlaceRobOrderDTO;
import com.atguigu.meet.model.dto.roborder.RobOrderBatchCancelDTO;
import com.atguigu.meet.model.dto.roborder.RobOrderBatchConfirmPayDTO;
import com.atguigu.meet.model.dto.roborder.RobOrderTransferDTO;
import com.atguigu.meet.model.entity.general.settings.SysSettings;
import com.atguigu.meet.model.entity.goods.consign.ConsignGoods;
import com.atguigu.meet.model.entity.permission.user.SysUser;
import com.atguigu.meet.model.entity.permission.user.AdminUser;
import com.atguigu.meet.model.entity.roborder.RobOrder;
import com.atguigu.meet.model.entity.roborder.RobOrderOperateLog;
import com.atguigu.meet.model.entity.seckill.session.Session;
import com.atguigu.meet.model.entity.seckill.sessionproduct.SessionProduct;
import com.atguigu.meet.model.entity.user.UserAddress;
import com.atguigu.meet.model.vo.roborder.InsufficientUserVO;
import com.atguigu.meet.model.vo.roborder.RobOrderBatchInsufficientVO;
import com.atguigu.meet.model.vo.roborder.PointsInsufficientVO;
import com.atguigu.meet.model.vo.roborder.RobGoodsDetailVO;
import com.atguigu.meet.model.vo.seckill.sessionproduct.SessionProductVO;
import com.atguigu.meet.service.general.settings.SysSettingsService;
import com.atguigu.meet.service.points.UserPointsService;
import com.atguigu.meet.service.roborder.impl.RobOrderServiceImpl;
import com.atguigu.meet.service.roborder.BatchRobOrderException;
import com.atguigu.meet.utils.AdminContext;
import com.atguigu.meet.service.user.UserAddressService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RobOrderServiceImpl 单元测试
 * <p>
 * 覆盖 spec「抢购订单收货地址快照」（地址归属校验先于扣库存）与
 * spec「取消/转移订单积分不足二次确认」（未确认不足整单不执行、确认后按原逻辑执行并标注审计）。
 */
@ExtendWith(MockitoExtension.class)
class RobOrderServiceImplTest {

    @Mock
    private RobOrderMapper robOrderMapper;
    @Mock
    private RobOrderOperateLogMapper operateLogMapper;
    @Mock
    private SessionProductMapper sessionProductMapper;
    @Mock
    private ConsignGoodsMapper consignGoodsMapper;
    @Mock
    private SessionMapper sessionMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private SysSettingsService sysSettingsService;
    @Mock
    private UserPointsService userPointsService;
    @Mock
    private UserAddressService userAddressService;

    @InjectMocks
    private RobOrderServiceImpl robOrderService;

    private static final Long BUYER_ID = 10L;
    private static final Long SP_ID = 5L;
    private static final Long ORDER_ID = 77L;
    private static final Long INVITER_ID = 20L;

    /**
     * 纯 Mockito 环境无 MyBatis-Plus 启动流程，手动初始化实体列缓存，
     * 使取消/转移订单的 LambdaUpdateWrapper.set(RobOrder::getXxx, ...) 可正常解析列名。
     */
    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, RobOrder.class);
    }

    @Test
    void placeOrder_invalidAddress_failsBeforeStockDeduction() {
        PlaceRobOrderDTO dto = new PlaceRobOrderDTO();
        dto.setSessionProductId(SP_ID);
        dto.setQuantity(1);
        dto.setAddressId(99L);

        stubCommonPrerequisites();
        // 地址不存在或不属于当前用户
        when(userAddressService.getByIdForOrder(99L, BUYER_ID)).thenReturn(null);

        Response resp = robOrderService.placeOrder(dto, BUYER_ID);

        assertEquals(500, resp.getCode());
        // 关键：扣库存与建单均不得发生（地址校验先于扣库存）
        verify(sessionProductMapper, never()).deductStock(anyLong(), anyInt());
        verify(robOrderMapper, never()).insert(any(RobOrder.class));
    }

    @Test
    void placeOrder_validAddress_snapshotsReceiverInfo() {
        PlaceRobOrderDTO dto = new PlaceRobOrderDTO();
        dto.setSessionProductId(SP_ID);
        dto.setQuantity(1);
        dto.setAddressId(7L);

        stubCommonPrerequisites();
        UserAddress addr = new UserAddress();
        addr.setId(7L);
        addr.setUserId(BUYER_ID);
        addr.setReceiverName("张三");
        addr.setReceiverPhone("13800001111");
        addr.setAddress("北京市朝阳区某街道1号");
        when(userAddressService.getByIdForOrder(7L, BUYER_ID)).thenReturn(addr);
        when(sessionProductMapper.deductStock(SP_ID, 1)).thenReturn(1);

        Response resp = robOrderService.placeOrder(dto, BUYER_ID);

        assertEquals(200, resp.getCode());
        ArgumentCaptor<RobOrder> captor = ArgumentCaptor.forClass(RobOrder.class);
        verify(robOrderMapper).insert(captor.capture());
        RobOrder order = captor.getValue();
        assertEquals("张三", order.getReceiverName());
        assertEquals("13800001111", order.getReceiverPhone());
        assertEquals("北京市朝阳区某街道1号", order.getReceiveAddress());
        assertEquals(BUYER_ID, order.getBuyerId());
        assertEquals(RobOrderStatus.NORMAL.getCode(), order.getOrderStatus());

        // 下单事件行落库事件买家快照（=下单买家），prev_buyer_* 一律为 NULL
        ArgumentCaptor<RobOrderOperateLog> placeLogCaptor = ArgumentCaptor.forClass(RobOrderOperateLog.class);
        verify(operateLogMapper).insert(placeLogCaptor.capture());
        RobOrderOperateLog placeLog = placeLogCaptor.getValue();
        assertEquals(BUYER_ID, placeLog.getBuyerId());
        assertEquals("buyer10", placeLog.getBuyerName());
        assertEquals("13900002222", placeLog.getBuyerPhone());
        assertNull(placeLog.getPrevBuyerId());
        assertNull(placeLog.getPrevBuyerName());
        assertNull(placeLog.getPrevBuyerPhone());
        assertNull(placeLog.getPrevInviterId());
    }

    // ====================== 跨零点场次 canPurchase 回归（fix-new-member-advance-midnight-wrap） ======================

    /** 构造可购详情 VO：场次开启、商品上架挂卖、库存充足，抢购窗口按用例给定 */
    private RobGoodsDetailVO saleGoodsDetailVO(LocalTime start, LocalTime end) {
        RobGoodsDetailVO vo = new RobGoodsDetailVO();
        vo.setSessionProductId(SP_ID);
        vo.setSessionId(3L);
        vo.setSessionStatus(1);
        vo.setOnlineStatus(1);
        vo.setGoodsStatus(1);
        vo.setStock(10);
        vo.setRushStartTime(start);
        vo.setRushEndTime(end);
        return vo;
    }

    /** 桩详情查询 + 指定会员类型买家 + 新会员双开关（newMemberDays=7、advance=30、不限购） */
    private void stubSaleGoodsDetail(LocalTime start, LocalTime end, Integer memberType) {
        when(sessionProductMapper.selectRobGoodsDetailById(SP_ID)).thenReturn(saleGoodsDetailVO(start, end));
        SysUser buyer = new SysUser();
        buyer.setId(BUYER_ID);
        buyer.setMemberType(memberType);
        when(userMapper.selectById(BUYER_ID)).thenReturn(buyer);
        SysSettings settings = new SysSettings();
        settings.setNewMemberDays(7);
        settings.setNewMemberAdvanceMinutes(30);
        settings.setLimitRule(0);
        when(sysSettingsService.get()).thenReturn(settings);
    }

    /** 在固定时钟下调用详情接口并取出 VO */
    private RobGoodsDetailVO detailAt(LocalTime fixedNow) {
        // CALLS_REAL_METHODS：仅桩 now()，LocalTime 的时间算术（minusMinutes 等内部静态调用）保持真实
        try (MockedStatic<LocalTime> mockedClock = mockStatic(LocalTime.class, Mockito.CALLS_REAL_METHODS)) {
            mockedClock.when(LocalTime::now).thenReturn(fixedNow);
            Response resp = robOrderService.getSaleGoodsDetail(SP_ID, BUYER_ID);
            assertEquals(200, resp.getCode());
            return (RobGoodsDetailVO) resp.getData();
        }
    }

    @Test
    void saleGoodsDetail_allDaySession_newMember_canPurchaseAtNoon() {
        // 全天场 00:00~23:59 + 新会员双开关：修复前 12:00 因 00:00-30分回绕为 23:30 而误判 false
        stubSaleGoodsDetail(LocalTime.of(0, 0), LocalTime.of(23, 59), 0);
        assertTrue(detailAt(LocalTime.of(12, 0)).getCanPurchase());
    }

    @Test
    void saleGoodsDetail_allDaySession_oldMember_canPurchaseAtNoon() {
        // 同条件老会员对照：正常窗口本就覆盖全天，新老结果一致
        stubSaleGoodsDetail(LocalTime.of(0, 0), LocalTime.of(23, 59), 1);
        assertTrue(detailAt(LocalTime.of(12, 0)).getCanPurchase());
    }

    @Test
    void saleGoodsDetail_normalSession_newMemberBeforeWindow_cannotPurchase() {
        // 防过度放宽：正常场 18:30 开场（提前后 18:00），新会员 12:00 仍不可购
        stubSaleGoodsDetail(LocalTime.of(18, 30), LocalTime.of(23, 30), 0);
        assertFalse(detailAt(LocalTime.of(12, 0)).getCanPurchase());
        // 提前窗口生效：18:00 整点可购
        assertTrue(detailAt(LocalTime.of(18, 0)).getCanPurchase());
    }

    /** 桩齐地址校验之前的全部下单前置：买家、场次商品关联、场次、商品、时间窗口/限购设置 */
    private void stubCommonPrerequisites() {
        SysUser buyer = new SysUser();
        buyer.setId(BUYER_ID);
        buyer.setUsername("buyer10");
        buyer.setPhone("13900002222");
        when(userMapper.selectById(BUYER_ID)).thenReturn(buyer);

        SessionProduct sp = new SessionProduct();
        sp.setId(SP_ID);
        sp.setSessionId(3L);
        sp.setGoodsId(8L);
        sp.setStock(10);
        when(sessionProductMapper.selectById(SP_ID)).thenReturn(sp);

        SessionProductVO spVO = new SessionProductVO();
        spVO.setPrice(new BigDecimal("10.00"));
        spVO.setGoodsName("测试商品");
        when(sessionProductMapper.selectSessionProductById(SP_ID)).thenReturn(spVO);

        Session session = new Session();
        session.setId(3L);
        session.setSessionStatus(1);
        session.setSessionName("午间场");
        session.setRushStartTime(LocalTime.MIN);
        session.setRushEndTime(LocalTime.MAX);
        when(sessionMapper.selectById(3L)).thenReturn(session);

        ConsignGoods goods = new ConsignGoods();
        goods.setId(8L);
        goods.setOnlineStatus(1);
        goods.setGoodsStatus(1);
        when(consignGoodsMapper.selectById(8L)).thenReturn(goods);

        when(sysSettingsService.get()).thenReturn(new SysSettings());
    }

    // ====================== 取消/转移订单积分不足二次确认 ======================

    /** 构造一条正常订单快照：推荐奖 5 + 自购奖金 3 + 购物券 2 */
    private RobOrder normalOrder() {
        RobOrder order = new RobOrder();
        order.setId(ORDER_ID);
        order.setOrderNo("RO20260910001");
        order.setSessionProductId(SP_ID);
        order.setQuantity(1);
        order.setOrderStatus(RobOrderStatus.NORMAL.getCode());
        order.setBuyerId(BUYER_ID);
        order.setBuyerName("buyer10");
        order.setBuyerPhone("13900002222");
        order.setInviterId(INVITER_ID);
        order.setRecommendAmount(new BigDecimal("5.00"));
        order.setSelfBuyBonusAmount(new BigDecimal("3.00"));
        order.setSelfBuyCouponAmount(new BigDecimal("2.00"));
        return order;
    }

    /** 构造积分不足提示 VO（单冲回用户，userId 为 null 表示买家视角用例无需断言具体人） */
    private PointsInsufficientVO insufficientVO(Long userId, boolean points) {
        InsufficientUserVO u = new InsufficientUserVO();
        u.setUserId(userId);
        u.setNickname(userId == null ? null : "用户" + userId);
        u.setPhone(userId == null ? null : "13800000000");
        u.setPointsInsufficient(points);
        PointsInsufficientVO vo = new PointsInsufficientVO();
        vo.setUsers(Collections.singletonList(u));
        return vo;
    }

    private void stubNormalOrder() {
        when(robOrderMapper.selectById(ORDER_ID)).thenReturn(normalOrder());
    }

    /** 桩订单条件更新成功（取消/转移执行写入的用例使用） */
    private void stubWrites() {
        doReturn(1).when(robOrderMapper).update(any(), any());
    }

    /** 桩库存回滚成功（仅取消订单用例使用，转移不回滚库存） */
    private void stubAddStock() {
        doReturn(1).when(sessionProductMapper).addStock(SP_ID, 1);
    }

    private RobOrderBatchCancelDTO cancelBatch(Boolean confirmed) {
        RobOrderBatchCancelDTO dto = new RobOrderBatchCancelDTO();
        dto.setOrderIds(List.of(ORDER_ID));
        dto.setConfirmInsufficient(confirmed);
        return dto;
    }

    private PointsInsufficientVO firstInsufficient(Response<?> result) {
        return ((RobOrderBatchInsufficientVO) ((List<?>) result.getData()).get(0)).getPointsInsufficient();
    }

    @Test
    void cancelOrders_collectsAllInsufficientOrdersBeforeWriting() {
        RobOrder first = normalOrder();
        RobOrder second = normalOrder();
        second.setId(78L);
        when(robOrderMapper.selectById(ORDER_ID)).thenReturn(first);
        when(robOrderMapper.selectById(78L)).thenReturn(second);
        when(userPointsService.checkReverseBalance(any()))
                .thenReturn(insufficientVO(INVITER_ID, true));

        RobOrderBatchCancelDTO dto = new RobOrderBatchCancelDTO();
        dto.setOrderIds(List.of(ORDER_ID, 78L));
        Response<?> result = robOrderService.cancelOrders(dto);

        assertEquals(200, result.getCode());
        List<?> data = (List<?>) result.getData();
        assertEquals(2, data.size());
        assertEquals(ORDER_ID, ((RobOrderBatchInsufficientVO) data.get(0)).getOrderId());
        assertEquals(78L, ((RobOrderBatchInsufficientVO) data.get(1)).getOrderId());
        verify(robOrderMapper, never()).update(isNull(), any());
        verify(sessionProductMapper, never()).addStock(anyLong(), anyInt());
    }

    @Test
    void cancelOrders_detectsInsufficientBalanceAcrossOrdersBeforeWriting() {
        RobOrder first = normalOrder();
        RobOrder second = normalOrder();
        second.setId(78L);
        when(robOrderMapper.selectById(ORDER_ID)).thenReturn(first);
        when(robOrderMapper.selectById(78L)).thenReturn(second);
        when(userPointsService.checkReverseBalance(any())).thenAnswer(invocation -> {
            List<?> items = invocation.getArgument(0);
            return items.size() > 2 ? insufficientVO(INVITER_ID, true) : null;
        });
        RobOrderBatchCancelDTO dto = new RobOrderBatchCancelDTO();
        dto.setOrderIds(List.of(ORDER_ID, 78L));

        Response<?> result = robOrderService.cancelOrders(dto);

        assertEquals(200, result.getCode());
        assertEquals(2, ((List<?>) result.getData()).size());
        verify(robOrderMapper, never()).update(isNull(), any());
        verify(sessionProductMapper, never()).addStock(anyLong(), anyInt());
    }

    @Test
    void cancelOrders_rejectsDuplicateIdsBeforeWriting() {
        RobOrderBatchCancelDTO dto = new RobOrderBatchCancelDTO();
        dto.setOrderIds(List.of(ORDER_ID, ORDER_ID));

        Response<?> result = robOrderService.cancelOrders(dto);

        assertEquals(400, result.getCode());
        verify(robOrderMapper, never()).selectById(anyLong());
    }

    @Test
    void cancelOrders_stockRestoreFailureAbortsBeforePointsReversal() {
        stubNormalOrder();
        stubWrites();
        when(userPointsService.checkReverseBalance(any())).thenReturn(null);
        when(sessionProductMapper.addStock(SP_ID, 1)).thenReturn(0);

        BatchRobOrderException error = assertThrows(BatchRobOrderException.class,
                () -> robOrderService.cancelOrders(cancelBatch(null)));

        assertEquals(500, error.getResponse().getCode());
        assertTrue(error.getResponse().getMsg().contains("库存回补失败"));
        verify(userPointsService, never()).reverse(anyLong(), anyInt(), any(), anyInt(), anyLong(), any(), any());
        verify(operateLogMapper, never()).insert(any(RobOrderOperateLog.class));
    }

    @Test
    void confirmPays_abortsOnFirstFailedOrder() {
        AdminUser admin = new AdminUser();
        admin.setUserId(1L);
        admin.setPermissions(Set.of(PermissionConst.ROB_ORDER_CONFIRM_RECEIPT));
        RobOrder first = normalOrder();
        first.setPayStatus(0);
        RobOrder second = normalOrder();
        second.setId(78L);
        second.setPayStatus(1);
        when(robOrderMapper.selectById(ORDER_ID)).thenReturn(first);
        when(robOrderMapper.selectById(78L)).thenReturn(second);
        stubWrites();
        RobOrderBatchConfirmPayDTO dto = new RobOrderBatchConfirmPayDTO();
        dto.setOrderIds(List.of(ORDER_ID, 78L, 79L));
        dto.setAction(1);

        AdminContext.set(admin);
        try {
            BatchRobOrderException error = assertThrows(BatchRobOrderException.class,
                    () -> robOrderService.confirmPays(dto));
            assertEquals(500, error.getResponse().getCode());
            assertTrue(error.getResponse().getMsg().contains("78"));
            verify(robOrderMapper, never()).selectById(79L);
            verify(robOrderMapper).update(isNull(), any());
        } finally {
            AdminContext.remove();
        }
    }

    @Test
    void cancelOrder_insufficientNotConfirmed_noWrites() {
        stubNormalOrder();
        // 邀请人可用积分不足（推荐奖冲回缺口）
        when(userPointsService.checkReverseBalance(any()))
                .thenReturn(insufficientVO(INVITER_ID, true));

        Response<?> resp = robOrderService.cancelOrders(cancelBatch(null));

        assertEquals(200, resp.getCode());
        PointsInsufficientVO data = firstInsufficient(resp);
        assertNotNull(data);
        assertEquals(1, data.getUsers().size());
        assertEquals(INVITER_ID, data.getUsers().get(0).getUserId());
        // 邀请人 → 推荐人身份
        assertEquals(1, data.getUsers().get(0).getIdentityType());
        assertEquals("推荐人", data.getUsers().get(0).getIdentityName());
        assertTrue(data.getUsers().get(0).getPointsInsufficient());
        // 预检先于一切写操作：订单/库存/积分/审计均不得变更
        verify(robOrderMapper, never()).update(isNull(), any());
        verify(sessionProductMapper, never()).addStock(anyLong(), anyInt());
        verify(userPointsService, never()).reverse(anyLong(), anyInt(), any(), anyInt(), anyLong(), any(), any());
        verify(operateLogMapper, never()).insert(any(RobOrderOperateLog.class));
    }

    @Test
    void cancelOrder_buyerInsufficient_identifiedAsBuyer() {
        // 无邀请人订单：买家自购奖金冲回缺口 → 身份应为买家
        RobOrder order = normalOrder();
        order.setInviterId(null);
        order.setRecommendAmount(BigDecimal.ZERO);
        when(robOrderMapper.selectById(ORDER_ID)).thenReturn(order);
        when(userPointsService.checkReverseBalance(any()))
                .thenReturn(insufficientVO(BUYER_ID, true));

        Response<?> resp = robOrderService.cancelOrders(cancelBatch(null));

        assertEquals(200, resp.getCode());
        PointsInsufficientVO data = firstInsufficient(resp);
        assertNotNull(data);
        assertEquals(1, data.getUsers().size());
        assertEquals(BUYER_ID, data.getUsers().get(0).getUserId());
        assertEquals(2, data.getUsers().get(0).getIdentityType());
        assertEquals("买家", data.getUsers().get(0).getIdentityName());
        assertTrue(data.getUsers().get(0).getPointsInsufficient());
    }

    @Test
    void cancelOrder_onlyCouponGap_executesWithoutConfirm() {
        // 购物券不参与预检：checkReverseBalance 收到 null（无可用积分缺口）即正常执行
        stubNormalOrder();
        stubWrites();
        stubAddStock();
        when(userPointsService.checkReverseBalance(any())).thenReturn(null);

        Response<?> resp = robOrderService.cancelOrders(cancelBatch(null));

        assertEquals(200, resp.getCode());
        assertNull(resp.getData());
        verify(robOrderMapper).update(isNull(), any());
        verify(sessionProductMapper).addStock(SP_ID, 1);
        // 购物券仍照常全额冲回（共三笔冲回）
        verify(userPointsService, times(3)).reverse(anyLong(), anyInt(), any(), anyInt(), anyLong(), any(), any());
        ArgumentCaptor<RobOrderOperateLog> logCaptor = ArgumentCaptor.forClass(RobOrderOperateLog.class);
        verify(operateLogMapper).insert(logCaptor.capture());
        assertFalse(logCaptor.getValue().getRemark().contains("积分不足经确认强制执行"));
    }

    @Test
    void cancelOrder_confirmed_executesRollbackWithAuditMark() {
        stubNormalOrder();
        stubWrites();
        stubAddStock();
        when(userPointsService.checkReverseBalance(any()))
                .thenReturn(insufficientVO(INVITER_ID, true));

        Response<?> resp = robOrderService.cancelOrders(cancelBatch(true));

        assertEquals(200, resp.getCode());
        assertNull(resp.getData());
        // 现有回滚链路完整执行：状态置已取消 + 库存回滚 + 三笔冲回
        verify(robOrderMapper).update(isNull(), any());
        verify(sessionProductMapper).addStock(SP_ID, 1);
        verify(userPointsService, times(3)).reverse(anyLong(), anyInt(), any(), anyInt(), anyLong(), any(), any());
        // 审计备注标注确认强制执行
        ArgumentCaptor<RobOrderOperateLog> logCaptor = ArgumentCaptor.forClass(RobOrderOperateLog.class);
        verify(operateLogMapper).insert(logCaptor.capture());
        assertTrue(logCaptor.getValue().getRemark().contains("积分不足经确认强制执行"));
    }

    @Test
    void cancelOrder_sufficient_normalSuccessWithoutMark() {
        stubNormalOrder();
        stubWrites();
        stubAddStock();
        when(userPointsService.checkReverseBalance(any())).thenReturn(null);

        Response<?> resp = robOrderService.cancelOrders(cancelBatch(null));

        assertEquals(200, resp.getCode());
        assertNull(resp.getData());
        verify(robOrderMapper).update(isNull(), any());
        verify(sessionProductMapper).addStock(SP_ID, 1);
        verify(userPointsService, times(3)).reverse(anyLong(), anyInt(), any(), anyInt(), anyLong(), any(), any());
        // 余额充足时审计备注不含确认标注（与改造前行为一致）
        ArgumentCaptor<RobOrderOperateLog> logCaptor = ArgumentCaptor.forClass(RobOrderOperateLog.class);
        verify(operateLogMapper).insert(logCaptor.capture());
        RobOrderOperateLog cancelLog = logCaptor.getValue();
        assertFalse(cancelLog.getRemark().contains("积分不足经确认强制执行"));
        // 取消事件行落库取消时买家快照（=订单当前买家），prev_buyer_* 为 NULL
        assertEquals(BUYER_ID, cancelLog.getBuyerId());
        assertEquals("buyer10", cancelLog.getBuyerName());
        assertEquals("13900002222", cancelLog.getBuyerPhone());
        assertNull(cancelLog.getPrevBuyerId());
        assertNull(cancelLog.getPrevInviterId());
    }

    private void stubTransferPrerequisites() {
        stubNormalOrder();
        SysUser newBuyer = new SysUser();
        newBuyer.setId(30L);
        newBuyer.setStatus("1");
        newBuyer.setUsername("buyer30");
        newBuyer.setPhone("13800003000");
        when(userMapper.selectById(30L)).thenReturn(newBuyer);
    }

    private RobOrderTransferDTO transferDTO(Boolean confirm) {
        RobOrderTransferDTO dto = new RobOrderTransferDTO();
        dto.setOrderId(ORDER_ID);
        dto.setNewBuyerId(30L);
        dto.setConfirmInsufficient(confirm);
        return dto;
    }

    @Test
    void transferOrder_insufficientNotConfirmed_noWrites() {
        stubTransferPrerequisites();
        // 邀请人可用积分不足（推荐奖划转缺口）
        when(userPointsService.checkReverseBalance(any()))
                .thenReturn(insufficientVO(INVITER_ID, true));

        Response resp = robOrderService.transferOrder(transferDTO(null));

        assertEquals(200, resp.getCode());
        PointsInsufficientVO data = (PointsInsufficientVO) resp.getData();
        assertNotNull(data);
        assertEquals(1, data.getUsers().size());
        assertEquals(INVITER_ID, data.getUsers().get(0).getUserId());
        // 邀请人 → 推荐人身份
        assertEquals(1, data.getUsers().get(0).getIdentityType());
        assertEquals("推荐人", data.getUsers().get(0).getIdentityName());
        assertTrue(data.getUsers().get(0).getPointsInsufficient());
        // 预检先于一切写操作：订单快照重写与积分划转均不得发生
        verify(robOrderMapper, never()).update(isNull(), any());
        verify(userPointsService, never()).reverse(anyLong(), anyInt(), any(), anyInt(), anyLong(), any(), any());
        verify(userPointsService, never()).credit(anyLong(), anyInt(), any(), anyInt(), anyLong(), any(), any());
        verify(operateLogMapper, never()).insert(any(RobOrderOperateLog.class));
    }

    @Test
    void transferOrder_confirmed_executesTransferWithAuditMark() {
        stubTransferPrerequisites();
        stubWrites();
        when(userPointsService.checkReverseBalance(any()))
                .thenReturn(insufficientVO(INVITER_ID, true));

        Response resp = robOrderService.transferOrder(transferDTO(true));

        assertEquals(200, resp.getCode());
        assertNull(resp.getData());
        // 现有划转链路完整执行：原受益人三笔冲回 + 新买家两笔入账（新买家无邀请人，推荐奖不发）
        verify(robOrderMapper).update(isNull(), any());
        verify(userPointsService, times(3)).reverse(anyLong(), anyInt(), any(), anyInt(), anyLong(), any(), any());
        verify(userPointsService, times(2)).credit(anyLong(), anyInt(), any(), anyInt(), anyLong(), any(), any());
        ArgumentCaptor<RobOrderOperateLog> logCaptor = ArgumentCaptor.forClass(RobOrderOperateLog.class);
        verify(operateLogMapper).insert(logCaptor.capture());
        assertTrue(logCaptor.getValue().getRemark().contains("积分不足经确认强制执行"));
    }

    @Test
    void transferOrder_sufficient_normalSuccessWithoutMark() {
        stubTransferPrerequisites();
        stubWrites();
        when(userPointsService.checkReverseBalance(any())).thenReturn(null);
        // 转移事件金额取同订单下单事件行正额快照（与取消同逻辑）
        RobOrderOperateLog placeAmounts = new RobOrderOperateLog();
        placeAmounts.setReceiptAmount(new BigDecimal("11.00"));
        placeAmounts.setPaymentAmount(new BigDecimal("22.00"));
        when(operateLogMapper.selectPlaceAmounts(ORDER_ID)).thenReturn(placeAmounts);

        Response resp = robOrderService.transferOrder(transferDTO(null));

        assertEquals(200, resp.getCode());
        assertNull(resp.getData());
        verify(robOrderMapper).update(isNull(), any());
        verify(userPointsService, times(3)).reverse(anyLong(), anyInt(), any(), anyInt(), anyLong(), any(), any());
        verify(userPointsService, times(2)).credit(anyLong(), anyInt(), any(), anyInt(), anyLong(), any(), any());
        ArgumentCaptor<RobOrderOperateLog> logCaptor = ArgumentCaptor.forClass(RobOrderOperateLog.class);
        verify(operateLogMapper).insert(logCaptor.capture());
        RobOrderOperateLog flowLog = logCaptor.getValue();
        assertFalse(flowLog.getRemark().contains("积分不足经确认强制执行"));
        // 原买家/原推荐人快照在订单 UPDATE 前捕获并落转移事件行；金额透传下单行正额快照
        assertEquals(BUYER_ID, flowLog.getPrevBuyerId());
        assertEquals("buyer10", flowLog.getPrevBuyerName());
        assertEquals("13900002222", flowLog.getPrevBuyerPhone());
        assertEquals(INVITER_ID, flowLog.getPrevInviterId());
        // 事件买家快照=转入新买家（正向行展示用）
        assertEquals(30L, flowLog.getBuyerId());
        assertEquals("buyer30", flowLog.getBuyerName());
        assertEquals("13800003000", flowLog.getBuyerPhone());
        assertEquals(new BigDecimal("11.00"), flowLog.getReceiptAmount());
        assertEquals(new BigDecimal("22.00"), flowLog.getPaymentAmount());
    }
}
