package com.github.wuzhudexiaopacai.oj.contest.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.github.wuzhudexiaopacai.oj.common.web.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 比赛实体（contest，状态由 ContestStatusScanTask 状态机扫描驱动迁移）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("contest")
public class Contest extends BaseEntity {

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
     * 封榜时间（该时间点后普通用户只看快照）
     */
    private LocalDateTime sealTime;

    /**
     * 创建者
     */
    private Long creatorId;

    /**
     * 私有赛密码（空为公开）
     */
    private String password;

    /**
     * 可见性：1公开 2私有
     */
    private Integer auth;

    /**
     * 乐观锁版本
     */
    @Version
    private Integer version;
}
