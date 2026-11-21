package com.github.wuzhudexiaopacai.oj.problem.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.github.wuzhudexiaopacai.oj.api.dto.UserBriefDTO;
import com.github.wuzhudexiaopacai.oj.api.feign.AuthFeignClient;
import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.problem.entity.Solution;
import com.github.wuzhudexiaopacai.oj.problem.entity.SolutionLike;
import com.github.wuzhudexiaopacai.oj.problem.mapper.ProblemMapper;
import com.github.wuzhudexiaopacai.oj.problem.mapper.SolutionLikeMapper;
import com.github.wuzhudexiaopacai.oj.problem.mapper.SolutionMapper;
import com.github.wuzhudexiaopacai.oj.problem.vo.SolutionLikeVO;
import com.github.wuzhudexiaopacai.oj.problem.vo.SolutionVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 题解服务测试：点赞切换与作者信息回填
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class SolutionServiceImplTest {

    @Mock
    private SolutionMapper solutionMapper;

    @Mock
    private ProblemMapper problemMapper;

    @Mock
    private SolutionLikeMapper solutionLikeMapper;

    @Mock
    private AuthFeignClient authFeignClient;

    @InjectMocks
    private SolutionServiceImpl solutionService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void login() {
        LoginUser loginUser = LoginUser.builder().id(1001L).username("alice").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, List.of()));
    }

    private Solution solution(Integer likeCount) {
        Solution solution = new Solution();
        solution.setId(1L);
        solution.setProblemId(10001L);
        solution.setUserId(1001L);
        solution.setTitle("题解标题");
        solution.setContent("## 正文");
        solution.setLikeCount(likeCount);
        solution.setStatus(1);
        return solution;
    }

    @Test
    void toggleLikeShouldAddWhenNotLiked() {
        login();
        when(solutionMapper.selectById(1L)).thenReturn(solution(5));
        when(solutionLikeMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(solutionLikeMapper.insert(any(SolutionLike.class))).thenReturn(1);

        SolutionLikeVO result = solutionService.toggleLike(1L);

        assertTrue(result.getLiked());
        assertEquals(6, result.getLikeCount());
        verify(solutionLikeMapper).insert(any(SolutionLike.class));
        verify(solutionMapper).update(isNull(), any());
    }

    @Test
    void toggleLikeShouldRemoveWhenAlreadyLiked() {
        login();
        when(solutionMapper.selectById(1L)).thenReturn(solution(5));
        when(solutionLikeMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        when(solutionLikeMapper.delete(any(Wrapper.class))).thenReturn(1);

        SolutionLikeVO result = solutionService.toggleLike(1L);

        assertEquals(false, result.getLiked());
        assertEquals(4, result.getLikeCount());
        verify(solutionLikeMapper).delete(any(Wrapper.class));
        verify(solutionMapper).update(isNull(), any());
    }

    @Test
    void toggleLikeShouldNotDoubleCountConcurrentAdd() {
        login();
        when(solutionMapper.selectById(1L)).thenReturn(solution(5));
        when(solutionLikeMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        // 唯一键兜底：并发重复点赞时插入冲突，本次不重复计数
        when(solutionLikeMapper.insert(any(SolutionLike.class)))
                .thenThrow(new DuplicateKeyException("uk_solution_user"));

        SolutionLikeVO result = solutionService.toggleLike(1L);

        assertTrue(result.getLiked());
        assertEquals(5, result.getLikeCount());
        verify(solutionMapper, never()).update(isNull(), any());
    }

    @Test
    void toggleLikeShouldNotDoubleDecrementConcurrentRemove() {
        login();
        when(solutionMapper.selectById(1L)).thenReturn(solution(5));
        when(solutionLikeMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        // 并发重复取消：本次未真正删除，不应再减计数
        when(solutionLikeMapper.delete(any(Wrapper.class))).thenReturn(0);

        SolutionLikeVO result = solutionService.toggleLike(1L);

        assertEquals(false, result.getLiked());
        assertEquals(5, result.getLikeCount());
        verify(solutionMapper, never()).update(isNull(), any());
    }

    @Test
    void toggleLikeShouldRejectMissingSolution() {
        login();
        when(solutionMapper.selectById(1L)).thenReturn(null);

        assertThrows(com.github.wuzhudexiaopacai.oj.common.core.exception.BizException.class,
                () -> solutionService.toggleLike(1L));
    }

    @Test
    void detailShouldFillAuthorNameAndLiked() {
        login();
        when(solutionMapper.selectById(1L)).thenReturn(solution(5));
        when(solutionLikeMapper.selectCount(any(Wrapper.class))).thenReturn(1L);
        UserBriefDTO brief = new UserBriefDTO();
        brief.setId(1001L);
        brief.setNickname("Alice");
        when(authFeignClient.batchGetUserBrief(any())).thenReturn(Result.ok(List.of(brief)));

        SolutionVO detail = solutionService.detail(1L);

        assertEquals("Alice", detail.getAuthorName());
        assertTrue(detail.getLiked());
        assertEquals("## 正文", detail.getContent());
    }

    @Test
    void detailShouldDegradeAuthorNameWhenFeignFails() {
        login();
        when(solutionMapper.selectById(1L)).thenReturn(solution(5));
        when(solutionLikeMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(authFeignClient.batchGetUserBrief(any())).thenThrow(new RuntimeException("auth down"));

        SolutionVO detail = solutionService.detail(1L);

        assertNull(detail.getAuthorName());
        assertEquals(false, detail.getLiked());
    }
}
