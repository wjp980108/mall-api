package com.atguigu.meet.service.seckill.session.impl;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.mapper.seckill.session.SessionMapper;
import com.atguigu.meet.model.dto.seckill.session.SessionPageQueryDTO;
import com.atguigu.meet.model.dto.seckill.session.SessionSaveDTO;
import com.atguigu.meet.model.dto.seckill.session.SessionStatusDTO;
import com.atguigu.meet.model.dto.seckill.session.SessionUpdateDTO;
import com.atguigu.meet.model.entity.seckill.session.Session;
import com.atguigu.meet.model.vo.OptionVO;
import com.atguigu.meet.model.vo.PageResultVO;
import com.atguigu.meet.service.seckill.session.SessionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import com.atguigu.meet.utils.BeanConvertUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 抢购场次 Service 实现
 * <p>
 * 核心保障：
 * - 抢购时间窗口校验：rush_end_time 必须晚于 rush_start_time（每日时段，不支持跨天）
 * - 排序：按 sort 正序，同序按创建时间倒序
 */
@Service
@Slf4j
public class SessionServiceImpl extends ServiceImpl<SessionMapper, Session> implements SessionService {

    @Override
    public Response getPageList(SessionPageQueryDTO parameter) {
        LambdaQueryWrapper<Session> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(parameter.getSessionName())) {
            wrapper.like(Session::getSessionName, parameter.getSessionName());
        }
        if (parameter.getSessionStatus() != null) {
            wrapper.eq(Session::getSessionStatus, Boolean.TRUE.equals(parameter.getSessionStatus()) ? 1 : 0);
        }
        wrapper.orderByAsc(Session::getSort);
        wrapper.orderByDesc(Session::getCreateTime);

        IPage<Session> page = new Page<>(parameter.getPageNum(), parameter.getPageSize());
        IPage<Session> result = page(page, wrapper);
        return Response.ok(PageResultVO.of(result));
    }

    @Override
    public Response getSessionById(Long id) {
        Session session = getById(id);
        if (session == null) {
            return Response.fail(500, "场次不存在");
        }
        return Response.ok(session);
    }

    @Override
    public Response addSession(SessionSaveDTO dto) {
        // 时间窗口校验（每日时段，不支持跨天）
        if (!dto.getRushEndTime().isAfter(dto.getRushStartTime())) {
            return Response.fail(500, "抢购结束时间必须晚于开始时间");
        }
        Session session = new Session();
        BeanConvertUtils.copyProperties(dto, session);
        // 默认值：场次状态不传则 1 开启
        if (session.getSessionStatus() == null) {
            session.setSessionStatus(1);
        }
        save(session);
        log.info("[抢购场次] 新增成功，id={}, sessionName={}",
                session.getId(), session.getSessionName());
        return Response.ok("新增成功", null);
    }

    @Override
    public Response updateSession(SessionUpdateDTO dto) {
        Session existSession = getById(dto.getId());
        if (existSession == null) {
            return Response.fail(500, "场次不存在");
        }
        // 时间窗口校验（每日时段，不支持跨天）
        if (!dto.getRushEndTime().isAfter(dto.getRushStartTime())) {
            return Response.fail(500, "抢购结束时间必须晚于开始时间");
        }
        Session session = new Session();
        BeanConvertUtils.copyProperties(dto, session);
        updateById(session);
        log.info("[抢购场次] 修改成功，id={}", dto.getId());
        return Response.ok("修改成功", null);
    }

    @Override
    public Response deleteSession(Long id) {
        Session existSession = getById(id);
        if (existSession == null) {
            return Response.fail(500, "场次不存在");
        }
        removeById(id);
        log.info("[抢购场次] 删除成功（逻辑删除），id={}", id);
        return Response.ok("删除成功", null);
    }

    @Override
    public Response updateStatus(SessionStatusDTO dto) {
        Session existSession = getById(dto.getId());
        if (existSession == null) {
            return Response.fail(500, "场次不存在");
        }
        // 实体字段带内联默认值，updateById 会把默认值一并写入覆盖真实数据，改用定点更新
        int status = Boolean.TRUE.equals(dto.getStatus()) ? 1 : 0;
        lambdaUpdate()
                .eq(Session::getId, dto.getId())
                .set(Session::getSessionStatus, status)
                .update();
        log.info("[抢购场次] 启停成功，id={}, {}->{}",
                dto.getId(), existSession.getSessionStatus() == 1, dto.getStatus());
        return Response.ok("场次启停成功", null);
    }

    @Override
    public Response getSessionOptions() {
        LambdaQueryWrapper<Session> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Session::getSessionStatus, 1);
        wrapper.orderByAsc(Session::getSort);
        wrapper.orderByDesc(Session::getCreateTime);
        List<Session> sessions = list(wrapper);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        List<OptionVO<Long>> options = new ArrayList<>(sessions.size());
        for (Session s : sessions) {
            String label = s.getSessionName() + ": "
                    + s.getRushStartTime().format(formatter)
                    + " - "
                    + s.getRushEndTime().format(formatter);
            options.add(new OptionVO<>(label, s.getId()));
        }
        return Response.ok(options);
    }
}
