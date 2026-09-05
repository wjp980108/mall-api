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

    /** 进场时间控制(分钟) */
    @Schema(description = "进场时间控制(分钟)", example = "30", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "进场时间控制不能为空")
    @Min(value = 0, message = "进场时间控制不能为负数")
    private Integer enterControlMinute;

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

    /** 最多购买次数(次) */
    @Schema(description = "最多购买次数", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "最多购买次数不能为空")
    @Min(value = 1, message = "最多购买次数至少为1")
    private Integer maxBuyCount;

    /** 开场前禁止委托时间(分钟) */
    @Schema(description = "开场前禁止委托时间(分钟)", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "开场前禁止委托时间不能为空")
    @Min(value = 0, message = "开场前禁止委托时间不能为负数")
    private Integer beforeForbidMinute;

    /** 结束后禁止委托时间(分钟) */
    @Schema(description = "结束后禁止委托时间(分钟)", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "结束后禁止委托时间不能为空")
    @Min(value = 0, message = "结束后禁止委托时间不能为负数")
    private Integer afterForbidMinute;

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