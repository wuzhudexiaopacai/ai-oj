package com.github.wuzhudexiaopacai.oj.judge.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 判题提交消息（AI_OJ_JUDGE_SUBMIT）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeSubmitMessage implements Serializable {

    /**
     * 幂等提交标识（RocketMQ keys）
     */
    private String submitId;

    /**
     * 提交记录 ID
     */
    private Long submissionId;
}
