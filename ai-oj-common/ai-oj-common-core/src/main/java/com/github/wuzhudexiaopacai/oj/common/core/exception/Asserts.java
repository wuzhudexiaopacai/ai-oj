package com.github.wuzhudexiaopacai.oj.common.core.exception;

import cn.hutool.core.util.StrUtil;
import com.github.wuzhudexiaopacai.oj.common.core.result.ResultCode;

/**
 * 断言工具，校验失败抛出业务异常
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public class Asserts {

    private Asserts() {
    }

    public static void fail(String message) {
        throw new BizException(message);
    }

    public static void fail(ResultCode resultCode) {
        throw new BizException(resultCode);
    }

    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            fail(message);
        }
    }

    public static void notNull(Object object, String message) {
        if (object == null) {
            fail(message);
        }
    }

    public static void notBlank(String str, String message) {
        if (StrUtil.isBlank(str)) {
            fail(message);
        }
    }
}
