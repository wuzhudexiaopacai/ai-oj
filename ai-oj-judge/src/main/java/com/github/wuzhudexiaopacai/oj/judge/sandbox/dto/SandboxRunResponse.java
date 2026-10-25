package com.github.wuzhudexiaopacai.oj.judge.sandbox.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * 沙箱自测运行响应
 *
 * @param status       状态字符串
 * @param output       程序输出
 * @param errorMessage 错误信息
 * @param timeMs       耗时（毫秒）
 * @param memoryKb     内存（KB）
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SandboxRunResponse(String status,
                                 String output,
                                 String errorMessage,
                                 Integer timeMs,
                                 Integer memoryKb) {
}
