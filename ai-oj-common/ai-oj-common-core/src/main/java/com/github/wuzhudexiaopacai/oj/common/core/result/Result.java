package com.github.wuzhudexiaopacai.oj.common.core.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回体
 *
 * @param <T> 数据类型
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class Result<T> implements Serializable {

    private int code;

    private String message;

    private T data;

    private long timestamp = System.currentTimeMillis();

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.setCode(ResultCode.SUCCESS.getCode());
        result.setMessage(ResultCode.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    public static <T> Result<T> failed(String message) {
        return failed(ResultCode.FAILED.getCode(), message);
    }

    public static <T> Result<T> failed(ResultCode resultCode) {
        return failed(resultCode.getCode(), resultCode.getMessage());
    }

    public static <T> Result<T> failed(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    public boolean isSuccess() {
        return this.code == ResultCode.SUCCESS.getCode();
    }
}
