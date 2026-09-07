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
 * 用户积分账户实体（对应 t_user_points）
 * <p>
 * 双余额：
 * <ul>
 *   <li>{@code points} 可用积分 = 推荐奖 + 自购奖金，<b>可转让</b></li>
 *   <li>{@code couponPoints} 购物券积分 = 自购返券，<b>锁死不可转让</b></li>
 * </ul>
 * 每个用户一行（uk_user_id）；账户行不存在时由业务 INSERT IGNORE 初始化。
 * 余额变动走行锁（SELECT ... FOR UPDATE）在订单事务内串行化，冲回允许负余额。
 */
@Data
@TableName("t_user_points")
@Schema(description = "用户积分账户数据")
public class UserPoints extends Model<UserPoints> {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    /** 用户ID，关联 sys_user.id */
    @Schema(description = "用户ID")
    private Long userId;

    /** 可用积分（推荐奖+自购奖金），可转让 */
    @Schema(description = "可用积分（推荐奖+自购奖金），可转让")
    private BigDecimal points;

    /** 购物券积分（自购返券），锁死不可转让 */
    @Schema(description = "购物券积分（自购返券），锁死不可转让")
    private BigDecimal couponPoints;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
