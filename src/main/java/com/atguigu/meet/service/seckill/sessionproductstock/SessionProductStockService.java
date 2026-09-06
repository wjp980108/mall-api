package com.atguigu.meet.service.seckill.sessionproductstock;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.seckill.sessionproduct.SessionProductPageQueryDTO;
import com.atguigu.meet.model.dto.seckill.sessionproductstock.SessionProductStockSetDTO;

/**
 * 场次商品库存 Service（场次商品库存模块）
 * <p>
 * 库存行级挂在 t_session_product 关联上：关联新增时随行创建，随关联删除而删除，
 * 本模块只做库存查询与设置。
 */
public interface SessionProductStockService {

    /** 库存分页列表（含场次名称 + 商品信息 + 当前库存） */
    Response getPageList(SessionProductPageQueryDTO parameter);

    /** 设置库存（按关联ID设置该场次该商品的抢购库存，绝对值） */
    Response setStock(SessionProductStockSetDTO dto);
}
