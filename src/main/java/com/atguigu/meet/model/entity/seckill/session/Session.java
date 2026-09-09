package com.atguigu.meet.model.entity.seckill.session;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.atguigu.meet.config.jackson.Integer01ToBooleanSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 抢购场次实体（对应 t_session）
 * <p>
 * 一个活动下有多场抢购场次，场次控制抢购时间窗口、背景图、排序等。
 * 注：进场时间控制/禁止委托时间为预留字段（写入路径已切断，业务零消费，详见各字段注释）。
 */
@Data
@TableName("t_session")
@Schema(description = "抢购场次数据")
public class Session extends Model<Session> {

    @TableId(type = IdType.AUTO)
    @Schema(description = "场次ID")
    private Long id;

    /** 场次名称：自定义名称 */
    @Schema(description = "场次名称")
    private String sessionName;

    /** 场次状态 1开启 0关闭 */
    @JsonSerialize(using = Integer01ToBooleanSerializer.class)
    @Schema(description = "场次状态 1开启 0关闭")
    private Integer sessionStatus = 1;

    /**
     * 进场时间控制(分钟)
     * <p>预留字段：写入路径已切断（cleanup-session-reserved-fields），当前业务零消费——
     * 提前进场窗口由全局 sys_settings 双开关（newMemberDays + newMemberAdvanceMinutes）控制。
     * 不带内联默认值：updateById 走 NOT_NULL 策略，null 不进 UPDATE SET，避免编辑场次时覆盖存量值；
     * 新增时为 null，由 DB 列 DEFAULT 0 兜底。未来启用：加回 SessionSaveDTO 字段+校验注解+前端表单项，并补业务逻辑。
     */
    @Schema(description = "进场时间控制(分钟)（预留字段，暂未生效）")
    private Integer enterControlMinute;

    /** 每日抢购开始时间（时:分，例：09:50，每天该时刻开启抢购） */
    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "每日抢购开始时间")
    private LocalTime rushStartTime;

    /** 每日抢购结束时间（时:分，例：17:00，不支持跨天） */
    @JsonFormat(pattern = "HH:mm")
    @Schema(description = "每日抢购结束时间")
    private LocalTime rushEndTime;

    /**
     * 开场前禁止委托时间(分钟)
     * <p>预留字段：写入路径已切断（cleanup-session-reserved-fields），"禁止委托"业务逻辑尚未实现，
     * 当前业务零消费。不带内联默认值（同 {@link #enterControlMinute} 的 null 跳过策略，防编辑覆盖存量值）。
     * 未来启用：加回 SessionSaveDTO 字段+校验注解+前端表单项，并在委托/寄售链路补时间窗口校验。
     */
    @Schema(description = "开场前禁止委托时间(分钟)（预留字段，暂未生效）")
    private Integer beforeForbidMinute;

    /**
     * 结束后禁止委托时间(分钟)
     * <p>预留字段：同 {@link #beforeForbidMinute}，写入路径已切断、业务零消费。
     */
    @Schema(description = "结束后禁止委托时间(分钟)（预留字段，暂未生效）")
    private Integer afterForbidMinute;

    /** 场次背景图地址 */
    @Schema(description = "场次背景图地址")
    private String bgImg;

    /** 场次背景图存储平台:local-1/aliyun-oss-1等 */
    @Schema(description = "场次背景图存储平台")
    private String bgImgPlatform;

    /** 排序号（前端按顺序展示第1场、第2场） */
    @Schema(description = "排序号")
    private Integer sort = 0;

    /** 逻辑删除 0未删 1已删 */
    @JsonIgnore
    @TableLogic
    private Integer isDeleted = 0;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 是否售卖中（接口派生字段，非表列）：场次启用且当前时间在抢购时间窗口内（含新会员提前窗口），
     * 由 Service 层通过 SeckillSellingGuard 填充；售卖中时管理端编辑/删除被锁定。
     */
    @TableField(exist = false)
    @Schema(description = "是否售卖中(接口派生，售卖中禁止编辑/删除)")
    private Boolean onSale;
}