package com.github.wuzhudexiaopacai.oj.common.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 接口幂等注解（基于 Redisson 实现，防止网络重试/用户连点造成重复提交）
 * <p>
 * 使用示例：@Idempotent(key = "#submitId + ':' + #loginUser.id", expire = 10)
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    /**
     * 幂等键 SpEL 表达式，可引用方法参数名
     *
     * @return SpEL 表达式
     */
    String key();

    /**
     * 幂等标记过期时间（窗口期内相同 key 直接拒绝）
     *
     * @return 过期时间
     */
    long expire() default 5;

    /**
     * 时间单位
     *
     * @return 时间单位
     */
    TimeUnit unit() default TimeUnit.SECONDS;

    /**
     * 重复提交提示语
     *
     * @return 提示语
     */
    String message() default "请勿重复提交";

    /**
     * 业务执行异常时是否删除幂等标记（true：允许用户重试；false：窗口期内拒绝）
     *
     * @return 是否删除
     */
    boolean deleteOnError() default true;
}
