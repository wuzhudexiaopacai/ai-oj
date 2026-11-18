package com.github.wuzhudexiaopacai.oj.common.core.constant;

/**
 * Redis Key 统一收口（全部带业务前缀，便于排查与治理）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public class RedisKeys {

    private RedisKeys() {
    }

    /**
     * 注销 Token 黑名单：+ jti
     */
    public static final String TOKEN_BLACKLIST = "auth:token:blacklist:";

    /**
     * 图形验证码：+ uuid
     */
    public static final String CAPTCHA = "auth:captcha:";

    /**
     * 用户信息缓存：+ userId
     */
    public static final String USER_INFO = "user:info:";

    /**
     * 用户个人信息：防止缓存击穿/穿透使用布隆过滤器的 key
     */
    public static final String USER_BLOOM = "user:bloom";

    /**
     * 题目详情缓存：+ problemId
     */
    public static final String PROBLEM_DETAIL = "problem:detail:";

    /**
     * 题目列表缓存：+ 查询条件 hash
     */
    public static final String PROBLEM_LIST = "problem:list:";

    /**
     * 接口幂等标记：+ 业务 key
     */
    public static final String IDEMPOTENT = "idem:";

    /**
     * 判题防重复提交锁：+ userId:problemId
     */
    public static final String JUDGE_USER_LOCK = "judge:lock:user:";

    /**
     * 判题执行互斥锁：+ submissionId（MQ 重投/卡死补偿可能并发消费同一提交）
     */
    public static final String JUDGE_EXECUTE_LOCK = "judge:lock:execute:";

    /**
     * 判题进度缓存（WebSocket 断线重连/轮询兜底）：+ userId:submitId
     */
    public static final String JUDGE_PROGRESS = "judge:progress:";

    /**
     * 沙箱负载 Hash：field=sandboxId, value=当前并行任务数
     */
    public static final String SANDBOX_LOAD = "judge:sandbox:load";

    /**
     * 比赛排行榜快照：+ contestId
     */
    public static final String CONTEST_RANK = "contest:rank:";

    /**
     * AI 会话记忆：+ sessionId
     */
    public static final String AI_SESSION = "ai:session:";

    /**
     * AI 调用限流：+ userId
     */
    public static final String AI_RATE_LIMIT = "ai:rate:";

    /**
     * AI 题目上下文缓存：+ problemId
     */
    public static final String AI_PROBLEM_CONTEXT = "ai:problem:ctx:";

    /**
     * 分片上传上下文：+ md5
     */
    public static final String FILE_UPLOAD = "file:upload:";

    /**
     * 文件 MD5 秒传映射：+ md5
     */
    public static final String FILE_MD5_MAPPING = "file:md5:";
}
