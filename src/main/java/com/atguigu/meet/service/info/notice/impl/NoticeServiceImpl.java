package com.atguigu.meet.service.info.notice.impl;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.mapper.info.notice.NoticeLogMapper;
import com.atguigu.meet.mapper.info.notice.NoticeMapper;
import com.atguigu.meet.mapper.permission.user.UserMapper;
import com.atguigu.meet.model.dto.info.notice.NoticePageQueryDTO;
import com.atguigu.meet.model.dto.info.notice.NoticeSaveDTO;
import com.atguigu.meet.model.dto.info.notice.NoticeUpdateDTO;
import com.atguigu.meet.model.entity.info.notice.Notice;
import com.atguigu.meet.model.entity.info.notice.NoticeLog;
import com.atguigu.meet.model.entity.permission.user.SysUser;
import com.atguigu.meet.model.vo.PageResultVO;
import com.atguigu.meet.model.vo.info.notice.NoticeVO;
import com.atguigu.meet.model.vo.permission.user.UserVO;
import com.atguigu.meet.service.info.notice.NoticeService;
import com.atguigu.meet.utils.AdminContext;
import com.atguigu.meet.utils.TimeRangeUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import com.atguigu.meet.utils.BeanConvertUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 公告管理 Service 实现
 */
@Service
@Slf4j
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {

    @Autowired
    private NoticeLogMapper noticeLogMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public Response getPageList(NoticePageQueryDTO parameter) {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(parameter.getTitle())) {
            wrapper.like(Notice::getTitle, parameter.getTitle());
        }
        if (parameter.getStatus() != null) {
            wrapper.eq(Notice::getStatus, parameter.getStatus());
        }
        // 解析时间范围：timeRange[0] -> 当天 00:00:00，timeRange[1] -> 当天 23:59:59
        List<String> timeRange = parameter.getTimeRange();
        if (timeRange != null && !timeRange.isEmpty()) {
            if (timeRange.size() >= 1) {
                LocalDateTime startTime = TimeRangeUtils.toStartOfDay(timeRange.get(0));
                if (startTime != null) {
                    wrapper.ge(Notice::getCreateTime, startTime);
                }
            }
            if (timeRange.size() >= 2) {
                LocalDateTime endTime = TimeRangeUtils.toEndOfDay(timeRange.get(1));
                if (endTime != null) {
                    wrapper.le(Notice::getCreateTime, endTime);
                }
            }
        }
        // sort 越大越靠前，同级按创建时间倒序
        wrapper.orderByDesc(Notice::getSort);
        wrapper.orderByDesc(Notice::getCreateTime);

        IPage<Notice> page = new Page<>(parameter.getPageNum(), parameter.getPageSize());
        IPage<Notice> result = page(page, wrapper);
        return Response.ok(PageResultVO.of(result));
    }

    @Override
    public Response getNoticeById(Long id) {
        Notice notice = getById(id);
        if (notice == null) {
            return Response.fail(500, "公告不存在");
        }
        NoticeVO vo = new NoticeVO();
        BeanConvertUtils.copyProperties(notice, vo);
        // 聚合阅读次数
        Long readCount = noticeLogMapper.selectCount(new LambdaQueryWrapper<NoticeLog>()
                .eq(NoticeLog::getNoticeId, id));
        vo.setReadCount(readCount);
        // 匹配创建人/更新人完整信息
        vo.setCreator(toUserVO(userMapper.selectById(notice.getCreateBy())));
        vo.setUpdater(toUserVO(userMapper.selectById(notice.getUpdateBy())));
        return Response.ok(vo);
    }

    @Override
    public Response getAllEnabledNotices() {
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notice::getStatus, 1);
        wrapper.orderByDesc(Notice::getSort);
        wrapper.orderByDesc(Notice::getCreateTime);
        List<Notice> notices = list(wrapper);
        return Response.ok(notices);
    }

    @Override
    public Response addNotice(NoticeSaveDTO dto) {
        Notice notice = new Notice();
        BeanConvertUtils.copyProperties(dto, notice);
        // 操作人 = 当前登录管理员
        notice.setCreateBy(AdminContext.getLoginUserId());
        // createTime/updateTime 由数据库默认值填充
        save(notice);
        log.info("[公告管理] 新增公告成功，id={}, title={}, 操作人={}", notice.getId(), notice.getTitle(), notice.getCreateBy());
        return Response.ok("新增公告成功", null);
    }

    @Override
    public Response updateNotice(NoticeUpdateDTO dto) {
        Notice existNotice = getById(dto.getId());
        if (existNotice == null) {
            return Response.fail(500, "公告不存在");
        }
        Notice notice = new Notice();
        BeanConvertUtils.copyProperties(dto, notice);
        notice.setUpdateBy(AdminContext.getLoginUserId());
        updateById(notice);
        log.info("[公告管理] 修改公告成功，id={}, 操作人={}", dto.getId(), notice.getUpdateBy());
        return Response.ok("修改公告成功", null);
    }

    @Override
    public Response deleteNotice(Long id) {
        Notice notice = getById(id);
        if (notice == null) {
            return Response.fail(500, "公告不存在");
        }
        // 逻辑删除（@TableLogic 注解生效）
        removeById(id);
        log.info("[公告管理] 删除公告成功，id={}", id);
        return Response.ok("删除公告成功", null);
    }

    // ====================== 私有方法 ======================

    /**
     * SysUser -> UserVO（password 字段在实体上 @JsonIgnore，且 UserVO 无该字段，天然不外泄）
     */
    private UserVO toUserVO(SysUser user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        BeanConvertUtils.copyProperties(user, vo);
        return vo;
    }
}