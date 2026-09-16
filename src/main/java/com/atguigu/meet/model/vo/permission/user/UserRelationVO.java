package com.atguigu.meet.model.vo.permission.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 用户上下级邀请关系 VO（一级直邀，不递归）
 */
@Data
@Schema(description = "用户上下级邀请关系数据")
public class UserRelationVO {

    @Schema(description = "上级简化对象（无上级或上级为内置超管时为 null）")
    private SimpleUserVO upline;

    @Schema(description = "直邀下级简化对象数组（仅一级，不递归）")
    private List<SimpleUserVO> downlines;
}
