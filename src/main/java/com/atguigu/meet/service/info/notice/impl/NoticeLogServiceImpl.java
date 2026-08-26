package com.atguigu.meet.service.info.notice.impl;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.mapper.info.notice.NoticeLogMapper;
import com.atguigu.meet.model.dto.info.notice.NoticeLogPageQueryDTO;
import com.atguigu.meet.model.entity.info.notice.NoticeLog;
import com.atguigu.meet.model.vo.PageResultVO;
import com.atguigu.meet.service.info.notice.NoticeLogService;
import com.atguigu.meet.utils.TimeRangeUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告阅读日志 Service 实现
 */
@Service
@Slf4j
public class NoticeLogServiceImpl extends ServiceImpl<NoticeLogMapper, NoticeLog> implements NoticeLogService {

    @Override
    public Response getPageList(NoticeLogPageQueryDTO parameter) {
        LambdaQueryWrapper<NoticeLog> wrapper = new LambdaQueryWrapper<>();
        if (parameter.getNoticeId() != null) {
            wrapper.eq(NoticeLog::getNoticeId, parameter.getNoticeId());
        }
        if (parameter.getUserId() != null) {
            wrapper.eq(NoticeLog::getUserId, parameter.getUserId());
        }
        // 解析时间范围：timeRange[0] -> 当天 00:00:00，timeRange[1] -> 当天 23:59:59
        List<String> timeRange = parameter.getTimeRange();
        if (timeRange != null && !timeRange.isEmpty()) {
            if (timeRange.size() >= 1) {
                LocalDateTime startTime = TimeRangeUtils.toStartOfDay(timeRange.get(0));
                if (startTime != null) {
                    wrapper.ge(NoticeLog::getCreateTime, startTime);
                }
            }
            if (timeRange.size() >= 2) {
                LocalDateTime endTime = TimeRangeUtils.toEndOfDay(timeRange.get(1));
                if (endTime != null) {
                    wrapper.le(NoticeLog::getCreateTime, endTime);
                }
            }
        }
        wrapper.orderByDesc(NoticeLog::getReadTime);

        IPage<NoticeLog> page = new Page<>(parameter.getPageNum(), parameter.getPageSize());
        IPage<NoticeLog> result = page(page, wrapper);
        return Response.ok(PageResultVO.of(result));
    }

    @Override
    public Response getReadersByNoticeId(Long noticeId) {
        LambdaQueryWrapper<NoticeLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoticeLog::getNoticeId, noticeId);
        wrapper.orderByDesc(NoticeLog::getReadTime);
        return Response.ok(list(wrapper));
    }

    @Override
    public Response getReadCount(Long noticeId) {
        long count = count(new LambdaQueryWrapper<NoticeLog>().eq(NoticeLog::getNoticeId, noticeId));
        return Response.ok(count);
    }

    @Override
    public Response recordRead(Long noticeId, Long userId) {
        // 依据唯一约束 uk_notice_user：同一用户同一公告只记录一次，重复阅读刷新 readTime
        LambdaQueryWrapper<NoticeLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(NoticeLog::getNoticeId, noticeId);
        wrapper.eq(NoticeLog::getUserId, userId);
        NoticeLog exist = getOne(wrapper);
        if (exist != null) {
            exist.setReadTime(LocalDateTime.now());
            updateById(exist);
            return Response.ok("刷新阅读时间成功", null);
        }
        NoticeLog logEntity = new NoticeLog();
        logEntity.setNoticeId(noticeId);
        logEntity.setUserId(userId);
        logEntity.setReadTime(LocalDateTime.now());
        save(logEntity);
        log.info("[公告阅读] 记录阅读成功，noticeId={}, userId={}", noticeId, userId);
        return Response.ok("记录阅读成功", null);
    }
}