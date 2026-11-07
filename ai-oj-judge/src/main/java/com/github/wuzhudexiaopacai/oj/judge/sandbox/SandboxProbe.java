package com.github.wuzhudexiaopacai.oj.judge.sandbox;

/**
 * 沙箱健康探测（抽象出接口便于单元测试）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface SandboxProbe {

    /**
     * 探测沙箱是否健康
     *
     * @param baseUrl 沙箱 HTTP 地址
     * @return true 健康
     */
    boolean healthy(String baseUrl);
}
