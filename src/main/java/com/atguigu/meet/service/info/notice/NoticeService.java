package com.atguigu.meet.service.info.notice;

import com.atguigu.meet.common.Response;
import com.atguigu.meet.model.dto.info.notice.NoticePageQueryDTO;
import com.atguigu.meet.model.dto.info.notice.NoticeSaveDTO;
import com.atguigu.meet.model.dto.info.notice.NoticeUpdateDTO;

/**
 * 公告管理 Service
 */
public interface NoticeService {

    /** 公告分页列表 */
    Response getPageList(NoticePageQueryDTO parameter);

    /** 根据ID查公告（聚合阅读次数） */
    Response getNoticeById(Long id);

    /** 所有启用公告（C端展示/下拉用） */
    Response getAllEnabledNotices(String position);

    /** 新增公告 */
    Response addNotice(NoticeSaveDTO dto);

    /** 修改公告 */
    Response updateNotice(NoticeUpdateDTO dto);

    /** 删除公告（逻辑删除） */
    Response deleteNotice(Long id);
}