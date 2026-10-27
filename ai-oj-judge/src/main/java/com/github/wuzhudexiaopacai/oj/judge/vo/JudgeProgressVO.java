package com.github.wuzhudexiaopacai.oj.judge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "判题进度（WebSocket 推送与轮询兜底共用）")
public class JudgeProgressVO implements Serializable {

    @Schema(description = "幂等提交标识（客户端生成的 UUID）")
    private String submitId;

    /**
     * 状态码（JudgeStatusEnum.code）
     */
    @Schema(description = "判题状态码：0 排队中、1 编译中、2 评测中、3 答案正确、4 答案错误、5 运行超时、6 内存超限、7 运行错误、8 编译错误、9 格式错误、10 输出超限、11 系统错误")
    private Integer status;

    @Schema(description = "判题状态中文描述（由状态码映射）")
    private String statusDesc;

    @Schema(description = "当前得分（各用例得分之和；排队/评测中为 0）")
    private Integer score;

    @Schema(description = "最大耗时（毫秒）；未产出时为 0")
    private Integer timeUsed;

    @Schema(description = "最大内存（KB）；未产出时为 0")
    private Integer memoryUsed;

    /**
     * 是否终态
     */
    @Schema(description = "是否终态：true 表示状态不会再变化（状态码 3~11），false 表示仍在排队/编译/评测中")
    private Boolean finished;
}
