//! 判题并发槽位（弹性线程池语义）
//!
//! `core_parallel` 为常驻槽位，任何时候都可用；并发打满时按需扩容，最多到 `max_parallel`；
//! 空闲后由后台任务按固定间隔逐级回收到 core，避免长期占用宿主机资源。
//! 与固定 `Semaphore` 的区别：容量随负载自动伸缩，而不是启动时写死。

use std::sync::atomic::{AtomicUsize, Ordering};
use std::sync::Arc;

use tokio::sync::Notify;
use tracing::debug;

/// 弹性并发限制器：limit 在 [core, max] 区间内按负载伸缩
pub struct ElasticLimiter {
    core: usize,
    max: usize,
    /// 当前允许的并发上限
    limit: AtomicUsize,
    /// 当前已占用的槽位
    active: AtomicUsize,
    /// 槽位释放/扩容通知
    notify: Notify,
}

impl ElasticLimiter {
    /// 创建限制器：core 自动收敛到 [1, max] 区间
    pub fn new(core: usize, max: usize) -> Self {
        let max = max.max(1);
        let core = core.clamp(1, max);
        Self {
            core,
            max,
            limit: AtomicUsize::new(core),
            active: AtomicUsize::new(0),
            notify: Notify::new(),
        }
    }

    pub fn core(&self) -> usize {
        self.core
    }

    pub fn max(&self) -> usize {
        self.max
    }

    pub fn limit(&self) -> usize {
        self.limit.load(Ordering::Relaxed)
    }

    pub fn active(&self) -> usize {
        self.active.load(Ordering::Relaxed)
    }

    /// 获取一个槽位：有空闲直接占用；已满且未达上限则先扩容再占用；达到上限则等待释放
    pub async fn acquire(self: &Arc<Self>) -> Permit {
        loop {
            let active = self.active.load(Ordering::Acquire);
            let limit = self.limit.load(Ordering::Acquire);
            if active < limit {
                if self
                    .active
                    .compare_exchange_weak(active, active + 1, Ordering::AcqRel, Ordering::Acquire)
                    .is_ok()
                {
                    return Permit {
                        limiter: self.clone(),
                    };
                }
                continue;
            }
            // 满载：线程池扩容语义——先拉起一个新槽位，再重新竞争
            if limit < self.max {
                let _ = self.limit.compare_exchange(
                    limit,
                    limit + 1,
                    Ordering::AcqRel,
                    Ordering::Acquire,
                );
                debug!(limit = self.limit(), max = self.max, "判题槽位已扩容");
                continue;
            }
            self.notify.notified().await;
        }
    }

    /// 空闲回收：占用数低于当前上限时收缩一个槽位，返回是否发生收缩
    pub fn shrink(&self) -> bool {
        let active = self.active.load(Ordering::Acquire);
        loop {
            let limit = self.limit.load(Ordering::Acquire);
            if limit <= self.core || active >= limit {
                return false;
            }
            if self
                .limit
                .compare_exchange(limit, limit - 1, Ordering::AcqRel, Ordering::Acquire)
                .is_ok()
            {
                return true;
            }
        }
    }

    fn release(&self) {
        self.active.fetch_sub(1, Ordering::AcqRel);
        self.notify.notify_one();
    }
}

/// 槽位占用凭证：drop 时自动归还
pub struct Permit {
    limiter: Arc<ElasticLimiter>,
}

impl Drop for Permit {
    fn drop(&mut self) {
        self.limiter.release();
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::time::Duration;

    #[tokio::test]
    async fn should_not_exceed_core_when_idle() {
        let limiter = Arc::new(ElasticLimiter::new(2, 8));
        let p1 = limiter.acquire().await;
        let p2 = limiter.acquire().await;

        assert_eq!(limiter.active(), 2);
        // 无竞争时不扩容
        assert_eq!(limiter.limit(), 2);
        drop(p1);
        drop(p2);
        assert_eq!(limiter.active(), 0);
    }

    #[tokio::test]
    async fn should_expand_to_max_under_contention() {
        let limiter = Arc::new(ElasticLimiter::new(1, 4));
        let mut permits = Vec::new();
        for _ in 0..4 {
            permits.push(limiter.acquire().await);
        }
        assert_eq!(limiter.active(), 4);
        assert_eq!(limiter.limit(), 4);

        // 达到上限后继续申请必须等待，不能超卖
        let pending = tokio::time::timeout(Duration::from_millis(80), limiter.acquire()).await;
        assert!(pending.is_err(), "达到 max 后新请求应排队");

        drop(permits.pop());
        let permit = tokio::time::timeout(Duration::from_millis(200), limiter.acquire())
            .await
            .expect("释放槽位后应能立即获取");
        assert_eq!(limiter.active(), 4);
        drop(permit);
    }

    #[tokio::test]
    async fn shrink_should_return_to_core_only_when_idle() {
        let limiter = Arc::new(ElasticLimiter::new(1, 4));
        let active = limiter.acquire().await;
        // 扩容到 4
        let extras: Vec<_> = {
            let mut v = Vec::new();
            for _ in 0..3 {
                v.push(limiter.acquire().await);
            }
            v
        };
        assert_eq!(limiter.limit(), 4);

        // 仍有占用时不回收
        assert!(!limiter.shrink());
        drop(extras);
        // 全部空闲后逐级回收到 core
        while limiter.limit() > 1 {
            assert!(limiter.shrink());
        }
        assert!(!limiter.shrink());
        assert_eq!(limiter.limit(), 1);
        drop(active);
        assert_eq!(limiter.active(), 0);
    }

    #[test]
    fn core_should_be_clamped_into_range() {
        let limiter = ElasticLimiter::new(0, 4);
        assert_eq!(limiter.core(), 1);
        let limiter = ElasticLimiter::new(10, 4);
        assert_eq!(limiter.core(), 4);
        assert_eq!(limiter.limit(), 4);
    }
}
