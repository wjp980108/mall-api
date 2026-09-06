package com.atguigu.meet.mapper.seckill.sessionproduct;

import com.atguigu.meet.model.entity.seckill.sessionproduct.SessionProduct;
import com.atguigu.meet.model.vo.seckill.sessionproduct.SessionProductVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 场次商品关联 Mapper
 * <p>
 * 通过 JOIN t_session / t_goods 实时获取场次名称与商品信息，关联数据更新后列表/详情自动同步。
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
}
