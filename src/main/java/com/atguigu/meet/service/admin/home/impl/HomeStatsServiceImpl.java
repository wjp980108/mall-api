package com.atguigu.meet.service.admin.home.impl;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.mapper.file.FileInfoMapper;
import com.atguigu.meet.mapper.permission.user.UserMapper;
import com.atguigu.meet.model.entity.permission.user.SysUser;
import com.atguigu.meet.model.vo.admin.home.HomeStatsVO;
import com.atguigu.meet.service.admin.home.HomeStatsService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 后台首页统计服务实现
 * <p>
 * 三次独立 COUNT 串行组装，纯只读、无事务。逻辑删除条件（is_deleted=0）
 * 由 MyBatis-Plus {@code @TableLogic} 自动追加，wrapper 中不再手写。
 */
@Service
@Slf4j
public class HomeStatsServiceImpl implements HomeStatsService {

    /** 业务统计口径时区：北京时间（不依赖 JVM 默认时区，UTC 部署环境下窗口不偏移） */
    private static final ZoneId BIZ_ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FileInfoMapper fileInfoMapper;

    @Override
    public Response<HomeStatsVO> getHomeStats() {
        // 1. 会员总数
        Long memberTotal = userMapper.selectCount(null);

        // 2. 今日新增会员：北京时间当日 [00:00, 次日00:00)
        LocalDateTime dayStart = LocalDate.now(BIZ_ZONE).atStartOfDay();
        Long memberTodayNew = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .ge(SysUser::getCreateTime, dayStart)
                        .lt(SysUser::getCreateTime, dayStart.plusDays(1)));

        // 3. 文件总数（不按 suffix / bizType 过滤）
        Long imageTotal = fileInfoMapper.selectCount(null);

        HomeStatsVO.MemberStats memberStats = new HomeStatsVO.MemberStats();
        memberStats.setTotal(memberTotal);
        memberStats.setTodayNew(memberTodayNew);

        HomeStatsVO.ImageStats imageStats = new HomeStatsVO.ImageStats();
        imageStats.setTotal(imageTotal);

        HomeStatsVO vo = new HomeStatsVO();
        vo.setMember(memberStats);
        vo.setImage(imageStats);

        return Response.ok(vo);
    }
}
