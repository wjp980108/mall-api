package com.atguigu.meet.service.points;

import com.atguigu.meet.enums.PointsAccountType;
import com.atguigu.meet.mapper.points.UserPointsMapper;
import com.atguigu.meet.model.dto.points.PointsReverseItem;
import com.atguigu.meet.model.entity.points.UserPoints;
import com.atguigu.meet.model.vo.roborder.PointsInsufficientVO;
import com.atguigu.meet.service.points.impl.UserPointsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * UserPointsServiceImpl.checkReverseBalance 冲回余额预检测试
 * <p>
 * 验证 spec「取消/转移订单积分不足二次确认」：
 * 充足返回 null；不足按账户类型聚合双标志；余额为负/账户不存在判为不足。
 */
@ExtendWith(MockitoExtension.class)
class UserPointsServiceImplTest {

    @Mock
    private UserPointsMapper userPointsMapper;

    @InjectMocks
    private UserPointsServiceImpl userPointsService;

    private static final Long USER_ID = 100L;
    private static final Integer POINTS = PointsAccountType.POINTS.getCode();
    private static final Integer COUPON = PointsAccountType.COUPON.getCode();

    @Test
    void checkReverseBalance_allSufficient_returnsNull() {
        when(userPointsMapper.selectOne(any())).thenReturn(account("50.00", "30.00"));

        List<PointsReverseItem> items = Arrays.asList(
                new PointsReverseItem(USER_ID, POINTS, new BigDecimal("50.00")),
                new PointsReverseItem(USER_ID, COUPON, new BigDecimal("30.00")));

        assertNull(userPointsService.checkReverseBalance(items));
    }

    @Test
    void checkReverseBalance_onlyPointsInsufficient_pointsFlagTrueCouponFalse() {
        when(userPointsMapper.selectOne(any())).thenReturn(account("10.00", "30.00"));

        List<PointsReverseItem> items = Arrays.asList(
                new PointsReverseItem(USER_ID, POINTS, new BigDecimal("50.00")),
                new PointsReverseItem(USER_ID, COUPON, new BigDecimal("30.00")));

        PointsInsufficientVO vo = userPointsService.checkReverseBalance(items);
        assertNotNull(vo);
        assertTrue(vo.getPointsInsufficient());
        assertFalse(vo.getCouponInsufficient());
    }

    @Test
    void checkReverseBalance_bothInsufficient_bothFlagsTrue() {
        when(userPointsMapper.selectOne(any())).thenReturn(account("1.00", "1.00"));

        List<PointsReverseItem> items = Arrays.asList(
                new PointsReverseItem(USER_ID, POINTS, new BigDecimal("50.00")),
                new PointsReverseItem(USER_ID, COUPON, new BigDecimal("30.00")));

        PointsInsufficientVO vo = userPointsService.checkReverseBalance(items);
        assertNotNull(vo);
        assertTrue(vo.getPointsInsufficient());
        assertTrue(vo.getCouponInsufficient());
    }

    @Test
    void checkReverseBalance_negativeBalance_treatedAsInsufficient() {
        when(userPointsMapper.selectOne(any())).thenReturn(account("-5.00", "0.00"));

        List<PointsReverseItem> items = Collections.singletonList(
                new PointsReverseItem(USER_ID, POINTS, new BigDecimal("0.01")));

        PointsInsufficientVO vo = userPointsService.checkReverseBalance(items);
        assertNotNull(vo);
        assertTrue(vo.getPointsInsufficient());
        assertFalse(vo.getCouponInsufficient());
    }

    @Test
    void checkReverseBalance_accountMissing_treatedAsZeroBalance() {
        when(userPointsMapper.selectOne(any())).thenReturn(null);

        List<PointsReverseItem> items = Collections.singletonList(
                new PointsReverseItem(USER_ID, COUPON, new BigDecimal("0.01")));

        PointsInsufficientVO vo = userPointsService.checkReverseBalance(items);
        assertNotNull(vo);
        assertFalse(vo.getPointsInsufficient());
        assertTrue(vo.getCouponInsufficient());
    }

    @Test
    void checkReverseBalance_emptyOrNullItems_returnsNull() {
        assertNull(userPointsService.checkReverseBalance(null));
        assertNull(userPointsService.checkReverseBalance(Collections.emptyList()));
    }

    private UserPoints account(String points, String couponPoints) {
        UserPoints acct = new UserPoints();
        acct.setUserId(USER_ID);
        acct.setPoints(new BigDecimal(points));
        acct.setCouponPoints(new BigDecimal(couponPoints));
        return acct;
    }
}
