package com.github.wuzhudexiaopacai.oj.common.web.aspect;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.wuzhudexiaopacai.oj.common.core.constant.CommonConstants;
import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import com.github.wuzhudexiaopacai.oj.common.core.dto.OperationLogMessage;
import com.github.wuzhudexiaopacai.oj.common.core.enums.OpType;
import com.github.wuzhudexiaopacai.oj.common.web.annotation.OperationLog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 操作日志切面：采集入参、出参、耗时、操作人、IP，异步投递到 RocketMQ，
 * 由 ai-oj-log 服务统一消费落库。日志链路异常不影响业务主流程。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    /**
     * 敏感字段名（小写匹配，包含即脱敏）
     */
    private static final List<String> SENSITIVE_KEYS = List.of(
            "password", "passwd", "pwd", "token", "secret", "accesskey", "authorization", "credential", "sign");

    private final ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider;

    private final ObjectMapper objectMapper;

    @Value("${oj.log.enabled:true}")
    private boolean enabled;

    @Value("${oj.log.max-length:2000}")
    private int maxLength;

    @Value("${spring.application.name:unknown}")
    private String serviceName;

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint point, OperationLog operationLog) throws Throwable {
        if (!enabled) {
            return point.proceed();
        }
        long start = System.currentTimeMillis();
        Object result = null;
        Throwable error = null;
        try {
            result = point.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            try {
                record(point, operationLog, result, error, System.currentTimeMillis() - start);
            } catch (Exception e) {
                // 日志采集失败绝不阻断业务
                log.warn("操作日志采集异常：{}", e.getMessage());
            }
        }
    }

    private void record(ProceedingJoinPoint point, OperationLog annotation, Object result, Throwable error, long costMs) {
        HttpServletRequest request = currentRequest();
        OperationLogMessage message = new OperationLogMessage();
        message.setTraceId(resolveTraceId(request));
        message.setServiceName(serviceName);
        message.setModule(annotation.module());
        message.setOperation(annotation.operation());
        message.setActionType(annotation.type().getCode());
        message.setHttpMethod(request == null ? "" : request.getMethod());
        message.setUri(request == null ? "" :
                StrUtil.maxLength(StrUtil.blankToDefault(request.getRequestURI(), ""), 255));
        message.setClassMethod(StrUtil.maxLength(
                point.getSignature().getDeclaringTypeName() + "#" + point.getSignature().getName(), 255));
        message.setIp(resolveIp(request));
        message.setUserAgent(request == null ? "" : StrUtil.maxLength(
                StrUtil.blankToDefault(request.getHeader("User-Agent"), ""), 512));
        message.setRequestParam(annotation.recordParam() ? buildParam(point) : "");
        message.setResponseResult(annotation.recordResult() ? buildResult(result) : "");
        message.setSuccess(error == null ? 1 : 0);
        message.setErrorMsg(error == null ? "" : truncate(error.getMessage(), 1024));
        message.setCostMs((int) Math.min(costMs, Integer.MAX_VALUE));
        message.setLogTime(LocalDateTime.now());

        LoginUser user = currentUser();
        if (user != null) {
            message.setUserId(user.getId());
            message.setUsername(StrUtil.blankToDefault(user.getUsername(), ""));
        }

        RocketMQTemplate template = rocketMQTemplateProvider.getIfAvailable();
        if (template == null) {
            log.debug("未引入 RocketMQ，跳过操作日志投递：{}#{}", annotation.module(), annotation.operation());
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(message);
            template.asyncSend(MqConstants.OPERATION_LOG_TOPIC, payload, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.debug("操作日志投递成功，msgId={}", sendResult.getMsgId());
                }

                @Override
                public void onException(Throwable throwable) {
                    log.warn("操作日志投递失败：{}", throwable.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("操作日志序列化失败：{}", e.getMessage());
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private LoginUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceId = MDC.get("traceId");
        if (StrUtil.isBlank(traceId) && request != null) {
            traceId = request.getHeader(CommonConstants.HEADER_TRACE_ID);
        }
        return StrUtil.isBlank(traceId) ? UUID.randomUUID().toString().replace("-", "").substring(0, 16) : truncate(traceId, 64);
    }

    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(forwarded)) {
            return StrUtil.maxLength(forwarded.split(",")[0].trim(), 64);
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StrUtil.isNotBlank(realIp)) {
            return StrUtil.maxLength(realIp, 64);
        }
        return StrUtil.blankToDefault(request.getRemoteAddr(), "");
    }

    /**
     * 序列化入参：跳过 Servlet 对象、文件、字节流等不可序列化参数
     */
    private String buildParam(ProceedingJoinPoint point) {
        List<Object> args = new ArrayList<>();
        for (Object arg : point.getArgs()) {
            if (arg == null || arg instanceof HttpServletRequest || arg instanceof HttpServletResponse
                    || arg instanceof MultipartFile || arg instanceof BindingResult
                    || arg instanceof byte[] || arg instanceof InputStream) {
                continue;
            }
            args.add(arg);
        }
        if (args.isEmpty()) {
            return "";
        }
        try {
            JsonNode node = objectMapper.valueToTree(args);
            maskSensitive(node);
            return truncate(node.toString(), maxLength);
        } catch (Exception e) {
            return truncate(String.valueOf(args), maxLength);
        }
    }

    private String buildResult(Object result) {
        if (result == null) {
            return "";
        }
        try {
            JsonNode node = objectMapper.valueToTree(result);
            maskSensitive(node);
            return truncate(node.toString(), maxLength);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 递归脱敏：字段名命中敏感词直接替换为 ***
     */
    private void maskSensitive(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            List<String> sensitiveFields = new ArrayList<>();
            Iterator<Map.Entry<String, JsonNode>> iterator = objectNode.fields();
            while (iterator.hasNext()) {
                Map.Entry<String, JsonNode> entry = iterator.next();
                if (isSensitive(entry.getKey())) {
                    sensitiveFields.add(entry.getKey());
                } else {
                    maskSensitive(entry.getValue());
                }
            }
            sensitiveFields.forEach(field -> objectNode.put(field, "***"));
        } else if (node instanceof ArrayNode arrayNode) {
            arrayNode.forEach(this::maskSensitive);
        }
    }

    private boolean isSensitive(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String lower = fieldName.toLowerCase();
        return SENSITIVE_KEYS.stream().anyMatch(lower::contains);
    }

    private String truncate(String value, int limit) {
        if (value == null) {
            return "";
        }
        int safeLimit = limit > 0 ? limit : 2000;
        return value.length() > safeLimit ? value.substring(0, safeLimit) + "...(truncated)" : value;
    }
}
