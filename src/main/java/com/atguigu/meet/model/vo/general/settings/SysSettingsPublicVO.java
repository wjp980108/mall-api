package com.atguigu.meet.model.vo.general.settings;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 系统设置公开查询 VO（无需 token, 登录页等展示站点信息用）
 * <p>
 * 只暴露站点名称/Logo 等前端展示字段, 脱敏内部业务参数.
 */
@Data
@Schema(description = "系统设置公开信息")
public class SysSettingsPublicVO implements Serializable {

    /** 站点名称 */
    @Schema(description = "站点名称")
    private String siteName;

    /** 站点Logo图片URL */
    @Schema(description = "站点Logo图片URL")
    private String siteLogo;
}
