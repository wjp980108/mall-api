package com.atguigu.meet.service.goods.list;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.goods.list.GoodsDeleteDTO;
import com.atguigu.meet.model.dto.goods.list.GoodsPageQueryDTO;
import com.atguigu.meet.model.dto.goods.list.GoodsSaveDTO;
import com.atguigu.meet.model.dto.goods.list.GoodsStatusDTO;
import com.atguigu.meet.model.dto.goods.list.GoodsUpdateDTO;

/**
 * 商品管理 Service（商品列表模块）
 */
public interface GoodsService {

    /** 商品分页列表 */
    Response getPageList(GoodsPageQueryDTO parameter);

    /** 根据ID查商品 */
    Response getGoodsById(Long id);

    /** 新增商品 */
    Response addGoods(GoodsSaveDTO dto);

    /** 修改商品 */
    Response updateGoods(GoodsUpdateDTO dto);

    /** 商品上下架 */
    Response updateStatus(GoodsStatusDTO dto);

    /** 删除商品（逻辑删除） */
    Response deleteGoods(Long id);

    /** 批量删除商品（逻辑删除） */
    Response deleteGoodsBatch(GoodsDeleteDTO dto);

    // ====================== C 端首页接口（JWT 登录态，基于 t_goods） ======================

    /**
     * C 端首页推荐商品：已上架(status=1)商品，按销量(sales)倒序 + id 倒序
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     */
    Response recommendGoods(Integer pageNum, Integer pageSize);

    /**
     * C 端首页搜索商品：已上架(status=1) + 商品名称模糊查询，按销量(sales)倒序 + id 倒序
     *
     * @param keyword  搜索关键字（商品名称模糊匹配，空白则不过滤）
     * @param pageNum  页码
     * @param pageSize 每页条数
     */
    Response searchGoods(String keyword, Integer pageNum, Integer pageSize);
}
