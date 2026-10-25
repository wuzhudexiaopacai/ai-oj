package com.github.wuzhudexiaopacai.oj.contest.service;

/**
 * 比赛报名服务
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface ContestRegistrationService {

    /**
     * 报名比赛（Seata 全局事务：本地 insert → 远程 initContestRecord，
     * 远程失败抛 BizException 触发全局回滚；数据库 uk 兜底幂等）
     *
     * @param contestId 比赛 ID
     */
    void register(Long contestId);

    /**
     * 取消报名（物理删除报名记录；成绩记录 contest_record 保留在判题库中不删除）
     *
     * @param contestId 比赛 ID
     */
    void cancel(Long contestId);
}
