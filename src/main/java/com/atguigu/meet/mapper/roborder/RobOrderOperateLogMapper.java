package com.atguigu.meet.mapper.roborder;

import com.atguigu.meet.model.entity.roborder.RobOrderOperateLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * 抢购订单操作审计日志 Mapper
 */
public interface RobOrderOperateLogMapper extends BaseMapper<RobOrderOperateLog> {

    /**
     * 订单流水取消红冲取数：查同订单下单事件行(operate_type=1)的回款金额
     * <p>取消事件行落库回款时使用；下单事件行与订单同事务先写，取消时必然存在；
     * 存量订单下单事件行回款为默认值 0。
     *
     * @param orderId 订单ID
     * @return 下单事件行回款金额；无下单事件行时返回 null（调用方按 0 处理）
     */
    @Select("SELECT receipt_amount FROM t_rob_order_operate_log "
            + "WHERE order_id = #{orderId} AND operate_type = 1 "
            + "ORDER BY id DESC LIMIT 1")
    BigDecimal selectPlaceReceiptAmount(@Param("orderId") Long orderId);
}
