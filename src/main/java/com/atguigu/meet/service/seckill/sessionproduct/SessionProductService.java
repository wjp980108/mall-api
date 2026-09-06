package com.atguigu.meet.service.seckill.sessionproduct;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.seckill.sessionproduct.SessionProductBatchSaveDTO;
import com.atguigu.meet.model.dto.seckill.sessionproduct.SessionProductPageQueryDTO;
import com.atguigu.meet.model.dto.seckill.sessionproduct.SessionProductUpdateDTO;

/**
 * 场次商品关联 Service（场次关联抢购商品模块）
 */
public interface SessionProductService {

    /** 分页列表（含场次名称 + 商品信息） */
    Response getPageList(SessionProductPageQueryDTO parameter);

    /** 根据ID查关联详情 */
    Response getSessionProductById(Long id);

    /** 查询某场次的全部关联商品（管理端场次编辑回显用） */
    Response listBySessionId(Long sessionId);

    /**
     * 批量新增关联（一个场次一次关联 n 个商品，每个商品指定库存）
     * <p>已存在的关联自动跳过并提示跳过数量，未冲突的正常新增。
     */
    Response batchAddSessionProduct(SessionProductBatchSaveDTO dto);

    /** 修改关联（可改场次/商品/库存/排序） */
    Response updateSessionProduct(SessionProductUpdateDTO dto);

    /** 删除关联（逻辑删除） */
    Response deleteSessionProduct(Long id);
}
