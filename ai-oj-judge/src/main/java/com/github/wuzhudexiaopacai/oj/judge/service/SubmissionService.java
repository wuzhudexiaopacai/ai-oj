package com.github.wuzhudexiaopacai.oj.judge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.wuzhudexiaopacai.oj.api.dto.ProblemBriefDTO;
import com.github.wuzhudexiaopacai.oj.api.feign.ProblemFeignClient;
import com.github.wuzhudexiaopacai.oj.common.core.constant.RedisKeys;
import com.github.wuzhudexiaopacai.oj.common.core.enums.JudgeStatusEnum;
import com.github.wuzhudexiaopacai.oj.common.core.exception.Asserts;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import com.github.wuzhudexiaopacai.oj.common.web.util.SecurityUtils;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeCaseResult;
import com.github.wuzhudexiaopacai.oj.judge.entity.JudgeSubmission;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeCaseResultMapper;
import com.github.wuzhudexiaopacai.oj.judge.mapper.JudgeSubmissionMapper;
import com.github.wuzhudexiaopacai.oj.judge.vo.JudgeProgressVO;
import com.github.wuzhudexiaopacai.oj.judge.vo.SubmissionDetailVO;
import com.github.wuzhudexiaopacai.oj.judge.vo.SubmissionPageVO;
import com.github.wuzhudexiaopacai.oj.judge.vo.UserProblemStatusVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 提交记录查询服务
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final JudgeSubmissionMapper judgeSubmissionMapper;

    private final JudgeCaseResultMapper judgeCaseResultMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    private final ProblemFeignClient problemFeignClient;

    /**
     * 题目简要信息本地缓存：提交记录分页时避免逐行远程调用
     */
    private final Map<Long, ProblemBriefDTO> problemBriefCache = new ConcurrentHashMap<>();

    /**
     * 当前用户提交分页
     *
     * @param pageNum   页码
     * @param pageSize  页大小
     * @param problemId 题目 ID（可选）
     * @param status    状态（可选）
     * @return 分页结果
     */
    public PageResult<SubmissionPageVO> page(long pageNum, long pageSize, Long problemId, Integer status) {
        Long userId = SecurityUtils.requireLogin().getId();
        Page<JudgeSubmission> page = judgeSubmissionMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<JudgeSubmission>()
                        .eq(JudgeSubmission::getUserId, userId)
                        .eq(problemId != null, JudgeSubmission::getProblemId, problemId)
                        .eq(status != null, JudgeSubmission::getStatus, status)
                        .orderByDesc(JudgeSubmission::getId));
        return PageResult.of(page, this::toPageVO);
    }

    /**
     * 提交详情（含用例明细）
     *
     * @param id 提交记录 ID
     * @return 详情
     */
    public SubmissionDetailVO detail(Long id) {
        Long userId = SecurityUtils.requireLogin().getId();
        JudgeSubmission submission = judgeSubmissionMapper.selectById(id);
        Asserts.notNull(submission, "提交记录不存在");
        Asserts.isTrue(submission.getUserId().equals(userId), "无权查看该提交");

        SubmissionDetailVO vo = new SubmissionDetailVO();
        BeanUtils.copyProperties(submission, vo);
        JudgeStatusEnum statusEnum = JudgeStatusEnum.fromCode(submission.getStatus());
        vo.setStatusDesc(statusEnum.getDesc());

        List<JudgeCaseResult> cases = judgeCaseResultMapper.selectList(
                new LambdaQueryWrapper<JudgeCaseResult>()
                        .eq(JudgeCaseResult::getSubmissionId, id)
                        .orderByAsc(JudgeCaseResult::getSeq));
        vo.setCases(cases.stream().map(c -> {
            SubmissionDetailVO.CaseResultItem item = new SubmissionDetailVO.CaseResultItem();
            BeanUtils.copyProperties(c, item);
            item.setStatusDesc(JudgeStatusEnum.fromCode(c.getStatus()).getDesc());
            return item;
        }).toList());
        return vo;
    }

    /**
     * 按 submitId 查询提交详情（前端轮询评测结果使用）
     *
     * @param submitId 幂等提交标识
     * @return 详情
     */
    public SubmissionDetailVO detailBySubmitId(String submitId) {
        Long userId = SecurityUtils.requireLogin().getId();
        JudgeSubmission submission = judgeSubmissionMapper.selectOne(
                new LambdaQueryWrapper<JudgeSubmission>().eq(JudgeSubmission::getSubmitId, submitId));
        Asserts.notNull(submission, "提交记录不存在");
        Asserts.isTrue(submission.getUserId().equals(userId), "无权查看该提交");
        return detail(submission.getId());
    }

    /**
     * 当前用户的题目做题状态（题库列表标记用）
     *
     * @return 已通过 / 提交过 的题目 ID
     */
    public UserProblemStatusVO userProblemStatus() {
        Long userId = SecurityUtils.requireLogin().getId();
        UserProblemStatusVO vo = new UserProblemStatusVO();
        vo.setSolvedIds(judgeSubmissionMapper.selectSolvedProblemIds(userId));
        vo.setAttemptedIds(judgeSubmissionMapper.selectAttemptedProblemIds(userId));
        return vo;
    }

    /**
     * 判题进度（轮询兜底：先查 Redis，miss 再查 DB 组装）
     *
     * @param submitId 幂等提交标识
     * @return 进度
     */
    public JudgeProgressVO progress(String submitId) {
        String cacheKey = RedisKeys.JUDGE_PROGRESS + submitId;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, JudgeProgressVO.class);
            } catch (Exception e) {
                log.warn("判题进度缓存反序列化失败 submitId={}", submitId, e);
            }
        }

        JudgeSubmission submission = judgeSubmissionMapper.selectOne(
                new LambdaQueryWrapper<JudgeSubmission>().eq(JudgeSubmission::getSubmitId, submitId));
        Asserts.notNull(submission, "提交记录不存在");
        return buildProgress(submission);
    }

    /**
     * 组装进度 VO
     */
    public static JudgeProgressVO buildProgress(JudgeSubmission submission) {
        JudgeStatusEnum statusEnum = JudgeStatusEnum.fromCode(submission.getStatus());
        return JudgeProgressVO.builder()
                .submitId(submission.getSubmitId())
                .status(submission.getStatus())
                .statusDesc(statusEnum.getDesc())
                .score(submission.getScore())
                .timeUsed(submission.getTimeUsed())
                .memoryUsed(submission.getMemoryUsed())
                .finished(statusEnum.isTerminal())
                .build();
    }

    private SubmissionPageVO toPageVO(JudgeSubmission submission) {
        SubmissionPageVO vo = new SubmissionPageVO();
        BeanUtils.copyProperties(submission, vo);
        vo.setStatusDesc(JudgeStatusEnum.fromCode(submission.getStatus()).getDesc());
        // 回填题号/标题，失败不影响列表主体展示
        ProblemBriefDTO brief = briefOf(submission.getProblemId());
        if (brief != null) {
            vo.setProblemKey(brief.getProblemKey());
            vo.setProblemTitle(brief.getTitle());
        }
        return vo;
    }

    /**
     * 查询题目简要信息（本地缓存 + 失败降级）
     */
    private ProblemBriefDTO briefOf(Long problemId) {
        if (problemId == null) {
            return null;
        }
        ProblemBriefDTO cached = problemBriefCache.get(problemId);
        if (cached != null) {
            return cached;
        }
        try {
            Result<ProblemBriefDTO> result = problemFeignClient.getBrief(problemId);
            ProblemBriefDTO brief = result == null ? null : result.getData();
            if (brief != null) {
                problemBriefCache.put(problemId, brief);
            }
            return brief;
        } catch (Exception e) {
            log.warn("回填题目信息失败 problemId={}", problemId, e);
            return null;
        }
    }
}
