package com.atguigu.meet.mapper.order;

import com.atguigu.meet.model.entity.order.Order;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.atguigu.meet.model.vo.order.OrderVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 订单 Mapper
 */
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 分页查询订单列表（返回VO，手机号原文输出）
     */
    IPage<OrderVO> selectOrderPage(Page<OrderVO> page,
                                   @Param("orderNo") String orderNo,
                                   @Param("goodsName") String goodsName,
                                   @Param("buyerName") String buyerName,
                                   @Param("buyerPhone") String buyerPhone,
                                   @Param("sellerName") String sellerName,
                                   @Param("sellerPhone") String sellerPhone,
                                   @Param("orderStatus") Integer orderStatus,
                                   @Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime);

    /**
     * 限购统计：用户在某场次的有效抢购次数（未取消订单）
     * <p>
     * t_order 无 session_id 字段，JOIN t_consign_goods 取 session_id；
     * 已取消(status=5)订单不占用抢购次数，故排除。
     */
    @Select("SELECT COUNT(1) FROM t_order o " +
            "JOIN t_consign_goods g ON o.goods_id = g.id " +
            "WHERE o.buyer_id = #{buyerId} AND g.session_id = #{sessionId} " +
            "AND o.is_deleted = 0 AND o.order_status != 5")
    int countRushedByUserAndSession(@Param("buyerId") Long buyerId, @Param("sessionId") Long sessionId);
}
