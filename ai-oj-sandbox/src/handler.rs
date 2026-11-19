use std::sync::Arc;
use std::time::Duration;

use axum::{
    extract::State,
    http::HeaderMap,
    Json,
};
use serde_json::{json, Value};

use crate::error::SandboxError;
use crate::executor::Executor;
use crate::language::LanguageRegistry;
use crate::model::{JudgeRequest, JudgeResponse, RunRequest, RunResponse};
use crate::pool::{ElasticLimiter, Permit};

/// 访问令牌请求头
const TOKEN_HEADER: &str = "x-sandbox-token";

/// 共享状态：语言注册表、执行器、弹性并发槽位、访问令牌
pub struct AppState {
    pub registry: Arc<LanguageRegistry>,
    pub executor: Executor,
    /// 弹性判题槽位：常驻 core、峰值 max，空闲自动回收
    pub limiter: Arc<ElasticLimiter>,
    /// 可选访问令牌；配置后判题/运行接口必须携带 X-Sandbox-Token
    pub api_token: Option<String>,
    /// 排队等待槽位超时（秒），超时返回 503
    pub queue_timeout_secs: u64,
}

impl AppState {
    /// 校验访问令牌：未配置时放行（内网单机部署）
    fn authorize(&self, headers: &HeaderMap) -> Result<(), SandboxError> {
        let Some(expected) = self.api_token.as_deref() else {
            return Ok(());
        };
        let provided = headers
            .get(TOKEN_HEADER)
            .and_then(|value| value.to_str().ok());
        if provided == Some(expected) {
            Ok(())
        } else {
            Err(SandboxError::Unauthorized)
        }
    }

    /// 获取判题槽位：超过 queue_timeout_secs 仍未排到则返回 503，避免请求悬挂
    async fn acquire_permit(&self) -> Result<Permit, SandboxError> {
        match tokio::time::timeout(
            Duration::from_secs(self.queue_timeout_secs),
            self.limiter.acquire(),
        )
        .await
        {
            Ok(permit) => Ok(permit),
            Err(_) => Err(SandboxError::Busy),
        }
    }
}

/// 健康检查：同时暴露槽位占用情况，便于判题端/运维观察扩容状态
pub async fn health(State(state): State<Arc<AppState>>) -> Json<Value> {
    Json(json!({
        "status": "ok",
        "active": state.limiter.active(),
        "limit": state.limiter.limit(),
        "core": state.limiter.core(),
        "max": state.limiter.max(),
    }))
}

pub async fn languages(State(state): State<Arc<AppState>>) -> Json<Value> {
    Json(json!({ "languages": state.registry.list() }))
}

pub async fn run(
    State(state): State<Arc<AppState>>,
    headers: HeaderMap,
    Json(req): Json<RunRequest>,
) -> Result<Json<RunResponse>, SandboxError> {
    state.authorize(&headers)?;
    // 先校验语言，避免无效请求占用并发额度
    state.registry.get(&req.language)?;
    let permit = state.acquire_permit().await?;
    let executor = state.executor.clone();
    let registry = state.registry.clone();
    // 槽位凭证随任务一起移入独立任务：判题端中途断开也不会中断执行与容器清理
    let task = tokio::spawn(async move {
        let _permit = permit;
        executor.run(&registry, req).await
    });
    let resp = task
        .await
        .map_err(|e| SandboxError::Internal(format!("运行任务异常终止: {e}")))??;
    Ok(Json(resp))
}

pub async fn judge(
    State(state): State<Arc<AppState>>,
    headers: HeaderMap,
    Json(req): Json<JudgeRequest>,
) -> Result<Json<JudgeResponse>, SandboxError> {
    state.authorize(&headers)?;
    state.registry.get(&req.language)?;
    let permit = state.acquire_permit().await?;
    let executor = state.executor.clone();
    let registry = state.registry.clone();
    // 槽位凭证随任务一起移入独立任务：判题端超时断开时仍会跑完并清理容器/任务目录，
    // 否则请求 future 被取消会导致子容器与任务目录泄漏
    let task = tokio::spawn(async move {
        let _permit = permit;
        executor.judge(&registry, req).await
    });
    let resp = task
        .await
        .map_err(|e| SandboxError::Internal(format!("判题任务异常终止: {e}")))??;
    Ok(Json(resp))
}
