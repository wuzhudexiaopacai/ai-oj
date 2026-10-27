package com.github.wuzhudexiaopacai.oj.judge.controller;

import com.github.wuzhudexiaopacai.oj.common.core.result.Result;
import com.github.wuzhudexiaopacai.oj.judge.dto.JudgeRunRequest;
import com.github.wuzhudexiaopacai.oj.judge.dto.JudgeSubmitRequest;
import com.github.wuzhudexiaopacai.oj.judge.service.JudgeService;
import com.github.wuzhudexiaopacai.oj.judge.service.SubmissionService;
import com.github.wuzhudexiaopacai.oj.judge.vo.JudgeProgressVO;
import com.github.wuzhudexiaopacai.oj.judge.vo.JudgeRunVO;
import com.github.wuzhudexiaopacai.oj.judge.vo.SubmitResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 判题接口
 *
 * @author wuzhudexiaopacai
 * @since 1.0
 */
@Tag(name = "判题接口", description = "判题提交、自测运行与判题进度查询；网关白名单不含 /judge/**，所有接口均需登录（携带 Token），并以当前登录用户身份执行")
@RestController
@RequestMapping("/judge")
@RequiredArgsConstructor
public class JudgeController {

    private final JudgeService judgeService;

    private final SubmissionService submissionService;

    /**
     * 提交判题
     */
    @Operation(summary = "提交判题", description = "需登录，按当前用户落库判题。submitId 由客户端生成，用于三层幂等（注解幂等 + 用户级频率锁 + 数据库唯一键），同一 submitId 重复提交直接返回已有记录；同一用户对同一题目在默认 5 秒内再次提交会被拒绝。contestId 非 0 时校验已报名、比赛进行中且题目属于该比赛；落库初始状态为 0 排队中，同步投递 RocketMQ 判题消息后立即返回提交记录 ID，判题结果异步经 MQ/WebSocket 广播（0 排队中 → 1 编译中 → 2 评测中 → 3~11 终态）。")
    @PostMapping("/submit")
    public Result<SubmitResultVO> submit(@Valid @RequestBody JudgeSubmitRequest req) {
        return Result.ok(judgeService.submit(req));
    }

    /**
     * 自测运行（不入库）
     */
    @Operation(summary = "自测运行", description = "需登录；不入库、不产生提交记录，直接占用一台在线沙箱调用 /run（固定 10 秒超时），每用户每分钟限 20 次。沙箱调用失败会自动拉黑该沙箱并返回“沙箱不可用”；成功时返回 JudgeStatusEnum 状态码与状态描述、程序输出、错误信息、耗时（毫秒）、内存（KB）。")
    @PostMapping("/run")
    public Result<JudgeRunVO> run(@Valid @RequestBody JudgeRunRequest req) {
        return Result.ok(judgeService.run(req));
    }

    /**
     * 判题进度（WebSocket 断线轮询兜底）
     */
    @Operation(summary = "判题进度", description = "需登录，作为 WebSocket 断线后的轮询兜底。先读取 Redis 判题进度缓存（默认 TTL 3600 秒），未命中时按 submitId 查数据库并组装；finished=true 表示已进入终态（状态码 3~11），false 表示排队/编译/评测中仍需继续轮询。提交不存在时返回业务异常“提交记录不存在”。")
    @GetMapping("/progress")
    public Result<JudgeProgressVO> progress(
            @Parameter(description = "幂等提交标识，客户端生成的 UUID", required = true,
                    example = "3f2b8c1e-9a4d-4e6b-8c2f-1d2e3a4b5c6d")
            @RequestParam("submitId") String submitId) {
        return Result.ok(submissionService.progress(submitId));
    }
}
