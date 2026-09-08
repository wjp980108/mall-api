package com.atguigu.meet.model.dto.seckill.session;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalTime;

/**
 * 抢购场次新增DTO
 */
@Data
@Schema(description = "抢购场次新增参数")
public class SessionSaveDTO {

    /** 场次名称：上午场/下午场 */
    @Schema(description = "场次名称", example = "上午场", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "场次名称不能为空")
    @Size(max = 64, message = "场次名称长度不能超过64")
    private String sessionName;

    /** 场次状态 true开启 false关闭（不传默认 true） */
    @Schema(description = "场次状态", example = "true")
    private Boolean sessionStatus;

    // 注：enterControlMinute（进场时间控制）为预留字段，写入路径已切断（cleanup-session-reserved-fields），
    // 不在新增/修改入参中接收；DB 列与实体字段保留，未来启用时加回本字段+校验注解+前端表单项并补业务逻辑

    /** 每日抢购开始时间（时:分，例："09:50"） */
    @Schema(description = "抢购开始时间", example = "09:50", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "抢购开始时间不能为空")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime rushStartTime;

    /** 每日抢购结束时间（时:分，例："17:00"，不支持跨天） */
    @Schema(description = "抢购结束时间", example = "17:00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "抢购结束时间不能为空")
    @JsonFormat(pattern = "HH:mm")
    private LocalTime rushEndTime;

    // 注：beforeForbidMinute/afterForbidMinute（开场前/结束后禁止委托时间）为预留字段，写入路径已切断
    // （cleanup-session-reserved-fields），"禁止委托"业务逻辑尚未实现；未来启用时加回字段+校验+前端表单项

    /** 场次背景图地址 */
    @Schema(description = "背景图地址", example = "https://example.com/bg.jpg")
    @Size(max = 255, message = "背景图地址长度不能超过255")
    private String bgImg;

    /** 场次背景图存储平台:local-1/aliyun-oss-1等 */
    @Schema(description = "背景图存储平台", example = "aliyun-oss-1")
    private String bgImgPlatform;

    /** 排序号 */
    @Schema(description = "排序号", example = "100")
    @Min(value = 0, message = "排序号不能为负数")
    private Integer sort;
}