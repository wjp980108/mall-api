package com.atguigu.meet.mapper.seckill.sessionproduct;

import com.atguigu.meet.model.entity.seckill.sessionproduct.SessionProduct;
import com.atguigu.meet.model.vo.roborder.RobGoodsDetailVO;
import com.atguigu.meet.model.vo.seckill.sessionproduct.SessionProductVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 场次商品关联 Mapper
 * <p>
 * 通过 JOIN t_session / t_consign_goods 实时获取场次名称与商品信息，关联数据更新后列表/详情自动同步。
 */
public interface SessionProductMapper extends BaseMapper<SessionProduct> {

    /**
     * 分页查询场次商品关联（含场次名称 + 商品信息）
     *
     * @param page      分页参数
     * @param sessionId 场次ID（精确过滤，传 null 忽略）
     * @param goodsName 商品名称（模糊查询，传 null 忽略）
     */
    IPage<SessionProductVO> selectSessionProductPage(Page<SessionProductVO> page,
                                                     @Param("sessionId") Long sessionId,
                                                     @Param("goodsName") String goodsName);

    /**
     * 根据ID查询关联详情（含场次名称 + 商品信息）
     */
    SessionProductVO selectSessionProductById(@Param("id") Long id);

    /**
     * 查询某场次的全部关联商品（含商品信息，按 sort 升序 + 创建时间倒序）
     * <p>管理端「新增/编辑场次-关联商品」回显用。
     *
     * @param sessionId 场次ID
     */
    List<SessionProductVO> selectListBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 抢购下单条件扣减库存（原子操作，防超卖）。
     * <p>仅当关联记录存在、未删除且库存充足时扣减；受影响行数=0 即库存不足/已下架，调用方应回滚并提示。
     *
     * @param id  场次商品关联ID（t_session_product.id）
     * @param qty 购买数量
     * @return 受影响行数（1=扣减成功，0=库存不足或记录不存在）
     */
    @Update("UPDATE t_session_product SET stock = stock - #{qty} " +
            "WHERE id = #{id} AND is_deleted = 0 AND stock >= #{qty}")
    int deductStock(@Param("id") Long id, @Param("qty") Integer qty);

    /**
     * 取消订单回滚库存（把购买数量加回，锚点为下单时的关联ID）。
     *
     * @param id  场次商品关联ID
     * @param qty 回滚数量
     * @return 受影响行数
     */
    @Update("UPDATE t_session_product SET stock = stock + #{qty} " +
            "WHERE id = #{id} AND is_deleted = 0")
    int addStock(@Param("id") Long id, @Param("qty") Integer qty);

    /**
     * C 端可抢商品分页：场次开启、商品上架的场次商品（库存为 0 也展示）。
     * <p>售罄态由前端按返回的 stock 判断；抢购时间窗口/新会员提前/库存不足等均由下单接口校验提示。
     *
     * @param page      分页参数
     * @param sessionId 场次ID（传 null 查全部场次）
     */
    IPage<SessionProductVO> selectRobSaleGoodsPage(Page<SessionProductVO> page,
                                                   @Param("sessionId") Long sessionId);

    /**
     * C 端抢购商品详情：JOIN t_session / t_consign_goods，含商品详情图/富文本与场次抢购时间窗口。
     * <p>不在 SQL 过滤场次/商品状态（详情页需展示状态标志），是否可抢由 Service 层按下单校验口径计算。
     *
     * @param id 场次商品关联ID（t_session_product.id）
     */
    RobGoodsDetailVO selectRobGoodsDetailById(@Param("id") Long id);
}
