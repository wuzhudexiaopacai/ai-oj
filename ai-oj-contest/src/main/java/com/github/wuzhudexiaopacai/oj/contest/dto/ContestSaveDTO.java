package com.github.wuzhudexiaopacai.oj.contest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "比赛创建/更新入参")
public class ContestSaveDTO implements Serializable {

    /**
     * 比赛标题
     */
    @Schema(description = "比赛标题，最长 128 字符", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026 春季新手赛")
    @NotBlank(message = "比赛标题不能为空")
    @Size(max = 128, message = "比赛标题长度不能超过128")
    private String title;

    /**
     * 比赛说明（Markdown）
     */
    @Schema(description = "比赛说明，支持 Markdown，可为空", example = "本场比赛为 ACM 赛制，共 6 题")
    private String description;

    /**
     * 赛制：0 ACM 1 OI
     */
    @Schema(description = "赛制：0 ACM / 1 OI", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "赛制不能为空")
    private Integer type;

    /**
     * 开始时间
     */
    @Schema(description = "开始时间，格式 yyyy-MM-dd HH:mm:ss", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-11-01 19:00:00")
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @Schema(description = "结束时间，格式 yyyy-MM-dd HH:mm:ss，必须晚于开始时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-11-01 22:00:00")
    @NotNull(message = "结束时间不能为空")
    private LocalDateTime endTime;

    /**
     * 封榜时间（可选）
     */
    @Schema(description = "封榜时间，格式 yyyy-MM-dd HH:mm:ss，须在开始时间与结束时间之间；留空表示不封榜", example = "2026-11-01 21:00:00")
    private LocalDateTime sealTime;

    /**
     * 私有赛密码（空为公开）
     */
    @Schema(description = "私有赛密码，最长 64 字符；公开赛可留空，更新时留空表示沿用原密码", example = "123456")
    @Size(max = 64, message = "密码长度不能超过64")
    private String password;

    /**
     * 可见性：1公开 2私有
     */
    @Schema(description = "可见性：1 公开 / 2 私有；留空默认公开，私有赛必须设置密码", example = "1")
    private Integer auth;
}
