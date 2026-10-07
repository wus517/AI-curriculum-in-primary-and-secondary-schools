import axios from 'axios'
import { message } from 'antd'
import type { BaseResponse } from '../types/api'

const service = axios.create({
  baseURL: '/api',
  timeout: 60000,
  // 后端使用 session，需携带 cookie
  withCredentials: true,
})

// 响应拦截：统一处理后端 { code, data, message } 结构
// 注意：这里直接返回 res.data（解包），返回类型注解为 any 以兼容 axios 拦截器签名
service.interceptors.response.use(
  (response): any => {
    const res = response.data as BaseResponse<unknown>
    if (res.code === 0) {
      // 直接返回 data，调用方无需再解包
      return res.data
    }
    // 未登录：派发事件由路由层处理跳转，且不弹 toast（避免每次刷新都提示）
    if (res.code === 40100) {
      window.dispatchEvent(new CustomEvent('app:unauthorized'))
      return Promise.reject(new Error(res.message || '未登录'))
    }
    message.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || 'Error'))
  },
  (error) => {
    message.error(error.message || '网络异常')
    return Promise.reject(error)
  },
)

export default service
