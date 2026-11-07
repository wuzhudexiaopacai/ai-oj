import request from '../request'

/**
 * 操作日志 API（需 log:operation:list 权限）
 */

/** 分页查询操作日志 */
export function fetchOperationLogPage(params) {
  return request.get('/admin/log/operation/page', { params })
}
