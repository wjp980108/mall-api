package com.atguigu.meet.model.entity.goods.consign;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 抢购托售商品操作日志实体
 * operate_type: 1新增 2编辑 3删除 4上下架 5业务状态流转
 * content JSON 约定: {"before":{...},"after":{...},"changedFields":["xxx"],"remark":"状态流转:挂卖中->待付款"}
 */
@Data
@TableName("t_consign_goods_operate_log")
@Schema(description = "托售商品操作日志数据")
public class ConsignGoodsOperateLog extends Model<ConsignGoodsOperateLog> {

    @TableId(type = IdType.AUTO)
    @Schema(description = "日志ID")
    private Long id;

    /** 托售商品ID */
    @Schema(description = "托售商品ID")
    private Long consignGoodsId;

    /** 操作管理员ID */
    @Schema(description = "操作管理员ID")
    private Long adminId;

    /** 操作类型 1新增 2编辑 3删除 4上下架 5业务状态流转 */
    @Schema(description = "操作类型 1新增 2编辑 3删除 4上下架 5业务状态流转")
    private Integer operateType;

    /** 操作中文描述(新增/编辑/删除/上下架/状态流转:挂卖中->待付款)，列表展示用，避免每次解析 JSON */
    @Schema(description = "操作描述")
    private String operateDesc;

    /** 业务流转前状态(仅operate_type=5有效 1挂卖中 2已抢购待付款 3等待确认付款 4待处理 5委托代卖) */
    @Schema(description = "流转前状态")
    private Integer fromStatus;

    /** 业务流转后状态(仅operate_type=5有效) */
    @Schema(description = "流转后状态")
    private Integer toStatus;

    /** 操作人客户端 IP，溯源定位操作来源 */
    @Schema(description = "操作人IP")
    private String ip;

    /** 操作人浏览器/客户端设备信息，安全审计用 */
    @Schema(description = "操作人设备信息")
    private String userAgent;

    /**
     * 变更内容 JSON
     * 格式: {"before":{...},"after":{...},"changedFields":["xxx"],"remark":"..."}
     * before/after 为前后快照，changedFields 为本次修改字段名集合，remark 为备注
     */
    @Schema(description = "变更内容JSON")
    private String content;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}