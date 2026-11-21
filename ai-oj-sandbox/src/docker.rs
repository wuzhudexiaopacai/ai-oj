//! bollard 封装：子容器创建、等待、日志、stats 采样、清理
//!
//! sibling container 模式：本服务通过 Docker 守护进程（socket 或 socket-proxy）创建子容器。
//! 子容器只挂载 ai-oj-work 卷中"本次任务"的子目录（volume subpath），
//! 无法读取其他任务的用例与期望输出；子容器内挂载点统一为 /work。

use std::collections::HashMap;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::Arc;
use std::time::Duration;

use bollard::container::{
    Config, CreateContainerOptions, InspectContainerOptions, KillContainerOptions,
    ListContainersOptions, LogOutput, LogsOptions, RemoveContainerOptions, StartContainerOptions,
    StatsOptions, WaitContainerOptions,
};
use bollard::models::{HostConfig, Mount, MountTypeEnum, MountVolumeOptions};
use bollard::Docker;
use bollard::API_DEFAULT_VERSION;
use futures_util::StreamExt;
use tracing::{info, warn};

use crate::config::AppConfig;
use crate::error::SandboxError;

/// 子容器运行用户：nobody(65534)，避免以 root 执行不可信代码
const SANDBOX_USER: &str = "65534:65534";

/// 一次容器执行的完整结果
pub struct ContainerResult {
    /// 容器退出码（兜底超时强杀时为 -1）
    pub exit_code: i64,
    /// 是否被外层 tokio timeout 兜底强杀（容器内 timeout 命令失效时的双保险）
    pub timed_out: bool,
    /// 容器 stdout/stderr 合并日志（编译场景作为 compile_log）
    pub log: String,
    /// 运行期间采样到的内存峰值（KB），采样失败为 0
    pub memory_kb: u64,
}

const MAX_LOG_BYTES: usize = 64 * 1024;

/// 截断日志时回退到 UTF-8 字符边界，避免多字节输出导致 panic
fn truncate_log(log: &mut String) -> bool {
    if log.len() <= MAX_LOG_BYTES {
        return false;
    }
    let mut boundary = MAX_LOG_BYTES;
    while !log.is_char_boundary(boundary) {
        boundary -= 1;
    }
    log.truncate(boundary);
    true
}

/// Docker 的 name 过滤器是部分匹配，删除前必须确认完整名称属于本实例前缀
fn has_managed_container_name(names: &[String], prefix: &str) -> bool {
    names
        .iter()
        .any(|name| name.trim_start_matches('/').starts_with(prefix))
}

#[derive(Clone)]
pub struct DockerManager {
    docker: Docker,
    work_volume: String,
    work_dir: String,
    container_prefix: String,
}

impl DockerManager {
    pub fn connect(config: &AppConfig) -> Result<Self, SandboxError> {
        // 支持两种守护进程地址：
        // - unix:///var/run/docker.sock：直接挂载 socket（仅限本地调试）
        // - tcp://docker-socket-proxy:2375：经代理访问，只放行容器相关接口
        let host = config.docker_host.trim();
        let docker = if host.starts_with("tcp://") || host.starts_with("http://") {
            Docker::connect_with_http(host, 120, API_DEFAULT_VERSION)?
        } else {
            Docker::connect_with_socket(host, 120, API_DEFAULT_VERSION)?
        };
        Ok(Self {
            docker,
            work_volume: config.work_volume.clone(),
            work_dir: config.work_dir.clone(),
            container_prefix: config.container_prefix.clone(),
        })
    }

    /// 子容器统一命名：{前缀}{taskId}-{后缀}，便于按前缀清理残留
    pub fn container_name(&self, task_id: &str, suffix: &str) -> String {
        format!("{}{}-{}", self.container_prefix, task_id, suffix)
    }

    /// 单任务挂载：volume ai-oj-work 的 {taskId} 子目录 -> 子容器 /work
    ///
    /// 使用 Docker 卷 subpath（要求 Docker Engine 26+），子容器只能看到本次任务的
    /// 输入/输出/编译产物，无法遍历同一卷下其他任务的用例。
    fn task_mount(&self, task_id: &str) -> Mount {
        Mount {
            target: Some(self.work_dir.clone()),
            source: Some(self.work_volume.clone()),
            typ: Some(MountTypeEnum::VOLUME),
            read_only: Some(false),
            volume_options: Some(MountVolumeOptions {
                subpath: Some(task_id.to_string()),
                // 不把镜像中原有 /work 内容复制进 volume 子目录
                no_copy: Some(true),
                ..Default::default()
            }),
            ..Default::default()
        }
    }

    /// 创建并启动一个子容器，等待退出后收集结果。
    /// 调用方负责传入安全限制（HostConfig）；本函数自动追加"仅本任务目录"的挂载。
    pub async fn run_container(
        &self,
        name: &str,
        image: &str,
        cmd: Vec<String>,
        task_id: &str,
        container_workdir: &str,
        host_config: HostConfig,
        timeout: Duration,
    ) -> Result<ContainerResult, SandboxError> {
        validate_task_id(task_id)?;
        // 防御：同名残留容器先强删
        let _ = self.remove_container(name).await;

        let mut hc = host_config;
        let mut mounts = hc.mounts.take().unwrap_or_default();
        mounts.push(self.task_mount(task_id));
        hc.mounts = Some(mounts);

        let config = Config {
            image: Some(image.to_string()),
            cmd: Some(cmd),
            working_dir: Some(container_workdir.to_string()),
            // 统一以 nobody 运行，HOME/TMPDIR 指向可写的 tmpfs
            user: Some(SANDBOX_USER.to_string()),
            env: Some(vec![
                "HOME=/tmp".to_string(),
                "TMPDIR=/tmp".to_string(),
            ]),
            attach_stdout: Some(true),
            attach_stderr: Some(true),
            host_config: Some(hc),
            ..Default::default()
        };
        self.docker
            .create_container(
                Some(CreateContainerOptions {
                    name: name.to_string(),
                    platform: None,
                }),
                config,
            )
            .await?;
        self.docker
            .start_container(name, None::<StartContainerOptions<String>>)
            .await?;

        // 后台任务：流式读取 stats，记录内存峰值（容器退出后 stats 流自然结束）
        let peak = Arc::new(AtomicU64::new(0));
        let sampler = {
            let docker = self.docker.clone();
            let name = name.to_string();
            let peak = peak.clone();
            tokio::spawn(async move {
                let mut stream = docker.stats(
                    &name,
                    Some(StatsOptions {
                        stream: true,
                        one_shot: false,
                    }),
                );
                while let Some(Ok(stats)) = stream.next().await {
                    // 经 JSON 取值，屏蔽不同 bollard-stubs 版本字段类型差异
                    if let Ok(v) = serde_json::to_value(&stats) {
                        let usage = v
                            .pointer("/memory_stats/max_usage")
                            .or_else(|| v.pointer("/memory_stats/usage"))
                            .and_then(|x| x.as_u64());
                        if let Some(u) = usage {
                            peak.fetch_max(u, Ordering::Relaxed);
                        }
                    }
                }
            })
        };

        // 等待容器退出；外层 tokio timeout 兜底（容器内 timeout 命令是主保险）
        let wait_result = tokio::time::timeout(timeout, self.wait_exit(name)).await;
        sampler.abort();

        let (exit_code, timed_out) = match wait_result {
            Ok(code) => (code?, false),
            Err(_) => {
                warn!(container = %name, "wait 兜底超时，强杀容器");
                let _ = self
                    .docker
                    .kill_container(name, None::<KillContainerOptions<String>>)
                    .await;
                (-1, true)
            }
        };

        let log = self.collect_logs(name).await.unwrap_or_default();

        Ok(ContainerResult {
            exit_code,
            timed_out,
            log,
            memory_kb: peak.load(Ordering::Relaxed) / 1024,
        })
    }

    /// 等待容器退出并返回退出码；流异常结束时用 inspect 兜底取状态码
    async fn wait_exit(&self, name: &str) -> Result<i64, SandboxError> {
        let mut stream = self.docker.wait_container(
            name,
            Some(WaitContainerOptions {
                condition: "not-running".to_string(),
            }),
        );
        while let Some(item) = stream.next().await {
            match item {
                Ok(resp) => return Ok(resp.status_code),
                // bollard 会把「非 0 退出码」当作 wait 错误返回，但 code 即真实退出码：
                // 编译失败 / 运行期异常 / timeout 124 都属于这种情况，必须还原为正常结果
                Err(bollard::errors::Error::DockerContainerWaitError { code, .. }) => {
                    return Ok(code)
                }
                Err(e) => return Err(SandboxError::Docker(e)),
            }
        }
        let info = self
            .docker
            .inspect_container(name, None::<InspectContainerOptions>)
            .await?;
        let code = info.state.and_then(|s| s.exit_code).unwrap_or(-1);
        Ok(code)
    }

    /// 收集容器 stdout/stderr 合并日志，截断到 64KB 防内存膨胀
    async fn collect_logs(&self, name: &str) -> Result<String, SandboxError> {
        let mut stream = self.docker.logs(
            name,
            Some(LogsOptions::<String> {
                stdout: true,
                stderr: true,
                tail: "all".to_string(),
                ..Default::default()
            }),
        );
        let mut out = String::new();
        while let Some(item) = stream.next().await {
            match item {
                Ok(LogOutput::StdOut { message }) | Ok(LogOutput::StdErr { message }) => {
                    out.push_str(&String::from_utf8_lossy(&message));
                }
                Ok(_) => {}
                Err(e) => return Err(SandboxError::Docker(e)),
            }
            if truncate_log(&mut out) {
                break;
            }
        }
        Ok(out)
    }

    /// 删除容器（force + 删 volume 挂载引用）
    pub async fn remove_container(&self, name: &str) -> Result<(), SandboxError> {
        self.docker
            .remove_container(
                name,
                Some(RemoveContainerOptions {
                    force: true,
                    v: true,
                    ..Default::default()
                }),
            )
            .await?;
        Ok(())
    }

    /// 服务启动时调用：清理上次崩溃残留的 ai-oj-sandbox-* 容器
    pub async fn cleanup_stale(&self) {
        let mut filters: HashMap<String, Vec<String>> = HashMap::new();
        filters.insert("name".to_string(), vec![self.container_prefix.clone()]);
        match self
            .docker
            .list_containers(Some(ListContainersOptions::<String> {
                all: true,
                filters,
                ..Default::default()
            }))
            .await
        {
            Ok(list) => {
                for c in list {
                    if !c
                        .names
                        .as_deref()
                        .is_some_and(|names| {
                            has_managed_container_name(names, &self.container_prefix)
                        })
                    {
                        continue;
                    }
                    if let Some(id) = c.id {
                        info!(container = %id, "清理残留沙箱容器");
                        let _ = self
                            .docker
                            .remove_container(
                                &id,
                                Some(RemoveContainerOptions {
                                    force: true,
                                    v: true,
                                    ..Default::default()
                                }),
                            )
                            .await;
                    }
                }
            }
            Err(e) => warn!("清理残留容器失败: {e}"),
        }
    }
}

/// 任务标识校验：仅允许 ASCII 字母数字与 `-`/`_`，长度 1..=64
///
/// 任务目录会作为卷 subpath 使用，必须杜绝 `..`、`/` 等路径穿越字符。
fn validate_task_id(task_id: &str) -> Result<(), SandboxError> {
    let valid = !task_id.is_empty()
        && task_id.len() <= 64
        && task_id
            .chars()
            .all(|c| c.is_ascii_alphanumeric() || c == '-' || c == '_');
    if valid {
        Ok(())
    } else {
        Err(SandboxError::Internal(format!("非法任务标识: {task_id}")))
    }
}

#[cfg(test)]
mod tests {
    use super::{
        has_managed_container_name, truncate_log, validate_task_id, MAX_LOG_BYTES,
    };

    #[test]
    fn uuid_task_id_should_pass() {
        assert!(validate_task_id("1f0c9b8e-4a52-4d1a-9f0b-3c2d1e4a5b6c").is_ok());
    }

    #[test]
    fn path_traversal_should_be_rejected() {
        assert!(validate_task_id("../other-task").is_err());
        assert!(validate_task_id("..").is_err());
        assert!(validate_task_id("a/b").is_err());
        assert!(validate_task_id("").is_err());
    }

    #[test]
    fn overlong_task_id_should_be_rejected() {
        assert!(validate_task_id(&"a".repeat(65)).is_err());
    }

    #[test]
    fn log_truncation_should_preserve_utf8_boundary() {
        let mut log = "a".repeat(MAX_LOG_BYTES - 1);
        log.push_str("世界");

        assert!(truncate_log(&mut log));
        assert_eq!(log.len(), MAX_LOG_BYTES - 1);
        assert!(log.is_char_boundary(log.len()));
    }

    #[test]
    fn log_at_limit_should_not_be_truncated() {
        let mut log = "a".repeat(MAX_LOG_BYTES);

        assert!(!truncate_log(&mut log));
        assert_eq!(log.len(), MAX_LOG_BYTES);
    }

    #[test]
    fn stale_cleanup_should_only_match_prefixed_container_names() {
        let prefix = "ai-oj-sandbox-";

        assert!(has_managed_container_name(
            &["/ai-oj-sandbox-task-1-compile".to_string()],
            prefix
        ));
        assert!(!has_managed_container_name(
            &["/other-ai-oj-sandbox-task-1-compile".to_string()],
            prefix
        ));
        assert!(!has_managed_container_name(&[], prefix));
    }
}
