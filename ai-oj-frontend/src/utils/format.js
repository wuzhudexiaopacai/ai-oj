import dayjs from 'dayjs'

export function formatTime(value, pattern = 'YYYY-MM-DD HH:mm:ss') {
  if (!value) return '-'
  return dayjs(value).format(pattern)
}

export function formatFromNow(value) {
  if (!value) return '-'
  const diff = dayjs().diff(dayjs(value), 'second')
  if (diff < 60) return `${diff} 秒前`
  if (diff < 3600) return `${Math.floor(diff / 60)} 分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)} 小时前`
  return dayjs(value).format('YYYY-MM-DD')
}

/** 通过率展示，如 62.5% */
export function formatRate(accept, total) {
  if (!total) return '-'
  return `${((accept / total) * 100).toFixed(1)}%`
}

/** 内存 KB → 友好展示 */
export function formatMemory(kb) {
  if (kb == null) return '-'
  if (kb >= 1024) return `${(kb / 1024).toFixed(1)} MB`
  return `${kb} KB`
}
