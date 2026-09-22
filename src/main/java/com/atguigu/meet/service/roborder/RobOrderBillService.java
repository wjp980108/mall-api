package com.atguigu.meet.service.roborder;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.roborder.RobOrderBillPageQueryDTO;

/**
 * 抢购订单用户账单 Service（管理端算账读模型，纯只读）
 */
public interface RobOrderBillService {

    /**
     * 按买家聚合账单分页：对选中日期及其前一天汇总资金指标。
     *
     * @param parameter 分页 + 选中日期 + 关键词
     * @return 账单分页数据
     */
    Response getBillPage(RobOrderBillPageQueryDTO parameter);
}
