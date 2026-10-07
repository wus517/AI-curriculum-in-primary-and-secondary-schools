import request from './request'
import type { UserVO } from '../types/api'

export const userApi = {
  /** 注册，返回新用户 id */
  register: (data: { username: string; password: string }) =>
    request.post<unknown, number>('/user/register', data),

  /** 登录，返回当前用户信息（脱敏） */
  login: (data: { username: string; password: string }) =>
    request.post<unknown, UserVO>('/user/login', data),

  /** 登出 */
  logout: () => request.get<unknown, boolean>('/user/logout'),

  /** 获取当前登录用户 */
  current: () => request.get<unknown, UserVO>('/user/current'),
}
