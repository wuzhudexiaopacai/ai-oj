package com.github.wuzhudexiaopacai.oj.judge.vo;

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
public class JudgeRunVO implements Serializable {

    /**
     * 状态码（JudgeStatusEnum.code）
     */
    private Integer status;

    private String statusDesc;

    /**
     * 程序输出
     */
    private String output;

    /**
     * 编译/运行错误信息
     */
    private String errorMessage;

    /**
     * 耗时（毫秒）
     */
    private Integer timeUsed;

    /**
     * 内存（KB）
     */
    private Integer memoryUsed;
}
