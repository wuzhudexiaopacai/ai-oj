import request from '../request'

/** 认证服务（ai-oj-auth） */
export function getCaptcha() {
  return request.get('/auth/captcha')
}

export function login(data) {
  return request.post('/auth/login', data)
}

export function register(data) {
  return request.post('/auth/register', data)
}

export function getUserInfo() {
  // 注意：网关 StripPrefix 后 auth 服务暴露的是 /user/info（UserController），
  // 不是 /auth/user/info
  return request.get('/user/info')
}

export function logout() {
  return request.post('/auth/logout')
}

export function updateProfile(data) {
  // 同 getUserInfo：资料接口在 UserController 上，网关 StripPrefix 后是 /user/profile
  return request.put('/user/profile', data)
}

export function updatePassword(data) {
  return request.put('/user/password', data)
}
