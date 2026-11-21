package com.github.wuzhudexiaopacai.oj.judge.statemachine;

import com.github.wuzhudexiaopacai.oj.common.core.enums.JudgeStatusEnum;
import com.github.wuzhudexiaopacai.oj.common.core.exception.Asserts;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSubmission;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeSubmissionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 判题状态机服务
 * <p>
 * 状态持久化取舍说明：不使用 StateMachinePersist（默认实现面向单实例且表结构重），
 * 判题状态以 judge_submission.status 为唯一权威存储，每次状态迁移由业务代码显式落库；
 * 状态机仅作为「迁移合法性校验 + 流转逻辑」的内存引擎，通过工厂模式每次消费时新建实例，
 * 避免多线程/集群下的状态共享问题。
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JudgeStateMachineService {

    private final StateMachineFactory<JudgeStatusEnum, JudgeEvent> stateMachineFactory;

    private final JudgeSubmissionMapper judgeSubmissionMapper;

    /**
     * 驱动状态迁移（自动选择事件并落库）
     *
     * @param submission 提交记录（内存中状态已更新）
     * @param event      触发事件
     * @param terminalStatus 若 event=COMPLETE，指定终态；否则为 null
     */
    @Transactional(rollbackFor = Exception.class)
    public void sendEvent(JudgeSubmission submission, JudgeEvent event, JudgeStatusEnum terminalStatus) {
        Asserts.notNull(submission, "提交记录不能为空");
        Asserts.notNull(event, "事件不能为空");

        JudgeStatusEnum currentStatus = JudgeStatusEnum.fromCode(submission.getStatus());
        Asserts.notNull(currentStatus, "非法判题状态");

        // 终态不可再迁移
        if (currentStatus.isTerminal()) {
            log.warn("提交 {} 已处于终态 {}，忽略事件 {}", submission.getSubmitId(), currentStatus, event);
            return;
        }

        // 创建状态机并设置当前状态
        StateMachine<JudgeStatusEnum, JudgeEvent> stateMachine = stateMachineFactory.getStateMachine(submission.getSubmitId());
        stateMachine.getStateMachineAccessor().doWithAllRegions(access ->
                access.resetStateMachine(new DefaultStateMachineContext<>(currentStatus, null, null, null)));

        // 发送事件前校验当前状态合法（状态机本身会校验，此处提前断言保证幂等语义清晰）
        stateMachine.start();

        Message<JudgeEvent> message = MessageBuilder.withPayload(event)
                .setHeader("terminalStatus", terminalStatus)
                .build();

        boolean accepted = stateMachine.sendEvent(message);
        Asserts.isTrue(accepted, String.format("非法状态迁移：%s --%s--> ?", currentStatus, event));

        JudgeStatusEnum nextStatus = stateMachine.getState().getId();
        // 若指定了终态，强制对齐（COMPLETE 事件有多个目标分支，状态机默认选第一个匹配）
        if (event == JudgeEvent.COMPLETE && terminalStatus != null && terminalStatus.isTerminal()) {
            nextStatus = terminalStatus;
        }

        // 每次迁移落库
        submission.setStatus(nextStatus.getCode());
        if (nextStatus.isTerminal()) {
            submission.setJudgeTime(LocalDateTime.now());
        }
        Asserts.isTrue(judgeSubmissionMapper.updateById(submission) > 0,
                "提交状态已并发修改，本次状态迁移失效");
        log.info("提交 {} 状态迁移 {} -> {}", submission.getSubmitId(), currentStatus, nextStatus);
    }

    /**
     * 将提交置为指定终态（用于兜底/死信场景）
     *
     * @param submission 提交记录
     * @param terminalStatus 终态
     * @param errorMessage 错误信息
     * @return 仅本次成功落库为终态返回 true；已终态或乐观锁冲突返回 false
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean transitionToTerminal(JudgeSubmission submission, JudgeStatusEnum terminalStatus, String errorMessage) {
        Asserts.notNull(submission, "提交记录不能为空");
        Asserts.isTrue(terminalStatus != null && terminalStatus.isTerminal(), "目标状态必须是终态");

        JudgeStatusEnum currentStatus = JudgeStatusEnum.fromCode(submission.getStatus());
        if (currentStatus.isTerminal()) {
            return false;
        }

        // 直接落库为终态，不经过状态机（兜底场景允许跨状态跳转）
        submission.setStatus(terminalStatus.getCode());
        submission.setJudgeTime(LocalDateTime.now());
        if (errorMessage != null) {
            submission.setErrorMessage(errorMessage);
        }
        if (judgeSubmissionMapper.updateById(submission) == 0) {
            // 乐观锁冲突：其他实例已更新该提交，终态以先落库者为准
            log.warn("提交 {} 兜底置终态时发生并发冲突，跳过", submission.getSubmitId());
            return false;
        }
        log.info("提交 {} 兜底置为终态 {}", submission.getSubmitId(), terminalStatus);
        return true;
    }
}
