package com.github.wuzhudexiaopacai.oj.problem.service.impl;

import com.github.wuzhudexiaopacai.oj.common.core.exception.BizException;
import com.github.wuzhudexiaopacai.oj.problem.config.ProblemCacheProperties;
import com.github.wuzhudexiaopacai.oj.problem.entity.Problem;
import com.github.wuzhudexiaopacai.oj.problem.mapper.ProblemMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 题目详情可见性测试：未上架/非公开题目不得通过公开详情接口读取
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class ProblemDetailVisibilityTest {

    @Mock
    private ProblemMapper problemMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @Mock
    private ProblemCacheProperties cacheProperties;

    @InjectMocks
    private ProblemServiceImpl problemService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redissonClient.getLock(anyString())).thenReturn(lock);
        lenient().when(valueOperations.get(anyString())).thenReturn(null);
    }

    @Test
    void offlineProblemShouldNotBeReadableByKey() {
        when(problemMapper.selectOne(any())).thenReturn(problemOf(0, 1));

        BizException ex = assertThrows(BizException.class, () -> problemService.detail("A1001"));

        assertTrue(ex.getMessage().contains("题目不存在"));
        verify(valueOperations).set(eq("problem:detail:A1001"), any(), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    void nonPublicProblemShouldNotBeReadableByKey() {
        when(problemMapper.selectOne(any())).thenReturn(problemOf(1, 2));

        BizException ex = assertThrows(BizException.class, () -> problemService.detail("A1001"));

        assertTrue(ex.getMessage().contains("题目不存在"));
    }

    private Problem problemOf(int status, int auth) {
        Problem problem = new Problem();
        problem.setId(1L);
        problem.setProblemKey("A1001");
        problem.setStatus(status);
        problem.setAuth(auth);
        return problem;
    }
}
