package com.github.wuzhudexiaopacai.oj.common.web.aspect;

import com.github.wuzhudexiaopacai.oj.common.core.annotation.Idempotent;
import com.github.wuzhudexiaopacai.oj.common.core.constant.RedisKeys;
import com.github.wuzhudexiaopacai.oj.common.core.exception.BizException;
import com.github.wuzhudexiaopacai.oj.common.core.result.ResultCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.time.Duration;

/**
 * 幂等切面：setIfAbsent 占位实现窗口期防重（分布式安全）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class IdempotentAspect {

    private final RedissonClient redissonClient;

    private final SpelExpressionParser spelParser = new SpelExpressionParser();

    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String businessKey = parseSpelKey(joinPoint, idempotent.key());
        String redisKey = RedisKeys.IDEMPOTENT + businessKey;
        RBucket<String> bucket = redissonClient.getBucket(redisKey);
        boolean acquired = bucket.setIfAbsent("1", Duration.ofMillis(idempotent.unit().toMillis(idempotent.expire())));
        if (!acquired) {
            log.warn("幂等拦截，key={}", redisKey);
            throw new BizException(ResultCode.CONFLICT.getCode(), idempotent.message());
        }
        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            if (idempotent.deleteOnError()) {
                bucket.delete();
            }
            throw throwable;
        }
    }

    /**
     * 解析 SpEL 幂等键（支持引用方法参数名，如 #submitId、#user.id）
     */
    private String parseSpelKey(ProceedingJoinPoint joinPoint, String spel) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        EvaluationContext context = new StandardEvaluationContext();
        Object[] args = joinPoint.getArgs();
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }
        Object value = spelParser.parseExpression(spel).getValue(context);
        return value == null ? "" : value.toString();
    }
}
