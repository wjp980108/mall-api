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
}