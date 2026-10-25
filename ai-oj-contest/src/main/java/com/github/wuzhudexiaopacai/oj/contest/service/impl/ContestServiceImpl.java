package com.github.wuzhudexiaopacai.oj.contest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.wuzhudexiaopacai.oj.common.core.enums.ContestStatusEnum;
import com.github.wuzhudexiaopacai.oj.common.core.exception.Asserts;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import com.github.wuzhudexiaopacai.oj.api.dto.ContestRuntimeDTO;
import com.github.wuzhudexiaopacai.oj.common.web.util.SecurityUtils;
import com.github.wuzhudexiaopacai.oj.contest.dto.ContestPageQueryDTO;
import com.github.wuzhudexiaopacai.oj.contest.dto.ContestProblemBindDTO;
import com.github.wuzhudexiaopacai.oj.contest.dto.ContestSaveDTO;
import com.github.wuzhudexiaopacai.oj.contest.entity.Contest;
import com.github.wuzhudexiaopacai.oj.contest.entity.ContestProblem;
import com.github.wuzhudexiaopacai.oj.contest.entity.ContestRegistration;
import com.github.wuzhudexiaopacai.oj.contest.mapper.ContestMapper;
import com.github.wuzhudexiaopacai.oj.contest.mapper.ContestProblemMapper;
import com.github.wuzhudexiaopacai.oj.contest.mapper.ContestRegistrationMapper;
import com.github.wuzhudexiaopacai.oj.contest.service.ContestService;
import com.github.wuzhudexiaopacai.oj.contest.vo.ContestDetailVO;
import com.github.wuzhudexiaopacai.oj.contest.vo.ContestPageVO;
import com.github.wuzhudexiaopacai.oj.contest.vo.ContestProblemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 比赛服务实现
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class ContestServiceImpl implements ContestService {

    /**
     * 比赛详情缓存前缀：+ contestId
     */
    public static final String CONTEST_INFO_CACHE = "contest:info:";

    /**
     * 详情缓存 TTL（秒），10min
     */
    private static final long CONTEST_INFO_TTL_SECONDS = 600L;

    /**
     * 默认可见性：公开
     */
    private static final int AUTH_PUBLIC = 1;

    /**
     * 单场比赛题目数量上限（display_id A~Z）
     */
    private static final int MAX_PROBLEM_COUNT = 26;

    private final ContestMapper contestMapper;

    private final ContestProblemMapper contestProblemMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    private final ContestRegistrationMapper contestRegistrationMapper;

    @Override
    public PageResult<ContestPageVO> page(ContestPageQueryDTO query) {
        Page<Contest> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<Contest> wrapper = new LambdaQueryWrapper<Contest>()
                .eq(StringUtils.hasText(query.getStatus()), Contest::getStatus, query.getStatus())
                .eq(query.getType() != null, Contest::getType, query.getType())
                .orderByDesc(Contest::getStartTime)
                .orderByDesc(Contest::getId);
        Page<Contest> result = contestMapper.selectPage(page, wrapper);
        PageResult<ContestPageVO> voPage = PageResult.of(result, this::toPageVO);
        // 报名人数走一次 group by 批量回填，避免逐条查询
        fillRegisterCount(voPage.getRecords());
        return voPage;
    }

    /**
     * 批量回填分页记录的报名人数
     *
     * @param records 当前页记录
     */
    private void fillRegisterCount(List<ContestPageVO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<Long> contestIds = records.stream().map(ContestPageVO::getId).collect(Collectors.toList());
        List<Map<String, Object>> rows = contestRegistrationMapper.selectMaps(new QueryWrapper<ContestRegistration>()
                .select("contest_id AS contestId", "COUNT(*) AS cnt")
                .in("contest_id", contestIds)
                .groupBy("contest_id"));
        Map<Long, Integer> counts = new HashMap<>(rows.size());
        for (Map<String, Object> row : rows) {
            counts.put(((Number) row.get("contestId")).longValue(), ((Number) row.get("cnt")).intValue());
        }
        records.forEach(vo -> vo.setRegisterCount(counts.getOrDefault(vo.getId(), 0)));
    }

    @Override
    public ContestDetailVO detail(Long id) {
        ContestDetailVO detailVO = loadDetail(id);
        // 报名人数/当前用户报名状态与用户相关，每次请求实时补全（不进入详情缓存）
        fillRegistration(id, detailVO);
        return detailVO;
    }

    /**
     * 加载比赛详情基础数据（用户无关，整体缓存）
     */
    private ContestDetailVO loadDetail(Long id) {
        String cacheKey = CONTEST_INFO_CACHE + id;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof ContestDetailVO detailVO) {
            return detailVO;
        }
        Contest contest = contestMapper.selectById(id);
        Asserts.notNull(contest, "比赛不存在");
        ContestDetailVO detailVO = toDetailVO(contest);
        List<ContestProblem> problems = contestProblemMapper.selectList(
                new LambdaQueryWrapper<ContestProblem>()
                        .eq(ContestProblem::getContestId, id)
                        .orderByAsc(ContestProblem::getSort));
        detailVO.setProblems(problems.stream().map(this::toProblemVO).collect(Collectors.toList()));
        // 详情为用户无关数据，可整体缓存；写操作（更新/删除/绑题/状态迁移）主动失效
        redisTemplate.opsForValue().set(cacheKey, detailVO, CONTEST_INFO_TTL_SECONDS, TimeUnit.SECONDS);
        return detailVO;
    }

    /**
     * 补全报名人数与当前用户报名状态
     */
    private void fillRegistration(Long contestId, ContestDetailVO detailVO) {
        Long count = contestRegistrationMapper.selectCount(new LambdaQueryWrapper<ContestRegistration>()
                .eq(ContestRegistration::getContestId, contestId));
        detailVO.setRegisterCount(count == null ? 0 : count.intValue());
        Long userId = SecurityUtils.currentUserIdOrNull();
        boolean registered = false;
        if (userId != null) {
            Long mine = contestRegistrationMapper.selectCount(new LambdaQueryWrapper<ContestRegistration>()
                    .eq(ContestRegistration::getContestId, contestId)
                    .eq(ContestRegistration::getUserId, userId));
            registered = mine != null && mine > 0;
        }
        detailVO.setRegistered(registered);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ContestSaveDTO saveDTO) {
        validateContest(saveDTO);
        Contest contest = new Contest();
        BeanUtils.copyProperties(saveDTO, contest);
        contest.setCreatorId(SecurityUtils.requireLogin().getId());
        contest.setStatus(ContestStatusEnum.SCHEDULED.name());
        if (contest.getAuth() == null) {
            contest.setAuth(AUTH_PUBLIC);
        }
        if (contest.getPassword() == null) {
            contest.setPassword("");
        }
        contestMapper.insert(contest);
        return contest.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ContestSaveDTO saveDTO) {
        Contest exist = contestMapper.selectById(id);
        Asserts.notNull(exist, "比赛不存在");
        validateContest(saveDTO);
        Contest contest = new Contest();
        BeanUtils.copyProperties(saveDTO, contest);
        contest.setId(id);
        // 乐观锁：携带当前版本，并发修改时更新失败由 MyBatis-Plus 乐观锁插件兜底
        contest.setVersion(exist.getVersion());
        contestMapper.updateById(contest);
        evictDetailCache(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Contest exist = contestMapper.selectById(id);
        Asserts.notNull(exist, "比赛不存在");
        contestMapper.deleteById(id);
        // 级联逻辑删除已绑定题目（报名与成绩记录保留，便于历史追溯）
        contestProblemMapper.delete(new LambdaQueryWrapper<ContestProblem>()
                .eq(ContestProblem::getContestId, id));
        evictDetailCache(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long bindProblem(Long contestId, ContestProblemBindDTO bindDTO) {
        Contest contest = contestMapper.selectById(contestId);
        Asserts.notNull(contest, "比赛不存在");
        Long duplicate = contestProblemMapper.selectCount(new LambdaQueryWrapper<ContestProblem>()
                .eq(ContestProblem::getContestId, contestId)
                .eq(ContestProblem::getProblemId, bindDTO.getProblemId()));
        Asserts.isTrue(duplicate == 0, "题目已绑定到该比赛");
        Long count = contestProblemMapper.selectCount(new LambdaQueryWrapper<ContestProblem>()
                .eq(ContestProblem::getContestId, contestId));
        Asserts.isTrue(count < MAX_PROBLEM_COUNT, "比赛题目数量已达上限");
        ContestProblem contestProblem = new ContestProblem();
        contestProblem.setContestId(contestId);
        contestProblem.setProblemId(bindDTO.getProblemId());
        // display_id 按已有数量自动生成 A/B/C...
        contestProblem.setDisplayId(String.valueOf((char) ('A' + count.intValue())));
        contestProblem.setTitle(bindDTO.getTitle() == null ? "" : bindDTO.getTitle());
        contestProblem.setScore(bindDTO.getScore() == null ? 0 : bindDTO.getScore());
        contestProblem.setSort(count.intValue());
        contestProblemMapper.insert(contestProblem);
        evictDetailCache(contestId);
        return contestProblem.getId();
    }

    @Override
    public ContestRuntimeDTO runtime(Long contestId, Long userId) {
        Contest contest = contestMapper.selectById(contestId);
        Asserts.notNull(contest, "比赛不存在");
        ContestRuntimeDTO dto = new ContestRuntimeDTO();
        dto.setContestId(contestId);
        dto.setStatus(contest.getStatus());
        dto.setType(contest.getType());
        dto.setStartTimeMillis(toEpochMilli(contest.getStartTime()));
        dto.setEndTimeMillis(toEpochMilli(contest.getEndTime()));
        dto.setProblemIds(contestProblemMapper.selectList(new LambdaQueryWrapper<ContestProblem>()
                        .eq(ContestProblem::getContestId, contestId))
                .stream()
                .map(ContestProblem::getProblemId)
                .toList());
        if (userId != null) {
            Long registered = contestRegistrationMapper.selectCount(new LambdaQueryWrapper<ContestRegistration>()
                    .eq(ContestRegistration::getContestId, contestId)
                    .eq(ContestRegistration::getUserId, userId)
                    .eq(ContestRegistration::getStatus, 1));
            dto.setRegistered(registered != null && registered > 0);
        }
        return dto;
    }

    /**
     * LocalDateTime → epoch 毫秒（按服务器时区换算，避免跨服务序列化差异）
     */
    private Long toEpochMilli(java.time.LocalDateTime time) {
        return time == null ? null : time.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 失效比赛详情缓存
     *
     * @param contestId 比赛 ID
     */
    public void evictDetailCache(Long contestId) {
        redisTemplate.delete(CONTEST_INFO_CACHE + contestId);
    }

    /**
     * 校验比赛时间合法性
     */
    private void validateContest(ContestSaveDTO saveDTO) {
        Asserts.isTrue(saveDTO.getEndTime().isAfter(saveDTO.getStartTime()), "结束时间必须晚于开始时间");
        if (saveDTO.getSealTime() != null) {
            Asserts.isTrue(!saveDTO.getSealTime().isBefore(saveDTO.getStartTime())
                            && !saveDTO.getSealTime().isAfter(saveDTO.getEndTime()),
                    "封榜时间必须在比赛时间范围内");
        }
    }

    private ContestPageVO toPageVO(Contest contest) {
        ContestPageVO vo = new ContestPageVO();
        BeanUtils.copyProperties(contest, vo);
        return vo;
    }

    private ContestDetailVO toDetailVO(Contest contest) {
        ContestDetailVO vo = new ContestDetailVO();
        // 密码不下发
        BeanUtils.copyProperties(contest, vo, "password");
        return vo;
    }

    private ContestProblemVO toProblemVO(ContestProblem contestProblem) {
        ContestProblemVO vo = new ContestProblemVO();
        BeanUtils.copyProperties(contestProblem, vo);
        return vo;
    }
}
