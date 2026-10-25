package com.github.wuzhudexiaopacai.oj.common.web.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
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
public class PageResult<T> implements Serializable {

    private List<T> records;

    private long total;

    private long pageNum;

    private long pageSize;

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
