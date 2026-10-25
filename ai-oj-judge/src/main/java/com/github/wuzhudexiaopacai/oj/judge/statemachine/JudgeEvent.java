package com.github.wuzhudexiaopacai.oj.judge.statemachine;

/**
 * 判题状态机事件
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public enum JudgeEvent {

    /**
     * 提交进入队列
     */
    SUBMIT,

    /**
     * 开始编译
     */
    COMPILE_START,

    /**
     * 开始评测
     */
    JUDGE_START,

    /**
     * 判题完成（进入终态，具体终态由结果决定）
     */
    COMPLETE,

    /**
     * 判题失败（系统错误）
     */
    FAIL,

    /**
     * 判题超时
     */
    TIMEOUT
}
