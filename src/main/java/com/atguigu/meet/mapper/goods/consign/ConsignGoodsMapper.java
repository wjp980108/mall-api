package com.atguigu.meet.mapper.goods.consign;

import com.atguigu.meet.model.entity.goods.consign.ConsignGoods;
import com.atguigu.meet.model.vo.goods.consign.ConsignGoodsVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

/**
 * 抢购托售商品 Mapper
 * <p>
 * 通过 JOIN sys_user 实时获取委托人信息，用户表数据更新后列表/详情自动同步。
 */
public interface ConsignGoodsMapper extends BaseMapper<ConsignGoods> {

    /**
     * 分页查询抢购托售商品（含委托人信息 + 场次名称）
     *
     * @param page      分页参数
     * @param goodsName 商品名称（模糊）
     * @param memberId  委托人ID
     * @param sessionId 场次ID
     * @param goodsStatus 业务状态
     * @param entrustStatus 委托状态 0未委托 1委托代卖中
     * @param auditStatus   审核状态 0无需审核 1待审核 2通过 3驳回
     * @param onlineStatus 上下架状态
     * @param startTime 创建开始时间
     * @param endTime   创建结束时间
     */
    IPage<ConsignGoodsVO> selectConsignGoodsPage(Page<ConsignGoodsVO> page,
                                                 @Param("goodsName") String goodsName,
                                                 @Param("memberId") Long memberId,
                                                 @Param("sessionId") Long sessionId,
                                                 @Param("goodsStatus") Integer goodsStatus,
                                                 @Param("entrustStatus") Integer entrustStatus,
                                                 @Param("auditStatus") Integer auditStatus,
                                                 @Param("onlineStatus") Integer onlineStatus,
                                                 @Param("startTime") Object startTime,
                                                 @Param("endTime") Object endTime);

    /**
     * 根据ID查询抢购托售商品详情（含委托人信息 + 场次名称）
     */
    ConsignGoodsVO selectConsignGoodsById(@Param("id") Long id);

    /**
     * C 端在售抢购商品分页：上架(onlineStatus=1) + 挂卖中(goodsStatus=1)
     * + 场次开启(sessionStatus=1) + 当前时间在场次抢购时间窗口内(CURTIME() BETWEEN rush_start_time AND rush_end_time)
     * <p>含委托人信息 + 场次名，按场次排序 + 创建时间倒序。
     */
    IPage<ConsignGoodsVO> selectSaleGoodsPage(Page<ConsignGoodsVO> page);
    /**
     * 抢购托售商品：按条件更新（用于并发安全的状态推进/回滚，affectedRows 判断冲突）
     * <p>除 id/状态外其余参数传 null 表示不修改该字段。
     *
     * @param expectStatus 期望的当前 goods_status；不匹配则返回 0（并发冲突/已被其他操作修改）
     * @param memberId     委托人（确认收款时变更为买家；其余场景传 null）
     * @param entrustStatus 委托状态 0未委托 1委托代卖中（场景需要时传入）
     * @param auditStatus   审核状态 0无需审核 1待审核 2通过 3驳回（场景需要时传入）
     * @param onlineStatus  上下架 0下架 1上架（委托审核通过重新上架时传 1）
     * @return 受影响行数；0 表示条件不匹配，调用方需中止并提示用户
     */
    int updateStatusWithCondition(@Param("id") Long id,
                                  @Param("newStatus") Integer newStatus,
                                  @Param("expectStatus") Integer expectStatus,
                                  @Param("memberId") Long memberId,
                                  @Param("entrustStatus") Integer entrustStatus,
                                  @Param("auditStatus") Integer auditStatus,
                                  @Param("onlineStatus") Integer onlineStatus);

    /**
     * 委托售卖次数 SQL 层自增（原子操作，避免 Java 读-算-写引发的丢失更新）
     * @return 受影响行数
     */
    int incrementSaleTimesById(@Param("id") Long id);
}
