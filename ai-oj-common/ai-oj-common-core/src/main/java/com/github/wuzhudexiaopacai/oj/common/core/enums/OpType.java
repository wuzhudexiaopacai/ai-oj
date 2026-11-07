package com.github.wuzhudexiaopacai.oj.common.core.enums;

import lombok.Getter;

/**
 * 操作日志类型
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Getter
public enum OpType {

    /**
     * 其他
     */
    OTHER(0, "其他"),

    /**
     * 新增
     */
    CREATE(1, "新增"),

    /**
     * 修改
     */
    UPDATE(2, "修改"),

    /**
     * 删除
     */
    DELETE(3, "删除"),

    /**
     * 查询
     */
    QUERY(4, "查询"),

    /**
     * 导出
     */
    EXPORT(5, "导出");

    private final int code;

    private final String label;

    OpType(int code, String label) {
        this.code = code;
        this.label = label;
    }
}
