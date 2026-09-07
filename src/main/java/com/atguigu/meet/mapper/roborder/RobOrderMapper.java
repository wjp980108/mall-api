package com.atguigu.meet.mapper.roborder;

import com.atguigu.meet.model.entity.roborder.RobOrder;
import com.atguigu.meet.model.vo.roborder.RobOrderVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 抢购订单 Mapper
 */
public interface RobOrderMapper extends BaseMapper<RobOrder> {

    /**
     * 分页查询抢购订单（管理端全量 / C端按 buyerId 归属）。
     * 全部字段为下单快照，无需 JOIN；关键词匹配买家姓名/手机号/买家ID/商品名称。
     *
     * @param page        分页参数
     * @param buyerId     买家ID（C端我的订单传，管理端传 null）
     * @param sessionId   场次ID（传 null 忽略）
     * @param keyword     姓名/手机号/用户ID/商品名关键词（传 null 忽略）
     * @param amount      订单总额模糊匹配（传 null 忽略）
     * @param orderStatus 订单状态（传 null 查全部）
     * @param startTime   下单开始时间（传 null 忽略）
     * @param endTime     下单结束时间（传 null 忽略）
     */
    IPage<RobOrderVO> selectRobOrderPage(Page<RobOrderVO> page,
                                         @Param("buyerId") Long buyerId,
                                         @Param("sessionId") Long sessionId,
                                         @Param("keyword") String keyword,
                                         @Param("amount") String amount,
                                         @Param("orderStatus") Integer orderStatus,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    /**
     * 统计某用户在某场次的正常订单数（limit_rule=1 同场次限购一次）
     */
    int countRushedByUserAndSession(@Param("buyerId") Long buyerId, @Param("sessionId") Long sessionId);

    /**
     * 统计某用户当天的正常订单数（limit_rule=2 当天限购一次）
     */
    int countRushedByUserAndDate(@Param("buyerId") Long buyerId,
                                 @Param("startTime") LocalDateTime startTime,
                                 @Param("endTime") LocalDateTime endTime);
}
