package com.atguigu.meet.model.dto.permission.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 后台创建用户请求DTO
 * 必传字段：username、password、phone
 * 其他字段非必传，未传时使用默认值：
 * - nickname、email：null（父类有 @Length/@Email 约束，null 跳过校验）
 * - gender："0"（未知）
 * - age：0
 * - avatar：""（空字符串）
 * - birthday：null（日期无自然默认值）
 * - status：true（正常）
 *
 * 角色由前端传入：roleIds（可选，为空时不分配角色）
 */
@Data
@Schema(description = "后台创建用户参数")
public class UserCreateDTO extends UserBaseDTO {

    public UserCreateDTO() {
        setGender(0);
        setAge(0);
        setAvatar("");
        setAvatarPlatform("");
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

    @Schema(description = "用户名", example = "zhangsan", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "用户名不能为空")
    @Override
    public String getUsername() {
        return super.getUsername();
    }

    @Schema(description = "密码", example = "123456")
    @Override
    public String getPassword() {
        return super.getPassword();
    }

    @Schema(description = "手机号", example = "13800138000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "手机号不能为空")
    @Override
    public String getPhone() {
        return super.getPhone();
    }

    /** 角色ID列表（由前端传入，可选，为空时不分配角色） */
    @Schema(description = "角色ID列表", example = "[1, 2]")
    private List<Long> roleIds;

}