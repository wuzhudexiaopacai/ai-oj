use std::sync::Arc;

use axum::{extract::State, Json};
use serde_json::{json, Value};
use tokio::sync::Semaphore;

use crate::error::SandboxError;
use crate::executor::Executor;
use crate::language::LanguageRegistry;
use crate::model::{JudgeRequest, JudgeResponse, RunRequest, RunResponse};

/// 共享状态：语言注册表、执行器、并发信号量
pub struct AppState {
    pub registry: LanguageRegistry,
    pub executor: Executor,
    pub semaphore: Semaphore,
}

pub async fn health() -> Json<Value> {
    Json(json!({"status": "ok"}))
}

pub async fn languages(State(state): State<Arc<AppState>>) -> Json<Value> {
    Json(json!({ "languages": state.registry.list() }))
}

pub async fn run(
    State(state): State<Arc<AppState>>,
    Json(req): Json<RunRequest>,
) -> Result<Json<RunResponse>, SandboxError> {
    // 先校验语言，避免无效请求占用并发额度
    state.registry.get(&req.language)?;
    // 并发控制：超出 max_parallel 的请求在此排队
    let _permit = state
        .semaphore
        .acquire()
        .await
        .map_err(|_| SandboxError::Internal("并发信号量已关闭".into()))?;
    let resp = state.executor.run(&state.registry, req).await?;
    Ok(Json(resp))
}

pub async fn judge(
    State(state): State<Arc<AppState>>,
    Json(req): Json<JudgeRequest>,
) -> Result<Json<JudgeResponse>, SandboxError> {
    state.registry.get(&req.language)?;
    let _permit = state
        .semaphore
        .acquire()
        .await
        .map_err(|_| SandboxError::Internal("并发信号量已关闭".into()))?;
    let resp = state.executor.judge(&state.registry, req).await?;
    Ok(Json(resp))
}
