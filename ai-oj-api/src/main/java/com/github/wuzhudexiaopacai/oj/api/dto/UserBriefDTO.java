package com.github.wuzhudexiaopacai.oj.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户简要信息（跨服务传输）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Data
public class UserBriefDTO implements Serializable {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;
}
