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
import com.atguigu.meet.model.entity.general.settings.SysSettings;
import com.atguigu.meet.model.entity.goods.consign.ConsignGoods;
import com.atguigu.meet.model.entity.permission.user.SysUser;
import com.atguigu.meet.model.entity.roborder.RobOrder;
import com.atguigu.meet.model.entity.seckill.session.Session;
import com.atguigu.meet.model.entity.seckill.sessionproduct.SessionProduct;
import com.atguigu.meet.model.entity.user.UserAddress;
import com.atguigu.meet.model.vo.seckill.sessionproduct.SessionProductVO;
import com.atguigu.meet.service.general.settings.SysSettingsService;
import com.atguigu.meet.service.points.UserPointsService;
import com.atguigu.meet.service.roborder.impl.RobOrderServiceImpl;
import com.atguigu.meet.service.user.UserAddressService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RobOrderServiceImpl.placeOrder 收货地址校验与快照测试
 * <p>
 * 验证 spec「抢购订单收货地址快照」：
 * 地址归属校验先于库存扣减（无效地址不扣库存不建单）；
 * 有效地址下单时收货人姓名/手机号/地址三列快照取自地址实体。
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
}
