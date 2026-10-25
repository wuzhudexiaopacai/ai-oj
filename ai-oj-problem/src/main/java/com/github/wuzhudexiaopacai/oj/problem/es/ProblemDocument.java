package com.github.wuzhudexiaopacai.oj.problem.es;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.util.List;

/**
 * 题目 ES 文档（index ai_oj_problem，MySQL → ES 由 MQ 最终一致同步）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@Document(indexName = "ai_oj_problem")
public class ProblemDocument implements Serializable {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String problemKey;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String title;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Integer)
    private Integer difficulty;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Integer)
    private Integer status;
}
