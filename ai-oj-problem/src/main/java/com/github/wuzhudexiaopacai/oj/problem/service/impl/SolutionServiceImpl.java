package com.github.wuzhudexiaopacai.oj.problem.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.wuzhudexiaopacai.oj.api.dto.UserBriefDTO;
import com.github.wuzhudexiaopacai.oj.api.feign.AuthFeignClient;
import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import com.github.wuzhudexiaopacai.oj.common.core.exception.Asserts;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import com.github.wuzhudexiaopacai.oj.common.web.util.SecurityUtils;
import com.github.wuzhudexiaopacai.oj.problem.dto.SolutionSaveDTO;
import com.github.wuzhudexiaopacai.oj.problem.entity.Problem;
import com.github.wuzhudexiaopacai.oj.problem.entity.Solution;
import com.github.wuzhudexiaopacai.oj.problem.mapper.ProblemMapper;
import com.github.wuzhudexiaopacai.oj.problem.mapper.SolutionMapper;
import com.github.wuzhudexiaopacai.oj.problem.service.SolutionService;
import com.github.wuzhudexiaopacai.oj.problem.vo.SolutionLikeVO;
import com.github.wuzhudexiaopacai.oj.problem.vo.SolutionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 题解服务实现
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SolutionServiceImpl implements SolutionService {

    /**
     * 题解状态：正常
     */
    private static final int STATUS_NORMAL = 1;

    /**
     * 题解点赞用户集合 Redis Key 前缀
     */
    private static final String LIKE_KEY_PREFIX = "oj:solution:like:";

    private final SolutionMapper solutionMapper;
    private final ProblemMapper problemMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final AuthFeignClient authFeignClient;

    @Override
    public PageResult<SolutionVO> page(Long problemId, long pageNum, long pageSize) {
        Asserts.notNull(problemId, "题目 ID 不能为空");
        Page<Solution> page = solutionMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Solution>()
                        .eq(Solution::getProblemId, problemId)
                        .eq(Solution::getStatus, STATUS_NORMAL)
                        .orderByDesc(Solution::getLikeCount)
                        .orderByDesc(Solution::getCreateTime));
        Map<Long, String> authorNames = loadAuthorNames(page.getRecords());
        Long currentUserId = SecurityUtils.currentUserIdOrNull();
        return PageResult.of(page, solution -> {
            SolutionVO vo = toVO(solution, authorNames, currentUserId);
            // 列表不返回正文，减小报文
            vo.setContent(null);
            return vo;
        });
    }

    @Override
    public SolutionVO detail(Long id) {
        Solution solution = solutionMapper.selectById(id);
        Asserts.notNull(solution, "题解不存在");
        Asserts.isTrue(solution.getStatus() == STATUS_NORMAL, "题解待审核中");
        return toVO(solution, loadAuthorNames(List.of(solution)), SecurityUtils.currentUserIdOrNull());
    }

    @Override
    public Long create(SolutionSaveDTO saveDTO) {
        LoginUser loginUser = SecurityUtils.requireLogin();
        Problem problem = problemMapper.selectById(saveDTO.getProblemId());
        Asserts.notNull(problem, "题目不存在");
        Solution solution = BeanUtil.copyProperties(saveDTO, Solution.class);
        solution.setUserId(loginUser.getId());
        solution.setLikeCount(0);
        solution.setStatus(STATUS_NORMAL);
        solutionMapper.insert(solution);
        return solution.getId();
    }

    @Override
    public SolutionLikeVO toggleLike(Long id) {
        LoginUser loginUser = SecurityUtils.requireLogin();
        Solution solution = solutionMapper.selectById(id);
        Asserts.notNull(solution, "题解不存在");
        Asserts.isTrue(solution.getStatus() == STATUS_NORMAL, "题解待审核中");

        int base = solution.getLikeCount() == null ? 0 : solution.getLikeCount();
        String key = LIKE_KEY_PREFIX + id;
        String member = loginUser.getId().toString();
        Boolean liked = stringRedisTemplate.opsForSet().isMember(key, member);
        boolean nowLiked;
        int count;
        if (Boolean.TRUE.equals(liked)) {
            stringRedisTemplate.opsForSet().remove(key, member);
            updateLikeCount(id, -1);
            nowLiked = false;
            count = Math.max(0, base - 1);
        } else {
            // add 返回 1 表示本次真正新增；并发重复点赞（返回 0）不再重复计数
            Long added = stringRedisTemplate.opsForSet().add(key, member);
            nowLiked = true;
            if (added != null && added > 0) {
                updateLikeCount(id, 1);
                count = base + 1;
            } else {
                count = base;
            }
        }
        log.info("题解点赞切换 solutionId={} userId={} liked={} likeCount={}", id, loginUser.getId(), nowLiked, count);
        return new SolutionLikeVO(nowLiked, count);
    }

    /**
     * 原子更新点赞数，避免并发覆盖
     *
     * @param id   题解 ID
     * @param delta +1 或 -1
     */
    private void updateLikeCount(Long id, int delta) {
        String sql = delta > 0
                ? "like_count = like_count + 1"
                : "like_count = GREATEST(like_count - 1, 0)";
        solutionMapper.update(null, new LambdaUpdateWrapper<Solution>()
                .eq(Solution::getId, id)
                .setSql(sql));
    }

    private SolutionVO toVO(Solution solution, Map<Long, String> authorNames, Long currentUserId) {
        SolutionVO vo = BeanUtil.copyProperties(solution, SolutionVO.class);
        vo.setAuthorName(authorNames.get(solution.getUserId()));
        vo.setLiked(isLiked(solution.getId(), currentUserId));
        return vo;
    }

    private boolean isLiked(Long solutionId, Long currentUserId) {
        if (currentUserId == null) {
            return false;
        }
        return Boolean.TRUE.equals(stringRedisTemplate.opsForSet()
                .isMember(LIKE_KEY_PREFIX + solutionId, currentUserId.toString()));
    }

    /**
     * 批量查询作者昵称，认证服务不可用时降级为空（不阻断题解浏览）
     *
     * @param solutions 题解列表
     * @return 用户 ID -> 展示名
     */
    private Map<Long, String> loadAuthorNames(List<Solution> solutions) {
        Set<Long> userIds = solutions.stream()
                .map(Solution::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            Result<List<UserBriefDTO>> result = authFeignClient.batchGetUserBrief(new ArrayList<>(userIds));
            if (result == null || result.getData() == null) {
                return Collections.emptyMap();
            }
            Map<Long, String> names = new LinkedHashMap<>();
            for (UserBriefDTO brief : result.getData()) {
                if (brief.getId() == null) {
                    continue;
                }
                String displayName = brief.getNickname() != null && !brief.getNickname().isBlank()
                        ? brief.getNickname()
                        : brief.getUsername();
                names.put(brief.getId(), displayName);
            }
            return names;
        } catch (Exception e) {
            log.warn("题解作者信息查询失败，降级为不展示作者名: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
