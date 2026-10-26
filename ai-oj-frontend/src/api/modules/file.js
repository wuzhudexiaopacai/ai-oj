import request from '../request'

/** 文件服务（ai-oj-file） */

/**
 * 上传头像（仅图片，最大 2MB）
 *
 * @param {File} file 图片文件
 * @returns {Promise<string>} 头像访问地址，如 /files/avatar/xxx.png
 */
export function uploadAvatar(file) {
  const form = new FormData()
  form.append('file', file)
  return request.post('/file/avatar', form)
}
