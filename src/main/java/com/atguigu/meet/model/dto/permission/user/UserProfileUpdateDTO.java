package com.atguigu.meet.model.dto.permission.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 当前登录用户修改个人信息请求DTO
 * <p>
 * 所有字段均非必传：未传（或传空字符串）的字段不更新，保持数据库原有值；
 * 不含用户名/密码/状态：用户名不可自助修改，密码走独立改密接口；
 * 手机号可自助绑定/修改（内置超管 admin 初始无手机号，首次绑定同样走格式与撞号校验）。
 */
@Data
@Schema(description = "当前用户修改个人信息参数")
public class UserProfileUpdateDTO {

    @Schema(description = "昵称", example = "张三")
    @Length(min = 2, max = 20, message = "昵称长度 2-20 位")
    private String nickname;

    @Schema(description = "手机号", example = "13800138000")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 性别 0未知 1男 2女 */
    @Schema(description = "性别", example = "1", allowableValues = {"0", "1", "2"})
    private Integer gender;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    /** 头像存储平台:local-1/aliyun-oss-1等 */
    @Schema(description = "头像存储平台", example = "aliyun-oss-1")
    private String avatarPlatform;

    /** 空字符串视为未传，避免 @Length/@Pattern/@Email 校验失败 */
    public void setNickname(String nickname) {
        this.nickname = (nickname == null || nickname.isEmpty()) ? null : nickname;
    }

    public void setPhone(String phone) {
        this.phone = (phone == null || phone.isEmpty()) ? null : phone;
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
