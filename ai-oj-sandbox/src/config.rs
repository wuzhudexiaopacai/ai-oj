use serde::Deserialize;

fn default_docker_host() -> String {
    "unix:///var/run/docker.sock".to_string()
}

/// 请求体默认上限 64MB：题面用例与源代码一次性提交，超大请求直接拒绝
fn default_max_body_bytes() -> u64 {
    64 * 1024 * 1024
}

/// 服务配置，从 config.toml 加载
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct AppConfig {
    pub listen_addr: String,
    pub max_parallel: usize,
    pub work_dir: String,
    pub work_volume: String,
    pub container_prefix: String,
    /// Docker 守护进程地址：unix:///var/run/docker.sock 或 tcp://docker-socket-proxy:2375
    #[serde(default = "default_docker_host")]
    pub docker_host: String,
    /// HTTP 访问令牌；为空表示不校验（仅允许内网部署时使用）
    #[serde(default)]
    pub api_token: Option<String>,
    pub default_time_limit_ms: u64,
    pub default_memory_limit_mb: u64,
    pub compile_timeout_secs: u64,
    pub compile_memory_mb: u64,
    pub output_limit_bytes: u64,
    /// HTTP 请求体上限（字节），超限请求在解析 JSON 前即被拒绝
    #[serde(default = "default_max_body_bytes")]
    pub max_body_bytes: u64,
    pub task_ttl_secs: u64,
}

impl AppConfig {
    /// 加载配置：config.toml 为基础，SANDBOX_* 环境变量覆盖
    /// （compose 部署时卷名带项目前缀，如 ai-oj_ai-oj-work，必须支持环境变量注入）
    pub fn load() -> Result<Self, crate::error::SandboxError> {
        let path = std::env::var("SANDBOX_CONFIG").unwrap_or_else(|_| "config.toml".to_string());
        let content = std::fs::read_to_string(&path)
            .map_err(|e| crate::error::SandboxError::Config(format!("读取 {path} 失败: {e}")))?;
        let mut config: Self = toml::from_str(&content)
            .map_err(|e| crate::error::SandboxError::Config(format!("解析 {path} 失败: {e}")))?;
        config.apply_env();
        // 空字符串等价于未配置，避免误开启"空令牌"校验
        config.api_token = config.api_token.filter(|token| !token.is_empty());
        Ok(config)
    }

    /// SANDBOX_* 环境变量覆盖同名 TOML 字段
    fn apply_env(&mut self) {
        fn env(key: &str) -> Option<String> {
            std::env::var(key).ok().filter(|v| !v.is_empty())
        }
        if let Some(v) = env("SANDBOX_LISTEN") {
            self.listen_addr = v;
        }
        if let Some(v) = env("SANDBOX_WORK_DIR") {
            self.work_dir = v;
        }
        if let Some(v) = env("SANDBOX_WORK_VOLUME") {
            self.work_volume = v;
        }
        if let Some(v) = env("SANDBOX_CONTAINER_PREFIX") {
            self.container_prefix = v;
        }
        if let Some(v) = env("SANDBOX_DOCKER_HOST") {
            self.docker_host = v;
        }
        if let Some(v) = env("SANDBOX_API_TOKEN") {
            self.api_token = Some(v);
        }
        if let Some(v) = env("SANDBOX_MAX_PARALLEL").and_then(|v| v.parse().ok()) {
            self.max_parallel = v;
        }
        if let Some(v) = env("SANDBOX_DEFAULT_TIME_LIMIT_MS").and_then(|v| v.parse().ok()) {
            self.default_time_limit_ms = v;
        }
        if let Some(v) = env("SANDBOX_DEFAULT_MEMORY_LIMIT_MB").and_then(|v| v.parse().ok()) {
            self.default_memory_limit_mb = v;
        }
        if let Some(v) = env("SANDBOX_COMPILE_TIMEOUT_SECS").and_then(|v| v.parse().ok()) {
            self.compile_timeout_secs = v;
        }
        if let Some(v) = env("SANDBOX_COMPILE_MEMORY_MB").and_then(|v| v.parse().ok()) {
            self.compile_memory_mb = v;
        }
        if let Some(v) = env("SANDBOX_OUTPUT_LIMIT_BYTES").and_then(|v| v.parse().ok()) {
            self.output_limit_bytes = v;
        }
        if let Some(v) = env("SANDBOX_MAX_BODY_BYTES").and_then(|v| v.parse().ok()) {
            self.max_body_bytes = v;
        }
        if let Some(v) = env("SANDBOX_TASK_TTL_SECS").and_then(|v| v.parse().ok()) {
            self.task_ttl_secs = v;
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    /// 除待测字段外的最小可解析配置
    const BASE: &str = r#"
listen_addr = "0.0.0.0:8000"
max_parallel = 4
work_dir = "/work"
work_volume = "ai-oj-work"
container_prefix = "ai-oj-sandbox-"
default_time_limit_ms = 2000
default_memory_limit_mb = 256
compile_timeout_secs = 60
compile_memory_mb = 512
output_limit_bytes = 16777216
task_ttl_secs = 300
"#;

    #[test]
    fn max_body_bytes_should_default_to_64mb() {
        let config: AppConfig = toml::from_str(BASE).unwrap();
        assert_eq!(config.max_body_bytes, 64 * 1024 * 1024);
    }

    #[test]
    fn max_body_bytes_should_be_configurable() {
        let config: AppConfig =
            toml::from_str(&format!("{BASE}\nmax_body_bytes = 1048576")).unwrap();
        assert_eq!(config.max_body_bytes, 1024 * 1024);
    }

    #[test]
    fn docker_host_should_default_to_local_socket() {
        let config: AppConfig = toml::from_str(BASE).unwrap();
        assert_eq!(config.docker_host, "unix:///var/run/docker.sock");
        assert!(config.api_token.is_none());
    }
}
