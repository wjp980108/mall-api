package com.atguigu.meet.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单号生成工具
 * <p>
 * 规则：yyyyMMddHHmmss(14) + userId后4位(4) + 随机数(4) = 22位，对齐 t_order.order_no varchar(64)
 * 同一秒内通过 userId + 随机数 区分，配合 t_order.uk_order_no 唯一索引兜底冲突。
 */
public final class OrderNoUtil {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private OrderNoUtil() {}

    public static String generate(Long userId) {
        String datePart = LocalDateTime.now().format(FMT);
        long uid = userId == null ? 0L : userId;
        String userPart = String.format("%04d", uid % 10000);
        String randPart = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        return datePart + userPart + randPart;
    }
}
