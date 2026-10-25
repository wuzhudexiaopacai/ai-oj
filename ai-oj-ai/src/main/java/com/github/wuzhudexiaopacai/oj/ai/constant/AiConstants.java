package com.github.wuzhudexiaopacai.oj.ai.constant;

/**
 * AI 模块常量收口
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public class AiConstants {

    private AiConstants() {
    }

    /**
     * 场景：自由对话
     */
    public static final String SCENE_CHAT = "chat";

    /**
     * 场景：解题提示
     */
    public static final String SCENE_HINT = "hint";

    /**
     * 场景：代码评审
     */
    public static final String SCENE_REVIEW = "review";

    /**
     * 场景：AI 出题
     */
    public static final String SCENE_GENERATE = "generate";

    /**
     * 提示词模板 Redis Hash key
     */
    public static final String PROMPT_TEMPLATE_HASH = "ai:prompt:template";

    /**
     * 异步任务 Redis key 前缀
     */
    public static final String AI_TASK_KEY = "ai:task:";

    /**
     * 异步任务状态：待执行
     */
    public static final String TASK_PENDING = "PENDING";

    /**
     * 异步任务状态：成功
     */
    public static final String TASK_SUCCESS = "SUCCESS";

    /**
     * 异步任务状态：失败
     */
    public static final String TASK_FAILED = "FAILED";

    /**
     * 异步任务类型：出题
     */
    public static final String TASK_TYPE_GENERATE = "generate";

    /**
     * 异步任务类型：代码评审
     */
    public static final String TASK_TYPE_REVIEW = "review";

    /**
     * SSE 超时时间（毫秒）
     */
    public static final long SSE_TIMEOUT_MS = 180_000L;

    /**
     * 角色：用户
     */
    public static final String ROLE_USER = "user";

    /**
     * 角色：助手
     */
    public static final String ROLE_ASSISTANT = "assistant";
}
