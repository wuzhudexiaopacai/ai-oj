//! 核心执行器：编译、单 case 运行、答案比对、SPJ

use std::collections::HashMap;
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

    /// 编译容器安全限制：断网、drop 所有 capabilities、限制进程数与内存、
    /// 禁止提权；根文件系统需可写（编译器会写临时文件），通过 tmpfs 限制 /tmp
    fn compile_host_config(&self) -> HostConfig {
        HostConfig {
            network_mode: Some("none".to_string()),
            memory: Some((self.config.compile_memory_mb * 1024 * 1024) as i64),
            pids_limit: Some(128),
            cap_drop: Some(vec!["ALL".to_string()]),
            security_opt: Some(vec!["no-new-privileges=true".to_string()]),
            tmpfs: Some(HashMap::from([(
                "/tmp".to_string(),
                "rw,nosuid,nodev,size=512m".to_string(),
            )])),
            auto_remove: Some(false),
            ..Default::default()
        }
    }

    /// 运行容器安全限制：只读根文件系统（唯一可写为 /work 挂载点与 /tmp tmpfs）、
    /// 断网、1 CPU、drop 所有 capabilities、禁止提权、限制进程数
    fn run_host_config(&self, memory_bytes: i64) -> HostConfig {
        HostConfig {
            network_mode: Some("none".to_string()),
            memory: Some(memory_bytes),
            nano_cpus: Some(1_000_000_000),
            pids_limit: Some(64),
            cap_drop: Some(vec!["ALL".to_string()]),
            security_opt: Some(vec!["no-new-privileges=true".to_string()]),
            readonly_rootfs: Some(true),
            tmpfs: Some(HashMap::from([(
                "/tmp".to_string(),
                "rw,nosuid,nodev,size=64m".to_string(),
            )])),
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
        let result = self
            .docker
            .run_container(
                container_name,
                &lang.image,
                cmd.clone(),
                task_id,
                &self.config.work_dir,
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
        // 容器内 timeout 支持小数秒，避免整秒向上取整带来最多 1s 的判定误差
        let limit_ms = (time_limit_ms as f64 * lang.time_factor) as u64;
        let timeout = timeout_arg(limit_ms);
        // 栈空间限制（KB）：ulimit 不受支持时忽略，不影响判题
        let stack_kb = stack_limit_mb * 1024;
        // 容器内存上限（含语言补偿），同时作为 -Xmx 等运行参数的模板取值
        let mem_bytes = (memory_limit_mb as f64 * lang.memory_factor * 1024.0 * 1024.0) as i64;
        let limit_kb = (mem_bytes / 1024) as u64;
        let container_mb = (mem_bytes / (1024 * 1024)).max(1) as u64;
        let run_cmd = render_run_cmd(&lang.run_cmd, limit_ms, container_mb);
        // timeout 触发 TLE 时退出码为 124；
        // 运行耗时在容器内测：只统计程序本身，排除容器创建/启动开销；
        // date +%s%N 不可用（部分 busybox）时跳过，由外层 elapsed 兜底。
        // 程序结束后读取容器自身 cgroup 的内存峰值（memory.peak）写入 .mem，
        // 短时间运行的程序无法被 Docker stats 秒级采样捕获，cgroup 峰值是准确的兜底来源。
        let shell_cmd = format!(
            "ulimit -s {stack_kb} 2>/dev/null; \
             start_ns=$(date +%s%N 2>/dev/null); \
             timeout {timeout} {run_cmd} < case_{seq}.in > case_{seq}.out 2> case_{seq}.err; rc=$?; \
             end_ns=$(date +%s%N 2>/dev/null); \
             if [ -n \"$start_ns\" ] && [ -n \"$end_ns\" ]; then \
               case \"$start_ns$end_ns\" in \
                 *[!0-9]*) : ;; \
                 *) echo $(( (end_ns - start_ns) / 1000000 )) > case_{seq}.time ;; \
               esac; \
             fi; \
             cat /sys/fs/cgroup/memory.peak > case_{seq}.mem 2>/dev/null; \
             cat /sys/fs/cgroup/memory/memory.max_usage_in_bytes >> case_{seq}.mem 2>/dev/null; \
             exit $rc",
            timeout = timeout,
            run_cmd = run_cmd,
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
                task_id,
                &self.config.work_dir,
                self.run_host_config(mem_bytes),
                // 兜底超时：容器内 timeout 之外再 +2s
                Duration::from_millis(limit_ms + 2000),
            )
            .await?;
        let elapsed_ms = start.elapsed().as_millis() as u64;
        // 优先使用容器内测得的真实运行耗时（不含容器启动），拿不到时用外层 elapsed 兜底
        let measured_ms = match fs::read_to_string(format!("{task_dir}/case_{seq}.time")).await {
            Ok(text) => text.trim().parse::<u64>().ok(),
            Err(_) => None,
        };
        let time_ms = resolve_time_ms(
            measured_ms,
            result.timed_out,
            result.exit_code,
            elapsed_ms,
            limit_ms,
        );

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
        ensure_shared_dir(&spj_dir).await?;
        write_shared_file(&format!("{spj_dir}/{}", lang.source_file), spj.code.as_bytes()).await?;
        match &lang.compile_cmd {
            Some(cmd) => {
                let result = self
                    .docker
                    .run_container(
                        container_name,
                        &lang.image,
                        cmd.clone(),
                        task_id,
                        &format!("{}/spj", self.config.work_dir),
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
    /// 退出码约定：0 -> AC；其余非 0 -> WA（兼容历史 checker）；
    /// 超时或崩溃信号（SIGSEGV/SIGABRT/SIGFPE/SIGKILL/SIGTERM）属于 checker 自身故障 -> SE
    async fn run_spj(
        &self,
        registry: &LanguageRegistry,
        task_id: &str,
        seq: u32,
        spj: &SpjSpec,
        container_name: &str,
    ) -> Result<Status, SandboxError> {
        let lang = registry.get(&spj.language)?;
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
                task_id,
                &format!("{}/spj", self.config.work_dir),
                self.run_host_config(256 * 1024 * 1024),
                Duration::from_secs(12),
            )
            .await?;
        Ok(spj_status(result.timed_out, result.exit_code))
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
        ensure_shared_dir(&task_dir).await?;
        let lang = registry.get(&req.language)?;
        write_shared_file(&format!("{task_dir}/{}", lang.source_file), req.code.as_bytes()).await?;

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
            write_shared_file(
                &format!("{task_dir}/case_{}.in", case.seq),
                case.input.as_bytes(),
            )
            .await?;
            if req.spj.is_some() {
                write_shared_file(
                    &format!("{task_dir}/case_{}.expected", case.seq),
                    case.expected_output.as_bytes(),
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
            ensure_shared_dir(&task_dir).await?;
            write_shared_file(
                &format!("{task_dir}/{}", lang.source_file),
                req.code.as_bytes(),
            )
            .await?;
            write_shared_file(&format!("{task_dir}/case_0.in"), req.stdin.as_bytes()).await?;

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

/// 创建任务目录并放开写权限：子容器以 nobody(65534) 运行，需要写入编译产物与输出
async fn ensure_shared_dir(path: &str) -> Result<(), SandboxError> {
    fs::create_dir_all(path).await?;
    set_mode(path, 0o777).await
}

/// 写入任务文件并放开读写权限，保证子容器内的 nobody 用户可以读取/覆盖
async fn write_shared_file(path: &str, content: &[u8]) -> Result<(), SandboxError> {
    fs::write(path, content).await?;
    set_mode(path, 0o666).await
}

/// 设置 POSIX 权限位；非 Unix 平台（本地单测）忽略
async fn set_mode(path: &str, mode: u32) -> Result<(), SandboxError> {
    #[cfg(unix)]
    {
        use std::os::unix::fs::PermissionsExt;
        fs::set_permissions(path, std::fs::Permissions::from_mode(mode)).await?;
    }
    #[cfg(not(unix))]
    {
        let _ = (path, mode);
    }
    Ok(())
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

/// 答案比对（主流 OJ 惯例）：
/// - 完全相等，或仅行尾空白/末尾空行差异 -> AC
/// - 忽略所有空白后 token 序列一致 -> PE（内容正确，格式不规范）
/// - 其余 -> WA
fn compare_output(expected: &str, actual: &str) -> Status {
    if expected == actual || normalize_lines(expected) == normalize_lines(actual) {
        Status::Accepted
    } else if expected.split_whitespace().eq(actual.split_whitespace()) {
        Status::PresentationError
    } else {
        Status::WrongAnswer
    }
}

/// 毫秒级 timeout 参数：GNU coreutils / 现代 BusyBox 的 timeout 均支持小数秒
fn timeout_arg(limit_ms: u64) -> String {
    format!("{:.3}", limit_ms.max(1) as f64 / 1000.0)
}

/// 运行耗时取值：超时报告题目限制；否则优先容器内实测值，缺失时用外层 elapsed 兜底
fn resolve_time_ms(
    measured_ms: Option<u64>,
    timed_out: bool,
    exit_code: i64,
    elapsed_ms: u64,
    limit_ms: u64,
) -> u64 {
    if timed_out || exit_code == 124 {
        return limit_ms;
    }
    measured_ms.unwrap_or(elapsed_ms)
}

/// SPJ 退出码语义：0 AC；其余非 0 视为 WA（兼容历史 checker）；
/// 超时或常见崩溃信号（SIGSEGV/SIGABRT/SIGFPE/SIGKILL/SIGTERM）视为 checker 故障 -> SE
fn spj_status(timed_out: bool, exit_code: i64) -> Status {
    if timed_out || exit_code == 124 {
        return Status::SystemError;
    }
    match exit_code {
        0 => Status::Accepted,
        134 | 136 | 137 | 139 | 143 => Status::SystemError,
        _ => Status::WrongAnswer,
    }
}

/// 汇总得分：OI 模式按用例分值累加；ACM 模式必须全部 AC 才计分，否则 0
/// <p>
/// PE 只影响用例展示（OI 下仍按用例得分计入），ACM 判定通过仍要求严格 AC，
/// 与个人/题目通过统计口径保持一致。
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

    #[test]
    fn compare_output_should_accept_trailing_whitespace() {
        assert_eq!(compare_output("1 2\n", "1 2   \n\n"), Status::Accepted);
    }

    #[test]
    fn compare_output_should_detect_presentation_error() {
        assert_eq!(
            compare_output("1 2\n3", "1\n2 3\n"),
            Status::PresentationError
        );
        assert_eq!(compare_output("1 2", "1  2"), Status::PresentationError);
    }

    #[test]
    fn compare_output_should_reject_different_tokens() {
        assert_eq!(compare_output("1 2", "12"), Status::WrongAnswer);
    }

    #[test]
    fn timeout_arg_should_keep_millisecond_precision() {
        assert_eq!(timeout_arg(1001), "1.001");
        assert_eq!(timeout_arg(1000), "1.000");
        assert_eq!(timeout_arg(0), "0.001");
    }

    #[test]
    fn resolve_time_ms_should_prefer_measured_value() {
        assert_eq!(resolve_time_ms(Some(15), false, 0, 120, 1000), 15);
        assert_eq!(resolve_time_ms(None, false, 0, 120, 1000), 120);
        assert_eq!(resolve_time_ms(None, true, -1, 1100, 1000), 1000);
        assert_eq!(resolve_time_ms(Some(10), false, 124, 1100, 1000), 1000);
    }

    #[test]
    fn spj_status_should_distinguish_checker_failure() {
        assert_eq!(spj_status(false, 0), Status::Accepted);
        assert_eq!(spj_status(false, 1), Status::WrongAnswer);
        // 255 是 C/C++ return -1 的表现，历史 checker 常用它表示 WA
        assert_eq!(spj_status(false, 255), Status::WrongAnswer);
        assert_eq!(spj_status(false, 124), Status::SystemError);
        assert_eq!(spj_status(true, 0), Status::SystemError);
        assert_eq!(spj_status(false, 137), Status::SystemError);
        assert_eq!(spj_status(false, 139), Status::SystemError);
        assert_eq!(spj_status(false, 143), Status::SystemError);
    }

    #[tokio::test]
    async fn shared_task_files_should_allow_nobody_write() {
        let dir = std::env::temp_dir().join(format!("sandbox-perm-{}", Uuid::new_v4()));
        let dir_path = dir.to_str().unwrap().to_string();
        ensure_shared_dir(&dir_path).await.unwrap();
        let file_path = format!("{dir_path}/main.c");
        write_shared_file(&file_path, b"int main() { return 0; }")
            .await
            .unwrap();

        #[cfg(unix)]
        {
            use std::os::unix::fs::PermissionsExt;
            let dir_mode = std::fs::metadata(&dir_path).unwrap().permissions().mode();
            let file_mode = std::fs::metadata(&file_path).unwrap().permissions().mode();
            assert_eq!(dir_mode & 0o777, 0o777);
            assert_eq!(file_mode & 0o777, 0o666);
        }
        let _ = std::fs::remove_dir_all(&dir);
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
