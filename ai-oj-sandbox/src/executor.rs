//! 核心执行器：编译、单 case 运行、答案比对、SPJ

use std::sync::Arc;
use std::time::{Duration, Instant};

use bollard::models::HostConfig;
use tokio::fs;
use tokio::io::AsyncReadExt;
use uuid::Uuid;

use crate::config::AppConfig;
use crate::docker::DockerManager;
use crate::error::SandboxError;
use crate::language::{LanguageConfig, LanguageRegistry};
use crate::model::*;

#[derive(Clone)]
pub struct Executor {
    docker: DockerManager,
    config: Arc<AppConfig>,
}

/// 单个 case 的运行结果（不含比对结论）
struct CaseRunOutcome {
    status: Status,
    time_ms: u64,
    memory_kb: u64,
    output: String,
    stderr: String,
}

impl Executor {
    pub fn new(docker: DockerManager, config: AppConfig) -> Self {
        Self {
            docker,
            config: Arc::new(config),
        }
    }

    /// 编译容器安全限制：断网、drop 所有 capabilities、限制进程数与内存
    /// （不能开只读根文件系统，编译产物要写入 /work）
    fn compile_host_config(&self) -> HostConfig {
        HostConfig {
            network_mode: Some("none".to_string()),
            memory: Some((self.config.compile_memory_mb * 1024 * 1024) as i64),
            pids_limit: Some(128),
            cap_drop: Some(vec!["ALL".to_string()]),
            auto_remove: Some(false),
            ..Default::default()
        }
    }

    /// 运行容器安全限制：只读根文件系统（唯一可写为 /work 挂载点）、
    /// 断网、1 CPU、drop 所有 capabilities、限制进程数
    fn run_host_config(&self, memory_bytes: i64) -> HostConfig {
        HostConfig {
            network_mode: Some("none".to_string()),
            memory: Some(memory_bytes),
            nano_cpus: Some(1_000_000_000),
            pids_limit: Some(64),
            cap_drop: Some(vec!["ALL".to_string()]),
            readonly_rootfs: Some(true),
            auto_remove: Some(false),
            ..Default::default()
        }
    }

    fn task_dir(&self, task_id: &str) -> String {
        format!("{}/{}", self.config.work_dir, task_id)
    }

    /// 编译阶段：返回编译结果；解释型语言（无 compile_cmd）直接成功
    async fn compile(
        &self,
        task_id: &str,
        lang: &LanguageConfig,
        container_name: &str,
    ) -> Result<CompileInfo, SandboxError> {
        let Some(cmd) = &lang.compile_cmd else {
            return Ok(CompileInfo {
                success: true,
                log: String::new(),
            });
        };
        let working_dir = self.task_dir(task_id);
        let result = self
            .docker
            .run_container(
                container_name,
                &lang.image,
                cmd.clone(),
                &working_dir,
                self.compile_host_config(),
                Duration::from_secs(self.config.compile_timeout_secs),
            )
            .await?;
        // 退出码非 0 或兜底超时都视为编译失败
        let success = !result.timed_out && result.exit_code == 0;
        Ok(CompileInfo {
            success,
            log: result.log,
        })
    }

    /// 运行单个 case：输入来自 case_{seq}.in，输出重定向到 case_{seq}.out
    async fn run_case(
        &self,
        task_id: &str,
        seq: u32,
        lang: &LanguageConfig,
        time_limit_ms: u64,
        memory_limit_mb: u64,
        stack_limit_mb: u64,
    ) -> Result<CaseRunOutcome, SandboxError> {
        let task_dir = self.task_dir(task_id);
        // 容器内 timeout 秒数（向上取整，含语言时间补偿）
        let limit_ms = (time_limit_ms as f64 * lang.time_factor) as u64;
        let timeout_secs = ((limit_ms + 999) / 1000).max(1);
        // 栈空间限制（KB）：ulimit 不受支持时忽略，不影响判题
        let stack_kb = stack_limit_mb * 1024;
        // 容器内存上限（含语言补偿），同时作为 -Xmx 等运行参数的模板取值
        let mem_bytes = (memory_limit_mb as f64 * lang.memory_factor * 1024.0 * 1024.0) as i64;
        let limit_kb = (mem_bytes / 1024) as u64;
        let container_mb = (mem_bytes / (1024 * 1024)).max(1) as u64;
        let run_cmd = render_run_cmd(&lang.run_cmd, limit_ms, container_mb);
        // timeout 触发 TLE 时退出码为 124；
        // 程序结束后读取容器自身 cgroup 的内存峰值（memory.peak）写入 .mem，
        // 短时间运行的程序无法被 Docker stats 秒级采样捕获，cgroup 峰值是准确的兜底来源。
        let shell_cmd = format!(
            "ulimit -s {stack_kb} 2>/dev/null; timeout {} {} < case_{seq}.in > case_{seq}.out 2> case_{seq}.err; rc=$?; \
             cat /sys/fs/cgroup/memory.peak > case_{seq}.mem 2>/dev/null; \
             cat /sys/fs/cgroup/memory/memory.max_usage_in_bytes >> case_{seq}.mem 2>/dev/null; \
             exit $rc",
            timeout_secs,
            run_cmd,
            seq = seq
        );
        let name = self.docker.container_name(task_id, &format!("case-{seq}"));

        let start = Instant::now();
        let result = self
            .docker
            .run_container(
                &name,
                &lang.image,
                vec!["sh".into(), "-c".into(), shell_cmd],
                &task_dir,
                self.run_host_config(mem_bytes),
                // 兜底超时：容器内 timeout 之外再 +2s
                Duration::from_millis(limit_ms + 2000),
            )
            .await?;
        let time_ms = start.elapsed().as_millis() as u64;

        // 判题状态流转：兜底超时/退出码 124 -> TLE；137 -> MLE；其他非 0 -> RE；0 -> 视输出/内存再定
        let mut status = if result.timed_out || result.exit_code == 124 {
            Status::TimeLimitExceeded
        } else if result.exit_code == 137 {
            // 137 = 128 + SIGKILL：容器内存超限时内核 OOM Killer 以 SIGKILL 强杀进程，
            // 该场景必须上报 MLE，否则内存限制会被误判成运行错误
            Status::MemoryLimitExceeded
        } else if result.exit_code != 0 {
            Status::RuntimeError
        } else {
            Status::Accepted
        };

        // 无论成败都尽量读回输出（/run 场景需要展示部分输出）；限制读取上限防 OLE 撑爆内存
        let out_path = format!("{task_dir}/case_{seq}.out");
        let err_path = format!("{task_dir}/case_{seq}.err");
        let (output, out_exceeded) =
            read_limited(&out_path, self.config.output_limit_bytes).await?;
        let (stderr, _) = read_limited(&err_path, 64 * 1024).await?;

        // 内存峰值优先取 cgroup 记录（字节），读不到时退回 Docker stats 采样值（KB）
        let mem_path = format!("{task_dir}/case_{seq}.mem");
        let cgroup_kb = match fs::read_to_string(&mem_path).await {
            Ok(text) => text
                .lines()
                .next()
                .and_then(|line| line.trim().parse::<u64>().ok())
                .map(|bytes| bytes / 1024)
                .unwrap_or(0),
            Err(_) => 0,
        };
        let memory_kb = if cgroup_kb > 0 { cgroup_kb } else { result.memory_kb };

        if status == Status::Accepted && out_exceeded {
            status = Status::OutputLimitExceeded;
        }
        // MLE 判定：有采样数据且超限（采样失败时 memory_kb 为 0，不误判）
        if status == Status::Accepted && memory_kb > 0 && memory_kb > limit_kb {
            status = Status::MemoryLimitExceeded;
        }

        Ok(CaseRunOutcome {
            status,
            time_ms,
            memory_kb,
            output,
            stderr,
        })
    }

    /// 编译 SPJ checker 到任务目录 spj/ 子目录
    async fn compile_spj(
        &self,
        registry: &LanguageRegistry,
        task_id: &str,
        spj: &SpjSpec,
        container_name: &str,
    ) -> Result<CompileInfo, SandboxError> {
        let lang = registry.get(&spj.language)?;
        let spj_dir = format!("{}/spj", self.task_dir(task_id));
        fs::create_dir_all(&spj_dir).await?;
        fs::write(format!("{spj_dir}/{}", lang.source_file), &spj.code).await?;
        match &lang.compile_cmd {
            Some(cmd) => {
                let result = self
                    .docker
                    .run_container(
                        container_name,
                        &lang.image,
                        cmd.clone(),
                        &spj_dir,
                        self.compile_host_config(),
                        Duration::from_secs(self.config.compile_timeout_secs),
                    )
                    .await?;
                Ok(CompileInfo {
                    success: !result.timed_out && result.exit_code == 0,
                    log: result.log,
                })
            }
            None => Ok(CompileInfo {
                success: true,
                log: String::new(),
            }),
        }
    }

    /// 运行 checker：工作目录为 spj/，参数为 [输入文件, 期望输出文件, 实际输出文件]
    /// exit 0 -> AC，其余一律 WA
    async fn run_spj(
        &self,
        registry: &LanguageRegistry,
        task_id: &str,
        seq: u32,
        spj: &SpjSpec,
        container_name: &str,
    ) -> Result<Status, SandboxError> {
        let lang = registry.get(&spj.language)?;
        let spj_dir = format!("{}/spj", self.task_dir(task_id));
        // checker 固定 10s / 256MB 容器，内存占位符按容器值渲染
        let run_cmd = render_run_cmd(&lang.run_cmd, 10_000, 256);
        let shell_cmd = format!(
            "timeout 10 {} ../case_{}.in ../case_{}.expected ../case_{}.out",
            run_cmd, seq, seq, seq
        );
        let result = self
            .docker
            .run_container(
                container_name,
                &lang.image,
                vec!["sh".into(), "-c".into(), shell_cmd],
                &spj_dir,
                self.run_host_config(256 * 1024 * 1024),
                Duration::from_secs(12),
            )
            .await?;
        Ok(if !result.timed_out && result.exit_code == 0 {
            Status::Accepted
        } else {
            Status::WrongAnswer
        })
    }

    /// 完整评测流程：编译 -> 逐 case 运行 + 比对 -> 汇总
    pub async fn judge(
        &self,
        registry: &LanguageRegistry,
        req: JudgeRequest,
    ) -> Result<JudgeResponse, SandboxError> {
        let task_id = Uuid::new_v4().to_string();
        let mut containers: Vec<String> = Vec::new();
        let result = self
            .judge_inner(registry, &req, &task_id, &mut containers)
            .await;
        // 清理：无论成功失败都删除本次任务的所有子容器
        for name in &containers {
            let _ = self.docker.remove_container(name).await;
        }
        // 任务目录异步删除，不阻塞响应
        let dir = self.task_dir(&task_id);
        tokio::spawn(async move {
            let _ = fs::remove_dir_all(&dir).await;
        });
        result
    }

    async fn judge_inner(
        &self,
        registry: &LanguageRegistry,
        req: &JudgeRequest,
        task_id: &str,
        containers: &mut Vec<String>,
    ) -> Result<JudgeResponse, SandboxError> {
        // 题目配置校验：空用例会让任何能编译的代码直接 AC，重复 seq 会让用例文件互相覆盖，
        // 二者都属于题目配置错误，按 SYSTEM_ERROR 上报，不能污染用户提交结论
        if let Err(reason) = validate_cases(&req.cases) {
            return Ok(JudgeResponse {
                submission_id: req.submission_id,
                compile: CompileInfo {
                    success: false,
                    log: reason,
                },
                overall_status: Status::SystemError,
                total_score: 0,
                cases: Vec::new(),
            });
        }
        let task_dir = self.task_dir(task_id);
        fs::create_dir_all(&task_dir).await?;
        let lang = registry.get(&req.language)?;
        fs::write(format!("{task_dir}/{}", lang.source_file), &req.code).await?;

        // 1. 编译用户代码
        let compile_name = self.docker.container_name(task_id, "compile");
        containers.push(compile_name.clone());
        let compile = self.compile(task_id, lang, &compile_name).await?;

        // 编译失败：所有 case 直接标记 CE，不执行
        if !compile.success {
            let cases = req
                .cases
                .iter()
                .map(|c| CaseResult {
                    seq: c.seq,
                    status: Status::CompileError,
                    time_ms: 0,
                    memory_kb: 0,
                    score: 0,
                    output_preview: String::new(),
                })
                .collect();
            return Ok(JudgeResponse {
                submission_id: req.submission_id,
                compile,
                overall_status: Status::CompileError,
                total_score: 0,
                cases,
            });
        }

        // 2. 可选：编译 SPJ checker；checker 编译失败属于系统侧问题，整体 SE
        if let Some(spj) = &req.spj {
            let spj_compile_name = self.docker.container_name(task_id, "spj-compile");
            containers.push(spj_compile_name.clone());
            let info = self
                .compile_spj(registry, task_id, spj, &spj_compile_name)
                .await?;
            if !info.success {
                let cases = req
                    .cases
                    .iter()
                    .map(|c| CaseResult {
                        seq: c.seq,
                        status: Status::SystemError,
                        time_ms: 0,
                        memory_kb: 0,
                        score: 0,
                        output_preview: String::new(),
                    })
                    .collect();
                return Ok(JudgeResponse {
                    submission_id: req.submission_id,
                    compile: CompileInfo {
                        success: false,
                        log: format!("SPJ checker 编译失败: {}", info.log),
                    },
                    overall_status: Status::SystemError,
                    total_score: 0,
                    cases,
                });
            }
        }

        // 3. 逐 case 运行并判定
        let mut results = Vec::with_capacity(req.cases.len());
        for case in &req.cases {
            fs::write(format!("{task_dir}/case_{}.in", case.seq), &case.input).await?;
            if req.spj.is_some() {
                fs::write(
                    format!("{task_dir}/case_{}.expected", case.seq),
                    &case.expected_output,
                )
                .await?;
            }

            let name = self
                .docker
                .container_name(task_id, &format!("case-{}", case.seq));
            containers.push(name.clone());
            let outcome = self
                .run_case(
                    task_id,
                    case.seq,
                    lang,
                    req.time_limit_ms,
                    req.memory_limit_mb,
                    req.stack_limit_mb.unwrap_or(256),
                )
                .await?;

            // 只有正常运行结束的 case 才做答案比对 / SPJ
            let final_status = match outcome.status {
                Status::Accepted => match &req.spj {
                    Some(spj) => {
                        let spj_name = self
                            .docker
                            .container_name(task_id, &format!("spj-{}", case.seq));
                        containers.push(spj_name.clone());
                        self.run_spj(registry, task_id, case.seq, spj, &spj_name)
                            .await?
                    }
                    None => compare_output(&case.expected_output, &outcome.output),
                },
                s => s,
            };

            // AC/PE 给分（PE 是否视为 AC 由 judge 端决定，沙箱如实上报状态），其余 0 分
            let score = if matches!(
                final_status,
                Status::Accepted | Status::PresentationError
            ) {
                case.score
            } else {
                0
            };
            let output_preview: String = outcome.output.chars().take(512).collect();
            results.push(CaseResult {
                seq: case.seq,
                status: final_status,
                time_ms: outcome.time_ms,
                memory_kb: outcome.memory_kb,
                score,
                output_preview,
            });
        }

        // 4. 汇总：overall_status 取所有 case 中最差状态；全 AC 才是 ACCEPTED
        let overall_status = results
            .iter()
            .map(|c| c.status)
            .max_by_key(|s| s.severity())
            .unwrap_or(Status::Accepted);
        let total_score = compute_total_score(req.io_score.unwrap_or(false), &results);

        Ok(JudgeResponse {
            submission_id: req.submission_id,
            compile,
            overall_status,
            total_score,
            cases: results,
        })
    }

    /// 在线运行：编译 + 单个 case（stdin 来自请求）
    pub async fn run(
        &self,
        registry: &LanguageRegistry,
        req: RunRequest,
    ) -> Result<RunResponse, SandboxError> {
        let lang = registry.get(&req.language)?;
        let task_id = Uuid::new_v4().to_string();
        let task_dir = self.task_dir(&task_id);
        let mut containers: Vec<String> = Vec::new();

        let result = async {
            fs::create_dir_all(&task_dir).await?;
            fs::write(format!("{task_dir}/{}", lang.source_file), &req.code).await?;
            fs::write(format!("{task_dir}/case_0.in"), &req.stdin).await?;

            // 编译阶段
            let compile_name = self.docker.container_name(&task_id, "compile");
            containers.push(compile_name.clone());
            let compile = self.compile(&task_id, lang, &compile_name).await?;
            if !compile.success {
                return Ok(RunResponse {
                    status: Status::CompileError,
                    stdout: String::new(),
                    stderr: String::new(),
                    compile_log: Some(compile.log),
                    time_ms: 0,
                    memory_kb: 0,
                });
            }

            let time_limit = req
                .time_limit_ms
                .unwrap_or(self.config.default_time_limit_ms);
            let memory_limit = req
                .memory_limit_mb
                .unwrap_or(self.config.default_memory_limit_mb);
            let outcome = self
                .run_case(&task_id, 0, lang, time_limit, memory_limit, 256)
                .await?;

            Ok(RunResponse {
                status: outcome.status,
                stdout: outcome.output,
                stderr: outcome.stderr,
                compile_log: if lang.compile_cmd.is_some() {
                    Some(compile.log)
                } else {
                    None
                },
                time_ms: outcome.time_ms,
                memory_kb: outcome.memory_kb,
            })
        }
        .await;

        for name in &containers {
            let _ = self.docker.remove_container(name).await;
        }
        let dir = task_dir.clone();
        tokio::spawn(async move {
            let _ = fs::remove_dir_all(&dir).await;
        });
        result
    }
}

/// 限制读取文件内容：超过 limit 截断并标记 exceeded；文件不存在视为空
async fn read_limited(path: &str, limit: u64) -> Result<(String, bool), SandboxError> {
    match fs::File::open(path).await {
        Ok(f) => {
            let mut buf = Vec::new();
            f.take(limit + 1).read_to_end(&mut buf).await?;
            let exceeded = buf.len() as u64 > limit;
            if exceeded {
                buf.truncate(limit as usize);
            }
            Ok((String::from_utf8_lossy(&buf).into_owned(), exceeded))
        }
        Err(e) if e.kind() == std::io::ErrorKind::NotFound => Ok((String::new(), false)),
        Err(e) => Err(e.into()),
    }
}

/// 归一化：去掉每行末尾空白，丢弃文件末尾空行
fn normalize_lines(s: &str) -> Vec<&str> {
    let mut lines: Vec<&str> = s.lines().map(|l| l.trim_end()).collect();
    while matches!(lines.last(), Some(l) if l.is_empty()) {
        lines.pop();
    }
    lines
}

/// 答案比对：完全相等或仅行尾空白/末尾空行差异 -> AC（主流 OJ 惯例）；否则 WA
fn compare_output(expected: &str, actual: &str) -> Status {
    if expected == actual || normalize_lines(expected) == normalize_lines(actual) {
        Status::Accepted
    } else {
        Status::WrongAnswer
    }
}

/// 汇总得分：OI 模式按用例分值累加；ACM 模式必须全部 AC 才计分，否则 0
fn compute_total_score(io_score: bool, results: &[CaseResult]) -> u32 {
    if io_score {
        return results.iter().map(|c| c.score).sum();
    }
    if results.iter().all(|c| matches!(c.status, Status::Accepted)) {
        results.iter().map(|c| c.score).sum()
    } else {
        0
    }
}

/// 渲染运行命令模板，支持占位符：
/// - `{time_ms}`：当前用例的有效时间限制（已含语言补偿）
/// - `{memory_mb}`：容器内存上限（已含语言补偿）
/// - `{heap_mb}`：堆内存建议值（容器上限的 75%，为元空间/线程栈/GC 预留余量）
fn render_run_cmd(template: &[String], time_limit_ms: u64, memory_limit_mb: u64) -> String {
    let heap_mb = (memory_limit_mb * 3 / 4).max(16);
    template
        .iter()
        .map(|part| {
            part.replace("{time_ms}", &time_limit_ms.to_string())
                .replace("{memory_mb}", &memory_limit_mb.to_string())
                .replace("{heap_mb}", &heap_mb.to_string())
        })
        .collect::<Vec<_>>()
        .join(" ")
}

/// 用例配置校验：空用例 / 重复 seq 均属于题目配置错误
fn validate_cases(cases: &[JudgeCase]) -> Result<(), String> {
    if cases.is_empty() {
        return Err("题目未配置评测用例，无法判题".to_string());
    }
    let mut seen = std::collections::HashSet::with_capacity(cases.len());
    for case in cases {
        if !seen.insert(case.seq) {
            return Err(format!("评测用例序号重复：{}", case.seq));
        }
    }
    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;

    fn case_result(status: Status, score: u32) -> CaseResult {
        CaseResult {
            seq: 1,
            status,
            time_ms: 0,
            memory_kb: 0,
            score,
            output_preview: String::new(),
        }
    }

    #[test]
    fn oi_mode_should_sum_partial_scores() {
        let results = vec![
            case_result(Status::Accepted, 30),
            case_result(Status::WrongAnswer, 0),
            case_result(Status::Accepted, 30),
        ];
        assert_eq!(compute_total_score(true, &results), 60);
    }

    #[test]
    fn acm_mode_should_require_all_accepted() {
        let partial = vec![
            case_result(Status::Accepted, 50),
            case_result(Status::WrongAnswer, 0),
        ];
        assert_eq!(compute_total_score(false, &partial), 0);

        let all_accepted = vec![
            case_result(Status::Accepted, 50),
            case_result(Status::Accepted, 50),
        ];
        assert_eq!(compute_total_score(false, &all_accepted), 100);
    }

    #[test]
    fn render_run_cmd_should_replace_placeholders() {
        let template = vec![
            "java".to_string(),
            "-Xmx{heap_mb}m".to_string(),
            "-Dlimit={memory_mb}".to_string(),
            "Main".to_string(),
        ];
        assert_eq!(
            render_run_cmd(&template, 2000, 512),
            "java -Xmx384m -Dlimit=512 Main"
        );
    }

    #[test]
    fn render_run_cmd_should_keep_plain_command() {
        let template = vec!["./main".to_string()];
        assert_eq!(render_run_cmd(&template, 1000, 256), "./main");
    }

    #[test]
    fn empty_cases_should_be_rejected() {
        assert!(validate_cases(&[]).is_err());
    }

    #[test]
    fn duplicated_case_seq_should_be_rejected() {
        let cases = vec![judge_case(1), judge_case(1)];
        assert!(validate_cases(&cases).is_err());
    }

    #[test]
    fn unique_case_seq_should_pass() {
        let cases = vec![judge_case(1), judge_case(2)];
        assert!(validate_cases(&cases).is_ok());
    }

    fn judge_case(seq: u32) -> JudgeCase {
        JudgeCase {
            seq,
            input: String::new(),
            expected_output: String::new(),
            score: 0,
        }
    }
}
