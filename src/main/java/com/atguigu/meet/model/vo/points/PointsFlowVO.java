package com.atguigu.meet.model.vo.points;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 积分明细 VO
 */
@Data
@Schema(description = "积分明细数据")
public class PointsFlowVO {

    @Schema(description = "流水ID")
    private Long id;

    @Schema(description = "关联订单ID")
    private Long orderId;

    @Schema(description = "关联订单编号")
    private String orderNo;

    /** 业务类型 1推荐奖 2自购奖 3购物券奖 4积分对冲 */
    @Schema(description = "业务类型 1推荐奖 2自购奖 3购物券奖 4积分对冲")
    private Integer bizType;

    /** 业务类型中文名（Service 层组装） */
    @Schema(description = "业务类型中文名")
    private String bizTypeName;

    /** 变动类型 1收入 2冲回 3转让转出 4转让转入 */
    @Schema(description = "变动类型 1收入 2冲回 3转让转出 4转让转入")
    private Integer flowType;

    /** 变动类型中文名（Service 层组装） */
    @Schema(description = "变动类型中文名")
    private String flowTypeName;

    /** 计入账户 1可用积分 2购物券积分 */
    @Schema(description = "计入账户 1可用积分 2购物券积分")
    private Integer accountType;

    /** 计入账户中文名（Service 层组装） */
    @Schema(description = "计入账户中文名")
    private String accountTypeName;

    /** 变动金额绝对值（方向由 flowType 表达） */
    @Schema(description = "变动金额绝对值")
    private BigDecimal amount;

    /** 变动前余额 */
    @Schema(description = "变动前余额")
    private BigDecimal beforePoints;

    /** 变动后余额 */
    @Schema(description = "变动后余额")
    private BigDecimal afterPoints;

    /** 转让对方用户ID */
    @Schema(description = "转让对方用户ID")
    private Long counterpartyUserId;

    /** 转让对方姓名 */
    @Schema(description = "转让对方姓名")
    private String counterpartyName;

    @Schema(description = "备注")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
