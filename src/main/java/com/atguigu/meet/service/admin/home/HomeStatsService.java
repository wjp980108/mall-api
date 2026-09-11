package com.atguigu.meet.service.admin.home;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.vo.admin.home.HomeStatsVO;

/**
 * 后台首页统计服务
 */
public interface HomeStatsService {

    /**
     * 获取首页聚合统计：会员总数、今日新增会员数（Asia/Shanghai 当日）、文件总数。
     * <p>
     * 纯只读聚合，无入参、无副作用。
     */
    Response<HomeStatsVO> getHomeStats();
}
