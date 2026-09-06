package com.atguigu.meet.task;

import com.atguigu.meet.service.goods.consign.ConsignGoodsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 商品定时任务
 * <p>
 * 每天 23:59:59 自动下架当日未委托的待处理商品
 */
@Component
@Slf4j
public class ConsignGoodsTask {

    @Autowired
    private ConsignGoodsService consignGoodsService;

    /**
     * 每天 23:59:59 执行：批量下架当日未委托的待处理商品
     * <p>cron 表达式：秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 59 23 * * ?")
    public void delistUnentrustedGoods() {
        log.info("[定时任务] 开始执行：批量下架当日未委托商品");
        try {
            consignGoodsService.scheduledDelistUnentrustedGoods();
        } catch (Exception e) {
            log.error("[定时任务] 批量下架当日未委托商品失败", e);
        }
    }

    /**
     * 每天 23:59:59 执行：下架当日上架仍未卖出的委托商品（上架当日必须卖出，未卖出退回终结）
     * <p>与"未委托下架"互不重叠：本任务处理 1挂卖中+上架+委托中，前者处理 4待处理+未委托。
     * 下架后委托记录 2已上架 → 4未售出下架，商品此后不可再委托。
     */
    @Scheduled(cron = "0 59 23 * * ?")
    public void delistUnsoldListedGoods() {
        log.info("[定时任务] 开始执行：批量下架当日上架未卖出商品");
        try {
            int count = consignGoodsService.scheduledDelistUnsoldListedGoods();
            if (count > 0) {
                log.info("[定时任务] 当日上架未卖出商品下架完成，共 {} 件", count);
            }
        } catch (Exception e) {
            log.error("[定时任务] 批量下架当日上架未卖出商品失败", e);
        }
    }
}