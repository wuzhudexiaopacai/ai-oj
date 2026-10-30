package com.github.wuzhudexiaopacai.oj.judge.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * 沙箱自测运行响应
 *
 * @param status       状态字符串
 * @param output       程序输出（沙箱 /run 返回键为 stdout，此处显式映射）
 * @param errorMessage 错误信息（沙箱 /run 返回键为 stderr，此处显式映射）
 * @param timeMs       耗时（毫秒）
 * @param memoryKb     内存（KB）
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SandboxRunResponse(String status,
                                 @JsonProperty("stdout") String output,
                                 @JsonProperty("stderr") String errorMessage,
                                 Integer timeMs,
                                 Integer memoryKb) {
}
