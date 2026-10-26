package com.github.wuzhudexiaopacai.oj.contest.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 比赛详情展示对象（含题目列表；用户无关数据，可整体缓存）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class ContestDetailVO implements Serializable {

    /**
     * 比赛 ID
     */
    private Long id;

    /**
     * 比赛标题
     */
    private String title;

    /**
     * 比赛说明（Markdown）
     */
    private String description;

    /**
     * 赛制：0 ACM 1 OI
     */
    private Integer type;

    /**
     * 状态机：SCHEDULED/RUNNING/ENDED/CANCELED
     */
    private String status;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 封榜时间
     */
    private LocalDateTime sealTime;

    /**
     * 创建者
     */
    private Long creatorId;

    /**
     * 可见性：1公开 2私有（密码不下发）
     */
    private Integer auth;

    /**
     * 是否已设置比赛密码（仅回显状态，不下发明文；编辑时留空表示不修改）
     */
    private Boolean hasPassword;

    /**
     * 比赛题目列表（按 sort 升序）
     */
    private List<ContestProblemVO> problems;

    /**
     * 已报名人数
     */
    private Integer registerCount;

    /**
     * 当前登录用户是否已报名（未登录恒为 false）
     */
    private Boolean registered;
}
