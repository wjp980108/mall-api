package com.atguigu.meet.model.vo.permission.invite;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 邀请码存量补偿结果
 * <p>
 * 一次性运维能力：扫描 sys_user 中存在但 sys_invite_code 中无对应邀请码的用户，
 * 逐个调用 {@code generateInviteCode}（依赖其「1 人 1 码」幂等检查）。
 * 失败时跳过单个用户、记录失败列表返回。
 */
@Data
@Schema(description = "邀请码存量补偿结果")
public class CompensateResultVO {

    /** 本次新补生成的邀请码数量 */
    @Schema(description = "本次新补生成数量")
    private int successCount;

    /** 已有邀请码被跳过的用户数量 */
    @Schema(description = "已有邀请码跳过数量")
    private int skippedCount;

    /** 补生成失败的用户列表（供人工排查） */
    @Schema(description = "补生成失败列表")
    private List<Failure> failures;

    public CompensateResultVO() {
    }

    public CompensateResultVO(int successCount, int skippedCount, List<Failure> failures) {
        this.successCount = successCount;
        this.skippedCount = skippedCount;
        this.failures = failures;
    }

    /**
     * 补生成失败记录
     */
    @Data
    @AllArgsConstructor
    @Schema(description = "补生成失败记录")
    public static class Failure {
        @Schema(description = "用户ID")
        private Long userId;
        @Schema(description = "用户名")
        private String username;
        @Schema(description = "失败原因")
        private String reason;
    }
}
