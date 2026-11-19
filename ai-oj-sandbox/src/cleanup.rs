//! 任务目录过期清理
//!
//! 正常情况下任务目录在判题结束后异步删除；但判题端中途断开、进程被杀等异常路径
//! 不会走清理逻辑，共享卷会无限增长。由后台任务按 `task_ttl_secs` 兜底回收。

use std::time::{Duration, SystemTime};

use tracing::{info, warn};

/// 修改时间是否已超过 TTL
///
/// 修改时间晚于当前时间（时钟回拨）时视为未过期，避免误删正在写入的任务目录。
pub fn is_expired(modified: SystemTime, now: SystemTime, ttl: Duration) -> bool {
    match now.duration_since(modified) {
        Ok(elapsed) => elapsed > ttl,
        Err(_) => false,
    }
}

/// 扫描工作目录并删除过期任务目录，返回删除数量
pub async fn cleanup_expired_tasks(work_dir: &str, ttl: Duration) -> u64 {
    let mut entries = match tokio::fs::read_dir(work_dir).await {
        Ok(entries) => entries,
        Err(e) => {
            warn!(dir = work_dir, error = %e, "任务目录扫描失败");
            return 0;
        }
    };
    let now = SystemTime::now();
    let mut removed = 0u64;
    loop {
        let entry = match entries.next_entry().await {
            Ok(Some(entry)) => entry,
            Ok(None) => break,
            Err(e) => {
                warn!(error = %e, "遍历任务目录失败");
                break;
            }
        };
        let path = entry.path();
        let metadata = match entry.metadata().await {
            Ok(metadata) => metadata,
            Err(_) => continue,
        };
        if !metadata.is_dir() {
            continue;
        }
        let Ok(modified) = metadata.modified() else {
            continue;
        };
        if !is_expired(modified, now, ttl) {
            continue;
        }
        match tokio::fs::remove_dir_all(&path).await {
            Ok(_) => removed += 1,
            Err(e) => warn!(dir = %path.display(), error = %e, "过期任务目录清理失败"),
        }
    }
    if removed > 0 {
        info!(count = removed, "已清理过期任务目录");
    }
    removed
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn expired_only_when_older_than_ttl() {
        let now = SystemTime::now();
        let ttl = Duration::from_secs(300);
        assert!(is_expired(now - Duration::from_secs(301), now, ttl));
        assert!(!is_expired(now - Duration::from_secs(299), now, ttl));
        // 修改时间在未来（时钟回拨）不视为过期
        assert!(!is_expired(now + Duration::from_secs(60), now, ttl));
    }
}
