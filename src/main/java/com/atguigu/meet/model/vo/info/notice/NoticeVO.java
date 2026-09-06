package com.atguigu.meet.model.vo.info.notice;

import com.atguigu.meet.model.vo.permission.user.UserVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告响应VO
 */
@Data
@Schema(description = "公告响应数据")
public class NoticeVO {
    @Schema(description = "公告ID")
    private Long id;
    @Schema(description = "公告标题")
    private String title;
    @Schema(description = "公告内容")
    private String content;
    @Schema(description = "公告位置")
    private String position;
    @Schema(description = "排序")
    private Integer sort;
    @Schema(description = "状态 1启用 0禁用")
    private Boolean status;
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    @Schema(description = "创建人ID")
    private Long createBy;
    @Schema(description = "更新人ID")
    private Long updateBy;

    /** 创建人完整信息 */
    @Schema(description = "创建人信息")
    private UserVO creator;
    /** 更新人完整信息 */
    @Schema(description = "更新人信息")
    private UserVO updater;

    /** 阅读次数（详情接口聚合返回） */
    @Schema(description = "阅读次数")
    private Long readCount;
}