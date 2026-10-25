package com.github.wuzhudexiaopacai.oj.problem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 题目 ES 同步消息（PROBLEM_SYNC_TOPIC）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProblemSyncMessage implements Serializable {

    /**
     * 操作类型：upsert / delete
     */
    private String op;

    /**
     * 题目 ID
     */
    private Long problemId;

    /**
     * 消息产生时间戳（毫秒），消费端幂等判重用
     */
    private Long timestamp;

    public static ProblemSyncMessage upsert(Long problemId) {
        return new ProblemSyncMessage("upsert", problemId, System.currentTimeMillis());
    }

    public static ProblemSyncMessage delete(Long problemId) {
        return new ProblemSyncMessage("delete", problemId, System.currentTimeMillis());
    }
}
