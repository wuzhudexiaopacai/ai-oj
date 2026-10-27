package com.github.wuzhudexiaopacai.oj.problem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.github.wuzhudexiaopacai.oj.common.web.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 题目实体（pms_problem）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("pms_problem")
@Schema(description = "题目实体（管理端分页直接返回该实体，含全部字段，含下架/私有题目）")
public class Problem extends BaseEntity {

    /**
     * 展示题号，如 A1001
     */
    @Schema(description = "展示题号，全局唯一，如 A1001", example = "A1001")
    private String problemKey;

    /**
     * 题目标题
     */
    @Schema(description = "题目标题", example = "两数之和")
    private String title;

    /**
     * 题目描述（Markdown）
     */
    @Schema(description = "题目描述（Markdown 原文）", example = "给定两个整数，求它们的和。")
    private String description;

    /**
     * 输入描述
     */
    @Schema(description = "输入格式描述", example = "一行两个整数 a b（-10^9 ≤ a, b ≤ 10^9）")
    private String inputDesc;

    /**
     * 输出描述
     */
    @Schema(description = "输出格式描述", example = "一行一个整数，表示 a + b")
    private String outputDesc;

    /**
     * 提示
     */
    @Schema(description = "提示信息，可为空", example = "注意使用 long 防止溢出")
    private String hint;

    /**
     * 来源
     */
    @Schema(description = "题目来源，可为空", example = "入门题库")
    private String source;

    /**
     * 难度：1简单 2中等 3困难
     */
    @Schema(description = "难度：0简单 1中等 2困难", example = "0")
    private Integer difficulty;

    /**
     * 时间限制（ms）
     */
    @Schema(description = "时间限制（毫秒）", example = "1000")
    private Integer timeLimit;

    /**
     * 内存限制（MB）
     */
    @Schema(description = "内存限制（MB）", example = "256")
    private Integer memoryLimit;

    /**
     * 栈限制（MB）
     */
    @Schema(description = "栈限制（MB），0 表示不限制", example = "64")
    private Integer stackLimit;

    /**
     * 是否特殊判题：0否 1是
     */
    @Schema(description = "是否特殊判题：0否 1是", example = "0")
    private Integer isSpj;

    /**
     * SPJ 判题代码
     */
    @Schema(description = "SPJ 判题代码，仅 isSpj=1 时有值", example = "")
    private String spjCode;

    /**
     * SPJ 语言
     */
    @Schema(description = "SPJ 判题代码语言，仅 isSpj=1 时有值", example = "cpp")
    private String spjLanguage;

    /**
     * 计分模式：0 ACM 1 OI
     */
    @Schema(description = "计分模式：0 ACM（按用例全通过计分） 1 OI（按用例分值累加）", example = "0")
    private Integer ioScore;

    /**
     * 状态：0下架 1上架 2比赛中
     */
    @Schema(description = "状态：0下架 1上架 2比赛中", example = "1")
    private Integer status;

    /**
     * 可见性：1公开 2私有 3比赛
     */
    @Schema(description = "可见性：1公开 2私有 3比赛", example = "1")
    private Integer auth;

    /**
     * 通过数（MQ 异步统计）
     */
    @Schema(description = "通过数，由判题结果消息异步累加", example = "128")
    private Integer acceptCount;

    /**
     * 提交数（MQ 异步统计）
     */
    @Schema(description = "提交数，由判题结果消息异步累加", example = "256")
    private Integer submitCount;

    /**
     * 乐观锁版本
     */
    @Version
    @Schema(description = "乐观锁版本号，更新时自动 +1", example = "1")
    private Integer version;
}
