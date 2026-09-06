package com.atguigu.meet.service.seckill.sessionproductstock.impl;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.mapper.seckill.sessionproduct.SessionProductMapper;
import com.atguigu.meet.model.dto.seckill.sessionproduct.SessionProductPageQueryDTO;
import com.atguigu.meet.model.dto.seckill.sessionproductstock.SessionProductStockSetDTO;
import com.atguigu.meet.model.entity.seckill.sessionproduct.SessionProduct;
import com.atguigu.meet.model.vo.PageResultVO;
import com.atguigu.meet.model.vo.seckill.sessionproduct.SessionProductVO;
import com.atguigu.meet.service.seckill.sessionproductstock.SessionProductStockService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 场次商品库存 Service 实现
 * <p>
 * 核心保障：
 * - 库存设置用定点更新（lambdaUpdate set stock），避免实体全量覆盖，后续对接抢购扣减互不干扰
 */
@Service
@Slf4j
public class SessionProductStockServiceImpl extends ServiceImpl<SessionProductMapper, SessionProduct>
        implements SessionProductStockService {

    @Override
    public Response getPageList(SessionProductPageQueryDTO parameter) {
        IPage<SessionProductVO> page = baseMapper.selectSessionProductPage(
                new Page<>(parameter.getPageNum(), parameter.getPageSize()),
                parameter.getSessionId(),
                parameter.getGoodsName());
        return Response.ok(PageResultVO.of(page));
    }

    @Override
    public Response setStock(SessionProductStockSetDTO dto) {
        SessionProduct exist = getById(dto.getId());
        if (exist == null) {
            return Response.fail(500, "场次商品关联不存在");
        }
        // 实体无内联默认值问题场景下仍用定点更新：只动 stock 一列，并发语义最清晰
        lambdaUpdate()
                .eq(SessionProduct::getId, dto.getId())
                .set(SessionProduct::getStock, dto.getStock())
                .update();
        log.info("[场次商品库存] 设置成功，id={}, sessionId={}, goodsId={}, {} -> {}",
                dto.getId(), exist.getSessionId(), exist.getGoodsId(), exist.getStock(), dto.getStock());
        return Response.ok("库存设置成功", null);
    }
}
