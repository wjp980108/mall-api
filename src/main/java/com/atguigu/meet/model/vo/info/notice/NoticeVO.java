package com.atguigu.meet.model.vo.info.notice;

import com.atguigu.meet.model.vo.permission.user.UserVO;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告响应VO
 */
@Data
public class NoticeVO {
    private Long id;
    private String title;
    private String content;
    private String position;
    private Integer sort;
    private Boolean status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;
    private Long updateBy;

    /** 创建人完整信息 */
    private UserVO creator;
    /** 更新人完整信息 */
    private UserVO updater;

    /** 阅读次数（详情接口聚合返回） */
    private Long readCount;
}