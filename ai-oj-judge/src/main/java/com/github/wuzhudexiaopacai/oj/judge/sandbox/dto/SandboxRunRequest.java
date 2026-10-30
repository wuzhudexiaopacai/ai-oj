package com.github.wuzhudexiaopacai.oj.judge.sandbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * 沙箱自测运行请求
 *
 * @param language    编程语言
 * @param code        代码
 * @param input       输入（沙箱 /run 契约字段名为 stdin，此处显式映射）
 * @param timeLimitMs 时间限制（毫秒）
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SandboxRunRequest(String language,
                                String code,
                                @JsonProperty("stdin") String input,
                                Integer timeLimitMs) {
}
