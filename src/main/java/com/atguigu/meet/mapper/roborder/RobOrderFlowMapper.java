package com.atguigu.meet.mapper.roborder;

import com.atguigu.meet.model.vo.roborder.RobOrderFlowSummaryVO;
import com.atguigu.meet.model.vo.roborder.RobOrderFlowVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单流水 Mapper（算账读模型，只读）
 * <p>
 * 以 t_rob_order_operate_log 事件为主表，JOIN t_rob_order 取下单冻结快照金额。
 * 分页为单层拆行行级分页：{@link #selectFlowPage} 直接对事件物理行分页
 * （转移事件拆红冲/正向两行），全局按事件时间倒序、同刻正向行在红冲行之前。
 */
public interface RobOrderFlowMapper {

    /**
     * 事件流水分页：下单/取消事件各一行，转移事件拆红冲(n=1)/正向(n=2)两行，分页单位=事件物理行。
     * 排序固定为事件时间倒序、日志 ID 倒序、拆行序号倒序——同一转移事件正向行(+)在红冲行(-)之前。
     * 无 GROUP BY，分页 count 为拆行行数（转移翻倍）。
     *
     * @param page        分页参数（页大小=事件行数）
     * @param operateType 事件类型（传 null 查全部：1下单 2取消 3转移）
     * @param startTime   事件起始时间（传 null 忽略）
     * @param endTime     事件结束时间（传 null 忽略）
     * @param keyword     买家模糊查询关键词（姓名/手机号/买家ID，按事件行展示买家匹配；传 null/空忽略）
     */
    IPage<RobOrderFlowVO> selectFlowPage(Page<RobOrderFlowVO> page,
                                         @Param("operateType") Integer operateType,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime,
                                         @Param("keyword") String keyword);

    /**
     * 当前筛选条件（与分页同条件，含 operateType、keyword）下全部匹配事件的带符号金额合计：
     * 下单正额、取消负额、转移拆 -X/+X 两行抵消为 0；无匹配返回 0（SQL COALESCE 兜底）。
     */
    BigDecimal selectFlowTotalAmount(@Param("operateType") Integer operateType,
                                     @Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime,
                                     @Param("keyword") String keyword);

    /**
     * 区间汇总：下单(1)/转移(3)事件进成交组、取消(2)/转移(3)事件进红冲组，返回成交/红冲两组原始指标（净额由 Service 计算）。
     * 无 GROUP BY，空区间也返回一行全 0。
     */
    RobOrderFlowSummaryVO selectFlowSummary(@Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);
}
