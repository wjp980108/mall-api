package com.atguigu.meet.controller.goods.consign;

import com.atguigu.meet.annotation.RequirePermission;
import com.atguigu.meet.common.Response;
import com.atguigu.meet.constant.PermissionConst;
import com.atguigu.meet.model.dto.goods.consign.ConsignRecordPageQueryDTO;
import com.atguigu.meet.service.goods.consign.ConsignRecordService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 委托代卖事件记录查询（履历）
 * <p>
 * 记录状态：1待审核 2审核通过·已上架 3已卖出 4未售出下架 5审核驳回
 * <p>
 * 写入接口不在此暴露：委托记录由后端在委托生命周期节点（发起委托/审核/卖出/下架）内部自动生成与更新。
 */
@RestController
@RequestMapping("/consign-record")
@Validated
public class ConsignRecordController {

    @Autowired
    private ConsignRecordService consignRecordService;

    /** 分页查询委托代卖事件记录（支持 商品ID/商品名/委托人/买家/状态/时间 筛选） */
    @GetMapping
    @RequirePermission(PermissionConst.CONSIGN_RECORD_QUERY)
    public Response getPageList(@Valid ConsignRecordPageQueryDTO parameter) {
        return consignRecordService.getPageList(parameter);
    }

    /** 按商品ID查询委托履历列表（商品全部委托代卖历史，按申请时间倒序） */
    @GetMapping("/goods/{consignGoodsId}")
    @RequirePermission(PermissionConst.CONSIGN_RECORD_QUERY)
    public Response listByConsignGoodsId(@PathVariable Long consignGoodsId) {
        return consignRecordService.listByConsignGoodsId(consignGoodsId);
    }

    /** 根据记录ID查详情（单条委托记录完整生命周期快照） */
    @GetMapping("/{id}")
    @RequirePermission(PermissionConst.CONSIGN_RECORD_QUERY)
    public Response getRecordById(@PathVariable Long id) {
        return consignRecordService.getRecordById(id);
    }
}
