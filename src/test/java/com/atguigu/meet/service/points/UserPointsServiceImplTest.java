package com.atguigu.meet.service.points;

import com.atguigu.meet.enums.PointsAccountType;
import com.atguigu.meet.mapper.permission.user.UserMapper;
import com.atguigu.meet.mapper.points.UserPointsMapper;
import com.atguigu.meet.model.dto.points.PointsReverseItem;
import com.atguigu.meet.model.entity.permission.user.SysUser;
import com.atguigu.meet.model.entity.points.UserPoints;
import com.atguigu.meet.model.vo.roborder.InsufficientUserVO;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * UserPointsServiceImpl.checkReverseBalance 冲回余额预检测试
 * <p>
 * 验证 spec「取消/转移订单积分不足二次确认」：充足返回 null；不足按用户聚合逐个返回
 * userId/nickname/phone/pointsInsufficient；余额为负/账户不存在判为不足；用户不存在信息置 null。
 */
@ExtendWith(MockitoExtension.class)
class UserPointsServiceImplTest {

    @Mock
    private UserPointsMapper userPointsMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserPointsServiceImpl userPointsService;

    private static final Long INVITER_ID = 100L;
    private static final Long BUYER_ID = 200L;
    private static final Integer POINTS = PointsAccountType.POINTS.getCode();

    @Test
    void checkReverseBalance_allSufficient_returnsNull() {
        mockAccounts(account(INVITER_ID, "50.00"));
        mockUsers(user(INVITER_ID, "张三", "13800000001"));

        List<PointsReverseItem> items = Collections.singletonList(
                new PointsReverseItem(INVITER_ID, POINTS, new BigDecimal("50.00")));

        assertNull(userPointsService.checkReverseBalance(items));
    }

    @Test
    void checkReverseBalance_singleUserInsufficient_returnsUserWithFlag() {
        mockAccounts(account(INVITER_ID, "10.00"));
        mockUsers(user(INVITER_ID, "张三", "13800000001"));

        List<PointsReverseItem> items = Collections.singletonList(
                new PointsReverseItem(INVITER_ID, POINTS, new BigDecimal("50.00")));

        PointsInsufficientVO vo = userPointsService.checkReverseBalance(items);
        assertNotNull(vo);
        assertEquals(1, vo.getUsers().size());
        InsufficientUserVO u = vo.getUsers().get(0);
        assertEquals(INVITER_ID, u.getUserId());
        assertEquals("张三", u.getNickname());
        assertEquals("13800000001", u.getPhone());
        assertTrue(u.getPointsInsufficient());
    }

    @Test
    void checkReverseBalance_bothUsersInsufficient_bothFlagsTrue() {
        mockAccounts(account(INVITER_ID, "1.00"), account(BUYER_ID, "0.00"));
        mockUsers(user(INVITER_ID, "张三", "13800000001"), user(BUYER_ID, "李四", "13800000002"));

        List<PointsReverseItem> items = Arrays.asList(
                new PointsReverseItem(INVITER_ID, POINTS, new BigDecimal("50.00")),
                new PointsReverseItem(BUYER_ID, POINTS, new BigDecimal("30.00")));

        PointsInsufficientVO vo = userPointsService.checkReverseBalance(items);
        assertNotNull(vo);
        assertEquals(2, vo.getUsers().size());
        assertTrue(vo.getUsers().get(0).getPointsInsufficient());
        assertEquals(INVITER_ID, vo.getUsers().get(0).getUserId());
        assertTrue(vo.getUsers().get(1).getPointsInsufficient());
        assertEquals(BUYER_ID, vo.getUsers().get(1).getUserId());
    }

    @Test
    void checkReverseBalance_partialUsersInsufficient_sufficientUserFlagFalse() {
        mockAccounts(account(INVITER_ID, "100.00"), account(BUYER_ID, "1.00"));
        mockUsers(user(INVITER_ID, "张三", "13800000001"), user(BUYER_ID, "李四", "13800000002"));

        List<PointsReverseItem> items = Arrays.asList(
                new PointsReverseItem(INVITER_ID, POINTS, new BigDecimal("50.00")),
                new PointsReverseItem(BUYER_ID, POINTS, new BigDecimal("30.00")));

        PointsInsufficientVO vo = userPointsService.checkReverseBalance(items);
        assertNotNull(vo);
        assertEquals(2, vo.getUsers().size());
        assertFalse(vo.getUsers().get(0).getPointsInsufficient());
        assertTrue(vo.getUsers().get(1).getPointsInsufficient());
    }

    @Test
    void checkReverseBalance_sameUserMultipleItems_aggregatedBeforeCompare() {
        mockAccounts(account(BUYER_ID, "50.00"));
        mockUsers(user(BUYER_ID, "李四", "13800000002"));

        List<PointsReverseItem> items = Arrays.asList(
                new PointsReverseItem(BUYER_ID, POINTS, new BigDecimal("30.00")),
                new PointsReverseItem(BUYER_ID, POINTS, new BigDecimal("25.00")));

        PointsInsufficientVO vo = userPointsService.checkReverseBalance(items);
        assertNotNull(vo);
        assertEquals(1, vo.getUsers().size());
        assertTrue(vo.getUsers().get(0).getPointsInsufficient());
    }

    @Test
    void checkReverseBalance_negativeBalance_treatedAsInsufficient() {
        mockAccounts(account(BUYER_ID, "-5.00"));
        mockUsers(user(BUYER_ID, "李四", "13800000002"));

        List<PointsReverseItem> items = Collections.singletonList(
                new PointsReverseItem(BUYER_ID, POINTS, new BigDecimal("0.01")));

        PointsInsufficientVO vo = userPointsService.checkReverseBalance(items);
        assertNotNull(vo);
        assertTrue(vo.getUsers().get(0).getPointsInsufficient());
    }

    @Test
    void checkReverseBalance_accountMissing_treatedAsZeroBalance() {
        when(userPointsMapper.selectList(any())).thenReturn(Collections.emptyList());
        mockUsers(user(BUYER_ID, "李四", "13800000002"));

        List<PointsReverseItem> items = Collections.singletonList(
                new PointsReverseItem(BUYER_ID, POINTS, new BigDecimal("0.01")));

        PointsInsufficientVO vo = userPointsService.checkReverseBalance(items);
        assertNotNull(vo);
        assertTrue(vo.getUsers().get(0).getPointsInsufficient());
    }

    @Test
    void checkReverseBalance_userMissing_keepsUserIdWithNullInfo() {
        mockAccounts(account(BUYER_ID, "1.00"));
        when(userMapper.selectBatchIds(any())).thenReturn(Collections.emptyList());

        List<PointsReverseItem> items = Collections.singletonList(
                new PointsReverseItem(BUYER_ID, POINTS, new BigDecimal("5.00")));

        PointsInsufficientVO vo = userPointsService.checkReverseBalance(items);
        assertNotNull(vo);
        InsufficientUserVO u = vo.getUsers().get(0);
        assertEquals(BUYER_ID, u.getUserId());
        assertNull(u.getNickname());
        assertNull(u.getPhone());
        assertTrue(u.getPointsInsufficient());
    }

    @Test
    void checkReverseBalance_emptyOrNullItems_returnsNull() {
        assertNull(userPointsService.checkReverseBalance(null));
        assertNull(userPointsService.checkReverseBalance(Collections.emptyList()));
    }

    @Test
    void checkReverseBalance_allItemsInvalid_returnsNull() {
        List<PointsReverseItem> items = Arrays.asList(
                null,
                new PointsReverseItem(null, POINTS, new BigDecimal("50.00")),
                new PointsReverseItem(BUYER_ID, POINTS, BigDecimal.ZERO));

        assertNull(userPointsService.checkReverseBalance(items));
    }

    private void mockAccounts(UserPoints... accounts) {
        when(userPointsMapper.selectList(any())).thenReturn(Arrays.asList(accounts));
    }

    private void mockUsers(SysUser... users) {
        lenient().when(userMapper.selectBatchIds(any())).thenReturn(Arrays.asList(users));
    }

    private UserPoints account(Long userId, String points) {
        UserPoints acct = new UserPoints();
        acct.setUserId(userId);
        acct.setPoints(new BigDecimal(points));
        acct.setCouponPoints(BigDecimal.ZERO);
        return acct;
    }

    private SysUser user(Long userId, String nickname, String phone) {
        SysUser u = new SysUser();
        u.setId(userId);
        u.setNickname(nickname);
        u.setPhone(phone);
        return u;
    }
}
