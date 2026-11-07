package com.github.wuzhudexiaopacai.oj.ai.service.impl;

import com.github.wuzhudexiaopacai.oj.ai.config.AiProperties;
import com.github.wuzhudexiaopacai.oj.ai.constant.AiConstants;
import com.github.wuzhudexiaopacai.oj.ai.dto.MemoryMessage;
import com.github.wuzhudexiaopacai.oj.ai.dto.ReviewRequestDTO;
import com.github.wuzhudexiaopacai.oj.ai.entity.AiSession;
import com.github.wuzhudexiaopacai.oj.ai.service.AiChatService;
import com.github.wuzhudexiaopacai.oj.ai.service.AiSessionService;
import com.github.wuzhudexiaopacai.oj.ai.strategy.AiSceneStrategyFactory;
import com.github.wuzhudexiaopacai.oj.ai.util.TokenEstimator;
import com.github.wuzhudexiaopacai.oj.api.dto.ProblemJudgeDTO;
import com.github.wuzhudexiaopacai.oj.api.feign.ProblemFeignClient;
import com.github.wuzhudexiaopacai.oj.common.core.constant.RedisKeys;
import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import com.github.wuzhudexiaopacai.oj.common.core.exception.Asserts;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.web.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AI 对话服务实现（Spring AI 流式输出 + Redis 会话记忆 + Redisson 限流）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    /**
     * 代码片段最大字符数（超出截断，防止占满模型上下文）
     */
    private static final int MAX_CODE_CHARS = 8000;

    /**
     * 题面单项最大字符数
     */
    private static final int MAX_PROBLEM_CHARS = 4000;

    /**
     * 语言名最大长度
     */
    private static final int MAX_LANGUAGE_CHARS = 20;

    private final ChatClient chatClient;

    private final AiSessionService sessionService;

    private final AiSceneStrategyFactory strategyFactory;

    private final ProblemFeignClient problemFeignClient;

    private final RedisTemplate<String, Object> redisTemplate;

    private final RedissonClient redissonClient;

    private final AiProperties aiProperties;

    @Override
    public SseEmitter streamChat(Long sessionId, String question, String code, String language) {
        LoginUser loginUser = SecurityUtils.requireLogin();
        Asserts.notBlank(question, "问题不能为空");

        // 1. Redisson 限流：每用户每分钟 ai-oj.ai.rate-limit-per-minute 次
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(RedisKeys.AI_RATE_LIMIT + loginUser.getId());
        rateLimiter.trySetRate(RateType.OVERALL, aiProperties.getRateLimitPerMinute(), 1, RateIntervalUnit.MINUTES);
        Asserts.isTrue(rateLimiter.tryAcquire(), "请求过于频繁，请稍后再试");

        // 2. 会话归属校验 + 场景策略渲染提示词（注入题目与当前代码上下文）
        AiSession session = sessionService.requireOwnedSession(sessionId);
        String prompt = strategyFactory.getStrategy(session.getScene())
                .buildPrompt(buildSceneVars(session, question, code, language));

        // 3. 装配消息：历史记忆 + 当前问题
        List<Message> messages = new ArrayList<>(loadMemory(sessionId));
        messages.add(new UserMessage(prompt));

        // 4. SSE 流式推送（180s 超时）
        SseEmitter emitter = new SseEmitter(AiConstants.SSE_TIMEOUT_MS);
        emitter.onTimeout(emitter::complete);
        StringBuilder answer = new StringBuilder();
        AtomicReference<Disposable> subscriptionRef = new AtomicReference<>();
        // 模型（如 DashScope）会在流式末帧携带 usage，逐帧捕获最后一次有效值用于 token 统计
        AtomicReference<Usage> usageRef = new AtomicReference<>();
        Disposable subscription = chatClient.prompt().messages(messages).stream().chatResponse()
                .subscribe(
                        response -> {
                            String token = extractText(response);
                            if (token != null && !token.isEmpty()) {
                                answer.append(token);
                                sendToken(emitter, token, subscriptionRef);
                            }
                            Usage usage = extractUsage(response);
                            if (usage != null) {
                                usageRef.set(usage);
                            }
                        },
                        error -> {
                            log.error("AI 流式对话失败: sessionId={}", sessionId, error);
                            emitter.completeWithError(error);
                        },
                        () -> onComplete(emitter, sessionId, question, answer.toString(), prompt, usageRef.get()));
        subscriptionRef.set(subscription);
        return emitter;
    }

    /**
     * 从流式帧中取出增量文本（usage-only 帧无文本）
     */
    private String extractText(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return null;
        }
        return response.getResult().getOutput().getText();
    }

    /**
     * 从流式帧中取出 usage，未携带或计数为空时返回 null
     */
    private Usage extractUsage(ChatResponse response) {
        if (response == null || response.getMetadata() == null) {
            return null;
        }
        Usage usage = response.getMetadata().getUsage();
        if (usage == null || usage.getTotalTokens() == null || usage.getTotalTokens() <= 0) {
            return null;
        }
        return usage;
    }

    @Override
    public String review(ReviewRequestDTO req) {
        SecurityUtils.requireLogin();
        Map<String, String> vars = new HashMap<>();
        vars.put("language", req.getLanguage());
        vars.put("code", req.getCode());
        String prompt = strategyFactory.getStrategy(AiConstants.SCENE_REVIEW).buildPrompt(vars);
        return chatClient.prompt().messages(new UserMessage(prompt)).call().content();
    }

    /**
     * 逐 token 推送（emitter 加锁保证线程安全，客户端断开时取消订阅）
     */
    private void sendToken(SseEmitter emitter, String token, AtomicReference<Disposable> subscriptionRef) {
        synchronized (emitter) {
            try {
                emitter.send(SseEmitter.event().data(token));
            } catch (IOException | IllegalStateException e) {
                log.warn("SSE 推送失败，取消流式订阅: {}", e.getMessage());
                Disposable disposable = subscriptionRef.get();
                if (disposable != null) {
                    disposable.dispose();
                }
            }
        }
    }

    /**
     * 流式完成：写入会话记忆并异步落库
     *
     * <p>token 统计优先取模型返回的 usage（prompt/completion），
     * 流式响应未携带 usage 时按字符数估算，保证 ai_message.tokens 有实际意义。</p>
     */
    private void onComplete(SseEmitter emitter, Long sessionId, String question, String answer,
                            String prompt, Usage usage) {
        try {
            appendMemory(sessionId, question, answer);
            int questionTokens = resolveTokens(usage == null ? null : usage.getPromptTokens(), prompt);
            int answerTokens = resolveTokens(usage == null ? null : usage.getCompletionTokens(), answer);
            sessionService.saveMessageAsync(sessionId, AiConstants.ROLE_USER, question, questionTokens);
            sessionService.saveMessageAsync(sessionId, AiConstants.ROLE_ASSISTANT, answer, answerTokens);
        } catch (Exception e) {
            log.error("AI 会话记忆写入失败: sessionId={}", sessionId, e);
        } finally {
            emitter.complete();
        }
    }

    /**
     * 取实际 token 数，模型未返回时按文本估算
     */
    private int resolveTokens(Integer actualTokens, String fallbackText) {
        if (actualTokens != null && actualTokens > 0) {
            return actualTokens;
        }
        return TokenEstimator.estimate(fallbackText);
    }

    /**
     * 构建场景模板变量（注入题目全文与用户当前代码，Feign 失败降级不阻塞主链路）
     */
    private Map<String, String> buildSceneVars(AiSession session, String question, String code, String language) {
        Map<String, String> vars = new HashMap<>();
        vars.put("question", question);
        // chat/hint 场景都携带题目上下文，AI 才能"记住"用户正在做的题
        vars.put("problem", loadProblemContext(session.getProblemId()));
        // 每次提问实时上报当前编辑器代码，用户改动代码后追问也能被理解
        vars.put("code", buildCodeContext(code, language));
        vars.put("language", truncate(StringUtils.hasText(language) ? language : "", MAX_LANGUAGE_CHARS));
        return vars;
    }

    /**
     * 加载题目上下文（含题面全文，Redis 缓存 30 分钟，避免每次提问都走 Feign）
     */
    private String loadProblemContext(Long problemId) {
        if (problemId == null) {
            return "（未关联具体题目）";
        }
        String cacheKey = RedisKeys.AI_PROBLEM_CONTEXT + problemId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof String text) {
            return text;
        }
        try {
            Result<ProblemJudgeDTO> result = problemFeignClient.getJudgeInfo(problemId);
            if (result != null && result.isSuccess() && result.getData() != null) {
                String text = formatProblem(result.getData());
                redisTemplate.opsForValue().set(cacheKey, text, 30, TimeUnit.MINUTES);
                return text;
            }
        } catch (Exception e) {
            log.warn("题目上下文加载失败，降级处理: problemId={}", problemId, e);
        }
        return "题目 ID " + problemId;
    }

    /**
     * 题目信息格式化为提示词片段
     */
    private String formatProblem(ProblemJudgeDTO problem) {
        StringBuilder sb = new StringBuilder("\n\n【当前题目】\n");
        sb.append("题号：").append(problem.getProblemKey()).append(" 标题：").append(problem.getTitle()).append("\n");
        sb.append("时间限制：").append(problem.getTimeLimit()).append("ms，内存限制：")
                .append(problem.getMemoryLimit()).append("MB\n");
        if (StringUtils.hasText(problem.getDescription())) {
            sb.append("题目描述：\n").append(truncate(problem.getDescription(), MAX_PROBLEM_CHARS)).append("\n");
        }
        if (StringUtils.hasText(problem.getInputDesc())) {
            sb.append("输入说明：\n").append(truncate(problem.getInputDesc(), MAX_PROBLEM_CHARS)).append("\n");
        }
        if (StringUtils.hasText(problem.getOutputDesc())) {
            sb.append("输出说明：\n").append(truncate(problem.getOutputDesc(), MAX_PROBLEM_CHARS)).append("\n");
        }
        return sb.toString();
    }

    /**
     * 用户当前代码格式化为提示词片段（空代码则完全不注入）
     */
    private String buildCodeContext(String code, String language) {
        if (!StringUtils.hasText(code)) {
            return "";
        }
        String lang = StringUtils.hasText(language) ? language : "";
        String label = lang.isEmpty() ? "用户当前代码" : "用户当前代码（" + lang + "）";
        return "\n\n【" + label + "】\n```" + lang + "\n" + truncate(code, MAX_CODE_CHARS) + "\n```\n";
    }

    /**
     * 超长内容截断，防止提示词超出模型上下文窗口
     */
    private String truncate(String text, int maxChars) {
        if (text == null || text.length() <= maxChars) {
            return text == null ? "" : text;
        }
        return text.substring(0, maxChars) + "\n…（内容过长已截断）";
    }

    /**
     * 读取 Redis 会话记忆（user/assistant 交替）
     */
    private List<Message> loadMemory(Long sessionId) {
        List<Object> raw = redisTemplate.opsForList().range(RedisKeys.AI_SESSION + sessionId, 0, -1);
        List<Message> messages = new ArrayList<>();
        if (raw == null) {
            return messages;
        }
        for (Object item : raw) {
            if (item instanceof MemoryMessage memory) {
                messages.add(AiConstants.ROLE_ASSISTANT.equals(memory.getRole())
                        ? new AssistantMessage(memory.getContent())
                        : new UserMessage(memory.getContent()));
            }
        }
        return messages;
    }

    /**
     * 追加会话记忆并裁剪到最近 N 轮（1 轮 = user+assistant 两条）
     */
    private void appendMemory(Long sessionId, String question, String answer) {
        String key = RedisKeys.AI_SESSION + sessionId;
        redisTemplate.opsForList().rightPush(key, new MemoryMessage(AiConstants.ROLE_USER, question));
        redisTemplate.opsForList().rightPush(key, new MemoryMessage(AiConstants.ROLE_ASSISTANT, answer));
        redisTemplate.opsForList().trim(key, -2L * aiProperties.getMaxMemoryRounds(), -1);
        // 滑动过期：1 天无对话自动清理
        redisTemplate.expire(key, 1, TimeUnit.DAYS);
    }
}
