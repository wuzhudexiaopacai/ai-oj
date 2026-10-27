package com.github.wuzhudexiaopacai.oj.problem.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 标签展示对象
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Schema(description = "标签展示对象")
public class TagVO implements Serializable {

    @Schema(description = "标签 ID")
    private Long id;

    @Schema(description = "标签名称")
    private String name;

    @Schema(description = "标签展示颜色（如 #FF0000），未配置时为空字符串")
    private String color;
}
