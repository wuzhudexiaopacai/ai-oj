package com.github.wuzhudexiaopacai.oj.ai.service;

/**
 * 提示词模板服务（DB 加载 + Redis Hash 缓存，管理端可热更新）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface PromptTemplateService {

    /**
     * 按模板编码获取启用中的模板内容（先查缓存，未命中回源 DB 并回填）
     *
     * @param code 模板编码：hint/review/generate/chat
     * @return 模板内容
     */
    String getTemplate(String code);

    /**
     * 更新模板（先改库，再失效缓存）
     *
     * @param code     模板编码
     * @param name     模板名称（可空表示不修改）
     * @param template 模板内容
     */
    void updateTemplate(String code, String name, String template);
}
