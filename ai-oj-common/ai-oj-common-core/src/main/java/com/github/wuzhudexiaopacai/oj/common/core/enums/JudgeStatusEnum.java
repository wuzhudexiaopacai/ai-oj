package com.github.wuzhudexiaopacai.oj.common.core.enums;

import lombok.Getter;

import java.util.EnumSet;
import java.util.Set;

/**
 * 判题状态机状态枚举
 * <p>
 * 状态流转：PENDING → COMPILING → JUDGING → 终态（ACCEPTED / WRONG_ANSWER / TLE / MLE / RE / CE / PE / OLE / SYSTEM_ERROR）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Getter
public enum JudgeStatusEnum {

    /**
     * 排队中（已入库待消费）
     */
    PENDING(0, "排队中", false),

    /**
     * 编译中
     */
    COMPILING(1, "编译中", false),

    /**
     * 评测中
     */
    JUDGING(2, "评测中", false),

    /**
     * 答案正确（终态）
     */
    ACCEPTED(3, "答案正确", true),

    /**
     * 答案错误（终态）
     */
    WRONG_ANSWER(4, "答案错误", true),

    /**
     * 运行超时（终态）
     */
    TIME_LIMIT_EXCEEDED(5, "运行超时", true),

    /**
     * 内存超限（终态）
     */
    MEMORY_LIMIT_EXCEEDED(6, "内存超限", true),

    /**
     * 运行错误（终态）
     */
    RUNTIME_ERROR(7, "运行错误", true),

    /**
     * 编译错误（终态）
     */
    COMPILE_ERROR(8, "编译错误", true),

    /**
     * 格式错误（终态，内容正确但输出格式有差异）
     */
    PRESENTATION_ERROR(9, "格式错误", true),

    /**
     * 输出超限（终态）
     */
    OUTPUT_LIMIT_EXCEEDED(10, "输出超限", true),

    /**
     * 系统错误（终态，沙箱异常等，可触发重判）
     */
    SYSTEM_ERROR(11, "系统错误", true);

    private final int code;

    private final String desc;

    private final boolean terminal;

    JudgeStatusEnum(int code, String desc, boolean terminal) {
        this.code = code;
        this.desc = desc;
        this.terminal = terminal;
    }

    /**
     * 非终态集合（用于超时补偿任务扫描卡死的提交）
     */
    public static final Set<JudgeStatusEnum> NON_TERMINAL = EnumSet.of(PENDING, COMPILING, JUDGING);

    public static JudgeStatusEnum fromCode(int code) {
        for (JudgeStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return SYSTEM_ERROR;
    }
}
