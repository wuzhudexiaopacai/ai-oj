package com.github.wuzhudexiaopacai.oj.judge.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 判题进度 VO（WebSocket 推送与轮询兜底共用）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeProgressVO implements Serializable {

    private String submitId;

    /**
     * 状态码（JudgeStatusEnum.code）
     */
    private Integer status;

    private String statusDesc;

    private Integer score;

    private Integer timeUsed;

    private Integer memoryUsed;

    /**
     * 是否终态
     */
    private Boolean finished;
}
