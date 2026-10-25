package com.github.wuzhudexiaopacai.oj.common.core.exception;

import com.github.wuzhudexiaopacai.oj.common.core.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常，全局异常处理器统一捕获
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = ResultCode.FAILED.getCode();
    }

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }
}
