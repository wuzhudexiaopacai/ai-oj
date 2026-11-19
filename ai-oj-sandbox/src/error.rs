use axum::{
    http::StatusCode,
    response::{IntoResponse, Response},
    Json,
};
use serde_json::json;

/// 沙箱统一错误类型
#[derive(Debug, thiserror::Error)]
pub enum SandboxError {
    #[error("配置错误: {0}")]
    Config(String),
    #[error("不支持的语言: {0}")]
    UnsupportedLanguage(String),
    #[error("未授权：访问令牌缺失或不匹配")]
    Unauthorized,
    #[error("判题队列繁忙：排队等待槽位超时")]
    Busy,
    #[error("Docker 调用失败: {0}")]
    Docker(#[from] bollard::errors::Error),
    #[error("IO 错误: {0}")]
    Io(#[from] std::io::Error),
    #[error("内部错误: {0}")]
    Internal(String),
}

impl IntoResponse for SandboxError {
    fn into_response(self) -> Response {
        // 参数类错误返回 400，未授权返回 401，队列繁忙返回 503（判题端换机/退避重试），其余返回 500
        let code = match &self {
            SandboxError::UnsupportedLanguage(_) => StatusCode::BAD_REQUEST,
            SandboxError::Unauthorized => StatusCode::UNAUTHORIZED,
            SandboxError::Busy => StatusCode::SERVICE_UNAVAILABLE,
            _ => StatusCode::INTERNAL_SERVER_ERROR,
        };
        tracing::error!(error = %self, "请求处理失败");
        (
            code,
            Json(json!({
                "status": "SYSTEM_ERROR",
                "error": self.to_string(),
            })),
        )
            .into_response()
    }
}
