package com.atguigu.meet.model.dto.seckill.session;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 抢购场次新增DTO
 */
@Data
public class SessionSaveDTO {

    /** 场次名称：上午场/下午场 */
    @NotBlank(message = "场次名称不能为空")
    @Size(max = 64, message = "场次名称长度不能超过64")
    private String sessionName;

    /** 场次状态 true开启 false关闭（不传默认 true） */
    private Boolean sessionStatus;

    /** 进场时间控制(分钟) */
    @NotNull(message = "进场时间控制不能为空")
    @Min(value = 0, message = "进场时间控制不能为负数")
    private Integer enterControlMinute;

    /** 抢购开始时间 */
    @NotNull(message = "抢购开始时间不能为空")
    private LocalDateTime rushStartTime;

    /** 抢购结束时间 */
    @NotNull(message = "抢购结束时间不能为空")
    private LocalDateTime rushEndTime;

    /** 最多购买次数(次) */
    @NotNull(message = "最多购买次数不能为空")
    @Min(value = 1, message = "最多购买次数至少为1")
    private Integer maxBuyCount;

    /** 开场前禁止委托时间(分钟) */
    @NotNull(message = "开场前禁止委托时间不能为空")
    @Min(value = 0, message = "开场前禁止委托时间不能为负数")
    private Integer beforeForbidMinute;

    /** 结束后禁止委托时间(分钟) */
    @NotNull(message = "结束后禁止委托时间不能为空")
    @Min(value = 0, message = "结束后禁止委托时间不能为负数")
    private Integer afterForbidMinute;

    /** 场次背景图地址 */
    @Size(max = 255, message = "背景图地址长度不能超过255")
    private String bgImg;

    /** 场次背景图存储平台:local-1/aliyun-oss-1等 */
    private String bgImgPlatform;

    /** 排序号 */
    @Min(value = 0, message = "排序号不能为负数")
    private Integer sort;
}
