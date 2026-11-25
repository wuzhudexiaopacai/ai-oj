package com.github.wuzhudexiaopacai.oj.ai.service.impl;

import com.github.wuzhudexiaopacai.oj.ai.constant.AiConstants;
import com.github.wuzhudexiaopacai.oj.ai.entity.AiPromptTemplate;
import com.github.wuzhudexiaopacai.oj.ai.mapper.AiPromptTemplateMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 提示词模板服务测试：更新后失效缓存
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class PromptTemplateServiceImplTest {

    @Mock
    private AiPromptTemplateMapper promptTemplateMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private PromptTemplateServiceImpl promptTemplateService;

    @Test
    void updateShouldPersistTemplateAndEvictCache() {
        AiPromptTemplate template = new AiPromptTemplate();
        template.setId(1L);
        template.setCode("review");
        template.setTemplate("旧模板");
        when(promptTemplateMapper.selectOne(any())).thenReturn(template);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        promptTemplateService.updateTemplate("review", "代码评审", "新模板");

        assertEquals("新模板", template.getTemplate());
        assertEquals("代码评审", template.getName());
        verify(promptTemplateMapper).updateById(template);
        verify(hashOperations).delete(AiConstants.PROMPT_TEMPLATE_HASH, "review");
    }
}
