package com.atguigu.meet.mapper.points;

import com.atguigu.meet.model.entity.points.UserPoints;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 用户积分账户 Mapper
 * <p>
 * 并发策略：账户变动走 {@code SELECT ... FOR UPDATE} 行锁，在订单/转让事务内串行化同一用户的积分变动；
 * 账户行不存在时先 INSERT IGNORE 初始化再上锁。
 */
public interface UserPointsMapper extends BaseMapper<UserPoints> {

    /**
     * 初始化积分账户（幂等，已存在则忽略）
     *
     * @param userId 用户ID
     * @return 受影响行数（首次插入=1，已存在=0）
     */
    @Update("INSERT IGNORE INTO t_user_points(user_id, points, coupon_points) " +
            "VALUES(#{userId}, 0, 0)")
    int initAccount(@Param("userId") Long userId);

    /**
     * 行锁查询账户（必须在事务内调用；调用前需先 {@link #initAccount}）
     *
     * @param userId 用户ID
     * @return 加行锁后的账户记录
     */
    @Select("SELECT id, user_id, points, coupon_points, create_time, update_time " +
            "FROM t_user_points WHERE user_id = #{userId} FOR UPDATE")
    UserPoints selectForUpdate(@Param("userId") Long userId);

    /**
     * 增加可用积分（原子加）
     */
    @Update("UPDATE t_user_points SET points = points + #{amount} WHERE user_id = #{userId}")
    int addPoints(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 增加购物券积分（原子加）
     */
    @Update("UPDATE t_user_points SET coupon_points = coupon_points + #{amount} WHERE user_id = #{userId}")
    int addCouponPoints(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 扣减可用积分（无条件，允许扣成负数——用于取消/转移冲回，平台待追回语义）
     */
    @Update("UPDATE t_user_points SET points = points - #{amount} WHERE user_id = #{userId}")
    int subtractPointsAllowNegative(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 扣减购物券积分（无条件，允许扣成负数——用于取消/转移冲回）
     */
    @Update("UPDATE t_user_points SET coupon_points = coupon_points - #{amount} WHERE user_id = #{userId}")
    int subtractCouponPointsAllowNegative(@Param("userId") Long userId, @Param("amount") BigDecimal amount);

    /**
     * 条件扣减可用积分（仅当余额充足时扣减——用于积分转让）
     *
     * @return 受影响行数；0 表示余额不足，调用方应回滚并提示
     */
    @Update("UPDATE t_user_points SET points = points - #{amount} " +
            "WHERE user_id = #{userId} AND points >= #{amount}")
    int subtractPointsIfEnough(@Param("userId") Long userId, @Param("amount") BigDecimal amount);
}
