package com.atguigu.meet.model.entity.general.agreement;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户协议实体
 * <p>按类型区分不同协议：1-用户协议 2-隐私协议 3-委托协议 4-分销说明
 */
@Data
@TableName("sys_user_agreement")
@Schema(description = "用户协议数据")
public class SysUserAgreement extends Model<SysUserAgreement> {

    @TableId(type = IdType.AUTO)
    @Schema(description = "协议ID")
    private Long id;

    /** 协议标题 */
    @Schema(description = "协议标题")
    private String title;

    /** 协议类型：1-用户协议 2-隐私协议 3-委托协议 4-分销说明 */
    @Schema(description = "协议类型：1-用户协议 2-隐私协议 3-委托协议 4-分销说明")
    private Integer type;

    /** 协议富文本内容(html) */
    @Schema(description = "协议内容")
    private String content;

    /** 创建人(用户名) */
    @Schema(description = "创建人")
    private String createBy;

    /** 更新人(用户名) */
    @Schema(description = "更新人")
    private String updateBy;

    /** 创建时间 */
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除 0未删 1已删 */
    @JsonIgnore
    @TableLogic
    private Integer isDeleted;
}