package com.github.wuzhudexiaopacai.oj.common.web.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类（雪花 ID + 自动填充 + 逻辑删除）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class BaseEntity implements Serializable {

    /**
     * 主键（雪花算法）
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键（雪花 ID，19 位，前端按字符串处理）", example = "1950000000000000001")
    private Long id;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建时间", example = "2026-10-27T22:57:45")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间", example = "2026-10-27T22:57:45")
    private LocalDateTime updateTime;

    /**
     * 创建人（当前登录用户 ID，未登录/系统任务为空）
     */
    @TableField(fill = FieldFill.INSERT)
    @Schema(description = "创建人用户 ID，系统任务或未登录时为空", example = "1")
    private Long createBy;

    /**
     * 更新人（当前登录用户 ID，未登录/系统任务为空）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新人用户 ID，系统任务或未登录时为空", example = "1")
    private Long updateBy;

    /**
     * 逻辑删除标记：0 正常 1 已删除
     */
    @TableLogic
    @Schema(description = "逻辑删除标记：0 正常 1 已删除（查询时自动过滤已删除数据）", example = "0")
    private Integer deleted;
}
