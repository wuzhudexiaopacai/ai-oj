package com.github.wuzhudexiaopacai.oj.contest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 比赛创建/更新入参
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class ContestSaveDTO implements Serializable {

    /**
     * 比赛标题
     */
    @NotBlank(message = "比赛标题不能为空")
    @Size(max = 128, message = "比赛标题长度不能超过128")
    private String title;

    /**
     * 比赛说明（Markdown）
     */
    private String description;

    /**
     * 赛制：0 ACM 1 OI
     */
    @NotNull(message = "赛制不能为空")
    private Integer type;

    /**
     * 开始时间
     */
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    /**
     * 封榜时间（可选）
     */
    private LocalDateTime sealTime;

    /**
     * 私有赛密码（空为公开）
     */
    @Size(max = 64, message = "密码长度不能超过64")
    private String password;

    /**
     * 可见性：1公开 2私有
     */
    private Integer auth;
}
