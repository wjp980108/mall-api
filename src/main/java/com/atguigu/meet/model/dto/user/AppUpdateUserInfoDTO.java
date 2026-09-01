package com.atguigu.meet.model.dto.user;

import jakarta.validation.constraints.Email;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;

/**
 * H5 端修改用户信息请求DTO
 * 所有字段均非必传：未传（或传空字符串）的字段不更新，保持数据库原有值
 * 不含手机号与密码：手机号绑定、密码修改走独立接口
 */
@Data
public class AppUpdateUserInfoDTO {

    @Length(min = 2, max = 20, message = "昵称长度 2-20 位")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

    /** 性别 0未知 1男 2女 */
    private Integer gender;

    private Integer age;

    private LocalDate birthday;

    private String avatar;

    /** 头像存储平台:local-1/aliyun-oss-1等 */
    private String avatarPlatform;

    /** 空字符串视为未传，避免 @Length/@Email 校验失败 */
    public void setNickname(String nickname) {
        this.nickname = (nickname == null || nickname.isEmpty()) ? null : nickname;
    }

    public void setEmail(String email) {
        this.email = (email == null || email.isEmpty()) ? null : email;
    }

    public void setAvatar(String avatar) {
        this.avatar = (avatar == null || avatar.isEmpty()) ? null : avatar;
    }

    public void setAvatarPlatform(String avatarPlatform) {
        this.avatarPlatform = (avatarPlatform == null || avatarPlatform.isEmpty()) ? null : avatarPlatform;
    }
}
