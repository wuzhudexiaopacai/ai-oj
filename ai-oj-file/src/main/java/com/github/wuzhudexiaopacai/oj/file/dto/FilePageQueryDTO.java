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
@Schema(description = "文件分页查询请求")
public class FilePageQueryDTO implements Serializable {

    /**
     * 页码
     */
    @Schema(description = "页码", defaultValue = "1")
    private long pageNum = 1;

    /**
     * 每页大小
     */
    @Schema(description = "每页大小", defaultValue = "10")
    private long pageSize = 10;

    /**
     * 文件名（模糊）
     */
    @Schema(description = "文件名（模糊）")
    private String fileName;

    /**
     * 业务类型
     */
    @Schema(description = "业务类型")
    private String bizType;

    /**
     * 状态：0上传中 1已完成 2已清理
     */
    @Schema(description = "状态：0上传中 1已完成 2已清理")
    private Integer status;
}
