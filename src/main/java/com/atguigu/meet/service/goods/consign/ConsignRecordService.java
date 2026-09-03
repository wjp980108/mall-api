package com.atguigu.meet.service.goods.consign;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.goods.consign.ConsignRecordPageQueryDTO;
import com.atguigu.meet.model.entity.goods.consign.ConsignGoods;

import java.math.BigDecimal;

/**
 * 委托代卖事件全生命周期快照 Service
 * <p>
 * 红线：
 * - 一次委托申请只 INSERT 一条；后续审核/卖出/下架均 UPDATE 该条，禁止新增第二条
 * - 快照字段写入后永不修改；审核/卖出/下架更新只动对应阶段字段
 * - 仅在委托节点调用；商品当前业务状态永远以主表 t_consign_goods 为准，本表禁止作为业务判断依据
 * - 调用方须保证 @Transactional 覆盖（主表+记录表同一事务）
 */
public interface ConsignRecordService {

    /**
     * 节点1 发起委托：INSERT 一条待审核记录
     * <p>冻结当时商品+委托人快照（memberId/memberName/goodsName/goodsPrice/coverImg/sessionId），
     * recordStatus=1 待审核，applyTime=now。
     *
     * @param goods      主表商品（快照来源）
     * @param memberName 委托人昵称（快照，由调用方查 sys_user 提供，优先 nickname 回退 username）
     */
    void recordApplyConsign(ConsignGoods goods, String memberName);

    /**
     * 节点2 审核通过：UPDATE 该条 recordStatus=2 + 审核快照
     * <p>查 consignGoodsId 下 recordStatus=1 的记录，更新 recordStatus=2、auditTime/auditOperatorId/auditOperatorName；
     * 不新增；不动快照。
     *
     * @param consignGoodsId    主表商品ID
     * @param auditOperatorId   审核管理员ID
     * @param auditOperatorName 审核管理员名称
     */
    void recordAuditPass(Long consignGoodsId, Long auditOperatorId, String auditOperatorName);

    /**
     * 节点3 审核驳回：UPDATE 该条 recordStatus=5 + 驳回原因
     * <p>查 consignGoodsId 下 recordStatus=1 的记录，更新 recordStatus=5、auditTime/auditOperatorId/auditOperatorName/rejectReason；
     * 不新增；不动快照。
     *
     * @param consignGoodsId    主表商品ID
     * @param auditOperatorId   审核管理员ID
     * @param auditOperatorName 审核管理员名称
     * @param rejectReason      驳回原因
     */
    void recordAuditReject(Long consignGoodsId, Long auditOperatorId, String auditOperatorName, String rejectReason);

    /**
     * 节点4 卖出成交：UPDATE 该条 recordStatus=3 + 成交快照
     * <p>查 consignGoodsId 下 recordStatus=2 的记录，更新 recordStatus=3、soldTime=now、soldPrice=成交价、
     * buyerId/buyerName/buyerPhone=新买家快照；不新增；不动其他快照。
     *
     * @param consignGoodsId 主表商品ID
     * @param soldPrice      成交价
     * @param buyerId        买家ID
     * @param buyerName      买家昵称
     * @param buyerPhone     买家手机号
     */
    void recordSold(Long consignGoodsId, BigDecimal soldPrice, Long buyerId, String buyerName, String buyerPhone);

    /**
     * 节点5 未售出下架：UPDATE 该条 recordStatus=4 + 下架快照
     * <p>查 consignGoodsId 下 recordStatus=2 的记录，更新 recordStatus=4、delistTime=now、delistReason；
     * 不新增；不动其他快照。
     *
     * @param consignGoodsId 主表商品ID
     * @param delistReason   下架原因
     */
    void recordDelist(Long consignGoodsId, String delistReason);

    // ====================== 查询履历接口 ======================

    /**
     * 分页查询委托代卖事件记录（管理端）
     * <p>支持按 商品ID/商品名模糊/委托人ID/买家ID/记录状态/申请时间范围 筛选，
     * 结果按申请时间倒序，每条记录组装 recordStatusName 中文名。
     *
     * @param parameter 分页查询参数
     */
    Response getPageList(ConsignRecordPageQueryDTO parameter);

    /**
     * 按商品ID查询委托履历列表（商品详情页展示历史）
     * <p>返回该商品全部委托代卖事件记录，按申请时间倒序；
     * 同一商品多轮委托生成多条独立记录，历史完整保留。
     *
     * @param consignGoodsId 主表商品ID
     */
    Response listByConsignGoodsId(Long consignGoodsId);

    /**
     * 根据记录ID查详情
     * <p>返回单条委托记录的完整生命周期快照，组装 recordStatusName 中文名。
     *
     * @param id 委托记录主键ID
     */
    Response getRecordById(Long id);

    // ====================== C 端用户履历接口（JWT 登录态） ======================

    /**
     * C 端用户「我的委托记录」：作为委托人(memberId)查询自己发起的委托履历
     * <p>按 applyTime 倒序分页，组装 recordStatusName 中文名。
     *
     * @param memberId 当前登录用户ID（委托人）
     * @param pageNum  页码
     * @param pageSize 每页条数
     */
    Response listMyConsign(Long memberId, Integer pageNum, Integer pageSize);

    /**
     * C 端用户「我的买入记录」：作为买家(buyerId)查询已成交(recordStatus=3已卖出)的记录
     * <p>按 soldTime 倒序分页，组装 recordStatusName 中文名。
     *
     * @param buyerId  当前登录用户ID（买家）
     * @param pageNum  页码
     * @param pageSize 每页条数
     */
    Response listMyBought(Long buyerId, Integer pageNum, Integer pageSize);
}
