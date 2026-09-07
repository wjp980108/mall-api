package com.atguigu.meet.model.entity.points;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户积分流水实体（对应 t_user_points_flow）
 * <p>
 * 每笔积分变动一行：
 * <ul>
 *   <li>{@code bizType} 业务来源：1推荐奖 2自购奖 3购物券奖 4积分对冲(转让)</li>
 *   <li>{@code flowType} 变动方向：1收入 2冲回 3转让转出 4转让转入</li>
 *   <li>{@code accountType} 计入账户：1可用积分 2购物券积分</li>
 *   <li>{@code amount} 变动金额绝对值（方向由 flowType 表达）</li>
 * </ul>
 */
@Data
@TableName("t_user_points_flow")
@Schema(description = "用户积分流水数据")
public class UserPointsFlow extends Model<UserPointsFlow> {

    @TableId(type = IdType.AUTO)
    @Schema(description = "流水ID")
    private Long id;

    /** 积分归属人ID，关联 sys_user.id */
    @Schema(description = "积分归属人ID")
    private Long userId;

    /** 关联订单ID（转让时为空） */
    @Schema(description = "关联订单ID")
    private Long orderId;

    /** 关联订单编号（冗余展示） */
    @Schema(description = "关联订单编号")
    private String orderNo;

    /** 业务类型 1推荐奖 2自购奖 3购物券奖 4积分对冲(转让)
     * @see com.atguigu.meet.enums.PointsBizType */
    @Schema(description = "业务类型 1推荐奖 2自购奖 3购物券奖 4积分对冲")
    private Integer bizType;

    /** 变动类型 1收入 2冲回 3转让转出 4转让转入
     * @see com.atguigu.meet.enums.PointsFlowType */
    @Schema(description = "变动类型 1收入 2冲回 3转让转出 4转让转入")
    private Integer flowType;

    /** 计入账户 1可用积分 2购物券积分
     * @see com.atguigu.meet.enums.PointsAccountType */
    @Schema(description = "计入账户 1可用积分 2购物券积分")
    private Integer accountType;

    /** 变动金额绝对值（方向由 flowType 表达） */
    @Schema(description = "变动金额绝对值")
    private BigDecimal amount;

    /** 变动前对应账户余额 */
    @Schema(description = "变动前余额")
    private BigDecimal beforePoints;

    /** 变动后对应账户余额 */
    @Schema(description = "变动后余额")
    private BigDecimal afterPoints;

    /** 转让对方用户ID（仅转让流水） */
    @Schema(description = "转让对方用户ID")
    private Long counterpartyUserId;

    /** 转让对方姓名（仅转让流水） */
    @Schema(description = "转让对方姓名")
    private String counterpartyName;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
