package com.atguigu.meet.model.vo.permission.user;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private LocalDate birthday;

    private String phone;

    private Boolean status;

    /** 邀请人ID */
    private Long inviterId;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 当前登录用户权限集合（仅 user-info 接口返回） */
    private Set<String> permissions;
}