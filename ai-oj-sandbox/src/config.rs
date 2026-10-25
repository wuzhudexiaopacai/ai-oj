use serde::Deserialize;

/// 服务配置，从 config.toml 加载
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct AppConfig {
    pub listen_addr: String,
    pub max_parallel: usize,
    pub work_dir: String,
    pub work_volume: String,
    pub container_prefix: String,
    pub default_time_limit_ms: u64,
    pub default_memory_limit_mb: u64,
    pub compile_timeout_secs: u64,
    pub compile_memory_mb: u64,
    pub output_limit_bytes: u64,
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
        if let Some(v) = env("SANDBOX_TASK_TTL_SECS").and_then(|v| v.parse().ok()) {
            self.task_ttl_secs = v;
        }
    }
}
