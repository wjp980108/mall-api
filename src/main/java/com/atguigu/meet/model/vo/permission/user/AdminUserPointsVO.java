package com.atguigu.meet.model.vo.permission.user;

import com.atguigu.meet.model.vo.PageResultVO;
import com.atguigu.meet.model.vo.points.PointsBalanceVO;
import com.atguigu.meet.model.vo.points.PointsFlowVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 后台用户积分详情 VO（分页壳最外层 + 余额附加字段）
 * <p>流水分页字段（list/total/pages/current/size）平铺最外层，与通用分页契约一致；
 * 余额以 {@code balance} 字段附加在分页外层（对齐 {@link com.atguigu.meet.model.vo.roborder.RobOrderFlowPageResultVO} 惯例）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "后台用户积分详情（流水分页平铺最外层，余额以 balance 字段附加）")
public class AdminUserPointsVO extends PageResultVO<PointsFlowVO> {

    @Schema(description = "积分余额（points 可用积分 + couponPoints 购物券积分；只读查询，不初始化账户）")
    private PointsBalanceVO balance;
}
