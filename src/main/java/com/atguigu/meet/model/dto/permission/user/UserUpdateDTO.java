package com.atguigu.meet.model.dto.permission.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 用户更新请求DTO
 * 所有字段（除 id 外）均非必传：
 * - 未传的字段保持 null，更新时不会覆盖数据库原有值
 * - 前端传 "" 时通过 setter 转为 null，避免父类 @Length/@Email/@Pattern 校验失败
 * - id 为必传字段（@NotNull）
 */
@Data
public class UserUpdateDTO extends UserBaseDTO {
    @NotNull(message = "用户 id 不能为空")
    private Long id;

    @Override
    public void setUsername(String username) {
        super.setUsername((username == null || username.isEmpty()) ? null : username);
    }

    @Override
    public void setNickname(String nickname) {
        super.setNickname((nickname == null || nickname.isEmpty()) ? null : nickname);
    }

    @Override
    public void setEmail(String email) {
        super.setEmail((email == null || email.isEmpty()) ? null : email);
    }

    /**
     * 用户信息更新接口禁止修改密码，password 字段永远设为 null。
     * 密码修改需走独立的改密接口。
     */
    @Override
    public void setPassword(String password) {
        super.setPassword(null);
    }

    @Override
    public void setGender(Integer gender) {
        super.setGender(gender);
    }

    @Override
    public void setAvatar(String avatar) {
        super.setAvatar((avatar == null || avatar.isEmpty()) ? null : avatar);
    }

    @Override
    public void setAvatarPlatform(String avatarPlatform) {
        super.setAvatarPlatform((avatarPlatform == null || avatarPlatform.isEmpty()) ? null : avatarPlatform);
    }

    @Override
    public void setPhone(String phone) {
        super.setPhone((phone == null || phone.isEmpty()) ? null : phone);
    }

    @Override
    public void setStatus(Boolean status) {
        super.setStatus(status);
    }
}
