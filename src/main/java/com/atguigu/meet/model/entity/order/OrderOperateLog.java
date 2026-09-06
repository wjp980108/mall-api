package com.atguigu.meet.model.entity.order;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单操作审计日志实体
 * 记录所有状态变更流水
 * <p>
 * operate_type 存数字 code（1上传凭证 2确认收款 3取消订单 4删除订单 5超时自动取消）
 * operate_desc 存中文描述（用于列表展示，避免每次解析枚举）
 * 对齐 t_goods_operate_log / t_consign_goods_operate_log 的数据模型
 */
@Data
@TableName("t_order_operate_log")
@Schema(description = "订单操作日志数据")
public class OrderOperateLog extends Model<OrderOperateLog> {

    @TableId(type = IdType.AUTO)
    @Schema(description = "日志ID")
    private Long id;

    /** 订单ID 关联t_order.id */
    @Schema(description = "订单ID")
    private Long orderId;

    /** 操作前订单状态 */
    @Schema(description = "操作前订单状态")
    private Integer beforeStatus;

    /** 操作后订单状态 */
    @Schema(description = "操作后订单状态")
    private Integer afterStatus;

    /**
     * 操作类型数字编码：1上传凭证 2确认收款 3取消订单 4删除订单 5超时自动取消
     * @see com.atguigu.meet.enums.OrderOperateType#getCode()
     */
    @Schema(description = "操作类型 1上传凭证 2确认收款 3取消订单 4删除订单 5超时自动取消")
    private Integer operateType;

    /**
     * 操作类型中文描述（冗余展示列）
     * @see com.atguigu.meet.enums.OrderOperateType#getDesc()
     */
    @Schema(description = "操作类型描述")
    private String operateDesc;

    /** 操作人管理员/会员ID 关联sys_user.id */
    @Schema(description = "操作人ID")
    private Long operateUserId;

    /** 操作人名称快照 */
    @Schema(description = "操作人名称")
    private String operateUserName;

    /** 操作备注 */
    @Schema(description = "操作备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}