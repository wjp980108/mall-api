package com.atguigu.meet.model.entity.seckill.session;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.atguigu.meet.config.jackson.Integer01ToBooleanSerializer;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 抢购场次实体（对应 t_session）
 * <p>
 * 一个活动下有多场抢购场次，场次控制进场时间、抢购时间窗口、购买次数、禁止委托时间等。
 */
@Data
@TableName("t_session")
public class Session extends Model<Session> {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 场次名称：自定义名称 */
    private String sessionName;

    /** 场次状态 1开启 0关闭 */
    @JsonSerialize(using = Integer01ToBooleanSerializer.class)
    private Integer sessionStatus = 1;

    /** 进场时间控制(分钟) */
    private Integer enterControlMinute = 0;

    /** 每日抢购开始时间（时:分，例：09:50，每天该时刻开启抢购） */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime rushStartTime;

    /** 每日抢购结束时间（时:分，例：17:00，不支持跨天） */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime rushEndTime;

    /** 最多购买次数(次) */
    private Integer maxBuyCount = 1;

    /** 开场前禁止委托时间(分钟) */
    private Integer beforeForbidMinute = 0;

    /** 结束后禁止委托时间(分钟) */
    private Integer afterForbidMinute = 0;

    /** 场次背景图地址 */
    private String bgImg;

    /** 场次背景图存储平台:local-1/aliyun-oss-1等 */
    private String bgImgPlatform;

    /** 排序号（前端按顺序展示第1场、第2场） */
    private Integer sort = 0;

    /** 逻辑删除 0未删 1已删 */
    @JsonIgnore
    @TableLogic
    private Integer isDeleted = 0;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
