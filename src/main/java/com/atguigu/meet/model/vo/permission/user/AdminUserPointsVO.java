package com.atguigu.meet.model.vo.permission.user;

import com.atguigu.meet.model.vo.PageResultVO;
import com.atguigu.meet.model.vo.points.PointsBalanceVO;
import com.atguigu.meet.model.vo.points.PointsFlowVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 后台用户积分详情 VO（余额 + 流水分页合并返回）
 */
@Data
@Schema(description = "后台用户积分详情（余额+流水分页）")
public class AdminUserPointsVO {

    @Schema(description = "积分余额（可用积分+购物券积分）")
    private PointsBalanceVO balance;

    @Schema(description = "积分流水分页")
    private PageResultVO<PointsFlowVO> flowPage;
}
