package com.github.wuzhudexiaopacai.oj.judge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.wuzhudexiaopacai.oj.common.web.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 比赛成绩记录
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("contest_record")
public class ContestRecord extends BaseEntity {

    private Long contestId;

    private Long userId;

    /**
     * 用户名快照（排行榜展示免回查）
     */
    private String username;

    /**
     * 总分
     */
    private Integer totalScore;

    /**
     * 通过题数
     */
    private Integer acceptCount;

    /**
     * ACM 罚时（秒）
     */
    private Long penaltySeconds;

    /**
     * 结算名次快照
     */
    private Integer rankSnapshot;
}
