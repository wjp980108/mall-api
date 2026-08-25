package com.atguigu.meet.model.dto.auth;

import com.atguigu.meet.model.dto.permission.user.UserBaseDTO;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 用户注册请求DTO
 * 必传字段：username、password、phone
 * 其他字段非必传，未传时使用默认值：
 * - nickname、email：null（父类有 @Length/@Email 约束，null 跳过校验）
 * - gender："0"（未知）
 * - age：0
 * - avatar：""（空字符串）
 * - birthday：null（日期无自然默认值）
 * - status："1"（正常）
 *
 * setter 覆盖策略：
 * - nickname/email：前端传 "" 时转 null，避免父类 @Length/@Email 校验失败
 * - gender/status：前端传 "" 时保留构造函数默认值
 *
 * getter 覆盖策略：
 * - 校验框架读 getter 上的注解规则做校验
 * - 只在 getter 上加 @NotBlank，确保只有三个必传字段
 */
@Data
public class AuthRegisterDTO extends UserBaseDTO {

    public AuthRegisterDTO() {
        setGender(0);
        setAge(0);
        setAvatar("");
        setStatus(Boolean.TRUE);
    }

    @Override
    public void setNickname(String nickname) {
        super.setNickname((nickname == null || nickname.isEmpty()) ? null : nickname);
    }

    @Override
    public void setEmail(String email) {
        super.setEmail((email == null || email.isEmpty()) ? null : email);
    }

    @Override
    public void setGender(Integer gender) {
        if (gender != null) {
            super.setGender(gender);
        }
    }

    @Override
    public void setStatus(Boolean status) {
        if (status != null) {
            super.setStatus(status);
        }
    }

    @NotBlank(message = "用户名不能为空")
    @Override
    public String getUsername() {
        return super.getUsername();
    }

    @NotBlank(message = "密码不能为空")
    @Override
    public String getPassword() {
        return super.getPassword();
    }

    @NotBlank(message = "手机号不能为空")
    @Override
    public String getPhone() {
        return super.getPhone();
    }

    /**
     * 邀请码（非必填，8位，区分大小写，数字+字母）
     * 未传或传空时跳过邀请码校验与邀请流水处理，inviterId 置空。
     */
    @Pattern(regexp = "^$|^[A-Za-z0-9]{8}$", message = "邀请码格式不正确")
    private String inviteCode;

    public void setInviteCode(String inviteCode) {
        this.inviteCode = (inviteCode == null || inviteCode.isEmpty()) ? null : inviteCode;
    }

}