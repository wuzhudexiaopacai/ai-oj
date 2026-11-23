package com.github.wuzhudexiaopacai.oj.judge.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.wuzhudexiaopacai.oj.api.dto.ContestProblemStatusDTO;
import com.github.wuzhudexiaopacai.oj.api.dto.JudgeCaseDTO;
import com.github.wuzhudexiaopacai.oj.api.dto.ProblemBriefDTO;
import com.github.wuzhudexiaopacai.oj.api.dto.ProblemJudgeDTO;
import com.github.wuzhudexiaopacai.oj.api.feign.FileFeignClient;
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
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    /**
     * 失败用例内容回显的最大长度（字符），避免大用例把详情响应撑大
     */
    private static final int CASE_PREVIEW_MAX_LENGTH = 4096;

    private final JudgeSubmissionMapper judgeSubmissionMapper;

    private final JudgeCaseResultMapper judgeCaseResultMapper;

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper;

    private final ProblemFeignClient problemFeignClient;

    private final FileFeignClient fileFeignClient;

    /**
     * 远程读并行执行器（有界线程池），用于并行回填题目简要信息
     */
    private final ExecutorService judgeRemoteReadExecutor;

    /**
     * 题目简要信息本地缓存上限：题目数可能持续增长，必须限制容量避免堆内存无限膨胀
     */
    private static final int PROBLEM_BRIEF_CACHE_SIZE = 5000;

    /**
     * 题目简要信息本地缓存：提交记录分页时避免逐行远程调用（按访问顺序淘汰的 LRU）
     */
    private final Map<Long, ProblemBriefDTO> problemBriefCache = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, ProblemBriefDTO> eldest) {
                    return size() > PROBLEM_BRIEF_CACHE_SIZE;
                }
            });

    /**
     * 当前用户提交分页
     *
     * @param pageNum   页码
     * @param pageSize  页大小
     * @param problemId 题目 ID（可选）
     * @param status    状态（可选）
     * @param language  编程语言（可选）
     * @param keyword   关键字（可选，匹配提交标识或题号/标题）
     * @param startTime 提交时间下界（可选）
     * @param endTime   提交时间上界（可选）
     * @return 分页结果
     */
    public PageResult<SubmissionPageVO> page(long pageNum, long pageSize, Long problemId, Integer status,
                                             String language, String keyword,
                                             LocalDateTime startTime, LocalDateTime endTime) {
        Long userId = SecurityUtils.requireLogin().getId();
        LambdaQueryWrapper<JudgeSubmission> wrapper = new LambdaQueryWrapper<JudgeSubmission>()
                .eq(JudgeSubmission::getUserId, userId)
                .eq(problemId != null, JudgeSubmission::getProblemId, problemId)
                .eq(status != null, JudgeSubmission::getStatus, status)
                .eq(StringUtils.hasText(language), JudgeSubmission::getLanguage, language)
                .ge(startTime != null, JudgeSubmission::getCreateTime, startTime)
                .le(endTime != null, JudgeSubmission::getCreateTime, endTime)
                .orderByDesc(JudgeSubmission::getId);
        if (StringUtils.hasText(keyword)) {
            List<Long> problemIds = searchProblemIds(keyword);
            wrapper.and(w -> {
                w.like(JudgeSubmission::getSubmitId, keyword);
                if (!problemIds.isEmpty()) {
                    w.or().in(JudgeSubmission::getProblemId, problemIds);
                }
            });
        }
        Page<JudgeSubmission> page = judgeSubmissionMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Map<Long, ProblemBriefDTO> briefs = loadProblemBriefs(page.getRecords());
        return PageResult.of(page, submission -> toPageVO(submission, briefs));
    }

    /**
     * 并行回填本页题目的简要信息
     * <p>
     * 缓存未命中的题目相互独立，串行远程调用会让分页耗时随行数增长；
     * 使用有界线程池并行拉取，失败仍按原逻辑降级为不展示题号/标题。
     *
     * @param records 本页提交记录
     * @return 题目 ID 到简要信息的映射（缺失即降级，不包含失败项）
     */
    private Map<Long, ProblemBriefDTO> loadProblemBriefs(List<JudgeSubmission> records) {
        Map<Long, ProblemBriefDTO> briefs = new LinkedHashMap<>();
        if (records == null || records.isEmpty()) {
            return briefs;
        }
        List<Long> missing = new ArrayList<>();
        for (JudgeSubmission submission : records) {
            Long problemId = submission.getProblemId();
            if (problemId == null || missing.contains(problemId)) {
                continue;
            }
            ProblemBriefDTO cached = problemBriefCache.get(problemId);
            if (cached != null) {
                briefs.put(problemId, cached);
            } else {
                missing.add(problemId);
            }
        }
        if (missing.isEmpty()) {
            return briefs;
        }
        List<CompletableFuture<ProblemBriefDTO>> futures = new ArrayList<>(missing.size());
        try {
            for (Long problemId : missing) {
                futures.add(CompletableFuture.supplyAsync(() -> briefOf(problemId), judgeRemoteReadExecutor));
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception e) {
            log.warn("并行回填题目信息失败，降级为不展示题号/标题", e);
        }
        // 已完成的结果逐个取回；未完成或失败的按 null 降级
        for (int i = 0; i < futures.size(); i++) {
            ProblemBriefDTO brief = futures.get(i).getNow(null);
            if (brief != null) {
                briefs.put(missing.get(i), brief);
            }
        }
        return briefs;
    }

    /**
     * 按关键字检索题目 ID（题号/标题），供提交记录关键字筛选使用
     */
    private List<Long> searchProblemIds(String keyword) {
        try {
            Result<List<Long>> result = problemFeignClient.searchIds(keyword);
            List<Long> ids = result == null ? null : result.getData();
            return ids == null ? List.of() : ids;
        } catch (Exception e) {
            log.warn("按关键字检索题目失败 keyword={}", keyword, e);
            return List.of();
        }
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
        List<SubmissionDetailVO.CaseResultItem> caseItems = cases.stream().map(c -> {
            SubmissionDetailVO.CaseResultItem item = new SubmissionDetailVO.CaseResultItem();
            BeanUtils.copyProperties(c, item);
            item.setStatusDesc(JudgeStatusEnum.fromCode(c.getStatus()).getDesc());
            return item;
        }).toList();
        vo.setCases(caseItems);
        // 仅普通题目（非比赛）回显失败用例的输入/期望输出；比赛提交一律不回显，避免泄露赛题数据
        if (isNormalSubmission(submission) && !caseItems.isEmpty()
                && statusEnum != JudgeStatusEnum.ACCEPTED) {
            fillFailedCaseContent(submission.getProblemId(), caseItems);
        }
        return vo;
    }

    /**
     * 是否为普通题目提交（非比赛）
     */
    private boolean isNormalSubmission(JudgeSubmission submission) {
        return submission.getContestId() == null || submission.getContestId() == 0L;
    }

    /**
     * 回填失败用例的输入与期望输出（超长截断），任一步失败只降级不阻断详情查询
     */
    private void fillFailedCaseContent(Long problemId, List<SubmissionDetailVO.CaseResultItem> items) {
        int acceptedCode = JudgeStatusEnum.ACCEPTED.getCode();
        List<SubmissionDetailVO.CaseResultItem> failedItems = items.stream()
                .filter(item -> item.getStatus() == null || item.getStatus() != acceptedCode)
                .toList();
        if (failedItems.isEmpty()) {
            return;
        }
        Map<Integer, JudgeCaseDTO> caseMap;
        try {
            Result<ProblemJudgeDTO> result = problemFeignClient.getJudgeInfo(problemId);
            if (result == null || !result.isSuccess() || result.getData() == null
                    || result.getData().getCases() == null) {
                return;
            }
            caseMap = result.getData().getCases().stream()
                    .collect(Collectors.toMap(JudgeCaseDTO::getSeq, Function.identity(), (a, b) -> a));
        } catch (Exception e) {
            log.warn("回显失败用例时拉取题目用例失败 problemId={}", problemId, e);
            return;
        }
        for (SubmissionDetailVO.CaseResultItem item : failedItems) {
            JudgeCaseDTO judgeCase = caseMap.get(item.getSeq());
            if (judgeCase == null) {
                continue;
            }
            item.setInputPreview(casePreview(judgeCase.getInput(), judgeCase.getInputFile()));
            item.setExpectedPreview(casePreview(judgeCase.getOutput(), judgeCase.getOutputFile()));
        }
    }

    /**
     * 用例内容预览：内联内容优先，大数据走 MinIO 文件读取，超长截断
     */
    private String casePreview(String inlineContent, String objectKey) {
        String content = inlineContent;
        if (!StringUtils.hasText(content) && StringUtils.hasText(objectKey)) {
            try {
                Result<String> fileResult = fileFeignClient.getContent(objectKey);
                content = fileResult == null || !fileResult.isSuccess() ? null : fileResult.getData();
            } catch (Exception e) {
                log.warn("读取用例文件失败 objectKey={}", objectKey, e);
                return null;
            }
        }
        if (content == null) {
            return null;
        }
        return content.length() <= CASE_PREVIEW_MAX_LENGTH
                ? content
                : StrUtil.subPre(content, CASE_PREVIEW_MAX_LENGTH) + "\n...（内容过长，已截断）";
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
                new LambdaQueryWrapper<JudgeSubmission>()
                        .eq(JudgeSubmission::getUserId, userId)
                        .eq(JudgeSubmission::getSubmitId, submitId));
        Asserts.notNull(submission, "提交记录不存在");
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
     * 用户在指定比赛内的题目通过状态（比赛题目列表标记用）
     * <p>
     * 仅统计携带该 contestId 的提交，赛前历史提交不计入。
     *
     * @param contestId 比赛 ID
     * @param userId    用户 ID
     * @return 已通过 / 提交过 的题目 ID
     */
    public ContestProblemStatusDTO contestProblemStatus(Long contestId, Long userId) {
        ContestProblemStatusDTO dto = new ContestProblemStatusDTO();
        dto.setSolvedIds(judgeSubmissionMapper.selectSolvedProblemIdsByContest(contestId, userId));
        dto.setAttemptedIds(judgeSubmissionMapper.selectAttemptedProblemIdsByContest(contestId, userId));
        return dto;
    }

    /**
     * 判题进度（轮询兜底：先查 Redis，miss 再查 DB 组装）
     *
     * @param submitId 幂等提交标识
     * @return 进度
     */
    public JudgeProgressVO progress(String submitId) {
        Long userId = SecurityUtils.requireLogin().getId();
        // 缓存 key 带用户维度，避免不同用户的同名 submitId 互相读到对方进度
        String cacheKey = RedisKeys.JUDGE_PROGRESS + userId + ":" + submitId;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, JudgeProgressVO.class);
            } catch (Exception e) {
                log.warn("判题进度缓存反序列化失败 submitId={}", submitId, e);
            }
        }

        JudgeSubmission submission = judgeSubmissionMapper.selectOne(
                new LambdaQueryWrapper<JudgeSubmission>()
                        .eq(JudgeSubmission::getUserId, userId)
                        .eq(JudgeSubmission::getSubmitId, submitId));
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

    private SubmissionPageVO toPageVO(JudgeSubmission submission, Map<Long, ProblemBriefDTO> briefs) {
        SubmissionPageVO vo = new SubmissionPageVO();
        BeanUtils.copyProperties(submission, vo);
        vo.setStatusDesc(JudgeStatusEnum.fromCode(submission.getStatus()).getDesc());
        // 回填题号/标题（已并行预取），缺失不影响列表主体展示
        ProblemBriefDTO brief = briefs.get(submission.getProblemId());
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
