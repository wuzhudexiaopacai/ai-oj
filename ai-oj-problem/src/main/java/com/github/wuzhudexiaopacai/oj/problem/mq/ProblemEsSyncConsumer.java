package com.github.wuzhudexiaopacai.oj.problem.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import com.github.wuzhudexiaopacai.oj.problem.dto.ProblemSyncMessage;
import com.github.wuzhudexiaopacai.oj.problem.entity.Problem;
import com.github.wuzhudexiaopacai.oj.problem.entity.ProblemTag;
import com.github.wuzhudexiaopacai.oj.problem.entity.Tag;
import com.github.wuzhudexiaopacai.oj.problem.es.ProblemDocument;
import com.github.wuzhudexiaopacai.oj.problem.es.ProblemEsRepository;
import com.github.wuzhudexiaopacai.oj.problem.mapper.ProblemMapper;
import com.github.wuzhudexiaopacai.oj.problem.mapper.ProblemTagMapper;
import com.github.wuzhudexiaopacai.oj.problem.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 题目 ES 同步消费者（PROBLEM_SYNC_TOPIC，MySQL → ES 最终一致）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(topic = MqConstants.PROBLEM_SYNC_TOPIC, consumerGroup = MqConstants.GROUP_PROBLEM_ES)
public class ProblemEsSyncConsumer implements RocketMQListener<ProblemSyncMessage> {

    /**
     * 消费幂等标记前缀（problemId + 消息时间戳判重）
     */
    private static final String ES_IDEM_KEY = "problem:es:idem:";

    private final ProblemMapper problemMapper;
    private final ProblemTagMapper problemTagMapper;
    private final TagMapper tagMapper;
    private final ProblemEsRepository problemEsRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void onMessage(ProblemSyncMessage message) {
        // 幂等：同一 problemId + 时间戳的消息只处理一次，重复投递直接消费成功
        String idemKey = ES_IDEM_KEY + message.getProblemId() + ":" + message.getTimestamp();
        Boolean first = redisTemplate.opsForValue().setIfAbsent(idemKey, "1", 1, TimeUnit.DAYS);
        if (Boolean.FALSE.equals(first)) {
            log.info("ES 同步消息重复，跳过: {}", idemKey);
            return;
        }
        try {
            if ("delete".equals(message.getOp())) {
                problemEsRepository.deleteById(message.getProblemId());
                return;
            }
            Problem problem = problemMapper.selectById(message.getProblemId());
            if (problem == null) {
                // 数据已删除（逻辑删查不到），按删除处理保证一致
                problemEsRepository.deleteById(message.getProblemId());
                return;
            }
            ProblemDocument doc = new ProblemDocument();
            doc.setId(problem.getId());
            doc.setProblemKey(problem.getProblemKey());
            doc.setTitle(problem.getTitle());
            doc.setDescription(problem.getDescription());
            doc.setDifficulty(problem.getDifficulty());
            doc.setStatus(problem.getStatus());
            doc.setTags(listTagNames(problem.getId()));
            problemEsRepository.save(doc);
        } catch (Exception e) {
            // 处理失败删除幂等标记，让消息重试时重新消费
            redisTemplate.delete(idemKey);
            log.error("ES 同步失败: {}", message, e);
            throw e;
        }
    }

    private List<String> listTagNames(Long problemId) {
        List<ProblemTag> relations = problemTagMapper.selectList(
                new LambdaQueryWrapper<ProblemTag>().eq(ProblemTag::getProblemId, problemId));
        if (relations.isEmpty()) {
            return List.of();
        }
        return tagMapper.selectBatchIds(relations.stream().map(ProblemTag::getTagId).toList())
                .stream().map(Tag::getName).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
