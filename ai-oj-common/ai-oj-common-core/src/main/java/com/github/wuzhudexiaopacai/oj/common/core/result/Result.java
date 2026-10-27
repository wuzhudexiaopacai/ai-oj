package com.github.wuzhudexiaopacai.oj.common.core.result;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "统一返回体：code=200 表示成功，其余为业务失败码，data 为业务数据")
public class Result<T> implements Serializable {

    @Schema(description = "业务状态码：200 成功，400 参数错误，401 未登录，403 无权限，500 服务异常", example = "200")
    private int code;

    @Schema(description = "提示信息，成功时固定为 success，失败时为具体原因", example = "success")
    private String message;

    @Schema(description = "业务数据，失败时为 null")
    private T data;

    @Schema(description = "服务端响应时间戳（epoch 毫秒）", example = "1735660800000")
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
