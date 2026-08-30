package com.atguigu.meet.model.entity.general.agreement;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户协议实体（单例行设计）
 * <p>系统只维护一份最新版本的用户协议，表内固定只有一条生效记录（id=1），
 * 首次保存后仅做覆盖更新，不做历史版本记录。
 */
@Data
@TableName("sys_user_agreement")
public class SysUserAgreement extends Model<SysUserAgreement> {

    /** 单例行固定主键：全表仅此一条生效数据 */
    public static final long SINGLETON_ID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 协议标题 */
    private String title;

    /** 协议富文本内容(html) */
    private String content;

    /** 创建人(用户名) */
    private String createBy;

    /** 更新人(用户名) */
    private String updateBy;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除 0未删 1已删 */
    @JsonIgnore
    @TableLogic
    private Integer isDeleted;
}
