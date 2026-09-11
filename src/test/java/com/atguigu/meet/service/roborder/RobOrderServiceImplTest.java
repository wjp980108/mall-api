package com.atguigu.meet.service.roborder;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.enums.RobOrderStatus;
import com.atguigu.meet.mapper.goods.consign.ConsignGoodsMapper;
import com.atguigu.meet.mapper.permission.user.UserMapper;
import com.atguigu.meet.mapper.roborder.RobOrderMapper;
import com.atguigu.meet.mapper.roborder.RobOrderOperateLogMapper;
import com.atguigu.meet.mapper.seckill.session.SessionMapper;
import com.atguigu.meet.mapper.seckill.sessionproduct.SessionProductMapper;
import com.atguigu.meet.model.dto.roborder.PlaceRobOrderDTO;
import com.atguigu.meet.model.dto.roborder.RobOrderCancelDTO;
import com.atguigu.meet.model.dto.roborder.RobOrderTransferDTO;
import com.atguigu.meet.model.entity.general.settings.SysSettings;
import com.atguigu.meet.model.entity.goods.consign.ConsignGoods;
import com.atguigu.meet.model.entity.permission.user.SysUser;
import com.atguigu.meet.model.entity.roborder.RobOrder;
import com.atguigu.meet.model.entity.roborder.RobOrderOperateLog;
import com.atguigu.meet.model.entity.seckill.session.Session;
import com.atguigu.meet.model.entity.seckill.sessionproduct.SessionProduct;
import com.atguigu.meet.model.entity.user.UserAddress;
import com.atguigu.meet.model.vo.roborder.PointsInsufficientVO;
import com.atguigu.meet.model.vo.seckill.sessionproduct.SessionProductVO;
import com.atguigu.meet.service.general.settings.SysSettingsService;
import com.atguigu.meet.service.points.UserPointsService;
import com.atguigu.meet.service.roborder.impl.RobOrderServiceImpl;
import com.atguigu.meet.service.user.UserAddressService;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
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
     * 使 LambdaUpdateWrapper.set(RobOrder::getXxx, ...) 可正常解析列名。
     */
    @BeforeAll
    static void initMybatisPlusTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), RobOrder.class);
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
        order.setInviterId(INVITER_ID);
        order.setRecommendAmount(new BigDecimal("5.00"));
        order.setSelfBuyBonusAmount(new BigDecimal("3.00"));
        order.setSelfBuyCouponAmount(new BigDecimal("2.00"));
        return order;
    }

    private PointsInsufficientVO insufficientVO(boolean points, boolean coupon) {
        PointsInsufficientVO vo = new PointsInsufficientVO();
        vo.setPointsInsufficient(points);
        vo.setCouponInsufficient(coupon);
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

    @Test
    void cancelOrder_insufficientNotConfirmed_noWrites() {
        stubNormalOrder();
        // 仅可用积分不足（推荐奖+自购奖金冲回缺口），购物券充足
        when(userPointsService.checkReverseBalance(any()))
                .thenReturn(insufficientVO(true, false));

        RobOrderCancelDTO dto = new RobOrderCancelDTO();
        dto.setOrderId(ORDER_ID);
        Response resp = robOrderService.cancelOrder(dto);

        assertEquals(200, resp.getCode());
        PointsInsufficientVO data = (PointsInsufficientVO) resp.getData();
        assertNotNull(data);
        assertTrue(data.getPointsInsufficient());
        assertFalse(data.getCouponInsufficient());
        // 预检先于一切写操作：订单/库存/积分/审计均不得变更
        verify(robOrderMapper, never()).update(isNull(), any());
        verify(sessionProductMapper, never()).addStock(anyLong(), anyInt());
        verify(userPointsService, never()).reverse(anyLong(), anyInt(), any(), anyInt(), anyLong(), any(), any());
        verify(operateLogMapper, never()).insert(any(RobOrderOperateLog.class));
    }

    @Test
    void cancelOrder_confirmed_executesRollbackWithAuditMark() {
        stubNormalOrder();
        stubWrites();
        stubAddStock();
        when(userPointsService.checkReverseBalance(any()))
                .thenReturn(insufficientVO(true, true));

        RobOrderCancelDTO dto = new RobOrderCancelDTO();
        dto.setOrderId(ORDER_ID);
        dto.setConfirmInsufficient(true);
        Response resp = robOrderService.cancelOrder(dto);

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

        RobOrderCancelDTO dto = new RobOrderCancelDTO();
        dto.setOrderId(ORDER_ID);
        Response resp = robOrderService.cancelOrder(dto);

        assertEquals(200, resp.getCode());
        assertNull(resp.getData());
        verify(robOrderMapper).update(isNull(), any());
        verify(sessionProductMapper).addStock(SP_ID, 1);
        verify(userPointsService, times(3)).reverse(anyLong(), anyInt(), any(), anyInt(), anyLong(), any(), any());
        // 余额充足时审计备注不含确认标注（与改造前行为一致）
        ArgumentCaptor<RobOrderOperateLog> logCaptor = ArgumentCaptor.forClass(RobOrderOperateLog.class);
        verify(operateLogMapper).insert(logCaptor.capture());
        assertFalse(logCaptor.getValue().getRemark().contains("积分不足经确认强制执行"));
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
        // 仅购物券不足，可用积分充足
        when(userPointsService.checkReverseBalance(any()))
                .thenReturn(insufficientVO(false, true));

        Response resp = robOrderService.transferOrder(transferDTO(null));

        assertEquals(200, resp.getCode());
        PointsInsufficientVO data = (PointsInsufficientVO) resp.getData();
        assertNotNull(data);
        assertFalse(data.getPointsInsufficient());
        assertTrue(data.getCouponInsufficient());
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
                .thenReturn(insufficientVO(true, true));

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

        Response resp = robOrderService.transferOrder(transferDTO(null));

        assertEquals(200, resp.getCode());
        assertNull(resp.getData());
        verify(robOrderMapper).update(isNull(), any());
        verify(userPointsService, times(3)).reverse(anyLong(), anyInt(), any(), anyInt(), anyLong(), any(), any());
        verify(userPointsService, times(2)).credit(anyLong(), anyInt(), any(), anyInt(), anyLong(), any(), any());
        ArgumentCaptor<RobOrderOperateLog> logCaptor = ArgumentCaptor.forClass(RobOrderOperateLog.class);
        verify(operateLogMapper).insert(logCaptor.capture());
        assertFalse(logCaptor.getValue().getRemark().contains("积分不足经确认强制执行"));
    }
}
