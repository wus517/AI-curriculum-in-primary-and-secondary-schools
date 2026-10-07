import request from './request'
import type { SubmissionVO, PageResult } from '../types/api'

export interface SubmissionAddDTO {
  problemId: number
  language: string
  code: string
}

export const submissionApi = {
  /** 提交代码（同步判题），返回提交记录 id */
  add: (data: SubmissionAddDTO) =>
    request.post<unknown, number>('/submission/add', data),

  /** 查询提交详情 */
  get: (id: number) =>
    request.get<unknown, SubmissionVO>('/submission/get', { params: { id } }),

  /** 分页查询当前用户的提交记录 */
  myList: (pageNum = 1, pageSize = 10) =>
    request.get<unknown, PageResult<SubmissionVO>>('/submission/list/my', {
      params: { pageNum, pageSize },
    }),
}
