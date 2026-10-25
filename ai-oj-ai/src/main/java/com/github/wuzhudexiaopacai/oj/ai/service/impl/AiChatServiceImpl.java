package com.github.wuzhudexiaopacai.oj.ai.service.impl;

import com.github.wuzhudexiaopacai.oj.ai.config.AiProperties;
import com.github.wuzhudexiaopacai.oj.ai.constant.AiConstants;
import com.github.wuzhudexiaopacai.oj.ai.dto.MemoryMessage;
import com.github.wuzhudexiaopacai.oj.ai.dto.ReviewRequestDTO;
import com.github.wuzhudexiaopacai.oj.ai.entity.AiSession;
import com.github.wuzhudexiaopacai.oj.ai.service.AiChatService;
import com.github.wuzhudexiaopacai.oj.ai.service.AiSessionService;
import com.github.wuzhudexiaopacai.oj.ai.strategy.AiSceneStrategyFactory;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
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

    private final ChatClient chatClient;

    private final AiSessionService sessionService;

    private final AiSceneStrategyFactory strategyFactory;

    private final ProblemFeignClient problemFeignClient;

    private final RedisTemplate<String, Object> redisTemplate;

    private final RedissonClient redissonClient;

    private final AiProperties aiProperties;

    @Override
    public SseEmitter streamChat(Long sessionId, String question) {
        LoginUser loginUser = SecurityUtils.requireLogin();
        Asserts.notBlank(question, "问题不能为空");

        // 1. Redisson 限流：每用户每分钟 ai-oj.ai.rate-limit-per-minute 次
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(RedisKeys.AI_RATE_LIMIT + loginUser.getId());
        rateLimiter.trySetRate(RateType.OVERALL, aiProperties.getRateLimitPerMinute(), 1, RateIntervalUnit.MINUTES);
        Asserts.isTrue(rateLimiter.tryAcquire(), "请求过于频繁，请稍后再试");

        // 2. 会话归属校验 + 场景策略渲染提示词
        AiSession session = sessionService.requireOwnedSession(sessionId);
        String prompt = strategyFactory.getStrategy(session.getScene())
                .buildPrompt(buildSceneVars(session, question));

        // 3. 装配消息：历史记忆 + 当前问题
        List<Message> messages = new ArrayList<>(loadMemory(sessionId));
        messages.add(new UserMessage(prompt));

        // 4. SSE 流式推送（180s 超时）
        SseEmitter emitter = new SseEmitter(AiConstants.SSE_TIMEOUT_MS);
        emitter.onTimeout(emitter::complete);
        StringBuilder answer = new StringBuilder();
        AtomicReference<Disposable> subscriptionRef = new AtomicReference<>();
        Disposable subscription = chatClient.prompt().messages(messages).stream().content()
                .subscribe(
                        token -> {
                            answer.append(token);
                            sendToken(emitter, token, subscriptionRef);
                        },
                        error -> {
                            log.error("AI 流式对话失败: sessionId={}", sessionId, error);
                            emitter.completeWithError(error);
                        },
                        () -> onComplete(emitter, sessionId, question, answer.toString()));
        subscriptionRef.set(subscription);
        return emitter;
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
     */
    private void onComplete(SseEmitter emitter, Long sessionId, String question, String answer) {
        try {
            appendMemory(sessionId, question, answer);
            sessionService.saveMessageAsync(sessionId, AiConstants.ROLE_USER, question);
            sessionService.saveMessageAsync(sessionId, AiConstants.ROLE_ASSISTANT, answer);
        } catch (Exception e) {
            log.error("AI 会话记忆写入失败: sessionId={}", sessionId, e);
        } finally {
            emitter.complete();
        }
    }

    /**
     * 构建场景模板变量（hint 场景通过 Feign 加载题目上下文，失败降级）
     */
    private Map<String, String> buildSceneVars(AiSession session, String question) {
        Map<String, String> vars = new HashMap<>();
        vars.put("question", question);
        if (AiConstants.SCENE_HINT.equals(session.getScene())) {
            vars.put("problem", loadProblemContext(session.getProblemId()));
        }
        return vars;
    }

    /**
     * 加载题目上下文（Feign 调用失败时降级为题目 ID，不阻塞主链路）
     */
    private String loadProblemContext(Long problemId) {
        if (problemId == null) {
            return "（未关联具体题目）";
        }
        try {
            Result<ProblemJudgeDTO> result = problemFeignClient.getJudgeInfo(problemId);
            if (result != null && result.isSuccess() && result.getData() != null) {
                ProblemJudgeDTO problem = result.getData();
                return "题号 " + problem.getProblemKey() + "《" + problem.getTitle() + "》"
                        + "（时间限制 " + problem.getTimeLimit() + "ms，内存限制 " + problem.getMemoryLimit() + "MB）";
            }
        } catch (Exception e) {
            log.warn("题目上下文加载失败，降级处理: problemId={}", problemId, e);
        }
        return "题目 ID " + problemId;
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
