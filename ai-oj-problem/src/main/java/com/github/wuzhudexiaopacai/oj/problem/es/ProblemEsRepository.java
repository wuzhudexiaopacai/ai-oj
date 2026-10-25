package com.github.wuzhudexiaopacai.oj.problem.es;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

/**
 * 题目 ES Repository
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface ProblemEsRepository extends ElasticsearchRepository<ProblemDocument, Long> {

    /**
     * 全文检索（title / description / problemKey 任一匹配）
     *
     * @param title       标题关键字
     * @param description 描述关键字
     * @param problemKey  题号关键字
     * @return 命中文档
     */
    List<ProblemDocument> findByTitleContainingOrDescriptionContainingOrProblemKeyContaining(
            String title, String description, String problemKey);
}
