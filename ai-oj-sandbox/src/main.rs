mod cleanup;
mod config;
mod docker;
mod error;
mod executor;
mod handler;
mod language;
mod model;
mod pool;

use std::sync::Arc;
use std::time::Duration;

use axum::{
    extract::DefaultBodyLimit,
    routing::{get, post},
    Router,
};
use tracing::info;
use tracing_subscriber::EnvFilter;

use crate::config::AppConfig;
use crate::docker::DockerManager;
use crate::executor::Executor;
use crate::handler::AppState;
use crate::language::LanguageRegistry;
use crate::pool::ElasticLimiter;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    tracing_subscriber::fmt()
        .with_env_filter(
            EnvFilter::try_from_default_env().unwrap_or_else(|_| EnvFilter::new("info")),
        )
        .init();

    let config = AppConfig::load()?;
    let registry = Arc::new(LanguageRegistry::load()?);
    let limiter = Arc::new(ElasticLimiter::new(config.core_parallel, config.max_parallel));
    info!(
        addr = %config.listen_addr,
        core = config.core_parallel,
        max = config.max_parallel,
        "配置加载完成"
    );

    // 确保工作目录存在（容器内挂载点 /work）
    tokio::fs::create_dir_all(&config.work_dir).await?;

    // 连接宿主 Docker，并清理上次运行残留的子容器
    let docker = DockerManager::connect(&config)?;
    docker.cleanup_stale().await;

    // 空闲槽位回收：每隔 shrink_interval_secs 收缩一个槽位，直至回落到 core_parallel
    {
        let limiter = limiter.clone();
        let interval_secs = config.shrink_interval_secs.max(1);
        tokio::spawn(async move {
            let mut ticker = tokio::time::interval(Duration::from_secs(interval_secs));
            loop {
                ticker.tick().await;
                if limiter.shrink() {
                    info!(limit = limiter.limit(), core = limiter.core(), "空闲判题槽位已回收");
                }
            }
        });
    }

    // 过期任务目录兜底清理：客户端断连/进程异常时正常清理路径不会执行
    {
        let work_dir = config.work_dir.clone();
        let ttl = Duration::from_secs(config.task_ttl_secs.max(60));
        tokio::spawn(async move {
            let mut ticker = tokio::time::interval(Duration::from_secs(60));
            loop {
                ticker.tick().await;
                cleanup::cleanup_expired_tasks(&work_dir, ttl).await;
            }
        });
    }

    let executor = Executor::new(docker, config.clone());
    let state = Arc::new(AppState {
        registry,
        executor,
        limiter,
        api_token: config.api_token.clone(),
        queue_timeout_secs: config.queue_timeout_secs,
    });

    let app = Router::new()
        .route("/health", get(handler::health))
        .route("/languages", get(handler::languages))
        .route("/run", post(handler::run))
        .route("/judge", post(handler::judge))
        // 请求体上限：避免超大 JSON 撑爆内存（axum 默认仅 2MB，判题用例通常更大）
        .layer(DefaultBodyLimit::max(config.max_body_bytes as usize))
        .with_state(state);

    let listener = tokio::net::TcpListener::bind(&config.listen_addr).await?;
    info!(addr = %config.listen_addr, "ai-oj-sandbox 已启动");
    axum::serve(listener, app).await?;
    Ok(())
}
