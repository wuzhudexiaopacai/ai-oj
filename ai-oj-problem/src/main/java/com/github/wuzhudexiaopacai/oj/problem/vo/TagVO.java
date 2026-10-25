package com.github.wuzhudexiaopacai.oj.problem.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 标签展示对象
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class TagVO implements Serializable {

    private Long id;

    private String name;

    private String color;
}
