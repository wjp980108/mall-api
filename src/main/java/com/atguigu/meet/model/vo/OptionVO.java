package com.atguigu.meet.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用下拉选项 VO
 * <p>
 * 适用于返回 label/value 结构的下拉框、选择器等场景。
 *
 * @param <T> value 的类型（常见为 Long、String、Integer）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "下拉选项数据")
public class OptionVO<T> {
    /** 选项标签 */
    @Schema(description = "选项标签")
    private String label;
    /** 选项值 */
    @Schema(description = "选项值")
    private T value;
}