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
import com.github.wuzhudexiaopacai.oj.problem.entity.SolutionLike;
import com.github.wuzhudexiaopacai.oj.problem.mapper.ProblemMapper;
import com.github.wuzhudexiaopacai.oj.problem.mapper.SolutionLikeMapper;
import com.github.wuzhudexiaopacai.oj.problem.mapper.SolutionMapper;
import com.github.wuzhudexiaopacai.oj.problem.service.SolutionService;
import com.github.wuzhudexiaopacai.oj.problem.vo.SolutionLikeVO;
import com.github.wuzhudexiaopacai.oj.problem.vo.SolutionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final SolutionMapper solutionMapper;
    private final ProblemMapper problemMapper;
    private final SolutionLikeMapper solutionLikeMapper;
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
        Set<Long> likedIds = likedSolutionIds(SecurityUtils.currentUserIdOrNull(),
                page.getRecords().stream().map(Solution::getId).toList());
        return PageResult.of(page, solution -> {
            SolutionVO vo = toVO(solution, authorNames, likedIds.contains(solution.getId()));
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
        return toVO(solution, loadAuthorNames(List.of(solution)),
                isLiked(id, SecurityUtils.currentUserIdOrNull()));
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

    @Transactional(rollbackFor = Exception.class)
    @Override
    public SolutionLikeVO toggleLike(Long id) {
        LoginUser loginUser = SecurityUtils.requireLogin();
        Solution solution = solutionMapper.selectById(id);
        Asserts.notNull(solution, "题解不存在");
        Asserts.isTrue(solution.getStatus() == STATUS_NORMAL, "题解待审核中");

        int base = solution.getLikeCount() == null ? 0 : solution.getLikeCount();
        Long userId = loginUser.getId();
        boolean nowLiked;
        int count;
        if (isLiked(id, userId)) {
            // 删除返回实际移除行数；并发重复取消时只有真正删除的那次才减计数
            int removed = solutionLikeMapper.delete(new LambdaQueryWrapper<SolutionLike>()
                    .eq(SolutionLike::getSolutionId, id)
                    .eq(SolutionLike::getUserId, userId));
            nowLiked = false;
            if (removed > 0) {
                updateLikeCount(id, -1);
                count = Math.max(0, base - 1);
            } else {
                count = base;
            }
        } else {
            SolutionLike like = new SolutionLike();
            like.setSolutionId(id);
            like.setUserId(userId);
            nowLiked = true;
            try {
                solutionLikeMapper.insert(like);
                updateLikeCount(id, 1);
                count = base + 1;
            } catch (DuplicateKeyException e) {
                // 唯一键兜底并发重复点赞：本次未真正新增，不重复计数
                count = base;
            }
        }
        log.info("题解点赞切换 solutionId={} userId={} liked={} likeCount={}", id, userId, nowLiked, count);
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

    private SolutionVO toVO(Solution solution, Map<Long, String> authorNames, boolean liked) {
        SolutionVO vo = BeanUtil.copyProperties(solution, SolutionVO.class);
        vo.setAuthorName(authorNames.get(solution.getUserId()));
        vo.setLiked(liked);
        return vo;
    }

    private boolean isLiked(Long solutionId, Long currentUserId) {
        if (currentUserId == null) {
            return false;
        }
        Long count = solutionLikeMapper.selectCount(new LambdaQueryWrapper<SolutionLike>()
                .eq(SolutionLike::getSolutionId, solutionId)
                .eq(SolutionLike::getUserId, currentUserId));
        return count != null && count > 0;
    }

    /**
     * 批量查询当前用户已点赞的题解 ID，避免列表逐条查询
     */
    private Set<Long> likedSolutionIds(Long currentUserId, List<Long> solutionIds) {
        if (currentUserId == null || solutionIds.isEmpty()) {
            return Collections.emptySet();
        }
        return solutionLikeMapper.selectList(new LambdaQueryWrapper<SolutionLike>()
                        .eq(SolutionLike::getUserId, currentUserId)
                        .in(SolutionLike::getSolutionId, solutionIds))
                .stream()
                .map(SolutionLike::getSolutionId)
                .collect(Collectors.toSet());
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
