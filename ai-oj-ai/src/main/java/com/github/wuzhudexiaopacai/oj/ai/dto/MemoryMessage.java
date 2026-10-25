package com.github.wuzhudexiaopacai.oj.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 会话记忆消息（Redis List 存储结构）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemoryMessage implements Serializable {

    /**
     * 角色：user/assistant
     */
    private String role;

    /**
     * 内容
     */
    private String content;
}
