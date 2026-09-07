package com.atguigu.meet.service.seckill.sessionproduct.impl;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.mapper.goods.consign.ConsignGoodsMapper;
import com.atguigu.meet.mapper.seckill.session.SessionMapper;
import com.atguigu.meet.mapper.seckill.sessionproduct.SessionProductMapper;
import com.atguigu.meet.model.dto.seckill.sessionproduct.SessionProductBatchSaveDTO;
import com.atguigu.meet.model.dto.seckill.sessionproduct.SessionProductPageQueryDTO;
import com.atguigu.meet.model.dto.seckill.sessionproduct.SessionProductUpdateDTO;
import com.atguigu.meet.model.entity.goods.consign.ConsignGoods;
import com.atguigu.meet.model.entity.seckill.session.Session;
import com.atguigu.meet.model.entity.seckill.sessionproduct.SessionProduct;
import com.atguigu.meet.model.vo.PageResultVO;
import com.atguigu.meet.model.vo.seckill.sessionproduct.SessionProductVO;
import com.atguigu.meet.service.seckill.sessionproduct.SessionProductService;
import com.atguigu.meet.utils.BeanConvertUtils;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 场次商品关联 Service 实现
 * <p>
 * 核心保障：
 * - 关联有效性：新增/修改时校验场次与商品均存在（逻辑删除的视为不存在）
 * - 唯一性：同场次同商品仅一条有效关联（逻辑删除后允许重新关联）
 */
@Service
@Slf4j
public class SessionProductServiceImpl extends ServiceImpl<SessionProductMapper, SessionProduct>
        implements SessionProductService {

    @Autowired
    private SessionMapper sessionMapper;

    @Autowired
    private ConsignGoodsMapper consignGoodsMapper;

    @Override
    public Response getPageList(SessionProductPageQueryDTO parameter) {
        IPage<SessionProductVO> page = baseMapper.selectSessionProductPage(
                new Page<>(parameter.getPageNum(), parameter.getPageSize()),
                parameter.getSessionId(),
                parameter.getGoodsName());
        return Response.ok(PageResultVO.of(page));
    }

    @Override
    public Response getSessionProductById(Long id) {
        SessionProductVO vo = baseMapper.selectSessionProductById(id);
        if (vo == null) {
            return Response.fail(500, "场次商品关联不存在");
        }
        return Response.ok(vo);
    }

    @Override
    public Response listBySessionId(Long sessionId) {
        if (sessionMapper.selectById(sessionId) == null) {
            return Response.fail(500, "场次不存在");
        }
        return Response.ok(baseMapper.selectListBySessionId(sessionId));
    }

    @Override
    public Response batchAddSessionProduct(SessionProductBatchSaveDTO dto) {
        Long sessionId = dto.getSessionId();
        // 场次有效性校验
        Session session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return Response.fail(500, "场次不存在");
        }
        List<SessionProductBatchSaveDTO.Item> items = dto.getItems();
        // 批次内 goodsId 去重
        Set<Long> goodsIdSet = new HashSet<>();
        for (SessionProductBatchSaveDTO.Item item : items) {
            if (!goodsIdSet.add(item.getGoodsId())) {
                return Response.fail(500, "关联商品列表中存在重复商品，goodsId=" + item.getGoodsId());
            }
        }
        // 批量校验商品存在性
        List<ConsignGoods> goodsList = consignGoodsMapper.selectBatchIds(goodsIdSet);
        if (goodsList.size() != goodsIdSet.size()) {
            Set<Long> existIds = new HashSet<>();
            for (ConsignGoods g : goodsList) {
                existIds.add(g.getId());
            }
            goodsIdSet.removeAll(existIds);
            return Response.fail(500, "商品不存在，goodsId=" + goodsIdSet);
        }
        // 查询该场次已存在的关联，跳过已关联商品
        List<SessionProduct> existList = lambdaQuery()
                .eq(SessionProduct::getSessionId, sessionId)
                .in(SessionProduct::getGoodsId, goodsIdSet)
                .list();
        Set<Long> existGoodsIds = new HashSet<>();
        for (SessionProduct sp : existList) {
            existGoodsIds.add(sp.getGoodsId());
        }
        List<SessionProduct> toSave = new ArrayList<>(items.size());
        for (SessionProductBatchSaveDTO.Item item : items) {
            if (existGoodsIds.contains(item.getGoodsId())) {
                continue; // 已存在关联，跳过
            }
            SessionProduct sp = new SessionProduct();
            sp.setSessionId(sessionId);
            sp.setGoodsId(item.getGoodsId());
            sp.setStock(item.getStock());
            sp.setSort(item.getSort() != null ? item.getSort() : 0);
            toSave.add(sp);
        }
        int skipped = items.size() - toSave.size();
        if (!toSave.isEmpty()) {
            saveBatch(toSave);
        }
        log.info("[场次商品关联] 批量新增完成，sessionId={}, 新增{}条, 跳过{}条(已关联)",
                sessionId, toSave.size(), skipped);
        String msg = skipped == 0
                ? "新增成功，共" + toSave.size() + "条"
                : "新增" + toSave.size() + "条，跳过" + skipped + "条已关联商品";
        return Response.ok(msg, null);
    }

    @Override
    public Response updateSessionProduct(SessionProductUpdateDTO dto) {
        SessionProduct exist = getById(dto.getId());
        if (exist == null) {
            return Response.fail(500, "场次商品关联不存在");
        }
        // 场次/商品发生变更时才做关联有效性与唯一性校验
        boolean relationChanged = !dto.getSessionId().equals(exist.getSessionId())
                || !dto.getGoodsId().equals(exist.getGoodsId());
        if (relationChanged) {
            String validateMsg = validateSessionAndGoods(dto.getSessionId(), dto.getGoodsId());
            if (validateMsg != null) {
                return Response.fail(500, validateMsg);
            }
            if (existsRelation(dto.getSessionId(), dto.getGoodsId(), dto.getId())) {
                return Response.fail(500, "该场次已关联此商品，请勿重复添加");
            }
        }
        SessionProduct sessionProduct = new SessionProduct();
        BeanConvertUtils.copyProperties(dto, sessionProduct);
        updateById(sessionProduct);
        log.info("[场次商品关联] 修改成功，id={}, sessionId={}, goodsId={}, stock={}",
                dto.getId(), dto.getSessionId(), dto.getGoodsId(), dto.getStock());
        return Response.ok("修改成功", null);
    }

    @Override
    public Response deleteSessionProduct(Long id) {
        SessionProduct exist = getById(id);
        if (exist == null) {
            return Response.fail(500, "场次商品关联不存在");
        }
        removeById(id);
        log.info("[场次商品关联] 删除成功（逻辑删除），id={}, sessionId={}, goodsId={}",
                id, exist.getSessionId(), exist.getGoodsId());
        return Response.ok("删除成功", null);
    }

    /**
     * 校验场次与商品均存在（逻辑删除的视为不存在）
     *
     * @return 校验通过返回 null，否则返回错误提示
     */
    private String validateSessionAndGoods(Long sessionId, Long goodsId) {
        Session session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return "场次不存在";
        }
        ConsignGoods goods = consignGoodsMapper.selectById(goodsId);
        if (goods == null) {
            return "商品不存在";
        }
        return null;
    }

    /**
     * 同场次同商品有效关联查重（@TableLogic 自动过滤已删除）
     *
     * @param excludeId 排除的关联ID（修改场景排除自身，新增传 null）
     */
    private boolean existsRelation(Long sessionId, Long goodsId, Long excludeId) {
        return lambdaQuery()
                .eq(SessionProduct::getSessionId, sessionId)
                .eq(SessionProduct::getGoodsId, goodsId)
                .ne(excludeId != null, SessionProduct::getId, excludeId)
                .exists();
    }
}
