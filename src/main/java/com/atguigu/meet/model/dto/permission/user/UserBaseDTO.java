package com.atguigu.meet.model.dto.permission.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;

/**
 * @Description
 * @Date 2026-05-11 14:56
 */
@Data
@Schema(description = "用户基础信息")
public class UserBaseDTO {
    @Schema(description = "用户名", example = "admin")
    @Length(min = 2, max = 20, message = "用户名长度 2-20 位")
    private String username;

    @Schema(description = "昵称", example = "张三")
    @Length(min = 2, max = 20, message = "昵称长度 2-20 位")
    private String nickname;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "密码", example = "123456")
    @Length(min = 6, max = 20, message = "密码长度 6-20 位")
    private String password;

    @Schema(description = "性别", example = "1")
    private Integer gender;

    @Schema(description = "年龄", example = "25")
    private Integer age;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatar;

    /** 头像存储平台:local-1/aliyun-oss-1等 */
    @Schema(description = "头像存储平台", example = "aliyun-oss-1")
    private String avatarPlatform;

    @Schema(description = "生日", example = "1990-01-01")
    private LocalDate birthday;

    @Schema(description = "手机号", example = "13800138000")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "状态", example = "true")
    private Boolean status;
}