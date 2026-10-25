package com.github.wuzhudexiaopacai.oj.problem.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import com.github.wuzhudexiaopacai.oj.common.core.exception.Asserts;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import com.github.wuzhudexiaopacai.oj.common.web.util.SecurityUtils;
import com.github.wuzhudexiaopacai.oj.problem.dto.SolutionSaveDTO;
import com.github.wuzhudexiaopacai.oj.problem.entity.Problem;
import com.github.wuzhudexiaopacai.oj.problem.entity.Solution;
import com.github.wuzhudexiaopacai.oj.problem.mapper.ProblemMapper;
import com.github.wuzhudexiaopacai.oj.problem.mapper.SolutionMapper;
import com.github.wuzhudexiaopacai.oj.problem.service.SolutionService;
import com.github.wuzhudexiaopacai.oj.problem.vo.SolutionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 题解服务实现
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class SolutionServiceImpl implements SolutionService {

    /**
     * 题解状态：正常
     */
    private static final int STATUS_NORMAL = 1;

    private final SolutionMapper solutionMapper;
    private final ProblemMapper problemMapper;

    @Override
    public PageResult<SolutionVO> page(Long problemId, long pageNum, long pageSize) {
        Asserts.notNull(problemId, "题目 ID 不能为空");
        Page<Solution> page = solutionMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Solution>()
                        .eq(Solution::getProblemId, problemId)
                        .eq(Solution::getStatus, STATUS_NORMAL)
                        .orderByDesc(Solution::getLikeCount)
                        .orderByDesc(Solution::getCreateTime));
        return PageResult.of(page, s -> {
            SolutionVO vo = BeanUtil.copyProperties(s, SolutionVO.class);
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
        return BeanUtil.copyProperties(solution, SolutionVO.class);
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
}
