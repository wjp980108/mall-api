
-- 抢购场次改为每日固定时段开启：抢购时间由完整日期时间(_DATETIME)降为每日时分(TIME)，旧数据自动截取时间部分
ALTER TABLE t_session MODIFY COLUMN rush_start_time TIME NOT NULL COMMENT '每日抢购开始时间（时:分:秒，例：09:50:00）';
ALTER TABLE t_session MODIFY COLUMN rush_end_time   TIME NOT NULL COMMENT '每日抢购结束时间（时:分:秒，例：17:00:00，不支持跨天）';