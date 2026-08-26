package com.atguigu.meet.model.vo.permission.user;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 用户响应VO
 */
@Data
public class UserVO {
    private Long id;

    private String username;

    private String nickname;

    private String email;

    private Integer gender;

    private Integer age;

    private String avatar;

    /** 头像存储平台:local-1/aliyun-oss-1等 */
    private String avatarPlatform;

    private LocalDate birthday;

    private String phone;

    private Boolean status;

    /** 邀请人ID */
    private Long inviterId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 当前登录用户权限集合（仅 user-info 接口返回） */
    private Set<String> permissions;

    /** 用户绑定的角色ID列表（用户列表、编辑用户回显时使用） */
    private List<Long> roleIds;

    /** 用户绑定的角色名称，逗号拼接，前端列表直接展示用 */
    private String roleNames;
}