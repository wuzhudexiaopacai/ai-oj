package com.github.wuzhudexiaopacai.oj.ai.service.impl;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.wuzhudexiaopacai.oj.ai.config.AiGenerateProperties;
import com.github.wuzhudexiaopacai.oj.ai.dto.ProblemGenerateDTO;
import com.github.wuzhudexiaopacai.oj.ai.service.AiGenerateService;
import com.github.wuzhudexiaopacai.oj.ai.vo.GeneratedCaseVO;
import com.github.wuzhudexiaopacai.oj.ai.vo.ProblemDraftVO;
import com.github.wuzhudexiaopacai.oj.common.core.exception.Asserts;
import com.github.wuzhudexiaopacai.oj.common.web.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 出题服务实现（同步调用大模型，输出严格 JSON 后解析为结构化题目草稿）
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiGenerateServiceImpl implements AiGenerateService {

    /**
     * 难度码到中文标签映射（下标即难度码 0/1/2）
     */
    private static final String[] DIFFICULTY_LABELS = {"简单", "中等", "困难"};

    private static final int DEFAULT_TIME_LIMIT = 1000;

    private static final int DEFAULT_MEMORY_LIMIT = 256;

    private static final int DEFAULT_CASE_COUNT = 10;

    private static final int MAX_CASE_COUNT = 20;

    private final ChatClient chatClient;

    private final ObjectMapper objectMapper;

    private final AiGenerateProperties generateProperties;

    @Override
    public ProblemDraftVO generate(ProblemGenerateDTO req) {
        SecurityUtils.requireLogin();
        int caseCount = normalizeCaseCount(req.getCaseCount());
        String prompt = buildPrompt(req, caseCount);

        long startAt = System.currentTimeMillis();
        String raw;
        try {
            // 出题输出量大（题面+用例+参考代码+题解），用 per-request options 覆盖全局 max-tokens，
            // 避免沿用对话场景的 2048 导致长 JSON 被截断
            DashScopeChatOptions options = DashScopeChatOptions.builder()
                    .withModel(generateProperties.getModel())
                    .withMaxToken(generateProperties.getMaxTokens())
                    .withTemperature(generateProperties.getTemperature())
                    .build();
            raw = chatClient.prompt().options(options).messages(new UserMessage(prompt)).call().content();
        } catch (Exception e) {
            log.error("AI 出题模型调用失败: topic={}, 耗时={}ms", req.getTopic(), System.currentTimeMillis() - startAt, e);
            Asserts.fail("AI 出题失败，请稍后重试：" + e.getMessage());
            return null;
        }
        // 出题含参考代码+题解，输出量大、耗时长，记录耗时便于定位超时问题
        log.info("AI 出题模型调用完成: topic={}, 耗时={}ms", req.getTopic(), System.currentTimeMillis() - startAt);
        return parse(raw, req, caseCount);
    }

    /**
     * 构建自包含出题提示词（不依赖 DB 模板，保证字段与用例数量约束稳定）
     */
    private String buildPrompt(ProblemGenerateDTO req, int caseCount) {
        String difficultyLabel = difficultyLabel(req.getDifficulty());
        StringBuilder sb = new StringBuilder();
        sb.append("你是一位资深算法竞赛出题人。请围绕知识点「").append(req.getTopic())
                .append("」出一道难度为「").append(difficultyLabel).append("」的编程题目。\n");
        if (StringUtils.hasText(req.getRequirement())) {
            sb.append("额外要求：").append(req.getRequirement().trim()).append("\n");
        }
        sb.append("\n【输出格式硬性要求】\n")
                .append("1. 只输出一个 JSON 对象，禁止任何解释性文字，禁止使用 ```json 代码块围栏。\n")
                .append("2. JSON 字段严格如下：\n")
                .append("{\"title\":\"题目标题\",\"description\":\"题目描述(Markdown)\",")
                .append("\"inputDesc\":\"输入格式说明(Markdown)\",\"outputDesc\":\"输出格式说明(Markdown)\",")
                .append("\"hint\":\"数据范围与提示(Markdown,可空)\",\"timeLimit\":1000,\"memoryLimit\":256,")
                .append("\"testCases\":[{\"inputData\":\"标准输入\",\"outputData\":\"期望输出\"}],")
                .append("\"referenceCode\":\"C++完整标准答案代码\",\"solution\":\"题解(Markdown)\"}\n")
                .append("3. testCases 必须恰好 ").append(caseCount)
                .append(" 组，覆盖样例、边界与典型情况；inputData/outputData 为纯文本，多行用真实换行。\n")
                .append("4. description 需自洽可解，inputDesc/outputDesc 与 testCases 的数据格式必须一致，")
                .append("且每例输出必须是按题面正确计算的结果。\n")
                .append("5. referenceCode 必须是完整、自包含、可直接编译运行的 C++17 程序（含 #include 与 main，")
                .append("从标准输入读入、向标准输出写出，无多余调试输出）；每组 testCases 的 outputData 必须等于")
                .append(" referenceCode 在对应 inputData 下的真实运行输出，二者以参考代码为准。\n")
                .append("6. solution 为中文题解，含解题思路、关键步骤与复杂度分析，可引用 referenceCode 作为参考实现。\n")
                .append("7. timeLimit 单位毫秒（建议 1000），memoryLimit 单位 MB（建议 256）。\n")
                .append("8. 全部文本使用中文；referenceCode 内的代码作为 JSON 字符串值，换行用 \\n 转义，禁止未转义裸换行。")
                .append("9. 【严格控制篇幅】为避免输出过长、生成过慢：description 控制在 300 字内，hint 150 字内，")
                .append("solution 300 字内（只讲思路、关键步骤与复杂度，不得整段复制 referenceCode 代码）；")
                .append("referenceCode 只保留核心实现，去掉冗余注释与无关样板，尽量控制在 60 行内；")
                .append("inputDesc/outputDesc 简明扼要。在满足题目自洽可解的前提下，能短则短。");
        return sb.toString();
    }

    /**
     * 解析模型输出为结构化草稿：剥离代码围栏、截取首个 JSON 对象，字段缺失回退默认值
     */
    private ProblemDraftVO parse(String raw, ProblemGenerateDTO req, int caseCount) {
        Asserts.isTrue(StringUtils.hasText(raw), "AI 未返回内容，请重试");
        String json = extractJson(raw);
        Asserts.notNull(json, "AI 返回内容无法解析为 JSON，请调整要求后重试");

        ProblemDraftVO vo = new ProblemDraftVO();
        vo.setDifficulty(req.getDifficulty() == null ? 1 : req.getDifficulty());
        vo.setTimeLimit(DEFAULT_TIME_LIMIT);
        vo.setMemoryLimit(DEFAULT_MEMORY_LIMIT);
        vo.setTestCases(new ArrayList<>());
        try {
            JsonNode root = objectMapper.readTree(json);
            vo.setTitle(text(root, "title"));
            vo.setDescription(text(root, "description"));
            vo.setInputDesc(text(root, "inputDesc"));
            vo.setOutputDesc(text(root, "outputDesc"));
            vo.setHint(text(root, "hint"));
            vo.setTimeLimit(intOr(root, "timeLimit", DEFAULT_TIME_LIMIT));
            vo.setMemoryLimit(intOr(root, "memoryLimit", DEFAULT_MEMORY_LIMIT));
            vo.setTestCases(parseCases(root.path("testCases"), caseCount));
            vo.setReferenceCode(text(root, "referenceCode"));
            vo.setSolution(text(root, "solution"));
        } catch (Exception e) {
            log.error("AI 出题结果解析失败: raw={}", raw, e);
            if (isTruncated(e)) {
                Asserts.fail("AI 输出内容过长被截断，请减少测试用例数量或降低题目难度后重试");
            }
            Asserts.fail("AI 返回结果解析失败，请重试");
        }
        Asserts.isTrue(StringUtils.hasText(vo.getTitle()) || StringUtils.hasText(vo.getDescription()),
                "AI 未生成有效题目内容，请调整要求后重试");
        return vo;
    }

    /**
     * 解析用例数组，最多保留 caseCount 组
     */
    private List<GeneratedCaseVO> parseCases(JsonNode arr, int caseCount) {
        List<GeneratedCaseVO> cases = new ArrayList<>();
        if (arr == null || !arr.isArray()) {
            return cases;
        }
        for (JsonNode node : arr) {
            if (cases.size() >= caseCount) {
                break;
            }
            GeneratedCaseVO c = new GeneratedCaseVO();
            c.setInputData(text(node, "inputData"));
            c.setOutputData(text(node, "outputData"));
            cases.add(c);
        }
        return cases;
    }

    /**
     * 从模型自由文本中抽取 JSON 对象：去除 ``` 围栏后截取首个 '{' 到末个 '}'
     */
    private String extractJson(String raw) {
        String s = raw.trim();
        int fenceStart = s.indexOf("```");
        if (fenceStart >= 0) {
            int contentStart = s.indexOf('\n', fenceStart);
            int fenceEnd = s.lastIndexOf("```");
            if (contentStart > 0 && fenceEnd > contentStart) {
                s = s.substring(contentStart + 1, fenceEnd).trim();
            }
        }
        int first = s.indexOf('{');
        int last = s.lastIndexOf('}');
        if (first >= 0 && last > first) {
            return s.substring(first, last + 1);
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? "" : v.asText("");
    }

    private int intOr(JsonNode node, String field, int def) {
        JsonNode v = node.path(field);
        return v.isNumber() ? v.asInt(def) : def;
    }

    private int normalizeCaseCount(Integer caseCount) {
        if (caseCount == null || caseCount < 1) {
            return DEFAULT_CASE_COUNT;
        }
        return Math.min(caseCount, MAX_CASE_COUNT);
    }

    /**
     * 判断解析异常是否由「模型输出被 max-tokens 截断」引起（JSON 提前结束 / 字符串未闭合）
     */
    private boolean isTruncated(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof com.fasterxml.jackson.core.io.JsonEOFException) {
                return true;
            }
            String msg = t.getMessage();
            if (msg != null && (msg.contains("end-of-input") || msg.contains("closing quote"))) {
                return true;
            }
        }
        return false;
    }

    private String difficultyLabel(Integer difficulty) {
        if (difficulty == null || difficulty < 0 || difficulty >= DIFFICULTY_LABELS.length) {
            return DIFFICULTY_LABELS[1];
        }
        return DIFFICULTY_LABELS[difficulty];
    }
}
