package com.github.wuzhudexiaopacai.oj.judge.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 远程读并行执行器配置
 * <p>
 * 判题与提交查询中存在多个相互独立的远程读调用（MinIO 用例文件、题目简要信息），
 * 串行执行会使耗时随调用数线性增长。统一使用有界线程池并行拉取：
 * 队列满时由调用线程兜底执行（CallerRunsPolicy），形成天然背压，避免线程数失控。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Configuration
public class RemoteReadConfig {

    /**
     * 远程读并行执行器
     *
     * @param coreSize        核心线程数
     * @param maxSize         最大线程数
     * @param queueCapacity   队列容量
     * @param keepAliveSeconds 空闲线程存活时间（秒）
     * @return 有界线程池
     */
    @Bean(name = "judgeRemoteReadExecutor", destroyMethod = "shutdown")
    public ExecutorService judgeRemoteReadExecutor(
            @Value("${ai-oj.judge.remote-read.core-size:8}") int coreSize,
            @Value("${ai-oj.judge.remote-read.max-size:16}") int maxSize,
            @Value("${ai-oj.judge.remote-read.queue-capacity:1000}") int queueCapacity,
            @Value("${ai-oj.judge.remote-read.keep-alive-seconds:60}") long keepAliveSeconds) {
        AtomicInteger counter = new AtomicInteger();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                coreSize, maxSize, keepAliveSeconds, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                runnable -> {
                    Thread thread = new Thread(runnable, "judge-remote-read-" + counter.incrementAndGet());
                    // 守护线程：不阻塞应用优雅停机
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy());
        // 空闲时回收核心线程，避免低频调用长期占用线程
        executor.allowCoreThreadTimeOut(true);
        log.info("远程读并行执行器初始化 coreSize={} maxSize={} queueCapacity={} keepAliveSeconds={}",
                coreSize, maxSize, queueCapacity, keepAliveSeconds);
        return executor;
    }
}
