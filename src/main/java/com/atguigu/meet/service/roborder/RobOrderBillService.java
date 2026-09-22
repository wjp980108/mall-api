package com.atguigu.meet.service.roborder;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.roborder.RobOrderBillExportDTO;
import com.atguigu.meet.model.dto.roborder.RobOrderBillPageQueryDTO;
import jakarta.servlet.http.HttpServletResponse;

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

    /**
     * 导出抢购订单用户账单 PDF。
     * <p>
     * 按与列表接口相同的日期与关键词条件查询全量数据，并直接把 PDF 字节写入响应流。
     *
     * @param parameter 选中日期 + 关键词
     * @param response  HTTP 响应流
     */
    void exportRobOrderBillsPdf(RobOrderBillExportDTO parameter, HttpServletResponse response);
}
