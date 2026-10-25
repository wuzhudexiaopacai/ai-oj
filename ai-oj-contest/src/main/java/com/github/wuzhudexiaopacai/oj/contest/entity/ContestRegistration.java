package com.github.wuzhudexiaopacai.oj.contest.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.github.wuzhudexiaopacai.oj.common.web.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 比赛报名实体（contest_registration，uk_contest_user 唯一键兜底幂等防重复报名）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("contest_registration")
public class ContestRegistration extends BaseEntity {

    /**
     * 比赛 ID
     */
    private Long contestId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 状态：1已报名 0已取消
     */
    private Integer status;
}
