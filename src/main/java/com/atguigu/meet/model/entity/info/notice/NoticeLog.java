package com.atguigu.meet.model.entity.info.notice;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公告阅读日志实体
 * 记录用户阅读公告的行为：谁读了哪条公告、什么时候读的
 */
@Data
@TableName("t_notice_log")
@Schema(description = "公告阅读日志数据")
public class NoticeLog extends Model<NoticeLog> {

    @TableId(type = IdType.AUTO)
    @Schema(description = "日志ID")
    private Long id;

    /** 公告ID(t_notice.id) */
    @Schema(description = "公告ID")
    private Long noticeId;

    /** 用户ID(sys_user.id) */
    @Schema(description = "用户ID")
    private Long userId;

    /** 阅读时间 */
    @Schema(description = "阅读时间")
    private LocalDateTime readTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}