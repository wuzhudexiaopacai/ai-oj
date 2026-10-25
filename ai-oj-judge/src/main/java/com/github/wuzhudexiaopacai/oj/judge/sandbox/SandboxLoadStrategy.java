package com.github.wuzhudexiaopacai.oj.judge.sandbox;

import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSandbox;

import java.util.List;

/**
 * 沙箱负载选择策略
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface SandboxLoadStrategy {

    /**
     * 从候选沙箱中选择一台
     *
     * @param candidates 在线且未被拉黑的沙箱
     * @return 选中的沙箱，无可用返回 null
     */
    JudgeSandbox select(List<JudgeSandbox> candidates);

    /**
     * 占座（负载 +1）
     *
     * @param sandboxKey 沙箱标识
     */
    void occupy(String sandboxKey);

    /**
     * 释放（负载 -1）
     *
     * @param sandboxKey 沙箱标识
     */
    void release(String sandboxKey);

    /**
     * 拉黑沙箱
     *
     * @param sandboxKey 沙箱标识
     */
    void blacklist(String sandboxKey);
}
