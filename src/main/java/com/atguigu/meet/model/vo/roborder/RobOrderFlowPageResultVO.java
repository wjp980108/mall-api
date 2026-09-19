package com.atguigu.meet.model.vo.roborder;

import com.atguigu.meet.model.vo.PageResultVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 订单流水分页响应 VO
 * <p>
 * 在通用 {@link PageResultVO} 分页壳基础上增加 {@code totalAmount}：
 * 当前查询条件（时间区间 + 事件类型筛选）下全部匹配事件的带符号订单总额合计
 * （下单正/取消负/转移拆红冲-正向两行抵消为 0；覆盖全部匹配事件而非仅当前页；无匹配为 0）。
 * 分页字段 total/pages/current/size 以事件物理行计（转移拆两行相应增加）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "订单流水分页响应（按事件物理行分页，含筛选结果金额合计）")
public class RobOrderFlowPageResultVO extends PageResultVO<RobOrderFlowVO> {

    @Schema(description = "当前筛选条件下全部事件的带符号订单总额合计（下单正/取消负/转移两行抵消，无匹配为0）")
    private BigDecimal totalAmount = BigDecimal.ZERO;
}
