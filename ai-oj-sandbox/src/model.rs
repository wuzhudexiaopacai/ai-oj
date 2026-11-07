use serde::{Deserialize, Serialize};

/// 判题状态枚举，序列化为 SCREAMING_SNAKE_CASE 字符串
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
pub enum Status {
    #[serde(rename = "ACCEPTED")]
    Accepted,
    #[serde(rename = "WRONG_ANSWER")]
    WrongAnswer,
    #[serde(rename = "PRESENTATION_ERROR")]
    PresentationError,
    #[serde(rename = "TIME_LIMIT_EXCEEDED")]
    TimeLimitExceeded,
    #[serde(rename = "MEMORY_LIMIT_EXCEEDED")]
    MemoryLimitExceeded,
    #[serde(rename = "RUNTIME_ERROR")]
    RuntimeError,
    #[serde(rename = "COMPILE_ERROR")]
    CompileError,
    #[serde(rename = "OUTPUT_LIMIT_EXCEEDED")]
    OutputLimitExceeded,
    #[serde(rename = "SYSTEM_ERROR")]
    SystemError,
}

impl Status {
    /// 状态严重级别，overall_status 取所有 case 中最差（数值大）者
    /// 优先级：SE > CE > RE > TLE > MLE > OLE > WA > PE > AC
    pub fn severity(self) -> u8 {
        match self {
            Status::Accepted => 0,
            Status::PresentationError => 1,
            Status::WrongAnswer => 2,
            Status::OutputLimitExceeded => 3,
            Status::MemoryLimitExceeded => 4,
            Status::TimeLimitExceeded => 5,
            Status::RuntimeError => 6,
            Status::CompileError => 7,
            Status::SystemError => 8,
        }
    }
}

// ---------- /run ----------

#[derive(Debug, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct RunRequest {
    pub language: String,
    pub code: String,
    #[serde(default)]
    pub stdin: String,
    pub time_limit_ms: Option<u64>,
    pub memory_limit_mb: Option<u64>,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "snake_case")]
pub struct RunResponse {
    pub status: Status,
    pub stdout: String,
    pub stderr: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub compile_log: Option<String>,
    pub time_ms: u64,
    pub memory_kb: u64,
}

// ---------- /judge ----------

#[derive(Debug, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct JudgeCase {
    pub seq: u32,
    pub input: String,
    pub expected_output: String,
    pub score: u32,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct SpjSpec {
    pub language: String,
    pub code: String,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "snake_case")]
pub struct JudgeRequest {
    pub submission_id: u64,
    pub language: String,
    pub code: String,
    pub time_limit_ms: u64,
    pub memory_limit_mb: u64,
    /// 栈空间限制（MB），缺省时按 256MB
    #[serde(default)]
    pub stack_limit_mb: Option<u64>,
    /// 是否 OI 计分：true 按用例分值累加，false 为 ACM 全对才计分
    #[serde(default)]
    pub io_score: Option<bool>,
    pub cases: Vec<JudgeCase>,
    pub spj: Option<SpjSpec>,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "snake_case")]
pub struct CompileInfo {
    pub success: bool,
    pub log: String,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "snake_case")]
pub struct CaseResult {
    pub seq: u32,
    pub status: Status,
    pub time_ms: u64,
    pub memory_kb: u64,
    pub score: u32,
    /// 截断到前 512 字符的实际输出预览
    pub output_preview: String,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "snake_case")]
pub struct JudgeResponse {
    pub submission_id: u64,
    pub compile: CompileInfo,
    pub overall_status: Status,
    pub total_score: u32,
    pub cases: Vec<CaseResult>,
}
