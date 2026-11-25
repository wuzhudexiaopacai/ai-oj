package com.github.wuzhudexiaopacai.oj.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.wuzhudexiaopacai.oj.ai.constant.AiConstants;
import com.github.wuzhudexiaopacai.oj.ai.entity.AiPromptTemplate;
import com.github.wuzhudexiaopacai.oj.ai.mapper.AiPromptTemplateMapper;
import com.github.wuzhudexiaopacai.oj.ai.service.PromptTemplateService;
import com.github.wuzhudexiaopacai.oj.common.core.exception.Asserts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

/**
 * 提示词模板服务实现（缓存击穿回源 DB，管理端更新后失效缓存）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptTemplateServiceImpl implements PromptTemplateService {

    private final AiPromptTemplateMapper promptTemplateMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public String getTemplate(String code) {
        Object cached = redisTemplate.opsForHash().get(AiConstants.PROMPT_TEMPLATE_HASH, code);
        if (cached != null) {
            return cached.toString();
        }
        AiPromptTemplate entity = promptTemplateMapper.selectOne(new LambdaQueryWrapper<AiPromptTemplate>()
                .eq(AiPromptTemplate::getCode, code)
                .eq(AiPromptTemplate::getStatus, 1));
        Asserts.notNull(entity, "提示词模板不存在或已停用: " + code);
        redisTemplate.opsForHash().put(AiConstants.PROMPT_TEMPLATE_HASH, code, entity.getTemplate());
        return entity.getTemplate();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTemplate(String code, String name, String template) {
        AiPromptTemplate entity = promptTemplateMapper.selectOne(new LambdaQueryWrapper<AiPromptTemplate>()
                .eq(AiPromptTemplate::getCode, code));
        Asserts.notNull(entity, "提示词模板不存在: " + code);
        entity.setTemplate(template);
        if (StringUtils.hasText(name)) {
            entity.setName(name);
        }
        promptTemplateMapper.updateById(entity);
        // 事务提交后再失效缓存：提交前删除会被并发读回源到旧数据并重新写回缓存
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evictCache(code);
                }
            });
        } else {
            evictCache(code);
        }
        log.info("提示词模板已更新: code={}", code);
    }

    /**
     * 失效模板缓存，下次读取回源 DB
     */
    private void evictCache(String code) {
        redisTemplate.opsForHash().delete(AiConstants.PROMPT_TEMPLATE_HASH, code);
    }
}
