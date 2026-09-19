package com.atguigu.meet.mapper.roborder;

import com.atguigu.meet.model.vo.roborder.RobOrderFlowGroupVO;
import com.atguigu.meet.model.vo.roborder.RobOrderFlowSummaryVO;
import com.atguigu.meet.model.vo.roborder.RobOrderFlowVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单流水 Mapper（算账读模型，只读）
 * <p>
 * 以 t_rob_order_operate_log 事件为主表，JOIN t_rob_order 取下单冻结快照金额。
 * 分页采用两层查询：{@link #selectFlowOrderGroups} 按订单组分页，
 * {@link #selectFlowRowsByOrderIds} 整组取事件行（转移事件拆红冲/正向两行），由 Service 按组序拼装平铺列表。
 */
public interface RobOrderFlowMapper {

    /**
     * 第 1 层·订单组分页：在时间区间/事件类型筛选下，至少含一条匹配事件的订单为一组。
     * 组间按组内最新匹配事件时间倒序、最新事件日志 ID 倒序；分页 total 为订单组数（转移拆行不翻倍）。
     *
     * @param page        分页参数（页大小=订单组数）
     * @param operateType 事件类型（传 null 查全部：1下单 2取消 3转移）
     * @param startTime   事件起始时间（传 null 忽略）
     * @param endTime     事件结束时间（传 null 忽略）
     */
    IPage<RobOrderFlowGroupVO> selectFlowOrderGroups(Page<RobOrderFlowGroupVO> page,
                                                     @Param("operateType") Integer operateType,
                                                     @Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 第 2 层·按订单组整组取事件行：沿用与组分页完全相同的时间/类型筛选并限定 orderIds，
     * 转移事件拆两行；组内按事件时间正序、日志 ID 正序、红冲(n=1)在前正向(n=2)在后。
     * 组间顺序不由本查询保证，由 Service 按第 1 层 orderId 顺序拼装。
     *
     * @param orderIds    当页订单组 ID 集合（调用方须保证非空）
     * @param operateType 事件类型（须与组分页同参，传 null 查全部）
     * @param startTime   事件起始时间（须与组分页同参，传 null 忽略）
     * @param endTime     事件结束时间（须与组分页同参，传 null 忽略）
     */
    List<RobOrderFlowVO> selectFlowRowsByOrderIds(@Param("orderIds") List<Long> orderIds,
                                                  @Param("operateType") Integer operateType,
                                                  @Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);

    /**
     * 当前筛选条件（与分页同条件，含 operateType）下全部匹配事件的带符号金额合计：
     * 下单正额、取消负额、转移拆 -X/+X 两行抵消为 0；无匹配返回 0（SQL COALESCE 兜底）。
     */
    BigDecimal selectFlowTotalAmount(@Param("operateType") Integer operateType,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 区间汇总：下单(1)/转移(3)事件进成交组、取消(2)/转移(3)事件进红冲组，返回成交/红冲两组原始指标（净额由 Service 计算）。
     * 无 GROUP BY，空区间也返回一行全 0。
     */
    RobOrderFlowSummaryVO selectFlowSummary(@Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);
}
