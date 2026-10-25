package com.github.wuzhudexiaopacai.oj.contest.service;

import com.github.wuzhudexiaopacai.oj.contest.vo.ContestRankVO;

import java.util.List;

/**
 * 比赛排行榜服务
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
public interface ContestRankService {

    /**
     * 查询比赛排行榜（先读 Redis ZSet → miss 分布式锁双重检查 → 拉取成绩 → 策略排序 → 重建缓存 → 批量补用户信息）
     * <p>
     * 封榜后普通用户读取快照（RedisKeys.CONTEST_RANK），管理员读取实时排名。
     *
     * @param contestId 比赛 ID
     * @return 排行榜列表
     */
    List<ContestRankVO> rank(Long contestId);
}
