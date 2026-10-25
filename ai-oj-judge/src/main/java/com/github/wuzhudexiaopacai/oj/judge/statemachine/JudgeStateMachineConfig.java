package com.github.wuzhudexiaopacai.oj.judge.statemachine;

import com.github.wuzhudexiaopacai.oj.common.core.enums.JudgeStatusEnum;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;
import java.util.Set;

/**
 * 判题状态机配置
 * <p>
 * 状态持久化说明：不使用 StateMachinePersist（其默认实现面向单实例且结构重），
 * 判题状态以 judge_submission.status 为唯一权威存储，每次状态迁移由业务代码显式落库；
 * 状态机仅作为「迁移合法性校验 + 流转逻辑」的内存引擎，通过工厂模式每次消费时新建实例，
 * 避免多线程/集群下的状态共享问题。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Configuration
@EnableStateMachineFactory
public class JudgeStateMachineConfig extends StateMachineConfigurerAdapter<JudgeStatusEnum, JudgeEvent> {

    /**
     * 全部终态（不可再迁移）
     */
    private static final Set<JudgeStatusEnum> TERMINAL_STATES = EnumSet.of(
            JudgeStatusEnum.ACCEPTED,
            JudgeStatusEnum.WRONG_ANSWER,
            JudgeStatusEnum.TIME_LIMIT_EXCEEDED,
            JudgeStatusEnum.MEMORY_LIMIT_EXCEEDED,
            JudgeStatusEnum.RUNTIME_ERROR,
            JudgeStatusEnum.COMPILE_ERROR,
            JudgeStatusEnum.PRESENTATION_ERROR,
            JudgeStatusEnum.OUTPUT_LIMIT_EXCEEDED,
            JudgeStatusEnum.SYSTEM_ERROR
    );

    @Override
    public void configure(StateMachineConfigurationConfigurer<JudgeStatusEnum, JudgeEvent> config) throws Exception {
        config.withConfiguration().autoStartup(true);
    }

    @Override
    public void configure(StateMachineStateConfigurer<JudgeStatusEnum, JudgeEvent> states) throws Exception {
        states.withStates()
                .initial(JudgeStatusEnum.PENDING)
                .states(EnumSet.allOf(JudgeStatusEnum.class))
                .end(JudgeStatusEnum.ACCEPTED)
                .end(JudgeStatusEnum.WRONG_ANSWER)
                .end(JudgeStatusEnum.TIME_LIMIT_EXCEEDED)
                .end(JudgeStatusEnum.MEMORY_LIMIT_EXCEEDED)
                .end(JudgeStatusEnum.RUNTIME_ERROR)
                .end(JudgeStatusEnum.COMPILE_ERROR)
                .end(JudgeStatusEnum.PRESENTATION_ERROR)
                .end(JudgeStatusEnum.OUTPUT_LIMIT_EXCEEDED)
                .end(JudgeStatusEnum.SYSTEM_ERROR);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<JudgeStatusEnum, JudgeEvent> transitions) throws Exception {
        transitions
                // 提交：PENDING → COMPILING
                .withExternal()
                .source(JudgeStatusEnum.PENDING).target(JudgeStatusEnum.COMPILING)
                .event(JudgeEvent.COMPILE_START)
                .and()
                // 开始评测：COMPILING → JUDGING
                .withExternal()
                .source(JudgeStatusEnum.COMPILING).target(JudgeStatusEnum.JUDGING)
                .event(JudgeEvent.JUDGE_START)
                .and()
                // 编译失败：COMPILING → COMPILE_ERROR
                .withExternal()
                .source(JudgeStatusEnum.COMPILING).target(JudgeStatusEnum.COMPILE_ERROR)
                .event(JudgeEvent.FAIL)
                .and()
                // 判题完成：JUDGING → 各终态（由结果状态决定具体终态，用 COMPLETE 事件统一入口）
                .withExternal()
                .source(JudgeStatusEnum.JUDGING).target(JudgeStatusEnum.ACCEPTED)
                .event(JudgeEvent.COMPLETE)
                .and()
                .withExternal()
                .source(JudgeStatusEnum.JUDGING).target(JudgeStatusEnum.WRONG_ANSWER)
                .event(JudgeEvent.COMPLETE)
                .and()
                .withExternal()
                .source(JudgeStatusEnum.JUDGING).target(JudgeStatusEnum.TIME_LIMIT_EXCEEDED)
                .event(JudgeEvent.COMPLETE)
                .and()
                .withExternal()
                .source(JudgeStatusEnum.JUDGING).target(JudgeStatusEnum.MEMORY_LIMIT_EXCEEDED)
                .event(JudgeEvent.COMPLETE)
                .and()
                .withExternal()
                .source(JudgeStatusEnum.JUDGING).target(JudgeStatusEnum.RUNTIME_ERROR)
                .event(JudgeEvent.COMPLETE)
                .and()
                // 沙箱在评测阶段才返回编译错误时：JUDGING → COMPILE_ERROR
                .withExternal()
                .source(JudgeStatusEnum.JUDGING).target(JudgeStatusEnum.COMPILE_ERROR)
                .event(JudgeEvent.COMPLETE)
                .and()
                .withExternal()
                .source(JudgeStatusEnum.JUDGING).target(JudgeStatusEnum.PRESENTATION_ERROR)
                .event(JudgeEvent.COMPLETE)
                .and()
                .withExternal()
                .source(JudgeStatusEnum.JUDGING).target(JudgeStatusEnum.OUTPUT_LIMIT_EXCEEDED)
                .event(JudgeEvent.COMPLETE)
                .and()
                // 系统错误：PENDING/COMPILING/JUDGING → SYSTEM_ERROR
                .withExternal()
                .source(JudgeStatusEnum.PENDING).target(JudgeStatusEnum.SYSTEM_ERROR)
                .event(JudgeEvent.FAIL)
                .and()
                .withExternal()
                .source(JudgeStatusEnum.COMPILING).target(JudgeStatusEnum.SYSTEM_ERROR)
                .event(JudgeEvent.FAIL)
                .and()
                .withExternal()
                .source(JudgeStatusEnum.JUDGING).target(JudgeStatusEnum.SYSTEM_ERROR)
                .event(JudgeEvent.FAIL)
                .and()
                // 超时：COMPILING/JUDGING → TIME_LIMIT_EXCEEDED
                .withExternal()
                .source(JudgeStatusEnum.COMPILING).target(JudgeStatusEnum.TIME_LIMIT_EXCEEDED)
                .event(JudgeEvent.TIMEOUT)
                .and()
                .withExternal()
                .source(JudgeStatusEnum.JUDGING).target(JudgeStatusEnum.TIME_LIMIT_EXCEEDED)
                .event(JudgeEvent.TIMEOUT);
    }

    /**
     * 判断状态是否为终态
     *
     * @param status 状态
     * @return true 终态
     */
    public static boolean isTerminal(JudgeStatusEnum status) {
        return status != null && status.isTerminal();
    }
}
