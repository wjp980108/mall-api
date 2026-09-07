package com.atguigu.meet.service.points;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.vo.points.PointsBalanceVO;
import com.atguigu.meet.model.vo.points.PointsFlowVO;
import com.atguigu.meet.model.vo.PageResultVO;

import java.math.BigDecimal;

/**
 * 用户积分账户 Service
 * <p>
 * 发放/冲回/转让均要求在调用方的订单/转让事务内执行（行锁 + 同事务原子性）。
 */
public interface UserPointsService {

    /**
     * 发放积分（收入）。账户不存在自动初始化；行锁内更新余额并写收入流水。
     *
     * @param userId      收款用户ID
     * @param accountType 计入账户：1可用积分 2购物券积分
     * @param amount      金额（正数）
     * @param bizType     业务类型：1推荐奖 2自购奖 3购物券奖
     * @param orderId     关联订单ID（可空）
     * @param orderNo     关联订单号（可空）
     * @param remark      备注（可空）
     */
    void credit(Long userId, Integer accountType, BigDecimal amount,
                Integer bizType, Long orderId, String orderNo, String remark);

    /**
     * 冲回积分（取消/转移收回）。允许冲回后余额为负（平台待追回语义），行锁内更新并写冲回流水。
     *
     * @param userId      被扣回用户ID
     * @param accountType 扣回账户：1可用积分 2购物券积分
     * @param amount      金额（正数，扣回的绝对值）
     * @param bizType     业务类型：1推荐奖 2自购奖 3购物券奖
     * @param orderId     关联订单ID（可空）
     * @param orderNo     关联订单号（可空）
     * @param remark      备注（可空）
     */
    void reverse(Long userId, Integer accountType, BigDecimal amount,
                 Integer bizType, Long orderId, String orderNo, String remark);

    /**
     * 积分转让：按对方手机号转出可用积分（购物券积分不参与）。
     * <p>同事务内条件扣款（余额不足即失败回滚）+ 对方加款 + 双方写积分对冲流水。
     *
     * @param fromUserId 转出用户ID
     * @param toPhone    受让方手机号
     * @param amount     转让数量（正数）
     */
    Response<Void> transfer(Long fromUserId, String toPhone, BigDecimal amount);

    /**
     * 查询我的积分余额（账户不存在返回双 0）
     */
    Response<PointsBalanceVO> getBalance(Long userId);

    /**
     * 分页查询我的积分明细（可按业务类型筛选）
     */
    Response<PageResultVO<PointsFlowVO>> pageFlow(Long userId, Integer bizType, Integer pageNum, Integer pageSize);
}
