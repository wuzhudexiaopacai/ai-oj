package com.github.wuzhudexiaopacai.oj.contest.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.wuzhudexiaopacai.oj.api.dto.ContestProblemStatusDTO;
import com.github.wuzhudexiaopacai.oj.api.feign.JudgeFeignClient;
import com.github.wuzhudexiaopacai.oj.common.core.dto.LoginUser;
import com.github.wuzhudexiaopacai.oj.common.core.enums.ContestStatusEnum;
import com.github.wuzhudexiaopacai.oj.common.core.exception.Asserts;
import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
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
import com.github.wuzhudexiaopacai.oj.contest.vo.ContestTimeVO;
import com.github.wuzhudexiaopacai.oj.contest.websocket.ContestWsPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 比赛服务实现
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
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
     * 可见性：私有（需凭密码报名后才能看到题目）
     */
    private static final int AUTH_PRIVATE = 2;

    /**
     * 比赛管理权限标识（管理员不受题目可见性限制）
     */
    private static final String AUTHORITY_CONTEST_MANAGE = "contest:manage";

    /**
     * 比赛密码哈希器：库内只保存 BCrypt 哈希，不保存明文
     */
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    /**
     * 单场比赛题目数量上限（display_id A~Z）
     */
    private static final int MAX_PROBLEM_COUNT = 26;

    /**
     * 比赛题目状态：已通过
     */
    private static final String PROBLEM_STATUS_AC = "AC";

    /**
     * 比赛题目状态：尝试过但未通过
     */
    private static final String PROBLEM_STATUS_ATTEMPTED = "ATTEMPTED";

    private final ContestMapper contestMapper;

    private final ContestProblemMapper contestProblemMapper;

    private final RedisTemplate<String, Object> redisTemplate;

    private final ContestRegistrationMapper contestRegistrationMapper;

    private final ContestWsPushService wsPushService;

    private final JudgeFeignClient judgeFeignClient;

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

    @Override
    public List<ContestPageVO> mine() {
        Long userId = SecurityUtils.requireLogin().getId();
        List<ContestRegistration> registrations = contestRegistrationMapper.selectList(
                new LambdaQueryWrapper<ContestRegistration>()
                        .eq(ContestRegistration::getUserId, userId));
        if (registrations.isEmpty()) {
            return List.of();
        }
        List<Long> contestIds = registrations.stream()
                .map(ContestRegistration::getContestId)
                .distinct()
                .toList();
        List<Contest> contests = contestMapper.selectBatchIds(contestIds);
        List<ContestPageVO> records = contests.stream()
                .sorted(Comparator.comparing(Contest::getStartTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toPageVO)
                .collect(Collectors.toList());
        fillRegisterCount(records);
        return records;
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
        // 题目可见性：开始前一律隐藏；私有赛还需先凭密码报名；管理员始终可见
        if (!isContestAdmin() && isProblemHidden(detailVO)) {
            detailVO.setProblems(List.of());
        }
        // 比赛题目做题状态同样与用户相关，按 contestId 独立计算后实时补全
        fillProblemStatus(id, detailVO);
        return detailVO;
    }

    /**
     * 是否隐藏题目列表：比赛开始前（含公开赛）或私有赛未报名
     *
     * @param detailVO 比赛详情（报名状态已回填）
     * @return true 表示不返回题目列表
     */
    private boolean isProblemHidden(ContestDetailVO detailVO) {
        if (detailVO.getStartTime() == null || LocalDateTime.now().isBefore(detailVO.getStartTime())) {
            return true;
        }
        return Integer.valueOf(AUTH_PRIVATE).equals(detailVO.getAuth())
                && !Boolean.TRUE.equals(detailVO.getRegistered());
    }

    /**
     * 当前登录用户是否具备比赛管理权限
     */
    private boolean isContestAdmin() {
        LoginUser loginUser = SecurityUtils.currentUser();
        return loginUser != null && loginUser.hasAuthority(AUTHORITY_CONTEST_MANAGE);
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

    /**
     * 补全当前用户在该比赛内的题目做题状态（未登录跳过，远程失败降级为空状态）
     *
     * @param contestId 比赛 ID
     * @param detailVO  比赛详情
     */
    private void fillProblemStatus(Long contestId, ContestDetailVO detailVO) {
        Long userId = SecurityUtils.currentUserIdOrNull();
        if (userId == null || detailVO.getProblems() == null || detailVO.getProblems().isEmpty()) {
            return;
        }
        try {
            Result<ContestProblemStatusDTO> result = judgeFeignClient.contestProblemStatus(contestId, userId);
            ContestProblemStatusDTO data = result == null ? null : result.getData();
            if (data == null) {
                return;
            }
            Set<Long> solved = data.getSolvedIds() == null ? Set.of() : new HashSet<>(data.getSolvedIds());
            Set<Long> attempted = data.getAttemptedIds() == null ? Set.of() : new HashSet<>(data.getAttemptedIds());
            detailVO.getProblems().forEach(problem -> {
                if (solved.contains(problem.getProblemId())) {
                    problem.setStatus(PROBLEM_STATUS_AC);
                } else if (attempted.contains(problem.getProblemId())) {
                    problem.setStatus(PROBLEM_STATUS_ATTEMPTED);
                }
            });
        } catch (Exception e) {
            // 状态标记属于增强信息，远程失败时保持「未提交」展示，不影响比赛详情主流程
            log.warn("回填比赛题目做题状态失败 contestId={}", contestId, e);
        }
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
        // 私有赛没有密码会导致任何人都无法报名，创建时直接拦下
        Asserts.isTrue(!Integer.valueOf(AUTH_PRIVATE).equals(contest.getAuth())
                || StringUtils.hasText(contest.getPassword()), "私有赛必须设置比赛密码");
        contest.setPassword(encodePassword(contest.getPassword()));
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
        if (Integer.valueOf(AUTH_PRIVATE).equals(contest.getAuth())) {
            // 密码留空表示沿用原密码（详情接口不回显明文密码）
            if (!StringUtils.hasText(contest.getPassword())) {
                contest.setPassword(exist.getPassword());
            } else {
                contest.setPassword(encodePassword(contest.getPassword()));
            }
            Asserts.isTrue(StringUtils.hasText(contest.getPassword()), "私有赛必须设置比赛密码");
        } else {
            // 改回公开赛时清空密码
            contest.setPassword("");
        }
        // 乐观锁：携带当前版本，并发修改时更新失败由 MyBatis-Plus 乐观锁插件兜底
        contest.setVersion(exist.getVersion());
        contestMapper.updateById(contest);
        evictDetailCache(id);
        // 时间变更实时推送：正在比赛做题页的用户无需刷新即可同步倒计时（重读保证以落库结果为准）
        Contest latest = contestMapper.selectById(id);
        if (latest != null) {
            wsPushService.publish(toTimeVO(latest));
        }
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

    @Override
    public ContestTimeVO timeSnapshot(Long contestId) {
        Contest contest = contestMapper.selectById(contestId);
        return contest == null ? null : toTimeVO(contest);
    }

    /**
     * 构建比赛时间快照（epoch 毫秒，供前端倒计时与时钟校正）
     */
    private ContestTimeVO toTimeVO(Contest contest) {
        ContestTimeVO vo = new ContestTimeVO();
        vo.setContestId(contest.getId());
        vo.setStatus(contest.getStatus());
        vo.setStartTimeMillis(toEpochMilli(contest.getStartTime()));
        vo.setEndTimeMillis(toEpochMilli(contest.getEndTime()));
        vo.setSealTimeMillis(toEpochMilli(contest.getSealTime()));
        vo.setServerTimeMillis(System.currentTimeMillis());
        return vo;
    }

    /**
     * LocalDateTime → epoch 毫秒（按服务器时区换算，避免跨服务序列化差异）
     */
    private Long toEpochMilli(java.time.LocalDateTime time) {
        return time == null ? null : time.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 失效比赛详情缓存
     * <p>
     * 在事务内调用时延迟到提交后失效：提交前删除会让并发读回源旧数据并重新写回缓存，导致缓存长期不更新。
     * 无事务上下文（如定时任务、消息消费）时立即删除。
     *
     * @param contestId 比赛 ID
     */
    public void evictDetailCache(Long contestId) {
        String cacheKey = CONTEST_INFO_CACHE + contestId;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    redisTemplate.delete(cacheKey);
                }
            });
        } else {
            redisTemplate.delete(cacheKey);
        }
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
        // 密码不下发，仅回显是否已设置
        BeanUtils.copyProperties(contest, vo, "password");
        vo.setHasPassword(StringUtils.hasText(contest.getPassword()));
        return vo;
    }

    private ContestProblemVO toProblemVO(ContestProblem contestProblem) {
        ContestProblemVO vo = new ContestProblemVO();
        BeanUtils.copyProperties(contestProblem, vo);
        return vo;
    }

    /**
     * 比赛密码哈希：空密码保持为空（公开赛）
     *
     * @param password 明文密码
     * @return BCrypt 哈希或空串
     */
    private String encodePassword(String password) {
        return StringUtils.hasText(password) ? PASSWORD_ENCODER.encode(password) : "";
    }
}
