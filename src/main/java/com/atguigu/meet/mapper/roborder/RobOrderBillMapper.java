package com.atguigu.meet.mapper.roborder;

import com.atguigu.meet.model.vo.roborder.RobOrderBillVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 抢购订单用户账单 Mapper（管理端算账读模型，只读）
 * <p>
 * 基于 t_rob_order 快照，LEFT JOIN t_rob_order_operate_log 取下单事件（operate_type = 1）
 * 的付款/回款金额；按买家聚合今日与昨日两日的资金指标。
 */
public interface RobOrderBillMapper {

    /**
     * 按买家聚合账单分页：返回选中日期（今日）及前一日（昨日）每个有数据买家的资金指标。
     * <p>
     * 仅统计正常订单（order_status = 1），按 buyer_id / buyer_name / buyer_phone 分组，
     * 无数据买家不出现。
     *
     * @param page           分页参数
     * @param todayStart     今日开始时间（00:00:00）
     * @param todayEnd       今日结束时间（23:59:59.999）
     * @param yesterdayStart 昨日开始时间（00:00:00）
     * @param yesterdayEnd   昨日结束时间（23:59:59.999）
     * @param keyword        买家姓名/手机号模糊关键词（传 null/空忽略）
     */
    IPage<RobOrderBillVO> selectBillPage(Page<RobOrderBillVO> page,
                                         @Param("todayStart") LocalDateTime todayStart,
                                         @Param("todayEnd") LocalDateTime todayEnd,
                                         @Param("yesterdayStart") LocalDateTime yesterdayStart,
                                         @Param("yesterdayEnd") LocalDateTime yesterdayEnd,
                                         @Param("keyword") String keyword);
}
