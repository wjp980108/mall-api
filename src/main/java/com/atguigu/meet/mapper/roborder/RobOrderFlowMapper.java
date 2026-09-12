package com.atguigu.meet.mapper.roborder;

import com.atguigu.meet.model.vo.roborder.RobOrderFlowSummaryVO;
import com.atguigu.meet.model.vo.roborder.RobOrderFlowVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 订单流水 Mapper（算账读模型，只读）
 * <p>
 * 以 t_rob_order_operate_log 事件为主表，JOIN t_rob_order 取下单冻结快照金额。
 */
public interface RobOrderFlowMapper {

    /**
     * 事件流水分页：按事件发生时间区间过滤，金额在 SQL 内带符号（下单正/取消负/转移0）。
     *
     * @param page        分页参数
     * @param operateType 事件类型（传 null 查全部：1下单 2取消 3转移）
     * @param startTime   事件起始时间（传 null 忽略）
     * @param endTime     事件结束时间（传 null 忽略）
     */
    IPage<RobOrderFlowVO> selectFlowPage(Page<RobOrderFlowVO> page,
                                         @Param("operateType") Integer operateType,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    /**
     * 区间汇总：仅下单(1)/取消(2)事件参与条件聚合，返回成交/红冲两组原始指标（净额由 Service 计算）。
     * 无 GROUP BY，空区间也返回一行全 0。
     */
    RobOrderFlowSummaryVO selectFlowSummary(@Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);
}
