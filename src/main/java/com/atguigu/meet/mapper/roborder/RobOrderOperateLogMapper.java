package com.atguigu.meet.mapper.roborder;

import com.atguigu.meet.model.entity.roborder.RobOrderOperateLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 抢购订单操作审计日志 Mapper
 */
public interface RobOrderOperateLogMapper extends BaseMapper<RobOrderOperateLog> {

    /**
     * 订单流水取消红冲取数：一条 SQL 查同订单下单事件行(operate_type=1)的回款金额与付款金额
     * <p>取消事件行落库两值时使用；下单事件行与订单同事务先写，取消时必然存在；
     * 存量订单下单事件行两值为默认值 0。返回投影仅 receiptAmount/paymentAmount 有值，其余字段为 null。
     *
     * @param orderId 订单ID
     * @return 下单事件行金额投影（仅回款/付款金额两列）；无下单事件行时返回 null（调用方两值均按 0 处理）
     */
    @Select("SELECT receipt_amount AS receiptAmount, payment_amount AS paymentAmount "
            + "FROM t_rob_order_operate_log "
            + "WHERE order_id = #{orderId} AND operate_type = 1 "
            + "ORDER BY id DESC LIMIT 1")
    RobOrderOperateLog selectPlaceAmounts(@Param("orderId") Long orderId);
}
