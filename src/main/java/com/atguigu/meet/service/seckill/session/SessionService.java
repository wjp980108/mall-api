package com.atguigu.meet.service.seckill.session;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.seckill.session.SessionPageQueryDTO;
import com.atguigu.meet.model.dto.seckill.session.SessionSaveDTO;
import com.atguigu.meet.model.dto.seckill.session.SessionStatusDTO;
import com.atguigu.meet.model.dto.seckill.session.SessionUpdateDTO;

/**
 * 抢购场次 Service（抢购系统设置/抢购时间设置模块）
 */
public interface SessionService {

    /** 分页列表 */
    Response getPageList(SessionPageQueryDTO parameter);

    /** 根据ID查场次 */
    Response getSessionById(Long id);

    /** 新增场次 */
    Response addSession(SessionSaveDTO dto);

    /** 修改场次 */
    Response updateSession(SessionUpdateDTO dto);

    /** 删除场次（逻辑删除） */
    Response deleteSession(Long id);

    /** 场次启用/禁用 */
    Response updateStatus(SessionStatusDTO dto);

    /** 场次下拉选项列表（仅启用场次） */
    Response getSessionOptions();

    /**
     * C 端「启用场次列表」：返回所有启用场次完整信息
     * <p>按 sort 升序 + 创建时间倒序，含抢购时间窗口、背景图、进场控制等，供 H5 首页展示。
     */
    Response getAllEnabledSessions();
}
