package com.github.wuzhudexiaopacai.oj.judge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 自测运行结果
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "自测运行结果（不入库）")
public class JudgeRunVO implements Serializable {

    /**
     * 状态码（JudgeStatusEnum.code）
     */
    @Schema(description = "判题状态码：3 答案正确、4 答案错误、5 运行超时、6 内存超限、7 运行错误、8 编译错误、9 格式错误、10 输出超限、11 系统错误（自测运行不会返回 0/1/2）")
    private Integer status;

    @Schema(description = "判题状态中文描述（由状态码映射）")
    private String statusDesc;

    /**
     * 程序输出
     */
    @Schema(description = "程序标准输出；无输出或编译失败时可能为 null")
    private String output;

    /**
     * 编译/运行错误信息
     */
    @Schema(description = "编译/运行错误信息；无错误时为 null")
    private String errorMessage;

    /**
     * 耗时（毫秒）
     */
    @Schema(description = "耗时（毫秒）")
    private Integer timeUsed;

    /**
     * 内存（KB）
     */
    @Schema(description = "内存（KB）")
    private Integer memoryUsed;
}
