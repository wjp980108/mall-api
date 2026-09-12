package com.atguigu.meet.service.points.impl;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.enums.PointsAccountType;
import com.atguigu.meet.enums.PointsBizType;
import com.atguigu.meet.enums.PointsFlowType;
import com.atguigu.meet.exception.BusinessException;
import com.atguigu.meet.mapper.permission.user.UserMapper;
import com.atguigu.meet.mapper.points.UserPointsFlowMapper;
import com.atguigu.meet.mapper.points.UserPointsMapper;
import com.atguigu.meet.model.dto.points.PointsReverseItem;
import com.atguigu.meet.model.entity.permission.user.SysUser;
import com.atguigu.meet.model.entity.points.UserPoints;
import com.atguigu.meet.model.entity.points.UserPointsFlow;
import com.atguigu.meet.model.vo.PageResultVO;
import com.atguigu.meet.model.vo.points.PointsBalanceVO;
import com.atguigu.meet.model.vo.points.PointsFlowVO;
import com.atguigu.meet.model.vo.roborder.InsufficientUserVO;
import com.atguigu.meet.model.vo.roborder.PointsInsufficientVO;
import com.atguigu.meet.service.points.UserPointsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户积分账户 Service 实现
 * <p>
 * 并发策略：账户变动走 {@code SELECT ... FOR UPDATE} 行锁（账户行先 INSERT IGNORE 初始化），
 * 在调用方事务内串行化同一用户的积分变动；冲回允许负余额（平台待追回），
 * 转让用条件扣款（points &gt;= amount）防并发超转。
 */
@Service
@Slf4j
public class UserPointsServiceImpl extends ServiceImpl<UserPointsMapper, UserPoints> implements UserPointsService {

    @Autowired
    private UserPointsMapper userPointsMapper;

    @Autowired
    private UserPointsFlowMapper userPointsFlowMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public void credit(Long userId, Integer accountType, BigDecimal amount,
                       Integer bizType, Long orderId, String orderNo, String remark) {
        if (userId == null || amount == null || amount.signum() <= 0) {
            return;
        }
        UserPoints account = lockAccount(userId);
        boolean isCoupon = PointsAccountType.COUPON.getCode() == accountType;
        BigDecimal before = isCoupon ? nz(account.getCouponPoints()) : nz(account.getPoints());
        BigDecimal after = before.add(amount);

        if (isCoupon) {
            userPointsMapper.addCouponPoints(userId, amount);
        } else {
            userPointsMapper.addPoints(userId, amount);
        }
        writeFlow(userId, accountType, bizType, PointsFlowType.INCOME.getCode(),
                amount, before, after, orderId, orderNo, null, null, remark);
    }

    @Override
    public void reverse(Long userId, Integer accountType, BigDecimal amount,
                        Integer bizType, Long orderId, String orderNo, String remark) {
        if (userId == null || amount == null || amount.signum() <= 0) {
            return;
        }
        UserPoints account = lockAccount(userId);
        boolean isCoupon = PointsAccountType.COUPON.getCode() == accountType;
        BigDecimal before = isCoupon ? nz(account.getCouponPoints()) : nz(account.getPoints());
        // 冲回允许为负（用户可能已把积分转走，负余额=平台待追回）
        BigDecimal after = before.subtract(amount);

        if (isCoupon) {
            userPointsMapper.subtractCouponPointsAllowNegative(userId, amount);
        } else {
            userPointsMapper.subtractPointsAllowNegative(userId, amount);
        }
        writeFlow(userId, accountType, bizType, PointsFlowType.REVERSE.getCode(),
                amount, before, after, orderId, orderNo, null, null, remark);
    }

    /**
     * 冲回余额预检（提示性检查，无锁读）：按用户聚合待冲回金额
     * （同用户多项合并判定；调用方保证传入同一账户类型口径，抢购订单仅传可用积分项），
     * 任一用户余额小于其待冲回合计即不足，返回逐用户信息（userId/nickname/phone/不足标志）；
     * 全部充足返回 null。账户/用户不存在分别按余额 0、信息 null 处理（与冲回时 initAccount 行为衔接）。
     */
    @Override
    public PointsInsufficientVO checkReverseBalance(List<PointsReverseItem> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        Map<Long, BigDecimal> reverseByUser = new LinkedHashMap<>();
        for (PointsReverseItem item : items) {
            if (item == null || item.getUserId() == null
                    || item.getAccountType() == null
                    || item.getAmount() == null || item.getAmount().signum() <= 0) {
                continue;
            }
            reverseByUser.merge(item.getUserId(), item.getAmount(), BigDecimal::add);
        }
        if (reverseByUser.isEmpty()) {
            return null;
        }
        // 提示性检查用无锁读；账户不存在按余额 0 处理（与冲回时 initAccount 行为衔接）
        Map<Long, UserPoints> accountMap = userPointsMapper.selectList(new LambdaQueryWrapper<UserPoints>()
                        .in(UserPoints::getUserId, reverseByUser.keySet()))
                .stream()
                .collect(Collectors.toMap(UserPoints::getUserId, a -> a, (a, b) -> a));
        Map<Long, SysUser> userMap = userMapper.selectBatchIds(reverseByUser.keySet())
                .stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));

        List<InsufficientUserVO> users = new ArrayList<>();
        boolean anyInsufficient = false;
        for (Map.Entry<Long, BigDecimal> entry : reverseByUser.entrySet()) {
            UserPoints account = accountMap.get(entry.getKey());
            BigDecimal balance = account == null ? BigDecimal.ZERO : nz(account.getPoints());
            boolean insufficient = balance.compareTo(entry.getValue()) < 0;
            anyInsufficient = anyInsufficient || insufficient;
            SysUser user = userMap.get(entry.getKey());
            InsufficientUserVO vo = new InsufficientUserVO();
            vo.setUserId(entry.getKey());
            vo.setNickname(user == null ? null : pickName(user));
            vo.setPhone(user == null ? null : user.getPhone());
            vo.setPointsInsufficient(insufficient);
            users.add(vo);
        }
        if (!anyInsufficient) {
            return null;
        }
        PointsInsufficientVO result = new PointsInsufficientVO();
        result.setUsers(users);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Void> transfer(Long fromUserId, String toPhone, BigDecimal amount) {
        if (fromUserId == null) {
            return Response.fail(401, "未登录");
        }
        if (!StringUtils.hasText(toPhone)) {
            return Response.fail(500, "对方手机号不能为空");
        }
        if (amount == null || amount.signum() <= 0) {
            return Response.fail(500, "转让数量必须大于0");
        }
        // 1. 校验受让方
        SysUser toUser = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getPhone, toPhone.trim()));
        if (toUser == null) {
            return Response.fail(500, "受让用户不存在");
        }
        Long toUserId = toUser.getId();
        if (toUserId.equals(fromUserId)) {
            return Response.fail(500, "不能转让给自己");
        }
        SysUser fromUser = userMapper.selectById(fromUserId);
        if (fromUser == null) {
            return Response.fail(500, "操作人不存在");
        }

        // 2. 初始化双方账户
        userPointsMapper.initAccount(fromUserId);
        userPointsMapper.initAccount(toUserId);
        // 3. 按 userId 升序加行锁，避免双向转让死锁
        Long firstId = Math.min(fromUserId, toUserId);
        Long secondId = Math.max(fromUserId, toUserId);
        userPointsMapper.selectForUpdate(firstId);
        userPointsMapper.selectForUpdate(secondId);

        // 4. 条件扣款（仅可用积分，余额不足即失败）
        int affected = userPointsMapper.subtractPointsIfEnough(fromUserId, amount);
        if (affected == 0) {
            return Response.fail(500, "可用积分余额不足");
        }
        userPointsMapper.addPoints(toUserId, amount);

        // 5. 双方写积分对冲流水（行锁后按 user_id 取最新余额）
        String fromName = pickName(fromUser);
        String toName = pickName(toUser);
        UserPoints fromAcct = userPointsMapper.selectOne(
                new LambdaQueryWrapper<UserPoints>().eq(UserPoints::getUserId, fromUserId));
        UserPoints toAcct = userPointsMapper.selectOne(
                new LambdaQueryWrapper<UserPoints>().eq(UserPoints::getUserId, toUserId));

        writeFlow(fromUserId, PointsAccountType.POINTS.getCode(), PointsBizType.TRANSFER.getCode(),
                PointsFlowType.TRANSFER_OUT.getCode(), amount,
                nz(fromAcct.getPoints()).add(amount), nz(fromAcct.getPoints()),
                null, null, toUserId, toName, "积分转让给 " + toName);
        writeFlow(toUserId, PointsAccountType.POINTS.getCode(), PointsBizType.TRANSFER.getCode(),
                PointsFlowType.TRANSFER_IN.getCode(), amount,
                nz(toAcct.getPoints()).subtract(amount), nz(toAcct.getPoints()),
                null, null, fromUserId, fromName, "收到 " + fromName + " 转让积分");

        log.info("[积分转让] {}({}) -> {}({}), amount={}", fromName, fromUserId, toName, toUserId, amount);
        return Response.ok("转让成功", null);
    }

    @Override
    public Response<PointsBalanceVO> getBalance(Long userId) {
        if (userId == null) {
            return Response.fail(401, "未登录");
        }
        userPointsMapper.initAccount(userId);
        UserPoints account = userPointsMapper.selectOne(
                new LambdaQueryWrapper<UserPoints>().eq(UserPoints::getUserId, userId));
        PointsBalanceVO vo = new PointsBalanceVO();
        vo.setPoints(nz(account.getPoints()));
        vo.setCouponPoints(nz(account.getCouponPoints()));
        return Response.ok(vo);
    }

    @Override
    public Response<PageResultVO<PointsFlowVO>> pageFlow(Long userId, Integer bizType, Integer pageNum, Integer pageSize) {
        if (userId == null) {
            return Response.fail(401, "未登录");
        }
        Page<PointsFlowVO> page = new Page<>(pageNum, pageSize);
        IPage<PointsFlowVO> result = userPointsFlowMapper.selectFlowPage(page, userId, bizType);
        result.getRecords().forEach(vo -> {
            vo.setBizTypeName(PointsBizType.descOf(vo.getBizType()));
            vo.setFlowTypeName(PointsFlowType.descOf(vo.getFlowType()));
            vo.setAccountTypeName(PointsAccountType.descOf(vo.getAccountType()));
        });
        return Response.ok(PageResultVO.of(result));
    }

    // ====================== 私有方法 ======================

    /**
     * 初始化账户并加行锁（必须在事务内调用），返回加锁后的账户
     */
    private UserPoints lockAccount(Long userId) {
        userPointsMapper.initAccount(userId);
        UserPoints account = userPointsMapper.selectForUpdate(userId);
        if (account == null) {
            // 理论不可能（initAccount 后立即上锁），兜底抛异常触发回滚
            throw new BusinessException("积分账户初始化失败：userId=" + userId);
        }
        return account;
    }

    /**
     * 写一条积分流水
     */
    private void writeFlow(Long userId, Integer accountType, Integer bizType, Integer flowType,
                           BigDecimal amount, BigDecimal before, BigDecimal after,
                           Long orderId, String orderNo,
                           Long counterpartyUserId, String counterpartyName, String remark) {
        UserPointsFlow flow = new UserPointsFlow();
        flow.setUserId(userId);
        flow.setOrderId(orderId);
        flow.setOrderNo(orderNo);
        flow.setBizType(bizType);
        flow.setFlowType(flowType);
        flow.setAccountType(accountType);
        flow.setAmount(amount);
        flow.setBeforePoints(before);
        flow.setAfterPoints(after);
        flow.setCounterpartyUserId(counterpartyUserId);
        flow.setCounterpartyName(counterpartyName);
        flow.setRemark(remark);
        userPointsFlowMapper.insert(flow);
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
