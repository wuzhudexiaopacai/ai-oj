package com.github.wuzhudexiaopacai.oj.problem.service;

import com.github.wuzhudexiaopacai.oj.problem.vo.TagTreeVO;

import java.util.List;

/**
 * 标签服务
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface TagService {

    /**
     * 标签分类树（带缓存）
     *
     * @return 分类树
     */
    List<TagTreeVO> tree();
}
