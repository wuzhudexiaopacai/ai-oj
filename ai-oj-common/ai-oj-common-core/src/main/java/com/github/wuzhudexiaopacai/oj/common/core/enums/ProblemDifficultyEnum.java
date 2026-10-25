package com.github.wuzhudexiaopacai.oj.common.core.enums;

import lombok.Getter;

/**
 * 题目难度枚举（前端语义色：绿/黄/红）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Getter
public enum ProblemDifficultyEnum {

    /**
     * 简单
     */
    EASY(0, "简单"),

    /**
     * 中等
     */
    MEDIUM(1, "中等"),

    /**
     * 困难
     */
    HARD(2, "困难");

    private final int code;

    private final String desc;

    ProblemDifficultyEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ProblemDifficultyEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (ProblemDifficultyEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }
}
