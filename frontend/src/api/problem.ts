import request from './request'
import type { ProblemVO, PageResult } from '../types/api'

export const problemApi = {
  /** 分页查询题目列表 */
  list: (pageNum = 1, pageSize = 10) =>
    request.get<unknown, PageResult<ProblemVO>>('/problem/list', {
      params: { pageNum, pageSize },
    }),

  /** 查询题目详情 */
  get: (id: number) =>
    request.get<unknown, ProblemVO>('/problem/get', { params: { id } }),
}
