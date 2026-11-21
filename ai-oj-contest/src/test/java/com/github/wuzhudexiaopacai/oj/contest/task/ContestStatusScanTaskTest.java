package com.github.wuzhudexiaopacai.oj.contest.task;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 比赛结算宽限期到 RocketMQ 延时级别的换算测试
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
class ContestStatusScanTaskTest {

    @Test
    void nonPositiveGraceShouldSendImmediately() {
        assertEquals(0, ContestStatusScanTask.delayLevelOf(0));
        assertEquals(0, ContestStatusScanTask.delayLevelOf(-10));
    }

    @Test
    void graceShouldRoundUpToDelayLevel() {
        assertEquals(1, ContestStatusScanTask.delayLevelOf(1));
        assertEquals(2, ContestStatusScanTask.delayLevelOf(2));
        assertEquals(9, ContestStatusScanTask.delayLevelOf(300));
        assertEquals(10, ContestStatusScanTask.delayLevelOf(301));
    }

    @Test
    void oversizedGraceShouldCapAtMaxLevel() {
        assertEquals(18, ContestStatusScanTask.delayLevelOf(7200));
        assertEquals(18, ContestStatusScanTask.delayLevelOf(100000));
    }
}
