package com.atguigu.meet.service.goods.consign;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsAuditDTO;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsBizStatusDTO;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsDeleteDTO;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsOnlineStatusDTO;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsPageQueryDTO;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsSaveDTO;
import com.atguigu.meet.model.dto.goods.consign.ConsignGoodsUpdateDTO;

/**
 * 抢购托售商品 Service
 * <p>
 * 业务状态流转：1挂卖中 -> 2已抢购待付款 -> 3等待确认付款 -> 4待处理(买家持有)
 *             -> 5委托代卖(买家申请,待审核) -> 审核通过 -> 1挂卖中(重新上架)；驳回 -> 4待处理
 */
public interface ConsignGoodsService {

    /** 分页列表（含委托人信息 + 场次名称） */
    Response getPageList(ConsignGoodsPageQueryDTO parameter);

    /** 根据ID查详情（含委托人信息 + 场次名称） */
    Response getConsignGoodsById(Long id);

    /** 新增 */
    Response addConsignGoods(ConsignGoodsSaveDTO dto);

    /** 修改 */
    Response updateConsignGoods(ConsignGoodsUpdateDTO dto);

    /** 上下架 */
    Response updateOnlineStatus(ConsignGoodsOnlineStatusDTO dto);

    /** 业务状态流转 */
    Response updateBizStatus(ConsignGoodsBizStatusDTO dto);

    /** 删除（逻辑删除） */
    Response deleteConsignGoods(Long id);

    /** 批量删除（逻辑删除） */
    Response deleteConsignGoodsBatch(ConsignGoodsDeleteDTO dto);

    /**
     * 记录跨模块商品业务状态流转审计日志（BIZ_FLOW）
     * <p>
     * 供订单模块等外部服务在「商品状态条件更新成功」后补充商品侧操作日志，
     * 保证商品状态每次变更在 t_consign_goods_operate_log 均有 before/after 审计记录。
     *
     * @param goodsId    托售商品ID
     * @param fromStatus 流转前状态
     * @param toStatus   流转后状态
     * @param remark     备注（如触发来源与订单号）
     */
    void recordExternalBizFlow(Long goodsId, Integer fromStatus, Integer toStatus, String remark);

    // ====================== 委托代卖审核 ======================

    /**
     * C 端买家申请委托代卖（商品 4待处理 -> 5委托代卖，委托状态=1委托代卖中，审核状态=1待审核）
     * <p>校验：仅商品当前委托人（即确认收款后的买家）可申请；商品状态必须为 4待处理。
     *
     * @param goodsId       托售商品ID
     * @param currentUserId 当前登录用户ID（商品持有者）
     */
    Response entrustByOwner(Long goodsId, Long currentUserId);

    /**
     * 后台管理员委托代卖审核
     * <p>通过：商品 5委托代卖 -> 1挂卖中 + 自动上架（审核状态=2通过）；
     * 驳回：商品 5委托代卖 -> 4待处理（委托状态=0未委托，审核状态=3驳回），买家可重新申请。
     *
     * @param dto 审核参数（商品ID + 通过/驳回 + 备注）
     */
    Response auditEntrust(ConsignGoodsAuditDTO dto);

    // ====================== C 端用户接口（JWT 登录态） ======================

    /**
     * C 端「我持有的商品」：查 goodsStatus=4待处理 + memberId=当前用户，可发起委托代卖的商品
     * <p>复用管理端分页查询（memberId + goodsStatus=4 条件），按创建时间倒序。
     *
     * @param memberId 当前登录用户ID（商品持有者）
     * @param pageNum  页码
     * @param pageSize 每页条数
     */
    Response listMyHeld(Long memberId, Integer pageNum, Integer pageSize);

    /**
     * C 端「在售抢购商品列表」：当前可抢购的商品
     * <p>过滤条件：上架(onlineStatus=1) + 挂卖中(goodsStatus=1) + 场次开启(sessionStatus=1)
     * + 当前时间在场次抢购时间窗口内(CURTIME() BETWEEN rushStartTime AND rushEndTime)；
     * 按场次排序 + 创建时间倒序，含委托人信息 + 场次名；可选 sessionId 精确过滤。
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @param sessionId 场次ID（传 null 则不按场次过滤，返回所有当前可抢购商品）
     */
    Response listSaleGoods(Integer pageNum, Integer pageSize, Long sessionId);

    /**
     * C 端首页搜索商品：上架(onlineStatus=1) + 挂卖中(goodsStatus=1)
     * <p>按商品名称模糊查询，按 sale_times 倒序(销量优先) + 创建时间倒序，含委托人信息 + 场次名。
     *
     * @param keyword  搜索关键字（商品名称模糊匹配）
     * @param pageNum  页码
     * @param pageSize 每页条数
     */
    Response searchGoods(String keyword, Integer pageNum, Integer pageSize);

    /**
     * C 端首页推荐商品：上架(onlineStatus=1) + 挂卖中(goodsStatus=1) + 场次开启 + 在抢购时间窗口内
     * <p>按 sale_times 倒序(销量优先) + 创建时间倒序，含委托人信息 + 场次名。
     *
     * @param pageNum  页码
     * @param pageSize 每页条数
     */
    Response recommendGoods(Integer pageNum, Integer pageSize);
}