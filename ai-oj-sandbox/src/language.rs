use serde::{Deserialize, Serialize};
use std::collections::HashMap;

use crate::error::SandboxError;

/// 单个语言的编译/运行配置
#[derive(Debug, Clone, Deserialize, Serialize)]
#[serde(rename_all = "snake_case")]
pub struct LanguageConfig {
    pub name: String,
    pub image: String,
    pub source_file: String,
    /// 编译命令，None 表示解释型语言
    pub compile_cmd: Option<Vec<String>>,
    pub run_cmd: Vec<String>,
    #[serde(default = "default_factor")]
    pub time_factor: f64,
    #[serde(default = "default_factor")]
    pub memory_factor: f64,
}

fn default_factor() -> f64 {
    1.0
}

#[derive(Debug, Deserialize)]
struct LanguagesFile {
    languages: Vec<LanguageConfig>,
}

/// 语言注册表，按 name 索引
#[derive(Debug, Clone)]
pub struct LanguageRegistry {
    map: HashMap<String, LanguageConfig>,
}

impl LanguageRegistry {
    pub fn load() -> Result<Self, SandboxError> {
        let path =
            std::env::var("SANDBOX_LANGUAGES").unwrap_or_else(|_| "languages.toml".to_string());
        let content = std::fs::read_to_string(&path)
            .map_err(|e| SandboxError::Config(format!("读取 {path} 失败: {e}")))?;
        let file: LanguagesFile = toml::from_str(&content)
            .map_err(|e| SandboxError::Config(format!("解析 {path} 失败: {e}")))?;
        let map = file
            .languages
            .into_iter()
            .map(|l| (l.name.clone(), l))
            .collect();
        Ok(Self { map })
    }

    pub fn get(&self, name: &str) -> Result<&LanguageConfig, SandboxError> {
        self.map
            .get(name)
            .ok_or_else(|| SandboxError::UnsupportedLanguage(name.to_string()))
    }

    pub fn list(&self) -> Vec<&LanguageConfig> {
        let mut v: Vec<_> = self.map.values().collect();
        v.sort_by(|a, b| a.name.cmp(&b.name));
        v
    }
}
