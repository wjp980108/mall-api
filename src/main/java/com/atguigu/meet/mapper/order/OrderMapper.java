package com.atguigu.meet.mapper.order;

import com.atguigu.meet.model.entity.order.Order;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.atguigu.meet.model.vo.order.OrderVO;
import org.apache.ibatis.annotations.Param;

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
}
