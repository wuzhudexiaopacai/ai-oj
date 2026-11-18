mod config;
mod docker;
mod error;
mod executor;
mod handler;
mod language;
mod model;

use std::sync::Arc;

use axum::{
    routing::{get, post},
    Router,
};
use tokio::sync::Semaphore;
use tracing::info;
use tracing_subscriber::EnvFilter;

use crate::config::AppConfig;
use crate::docker::DockerManager;
use crate::executor::Executor;
use crate::handler::AppState;
use crate::language::LanguageRegistry;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    tracing_subscriber::fmt()
        .with_env_filter(
            EnvFilter::try_from_default_env().unwrap_or_else(|_| EnvFilter::new("info")),
        )
        .init();

    let config = AppConfig::load()?;
    let registry = LanguageRegistry::load()?;
    info!(addr = %config.listen_addr, max_parallel = config.max_parallel, "配置加载完成");

    // 确保工作目录存在（容器内挂载点 /work）
    tokio::fs::create_dir_all(&config.work_dir).await?;

    // 连接宿主 Docker，并清理上次运行残留的子容器
    let docker = DockerManager::connect(&config)?;
    docker.cleanup_stale().await;

    let executor = Executor::new(docker, config.clone());
    let state = Arc::new(AppState {
        registry,
        executor,
        semaphore: Semaphore::new(config.max_parallel),
        api_token: config.api_token.clone(),
    });

    let app = Router::new()
        .route("/health", get(handler::health))
        .route("/languages", get(handler::languages))
        .route("/run", post(handler::run))
        .route("/judge", post(handler::judge))
        .with_state(state);

    let listener = tokio::net::TcpListener::bind(&config.listen_addr).await?;
    info!(addr = %config.listen_addr, "ai-oj-sandbox 已启动");
    axum::serve(listener, app).await?;
    Ok(())
}
