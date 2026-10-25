package com.github.wuzhudexiaopacai.oj.problem.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.github.wuzhudexiaopacai.oj.common.web.entity.BaseEntity;
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
public class Problem extends BaseEntity {

    /**
     * 展示题号，如 A1001
     */
    private String problemKey;

    /**
     * 题目标题
     */
    private String title;

    /**
     * 题目描述（Markdown）
     */
    private String description;

    /**
     * 输入描述
     */
    private String inputDesc;

    /**
     * 输出描述
     */
    private String outputDesc;

    /**
     * 提示
     */
    private String hint;

    /**
     * 来源
     */
    private String source;

    /**
     * 难度：1简单 2中等 3困难
     */
    private Integer difficulty;

    /**
     * 时间限制（ms）
     */
    private Integer timeLimit;

    /**
     * 内存限制（MB）
     */
    private Integer memoryLimit;

    /**
     * 栈限制（MB）
     */
    private Integer stackLimit;

    /**
     * 是否特殊判题：0否 1是
     */
    private Integer isSpj;

    /**
     * SPJ 判题代码
     */
    private String spjCode;

    /**
     * SPJ 语言
     */
    private String spjLanguage;

    /**
     * 计分模式：0 ACM 1 OI
     */
    private Integer ioScore;

    /**
     * 状态：0下架 1上架 2比赛中
     */
    private Integer status;

    /**
     * 可见性：1公开 2私有 3比赛
     */
    private Integer auth;

    /**
     * 通过数（MQ 异步统计）
     */
    private Integer acceptCount;

    /**
     * 提交数（MQ 异步统计）
     */
    private Integer submitCount;

    /**
     * 乐观锁版本
     */
    @Version
    private Integer version;
}
