package com.github.wuzhudexiaopacai.oj.problem.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.wuzhudexiaopacai.oj.api.dto.JudgeCaseDTO;
import com.github.wuzhudexiaopacai.oj.api.dto.ProblemBriefDTO;
import com.github.wuzhudexiaopacai.oj.api.dto.ProblemJudgeDTO;
import com.github.wuzhudexiaopacai.oj.common.core.constant.MqConstants;
import com.github.wuzhudexiaopacai.oj.common.core.constant.RedisKeys;
import com.github.wuzhudexiaopacai.oj.common.core.exception.Asserts;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import com.github.wuzhudexiaopacai.oj.problem.config.ProblemCacheProperties;
import com.github.wuzhudexiaopacai.oj.problem.dto.LanguageTemplateDTO;
import com.github.wuzhudexiaopacai.oj.problem.dto.ProblemImportRow;
import com.github.wuzhudexiaopacai.oj.problem.dto.ProblemPageQueryDTO;
import com.github.wuzhudexiaopacai.oj.problem.dto.ProblemSaveDTO;
import com.github.wuzhudexiaopacai.oj.problem.dto.ProblemSyncMessage;
import com.github.wuzhudexiaopacai.oj.problem.dto.TestCaseSaveDTO;
import com.github.wuzhudexiaopacai.oj.problem.entity.Problem;
import com.github.wuzhudexiaopacai.oj.problem.entity.ProblemLanguage;
import com.github.wuzhudexiaopacai.oj.problem.entity.ProblemTag;
import com.github.wuzhudexiaopacai.oj.problem.entity.Tag;
import com.github.wuzhudexiaopacai.oj.problem.entity.TestCase;
import com.github.wuzhudexiaopacai.oj.problem.es.ProblemDocument;
import com.github.wuzhudexiaopacai.oj.problem.es.ProblemEsRepository;
import com.github.wuzhudexiaopacai.oj.problem.mapper.ProblemLanguageMapper;
import com.github.wuzhudexiaopacai.oj.problem.mapper.ProblemMapper;
import com.github.wuzhudexiaopacai.oj.problem.mapper.ProblemTagMapper;
import com.github.wuzhudexiaopacai.oj.problem.mapper.TagMapper;
import com.github.wuzhudexiaopacai.oj.problem.mapper.TestCaseMapper;
import com.github.wuzhudexiaopacai.oj.problem.service.ProblemService;
import com.github.wuzhudexiaopacai.oj.problem.vo.LanguageTemplateVO;
import com.github.wuzhudexiaopacai.oj.problem.vo.ProblemDetailVO;
import com.github.wuzhudexiaopacai.oj.problem.vo.ProblemImportResultVO;
import com.github.wuzhudexiaopacai.oj.problem.vo.ProblemPageVO;
import com.github.wuzhudexiaopacai.oj.problem.vo.SampleCaseVO;
import com.github.wuzhudexiaopacai.oj.problem.vo.TagVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 题目服务实现
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProblemServiceImpl implements ProblemService {

    /**
     * 空值缓存占位标记（防穿透）
     */
    private static final String NULL_FLAG = "NULL";

    /**
     * 状态：上架
     */
    private static final int STATUS_ONLINE = 1;

    /**
     * 可见性：公开
     */
    private static final int AUTH_PUBLIC = 1;

    /**
     * 详情缓存随机 TTL 上限（秒，防雪崩）
     */
    private static final int TTL_RANDOM_BOUND = 300;

    private final ProblemMapper problemMapper;
    private final ProblemTagMapper problemTagMapper;
    private final TagMapper tagMapper;
    private final TestCaseMapper testCaseMapper;
    private final ProblemLanguageMapper problemLanguageMapper;
    private final ProblemEsRepository problemEsRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;
    private final RocketMQTemplate rocketMQTemplate;
    private final ProblemCacheProperties cacheProperties;

    @Override
    @SuppressWarnings("unchecked")
    public PageResult<ProblemPageVO> page(ProblemPageQueryDTO query) {
        String cacheKey = RedisKeys.PROBLEM_LIST + DigestUtil.md5Hex(JSONUtil.toJsonStr(query));
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof PageResult<?> pageResult) {
            return (PageResult<ProblemPageVO>) pageResult;
        }

        Page<Problem> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<Problem> wrapper = new LambdaQueryWrapper<Problem>()
                .eq(Problem::getStatus, STATUS_ONLINE)
                .eq(Problem::getAuth, AUTH_PUBLIC)
                .and(StrUtil.isNotBlank(query.getKeyword()), w -> w
                        .like(Problem::getTitle, query.getKeyword())
                        .or()
                        .like(Problem::getProblemKey, query.getKeyword()))
                .eq(query.getDifficulty() != null, Problem::getDifficulty, query.getDifficulty())
                .orderByAsc(Problem::getProblemKey);
        // 标签多选：需同时满足，逐个 exists 子查询 pms_problem_tag
        if (CollUtil.isNotEmpty(query.getTagIds())) {
            for (Long tagId : query.getTagIds()) {
                wrapper.exists("SELECT 1 FROM pms_problem_tag pt WHERE pt.problem_id = pms_problem.id "
                        + "AND pt.tag_id = " + tagId + " AND pt.deleted = 0");
            }
        }
        Page<Problem> result = problemMapper.selectPage(page, wrapper);

        PageResult<ProblemPageVO> voPage = PageResult.of(result, this::toPageVO);
        fillTags(voPage.getRecords());
        redisTemplate.opsForValue().set(cacheKey, voPage, cacheProperties.getListTtlSeconds(), TimeUnit.SECONDS);
        return voPage;
    }

    @Override
    public PageResult<Problem> adminPage(ProblemPageQueryDTO query) {
        Page<Problem> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<Problem> wrapper = new LambdaQueryWrapper<Problem>()
                .and(StrUtil.isNotBlank(query.getKeyword()), w -> w
                        .like(Problem::getTitle, query.getKeyword())
                        .or()
                        .like(Problem::getProblemKey, query.getKeyword()))
                .eq(query.getDifficulty() != null, Problem::getDifficulty, query.getDifficulty())
                .orderByDesc(Problem::getId);
        return PageResult.of(problemMapper.selectPage(page, wrapper));
    }

    @Override
    public ProblemSaveDTO adminDetail(Long id) {
        Problem problem = problemMapper.selectById(id);
        Asserts.notNull(problem, "题目不存在");
        ProblemSaveDTO dto = BeanUtil.copyProperties(problem, ProblemSaveDTO.class);
        // 标签 ID 列表
        dto.setTagIds(problemTagMapper.selectList(new LambdaQueryWrapper<ProblemTag>()
                        .eq(ProblemTag::getProblemId, id))
                .stream().map(ProblemTag::getTagId).collect(Collectors.toList()));
        // 评测用例（按 seq 排序）
        dto.setTestCases(testCaseMapper.selectList(new LambdaQueryWrapper<TestCase>()
                        .eq(TestCase::getProblemId, id).orderByAsc(TestCase::getSeq))
                .stream().map(c -> BeanUtil.copyProperties(c, TestCaseSaveDTO.class))
                .collect(Collectors.toList()));
        // 语言模板
        dto.setLanguageTemplates(problemLanguageMapper.selectList(new LambdaQueryWrapper<ProblemLanguage>()
                        .eq(ProblemLanguage::getProblemId, id))
                .stream().map(l -> BeanUtil.copyProperties(l, LanguageTemplateDTO.class))
                .collect(Collectors.toList()));
        return dto;
    }

    @Override
    public ProblemDetailVO detail(String problemKey) {
        Asserts.notBlank(problemKey, "题号不能为空");
        // 兼容以题目 ID 访问（前端路由 /problem/{id}）：纯数字视为 ID，先换算成展示题号
        if (problemKey.length() <= 18 && problemKey.chars().allMatch(Character::isDigit)) {
            Problem problem = problemMapper.selectById(Long.parseLong(problemKey));
            Asserts.notNull(problem, "题目不存在");
            problemKey = problem.getProblemKey();
        }
        String cacheKey = RedisKeys.PROBLEM_DETAIL + problemKey;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (NULL_FLAG.equals(cached)) {
            Asserts.fail("题目不存在");
        }
        if (cached instanceof ProblemDetailVO detailVO) {
            return detailVO;
        }

        // 防击穿：分布式锁 + 双重检查
        RLock lock = redissonClient.getLock("problem:lock:detail:" + problemKey);
        lock.lock();
        try {
            cached = redisTemplate.opsForValue().get(cacheKey);
            if (NULL_FLAG.equals(cached)) {
                Asserts.fail("题目不存在");
            }
            if (cached instanceof ProblemDetailVO detailVO) {
                return detailVO;
            }
            ProblemDetailVO detailVO = loadDetail(problemKey);
            if (detailVO == null) {
                // 防穿透：空值缓存
                redisTemplate.opsForValue().set(cacheKey, NULL_FLAG,
                        cacheProperties.getNullTtlSeconds(), TimeUnit.SECONDS);
                Asserts.fail("题目不存在");
            }
            // 防雪崩：基础 TTL + 0~300s 随机
            long ttl = cacheProperties.getDetailTtlSeconds() + ThreadLocalRandom.current().nextInt(TTL_RANDOM_BOUND);
            redisTemplate.opsForValue().set(cacheKey, detailVO, ttl, TimeUnit.SECONDS);
            return detailVO;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<ProblemPageVO> search(String keyword) {
        Asserts.notBlank(keyword, "搜索关键字不能为空");
        List<Long> ids;
        try {
            // ES 全文检索（match title + description + problemKey）
            ids = problemEsRepository
                    .findByTitleContainingOrDescriptionContainingOrProblemKeyContaining(keyword, keyword, keyword)
                    .stream()
                    .filter(doc -> Objects.equals(doc.getStatus(), STATUS_ONLINE))
                    .map(ProblemDocument::getId)
                    .toList();
        } catch (Exception e) {
            // ES 不可用降级为 DB LIKE
            log.warn("ES 检索不可用，降级为数据库 LIKE 查询，keyword={}", keyword, e);
            ids = null;
        }
        List<Problem> problems;
        if (ids == null) {
            problems = problemMapper.selectList(new LambdaQueryWrapper<Problem>()
                    .eq(Problem::getStatus, STATUS_ONLINE)
                    .eq(Problem::getAuth, AUTH_PUBLIC)
                    .and(w -> w.like(Problem::getTitle, keyword).or().like(Problem::getProblemKey, keyword))
                    .last("LIMIT 50"));
        } else {
            if (ids.isEmpty()) {
                return Collections.emptyList();
            }
            problems = problemMapper.selectList(new LambdaQueryWrapper<Problem>()
                    .in(Problem::getId, ids)
                    .eq(Problem::getStatus, STATUS_ONLINE)
                    .eq(Problem::getAuth, AUTH_PUBLIC));
        }
        List<ProblemPageVO> voList = problems.stream().map(this::toPageVO).collect(Collectors.toList());
        fillTags(voList);
        return voList;
    }

    @Override
    public ProblemJudgeDTO judgeInfo(Long problemId) {
        Problem problem = problemMapper.selectById(problemId);
        Asserts.notNull(problem, "题目不存在");
        List<TestCase> cases = testCaseMapper.selectList(new LambdaQueryWrapper<TestCase>()
                .eq(TestCase::getProblemId, problemId)
                .orderByAsc(TestCase::getSeq));
        ProblemJudgeDTO dto = new ProblemJudgeDTO();
        dto.setProblemId(problem.getId());
        dto.setProblemKey(problem.getProblemKey());
        dto.setTitle(problem.getTitle());
        dto.setDescription(problem.getDescription());
        dto.setInputDesc(problem.getInputDesc());
        dto.setOutputDesc(problem.getOutputDesc());
        dto.setTimeLimit(problem.getTimeLimit());
        dto.setMemoryLimit(problem.getMemoryLimit());
        dto.setIsSpj(Objects.equals(problem.getIsSpj(), 1));
        dto.setSpjLanguage(problem.getSpjLanguage());
        dto.setSpjCode(problem.getSpjCode());
        dto.setCases(cases.stream().map(c -> {
            JudgeCaseDTO caseDTO = new JudgeCaseDTO();
            caseDTO.setCaseId(c.getId());
            caseDTO.setSeq(c.getSeq());
            // 小数据内联；大数据给 objectKey，由判题服务走 FileFeignClient 拉取
            caseDTO.setInput(c.getInputData());
            caseDTO.setOutput(c.getOutputData());
            caseDTO.setInputFile(c.getInputFile());
            caseDTO.setOutputFile(c.getOutputFile());
            caseDTO.setScore(c.getScore());
            return caseDTO;
        }).collect(Collectors.toList()));
        return dto;
    }

    @Override
    public ProblemBriefDTO brief(Long problemId) {
        Problem problem = problemMapper.selectById(problemId);
        if (problem == null) {
            return null;
        }
        ProblemBriefDTO dto = new ProblemBriefDTO();
        dto.setProblemId(problem.getId());
        dto.setProblemKey(problem.getProblemKey());
        dto.setTitle(problem.getTitle());
        dto.setDifficulty(problem.getDifficulty());
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ProblemSaveDTO saveDTO) {
        Long count = problemMapper.selectCount(new LambdaQueryWrapper<Problem>()
                .eq(Problem::getProblemKey, saveDTO.getProblemKey()));
        Asserts.isTrue(count == 0, "题号已存在");
        Problem problem = new Problem();
        BeanUtil.copyProperties(saveDTO, problem, "id");
        problem.setAcceptCount(0);
        problem.setSubmitCount(0);
        if (problem.getStatus() == null) {
            problem.setStatus(STATUS_ONLINE);
        }
        if (problem.getAuth() == null) {
            problem.setAuth(AUTH_PUBLIC);
        }
        problemMapper.insert(problem);
        saveRelations(problem.getId(), saveDTO);
        afterWrite(problem.getId(), problem.getProblemKey(), ProblemSyncMessage.upsert(problem.getId()));
        return problem.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ProblemSaveDTO saveDTO) {
        Problem exist = problemMapper.selectById(id);
        Asserts.notNull(exist, "题目不存在");
        Long count = problemMapper.selectCount(new LambdaQueryWrapper<Problem>()
                .eq(Problem::getProblemKey, saveDTO.getProblemKey())
                .ne(Problem::getId, id));
        Asserts.isTrue(count == 0, "题号已存在");
        Problem problem = new Problem();
        BeanUtil.copyProperties(saveDTO, problem);
        problem.setId(id);
        problemMapper.updateById(problem);
        saveRelations(id, saveDTO);
        afterWrite(id, problem.getProblemKey(), ProblemSyncMessage.upsert(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Problem exist = problemMapper.selectById(id);
        Asserts.notNull(exist, "题目不存在");
        problemMapper.deleteById(id);
        afterWrite(id, exist.getProblemKey(), ProblemSyncMessage.delete(id));
    }

    @Override
    public void export(HttpServletResponse response) {
        List<Problem> problems = problemMapper.selectList(new LambdaQueryWrapper<Problem>()
                .orderByAsc(Problem::getProblemKey));
        List<ProblemImportRow> rows = problems.stream()
                .map(p -> BeanUtil.copyProperties(p, ProblemImportRow.class))
                .collect(Collectors.toList());
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String fileName = URLEncoder.encode("problems.xlsx", StandardCharsets.UTF_8);
            response.setHeader("Content-Disposition", "attachment;filename*=UTF-8''" + fileName);
            EasyExcel.write(response.getOutputStream(), ProblemImportRow.class).sheet("题目").doWrite(rows);
        } catch (IOException e) {
            log.error("题目导出失败", e);
            Asserts.fail("导出失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProblemImportResultVO importProblems(MultipartFile file) {
        Asserts.notNull(file, "导入文件不能为空");
        List<ProblemImportRow> rows;
        try {
            rows = EasyExcel.read(file.getInputStream()).head(ProblemImportRow.class).sheet().doReadSync()
                    .stream().map(r -> (ProblemImportRow) r).collect(Collectors.toList());
        } catch (IOException e) {
            log.error("题目导入读取失败", e);
            Asserts.fail("文件读取失败");
            return null;
        }
        ProblemImportResultVO result = new ProblemImportResultVO();
        List<String> errors = new ArrayList<>();
        int rowNo = 1;
        for (ProblemImportRow row : rows) {
            rowNo++;
            try {
                validateRow(row);
                Problem exist = problemMapper.selectOne(new LambdaQueryWrapper<Problem>()
                        .eq(Problem::getProblemKey, row.getProblemKey()));
                if (exist == null) {
                    Problem problem = BeanUtil.copyProperties(row, Problem.class);
                    problem.setStatus(STATUS_ONLINE);
                    problem.setAuth(AUTH_PUBLIC);
                    problem.setAcceptCount(0);
                    problem.setSubmitCount(0);
                    problemMapper.insert(problem);
                    afterWrite(problem.getId(), problem.getProblemKey(),
                            ProblemSyncMessage.upsert(problem.getId()));
                } else {
                    Problem update = BeanUtil.copyProperties(row, Problem.class);
                    update.setId(exist.getId());
                    problemMapper.updateById(update);
                    afterWrite(exist.getId(), exist.getProblemKey(), ProblemSyncMessage.upsert(exist.getId()));
                }
                result.setSuccessCount(result.getSuccessCount() + 1);
            } catch (Exception e) {
                result.setFailCount(result.getFailCount() + 1);
                errors.add("第" + rowNo + "行: " + e.getMessage());
            }
        }
        result.setErrors(errors);
        return result;
    }

    /**
     * 导入行校验
     */
    private void validateRow(ProblemImportRow row) {
        Asserts.notBlank(row.getProblemKey(), "题号不能为空");
        Asserts.notBlank(row.getTitle(), "标题不能为空");
        Asserts.notBlank(row.getDescription(), "描述不能为空");
        Asserts.notNull(row.getDifficulty(), "难度不能为空");
        Asserts.isTrue(row.getDifficulty() >= 0 && row.getDifficulty() <= 2, "难度取值须为 0~2（0简单 1中等 2困难）");
        if (row.getTimeLimit() == null) {
            row.setTimeLimit(1000);
        }
        if (row.getMemoryLimit() == null) {
            row.setMemoryLimit(256);
        }
    }

    /**
     * 写操作后处理：发 MQ 同步 ES + 主动删除详情缓存
     */
    private void afterWrite(Long problemId, String problemKey, ProblemSyncMessage message) {
        rocketMQTemplate.syncSend(MqConstants.PROBLEM_SYNC_TOPIC, message);
        redisTemplate.delete(RedisKeys.PROBLEM_DETAIL + problemKey);
    }

    /**
     * 保存关联数据（标签/用例/语言模板，整体替换）
     */
    private void saveRelations(Long problemId, ProblemSaveDTO saveDTO) {
        problemTagMapper.delete(new LambdaQueryWrapper<ProblemTag>().eq(ProblemTag::getProblemId, problemId));
        if (CollUtil.isNotEmpty(saveDTO.getTagIds())) {
            for (Long tagId : saveDTO.getTagIds()) {
                ProblemTag pt = new ProblemTag();
                pt.setProblemId(problemId);
                pt.setTagId(tagId);
                problemTagMapper.insert(pt);
            }
        }
        if (saveDTO.getTestCases() != null) {
            testCaseMapper.delete(new LambdaQueryWrapper<TestCase>().eq(TestCase::getProblemId, problemId));
            for (TestCaseSaveDTO caseDTO : saveDTO.getTestCases()) {
                TestCase testCase = BeanUtil.copyProperties(caseDTO, TestCase.class, "id");
                testCase.setId(null);
                testCase.setProblemId(problemId);
                testCaseMapper.insert(testCase);
            }
        }
        if (saveDTO.getLanguageTemplates() != null) {
            problemLanguageMapper.delete(new LambdaQueryWrapper<ProblemLanguage>()
                    .eq(ProblemLanguage::getProblemId, problemId));
            saveDTO.getLanguageTemplates().forEach(t -> {
                ProblemLanguage language = BeanUtil.copyProperties(t, ProblemLanguage.class);
                language.setProblemId(problemId);
                problemLanguageMapper.insert(language);
            });
        }
    }

    /**
     * 加载题目详情（含样例与语言模板）
     */
    private ProblemDetailVO loadDetail(String problemKey) {
        Problem problem = problemMapper.selectOne(new LambdaQueryWrapper<Problem>()
                .eq(Problem::getProblemKey, problemKey));
        if (problem == null) {
            return null;
        }
        ProblemDetailVO vo = BeanUtil.copyProperties(problem, ProblemDetailVO.class);
        vo.setTags(listTagsByProblemIds(List.of(problem.getId()))
                .getOrDefault(problem.getId(), Collections.emptyList()));

        // 样例：seq 最小的 2 条；大数据文件用例只返回标志不返回内容
        List<TestCase> samples = testCaseMapper.selectList(new LambdaQueryWrapper<TestCase>()
                .eq(TestCase::getProblemId, problem.getId())
                .orderByAsc(TestCase::getSeq)
                .last("LIMIT 2"));
        vo.setSamples(samples.stream().map(c -> {
            SampleCaseVO sample = new SampleCaseVO();
            sample.setSeq(c.getSeq());
            boolean inputBig = StrUtil.isNotBlank(c.getInputFile());
            boolean outputBig = StrUtil.isNotBlank(c.getOutputFile());
            sample.setInputBig(inputBig);
            sample.setOutputBig(outputBig);
            sample.setInput(inputBig ? null : c.getInputData());
            sample.setOutput(outputBig ? null : c.getOutputData());
            return sample;
        }).collect(Collectors.toList()));

        List<ProblemLanguage> languages = problemLanguageMapper.selectList(
                new LambdaQueryWrapper<ProblemLanguage>().eq(ProblemLanguage::getProblemId, problem.getId()));
        vo.setTemplates(languages.stream().map(l -> {
            LanguageTemplateVO templateVO = new LanguageTemplateVO();
            templateVO.setLanguage(l.getLanguage());
            templateVO.setCodeTemplate(l.getCodeTemplate());
            return templateVO;
        }).collect(Collectors.toList()));
        return vo;
    }

    private ProblemPageVO toPageVO(Problem problem) {
        ProblemPageVO vo = new ProblemPageVO();
        vo.setId(problem.getId());
        vo.setProblemKey(problem.getProblemKey());
        vo.setTitle(problem.getTitle());
        vo.setDifficulty(problem.getDifficulty());
        vo.setAcceptCount(problem.getAcceptCount());
        vo.setSubmitCount(problem.getSubmitCount());
        int submit = problem.getSubmitCount() == null ? 0 : problem.getSubmitCount();
        int accept = problem.getAcceptCount() == null ? 0 : problem.getAcceptCount();
        vo.setPassRate(submit == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(accept * 100.0 / submit).setScale(2, RoundingMode.HALF_UP));
        // 用户完成状态由调用方走 submission 服务接口获取后自行合并，本接口保持无状态
        vo.setUserStatus(null);
        return vo;
    }

    private void fillTags(List<ProblemPageVO> records) {
        if (CollUtil.isEmpty(records)) {
            return;
        }
        Map<Long, List<TagVO>> tagMap = listTagsByProblemIds(
                records.stream().map(ProblemPageVO::getId).toList());
        records.forEach(r -> r.setTags(tagMap.getOrDefault(r.getId(), Collections.emptyList())));
    }

    /**
     * 批量查询题目标签（problemId → 标签列表）
     */
    private Map<Long, List<TagVO>> listTagsByProblemIds(List<Long> problemIds) {
        if (CollUtil.isEmpty(problemIds)) {
            return Collections.emptyMap();
        }
        List<ProblemTag> relations = problemTagMapper.selectList(
                new LambdaQueryWrapper<ProblemTag>().in(ProblemTag::getProblemId, problemIds));
        if (relations.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Tag> tagMap = tagMapper.selectBatchIds(
                        relations.stream().map(ProblemTag::getTagId).distinct().toList())
                .stream().collect(Collectors.toMap(Tag::getId, t -> t));
        return relations.stream().collect(Collectors.groupingBy(ProblemTag::getProblemId,
                Collectors.mapping(r -> {
                    Tag tag = tagMap.get(r.getTagId());
                    if (tag == null) {
                        return null;
                    }
                    TagVO tagVO = new TagVO();
                    tagVO.setId(tag.getId());
                    tagVO.setName(tag.getName());
                    tagVO.setColor(tag.getColor());
                    return tagVO;
                }, Collectors.collectingAndThen(Collectors.toList(),
                        list -> list.stream().filter(Objects::nonNull).collect(Collectors.toList())))));
    }
}
