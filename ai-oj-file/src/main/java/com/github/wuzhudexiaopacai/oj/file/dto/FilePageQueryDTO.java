package com.github.wuzhudexiaopacai.oj.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 文件分页查询请求（管理端）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "文件分页查询请求（管理端，需 file:manage 权限）")
public class FilePageQueryDTO implements Serializable {

    /**
     * 页码
     */
    @Schema(description = "页码，从 1 开始，默认 1", defaultValue = "1", example = "1")
    private long pageNum = 1;

    /**
     * 每页大小
     */
    @Schema(description = "每页条数，默认 10", defaultValue = "10", example = "10")
    private long pageSize = 10;

    /**
     * 文件名（模糊）
     */
    @Schema(description = "文件名模糊匹配条件，可为空", example = "case")
    private String fileName;

    /**
     * 业务类型
     */
    @Schema(description = "业务类型精确匹配：avatar/testcase/attachment/common，可为空", example = "testcase")
    private String bizType;

    /**
     * 状态：0上传中 1已完成 2已清理
     */
    @Schema(description = "上传状态精确匹配：0 上传中、1 已完成、2 已清理，可为空", example = "1")
    private Integer status;
}
