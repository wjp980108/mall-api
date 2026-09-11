package com.atguigu.meet.model.vo.admin.home;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 后台首页统计聚合结果
 * <p>
 * 单接口返回首页看板核心指标：会员统计（总数 / 今日新增）与文件统计（总数）。
 */
@Data
@Schema(description = "后台首页统计")
public class HomeStatsVO {

    /** 会员统计 */
    @Schema(description = "会员统计")
    private MemberStats member;

    /** 文件统计 */
    @Schema(description = "文件统计")
    private ImageStats image;

    /**
     * 会员统计
     */
    @Data
    @Schema(description = "会员统计")
    public static class MemberStats {

        /** 会员总数（sys_user 中未逻辑删除的记录数） */
        @Schema(description = "会员总数")
        private Long total;

        /** 今日新增会员数（Asia/Shanghai 当日 [00:00, 次日00:00)） */
        @Schema(description = "今日新增会员数")
        private Long todayNew;
    }

    /**
     * 文件统计
     * <p>
     * 注意：total 为 t_file_info 中全部未逻辑删除文件数，不按后缀 / 业务类型过滤。
     */
    @Data
    @Schema(description = "文件统计")
    public static class ImageStats {

        /** 文件总数（t_file_info 中未逻辑删除的全部记录数，不区分图片/文档） */
        @Schema(description = "文件总数")
        private Long total;
    }
}
