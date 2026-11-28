package com.github.wuzhudexiaopacai.oj.common.core.constant;

/**
 * RocketMQ Topic / Tag / 消费组统一常量
 * <p>
 * 死信队列说明：RocketMQ 自动为消费失败超限的消息生成 %DLQ%${consumerGroup} 队列，
 * 各服务需为核心消费组注册对应 DLQ 消费者做归档与补偿。判题提交链路有独立死信表
 * （biz_dead_letter，支持管理端重放）；其余消费组以 ERROR 日志归档消息体（Kibana 可检索），
 * 需要重放时可在 RocketMQ 控制台按 msgId 处理。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public class MqConstants {

    private MqConstants() {
    }

    /**
     * 判题提交 Topic（核心链路：提交 → 异步判题）
     */
    public static final String JUDGE_SUBMIT_TOPIC = "AI_OJ_JUDGE_SUBMIT";

    /**
     * 判题结果 Topic（判题完成广播：统计、比赛记录等多方订阅）
     */
    public static final String JUDGE_RESULT_TOPIC = "AI_OJ_JUDGE_RESULT";

    /**
     * 题目数据同步 Topic（MySQL → Elasticsearch 最终一致性）
     */
    public static final String PROBLEM_SYNC_TOPIC = "AI_OJ_PROBLEM_SYNC";

    /**
     * 登录日志 Topic（登录行为异步落库）
     */
    public static final String LOGIN_LOG_TOPIC = "AI_OJ_LOGIN_LOG";

    /**
     * 操作日志 Topic（业务写操作异步落库，统一由 ai-oj-log 服务消费）
     */
    public static final String OPERATION_LOG_TOPIC = "AI_OJ_OPERATION_LOG";

    /**
     * 文件清理 Topic（分片临时块延迟清理，利用 RocketMQ 延时消息）
     */
    public static final String FILE_CLEAN_TOPIC = "AI_OJ_FILE_CLEAN";

    /**
     * AI 异步任务 Topic（批量代码评审、出题等非实时任务）
     */
    public static final String AI_TASK_TOPIC = "AI_OJ_AI_TASK";

    /**
     * 比赛事件 Topic（开赛/封榜/结束广播）
     */
    public static final String CONTEST_EVENT_TOPIC = "AI_OJ_CONTEST_EVENT";

    /**
     * 判题消费组
     */
    public static final String GROUP_JUDGE = "ai-oj-judge-submit-consumer";

    /**
     * 判题结果消费组（题目统计）
     */
    public static final String GROUP_PROBLEM_STAT = "ai-oj-problem-stat-consumer";

    /**
     * 判题结果消费组（比赛成绩）
     */
    public static final String GROUP_CONTEST_RECORD = "ai-oj-contest-record-consumer";

    /**
     * 判题结果消费组（用户提交/通过统计）
     */
    public static final String GROUP_USER_STAT = "ai-oj-user-stat-consumer";

    /**
     * 题目 ES 同步消费组
     */
    public static final String GROUP_PROBLEM_ES = "ai-oj-problem-es-consumer";

    /**
     * 登录日志消费组
     */
    public static final String GROUP_LOGIN_LOG = "ai-oj-login-log-consumer";

    /**
     * 操作日志消费组
     */
    public static final String GROUP_OPERATION_LOG = "ai-oj-operation-log-consumer";

    /**
     * 文件清理消费组
     */
    public static final String GROUP_FILE_CLEAN = "ai-oj-file-clean-consumer";

    /**
     * AI 异步任务消费组
     */
    public static final String GROUP_AI_TASK = "ai-oj-ai-task-consumer";

    /**
     * 判题死信处理消费组（订阅 %DLQ%ai-oj-judge-submit-consumer）
     */
    public static final String GROUP_JUDGE_DLQ = "ai-oj-judge-dlq-consumer";

    /**
     * 用户统计死信消费组（订阅 %DLQ%ai-oj-user-stat-consumer）
     */
    public static final String GROUP_USER_STAT_DLQ = "ai-oj-user-stat-dlq-consumer";

    /**
     * 题目统计死信消费组（订阅 %DLQ%ai-oj-problem-stat-consumer）
     */
    public static final String GROUP_PROBLEM_STAT_DLQ = "ai-oj-problem-stat-dlq-consumer";

    /**
     * 题目 ES 同步死信消费组（订阅 %DLQ%ai-oj-problem-es-consumer）
     */
    public static final String GROUP_PROBLEM_ES_DLQ = "ai-oj-problem-es-dlq-consumer";

    /**
     * 文件清理死信消费组（订阅 %DLQ%ai-oj-file-clean-consumer）
     */
    public static final String GROUP_FILE_CLEAN_DLQ = "ai-oj-file-clean-dlq-consumer";

    /**
     * 比赛成绩死信消费组（订阅 %DLQ%ai-oj-contest-record-consumer）
     */
    public static final String GROUP_CONTEST_RECORD_DLQ = "ai-oj-contest-record-dlq-consumer";

    /**
     * 判题失败最大业务重试次数（超过后进入死信）
     */
    public static final int JUDGE_MAX_RETRY = 3;
}
