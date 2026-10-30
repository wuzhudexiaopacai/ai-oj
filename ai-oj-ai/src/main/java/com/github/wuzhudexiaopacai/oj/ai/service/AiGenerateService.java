package com.github.wuzhudexiaopacai.oj.ai.service;

import com.github.wuzhudexiaopacai.oj.ai.dto.ProblemGenerateDTO;
import com.github.wuzhudexiaopacai.oj.ai.vo.ProblemDraftVO;

/**
 * AI 出题服务（同步生成结构化题目草稿，供前端回填编辑表单）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface AiGenerateService {

    /**
     * 根据知识点、难度与自定义要求，同步生成一道结构化题目草稿。
     *
     * @param req 出题请求
     * @return 结构化题目草稿
     */
    ProblemDraftVO generate(ProblemGenerateDTO req);
}
