package com.github.wuzhudexiaopacai.oj.common.web.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;

/**
 * 分页返回体
 *
 * @param <T> 记录类型
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "分页返回体")
public class PageResult<T> implements Serializable {

    @Schema(description = "当前页记录列表")
    private List<T> records;

    @Schema(description = "总记录数", example = "128")
    private long total;

    @Schema(description = "当前页码，从 1 开始", example = "1")
    private long pageNum;

    @Schema(description = "每页条数", example = "10")
    private long pageSize;

    @Schema(description = "总页数", example = "13")
    private long pages;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(page.getRecords());
        result.setTotal(page.getTotal());
        result.setPageNum(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setPages(page.getPages());
        return result;
    }

    /**
     * 分页转换（DO → VO 场景）
     *
     * @param page      原分页
     * @param converter 转换函数
     * @param <T>       目标类型
     * @param <R>       源类型
     * @return 转换后的分页
     */
    public static <T, R> PageResult<T> of(IPage<R> page, Function<R, T> converter) {
        PageResult<T> result = new PageResult<>();
        result.setRecords(page.getRecords().stream().map(converter).toList());
        result.setTotal(page.getTotal());
        result.setPageNum(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setPages(page.getPages());
        return result;
    }
}
