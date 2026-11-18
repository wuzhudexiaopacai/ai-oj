//! bollard 封装：子容器创建、等待、日志、stats 采样、清理
//!
//! sibling container 模式：本服务通过挂载的 /var/run/docker.sock 操作宿主 Docker，
//! 子容器与本服务挂载同一个 ai-oj-work volume 到相同路径 /work，路径天然一致。

use std::collections::HashMap;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::Arc;
use std::time::Duration;

use bollard::container::{
    Config, CreateContainerOptions, InspectContainerOptions, KillContainerOptions,
    ListContainersOptions, LogOutput, LogsOptions, RemoveContainerOptions, StartContainerOptions,
    StatsOptions, WaitContainerOptions,
};
use bollard::models::{HostConfig, Mount, MountTypeEnum};
use bollard::Docker;
use bollard::API_DEFAULT_VERSION;
use futures_util::StreamExt;
use tracing::{info, warn};

use crate::config::AppConfig;
use crate::error::SandboxError;

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

    /// 共享工作目录挂载：volume ai-oj-work -> /work（子容器内可写）
    fn work_mount(&self) -> Mount {
        Mount {
            target: Some(self.work_dir.clone()),
            source: Some(self.work_volume.clone()),
            typ: Some(MountTypeEnum::VOLUME),
            read_only: Some(false),
            ..Default::default()
        }
    }

    /// 创建并启动一个子容器，等待退出后收集结果。
    /// 调用方负责传入安全限制（HostConfig），本函数自动追加 /work 挂载。
    pub async fn run_container(
        &self,
        name: &str,
        image: &str,
        cmd: Vec<String>,
        working_dir: &str,
        host_config: HostConfig,
        timeout: Duration,
    ) -> Result<ContainerResult, SandboxError> {
        // 防御：同名残留容器先强删
        let _ = self.remove_container(name).await;

        let mut hc = host_config;
        let mut mounts = hc.mounts.take().unwrap_or_default();
        mounts.push(self.work_mount());
        hc.mounts = Some(mounts);

        let config = Config {
            image: Some(image.to_string()),
            cmd: Some(cmd),
            working_dir: Some(working_dir.to_string()),
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
            if out.len() > 64 * 1024 {
                out.truncate(64 * 1024);
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
