package com.github.wuzhudexiaopacai.oj.common.web.annotation;

import com.github.wuzhudexiaopacai.oj.common.core.enums.OpType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解：标注在 Controller 方法上，由 {@link com.github.wuzhudexiaopacai.oj.common.web.aspect.OperationLogAspect}
 * 统一采集入参/出参/耗时/操作人并异步落库
 * <p>
 * 默认只在写操作（POST/PUT/DELETE）与导出接口上标注，避免日志量失控。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 业务模块，如「题目管理」
     */
    String module() default "";

    /**
     * 操作描述，如「新建题目」
     */
    String operation() default "";

    /**
     * 操作类型
     */
    OpType type() default OpType.OTHER;

    /**
     * 是否记录请求入参
     */
    boolean recordParam() default true;

    /**
     * 是否记录响应出参
     */
    boolean recordResult() default true;
}
