package com.atguigu.meet.model.entity.roborder;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 抢购订单操作审计日志实体（对应 t_rob_order_operate_log）
 * <p>
 * operate_type：1下单 2取消订单 3转移订单；结构对齐 t_order_operate_log，不做逻辑删除。
 */
@Data
@TableName("t_rob_order_operate_log")
@Schema(description = "抢购订单操作日志数据")
public class RobOrderOperateLog extends Model<RobOrderOperateLog> {

    @TableId(type = IdType.AUTO)
    @Schema(description = "日志ID")
    private Long id;

    /** 订单ID，关联 t_rob_order.id */
    @Schema(description = "订单ID")
    private Long orderId;

    /** 操作前订单状态 */
    @Schema(description = "操作前订单状态")
    private Integer beforeStatus;

    /** 操作后订单状态 */
    @Schema(description = "操作后订单状态")
    private Integer afterStatus;

    /**
     * 操作类型：1下单 2取消订单 3转移订单
     * @see com.atguigu.meet.enums.RobOrderOperateType
     */
    @Schema(description = "操作类型 1下单 2取消订单 3转移订单")
    private Integer operateType;

    /** 操作类型中文描述（冗余展示） */
    @Schema(description = "操作类型描述")
    private String operateDesc;

    /** 操作人ID（管理员/会员） */
    @Schema(description = "操作人ID")
    private Long operateUserId;

    /** 操作人名称快照 */
    @Schema(description = "操作人名称")
    private String operateUserName;

    /** 操作备注（转移记录原买家→新买家） */
    @Schema(description = "操作备注")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
