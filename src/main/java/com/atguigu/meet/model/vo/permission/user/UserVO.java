package com.atguigu.meet.model.vo.permission.user;

import com.atguigu.meet.model.vo.permission.role.RoleVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 用户响应VO
 */
@Data
@Schema(description = "用户响应数据")
public class UserVO {
    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    /** 性别 0未知 1男 2女
     * @see com.atguigu.meet.enums.Gender
     */
    @Schema(description = "性别 0未知 1男 2女")
    private Integer gender;

    /** 性别中文名：未知/男/女（由 Service 层通过枚举组装） */
    @Schema(description = "性别中文名")
    private String genderName;

    @Schema(description = "年龄")
    private Integer age;

    @Schema(description = "头像URL")
    private String avatar;

    /** 头像存储平台:local-1/aliyun-oss-1等 */
    @Schema(description = "头像存储平台")
    private String avatarPlatform;

    @Schema(description = "生日")
    private LocalDate birthday;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "状态 1启用 0禁用")
    private Boolean status;

    /** 邀请人ID */
    @Schema(description = "邀请人ID")
    private Long inviterId;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 当前登录用户权限集合（仅 user-info 接口返回） */
    @Schema(description = "权限集合")
    private Set<String> permissions;

    /** 用户绑定的角色ID列表（用户列表、编辑用户回显时使用） */
    @Schema(description = "角色ID列表")
    private List<Long> roleIds;

    /** 用户绑定的角色名称，逗号拼接，前端列表直接展示用 */
    @Schema(description = "角色名称")
    private String roleNames;

    /** 用户绑定的角色完整信息列表（含 id、roleName、roleCode、status 等） */
    @Schema(description = "角色信息列表")
    private List<RoleVO> roles;
}