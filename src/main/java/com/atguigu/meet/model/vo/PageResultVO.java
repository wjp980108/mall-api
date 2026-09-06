package com.atguigu.meet.model.vo;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @Description
 * @Date 2026-05-09 15:40
 */
@Data
@Schema(description = "分页响应数据")
public class PageResultVO<T> {
    @Schema(description = "数据列表")
    private List<T> list;        // 数据列表
    @Schema(description = "总条数")
    private Long total;          // 总条数
    @Schema(description = "总页数")
    private Long pages;          // 总页数
    @Schema(description = "当前页")
    private Long current;        // 当前页
    @Schema(description = "每页条数")
    private Long size;           // 每页条数

    public static <T> PageResultVO<T> of(IPage<T> page) {
        PageResultVO<T> result = new PageResultVO<>();
        result.setList(page.getRecords());    // 数据列表
        result.setTotal(page.getTotal());    // 总条数
        result.setPages(page.getPages());    // 总页数
        result.setCurrent(page.getCurrent());// 当前页
        result.setSize(page.getSize());      // 每页条数
        return result;
    }
}