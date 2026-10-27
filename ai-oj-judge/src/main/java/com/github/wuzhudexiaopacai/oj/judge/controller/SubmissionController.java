package com.github.wuzhudexiaopacai.oj.judge.controller;

import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.common.web.result.PageResult;
import com.github.wuzhudexiaopacai.oj.judge.service.SubmissionService;
import com.github.wuzhudexiaopacai.oj.judge.vo.SubmissionDetailVO;
import com.github.wuzhudexiaopacai.oj.judge.vo.SubmissionPageVO;
import com.github.wuzhudexiaopacai.oj.judge.vo.UserProblemStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 提交记录接口
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Tag(name = "提交记录接口", description = "当前用户提交记录分页、做题状态与详情查询；网关白名单不含 /submission/**，所有接口均需登录（携带 Token），并强制按当前登录用户隔离数据（详情仅提交本人可见）")
@RestController
@RequestMapping("/submission")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    /**
     * 当前用户提交分页（支持题目、状态、语言、时间范围、关键字筛选）
     */
    @Operation(summary = "当前用户提交分页", description = "需登录，强制按当前登录用户过滤，并按提交记录 ID 倒序返回。pageNum 从 1 开始、pageSize 默认 10 且服务端最大 500；支持题目、状态、语言、创建时间范围与关键字筛选（关键字匹配 submitId 或题号/标题，题目检索失败时仅按 submitId 匹配）。题号与标题由题目服务回填，回填失败时 problemKey/problemTitle 为 null，不影响列表主体。")
    @GetMapping("/page")
    public Result<PageResult<SubmissionPageVO>> page(
            @Parameter(description = "页码，从 1 开始", required = false, example = "1")
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @Parameter(description = "每页条数，默认 10，服务端最大 500", required = false, example = "10")
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize,
            @Parameter(description = "题目 ID，精确筛选", required = false, example = "1001")
            @RequestParam(value = "problemId", required = false) Long problemId,
            @Parameter(description = "判题状态精确筛选：0 排队中、1 编译中、2 评测中、3 答案正确、4 答案错误、5 运行超时、6 内存超限、7 运行错误、8 编译错误、9 格式错误、10 输出超限、11 系统错误",
                    required = false, example = "3")
            @RequestParam(value = "status", required = false) Integer status,
            @Parameter(description = "编程语言精确筛选：cpp/C++17、c/C11、java/Java17、python3/Python3、go、javascript",
                    required = false, example = "cpp")
            @RequestParam(value = "language", required = false) String language,
            @Parameter(description = "关键字，匹配提交标识 submitId 或题号/标题", required = false,
                    example = "A1001")
            @RequestParam(value = "keyword", required = false) String keyword,
            @Parameter(description = "创建时间下界，格式 yyyy-MM-dd HH:mm:ss", required = false,
                    example = "2026-01-01 00:00:00")
            @RequestParam(value = "startTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Parameter(description = "创建时间上界，格式 yyyy-MM-dd HH:mm:ss", required = false,
                    example = "2026-12-31 23:59:59")
            @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        return Result.ok(submissionService.page(pageNum, pageSize, problemId, status,
                language, keyword, startTime, endTime));
    }

    /**
     * 当前用户的题目做题状态（题库列表标记「已通过 / 尝试过」）
     */
    @Operation(summary = "当前用户题目做题状态", description = "需登录；返回当前用户已通过（状态 3 答案正确）与提交过（含未通过）的题目 ID 列表，供题库列表标记「已通过 / 尝试过」。数据来自判题库聚合查询，无记录时返回空列表。")
    @GetMapping("/user-problem-status")
    public Result<UserProblemStatusVO> userProblemStatus() {
        return Result.ok(submissionService.userProblemStatus());
    }

    /**
     * 按 submitId 查询提交详情（前端提交后轮询使用）
     */
    @Operation(summary = "按 submitId 查询提交详情", description = "需登录；按客户端生成的 submitId 查询提交详情（前端提交后轮询使用），仅提交本人可见，否则返回“无权查看该提交”。返回包含用例明细、最大耗时/内存与编译错误信息；普通题目且非 AC 时回显失败用例的输入与期望输出（超 4096 字符截断），比赛提交一律不回显。")
    @GetMapping("/submit/{submitId}")
    public Result<SubmissionDetailVO> detailBySubmitId(
            @Parameter(description = "幂等提交标识，客户端生成的 UUID", required = true,
                    example = "3f2b8c1e-9a4d-4e6b-8c2f-1d2e3a4b5c6d")
            @PathVariable("submitId") String submitId) {
        return Result.ok(submissionService.detailBySubmitId(submitId));
    }

    /**
     * 提交详情（含用例明细）
     */
    @Operation(summary = "提交详情", description = "需登录；按提交记录主键 ID 查询详情并校验归属，仅提交本人可见。用例明细按 seq 升序返回；失败用例输入/期望输出的回显规则与按 submitId 查询一致（仅普通题目、非 AC 且内容超 4096 字符时截断，比赛提交不回显）。")
    @GetMapping("/{id}")
    public Result<SubmissionDetailVO> detail(
            @Parameter(description = "提交记录主键 ID（雪花 ID）", required = true,
                    example = "1950000000000000001")
            @PathVariable("id") Long id) {
        return Result.ok(submissionService.detail(id));
    }
}
