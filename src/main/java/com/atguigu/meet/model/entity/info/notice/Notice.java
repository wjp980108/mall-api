package com.atguigu.meet.model.entity.info.notice;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.atguigu.meet.config.jackson.Integer01ToBooleanSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台公告实体
 */
@Data
@TableName("t_notice")
@Schema(description = "公告数据")
public class Notice extends Model<Notice> {

    @TableId(type = IdType.AUTO)
    @Schema(description = "公告ID")
    private Long id;

    /** 公告标题 */
    @Schema(description = "公告标题")
    private String title;

    /** 富文本公告内容 */
    @Schema(description = "公告内容")
    private String content;

    /** 公告位置：home=首页 */
    @Schema(description = "公告位置")
    private String position;

    /** 排序，数值越大越靠前展示 */
    @Schema(description = "排序")
    private Integer sort = 0;

    /** 状态：0-禁用，1-启用 */
    @JsonSerialize(using = Integer01ToBooleanSerializer.class)
    @Schema(description = "状态 0禁用 1启用")
    private Integer status = 1;

    /** 逻辑删除 0未删 1已删 */
    @JsonIgnore
    @TableLogic
    private Integer isDeleted = 0;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 操作人ID(管理员id) */
    @Schema(description = "创建人ID")
    private Long createBy;

    /** 更新人ID */
    @Schema(description = "更新人ID")
    private Long updateBy;
}