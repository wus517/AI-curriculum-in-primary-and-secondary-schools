import request from './request'
import type { CourseVO, CourseDetailVO, CourseResourceVO, PageResult, ProblemVO } from '../types/api'

// ==================== 请求 DTO ====================

export interface CourseAddDTO {
  title: string
  description?: string
  difficulty?: string
  tags?: string[]
}

export interface CourseUpdateDTO {
  id: number
  title?: string
  description?: string
  difficulty?: string
  tags?: string[]
  status?: string
}

export interface CourseQueryDTO {
  pageNum?: number
  pageSize?: number
  keyword?: string
  difficulty?: string
  status?: string
}

export interface CourseProblemBindDTO {
  courseId: number
  problemIdList: number[]
}

export interface CourseResourceUpdateDTO {
  id: number
  title?: string
  description?: string
  sortOrder?: number
}

// ==================== API ====================

export const courseApi = {
  // ---- 课程基本信息 ----
  add: (data: CourseAddDTO) =>
    request.post<unknown, number>('/course/add', data),

  update: (data: CourseUpdateDTO) =>
    request.post<unknown, boolean>('/course/update', data),

  delete: (id: number) =>
    request.post<unknown, boolean>('/course/delete', null, { params: { id } }),

  list: (params: CourseQueryDTO) =>
    request.get<unknown, PageResult<CourseVO>>('/course/list', { params }),

  get: (id: number) =>
    request.get<unknown, CourseDetailVO>('/course/get', { params: { id } }),

  // ---- 文件上传 ----
  uploadCover: (courseId: number, file: File) => {
    const formData = new FormData()
    formData.append('courseId', String(courseId))
    formData.append('file', file)
    return request.post<unknown, string>('/course/upload/cover', formData)
  },

  uploadResource: (
    courseId: number,
    resourceType: 'video' | 'ppt',
    file: File,
    extra?: { title?: string; description?: string; sortOrder?: number },
  ) => {
    const formData = new FormData()
    formData.append('courseId', String(courseId))
    formData.append('resourceType', resourceType)
    formData.append('file', file)
    if (extra?.title) formData.append('title', extra.title)
    if (extra?.description) formData.append('description', extra.description)
    if (extra?.sortOrder !== undefined) formData.append('sortOrder', String(extra.sortOrder))
    return request.post<unknown, CourseResourceVO>('/course/upload/' + resourceType, formData)
  },

  // ---- 资源管理 ----
  deleteResource: (id: number) =>
    request.post<unknown, boolean>('/course/resource/delete', null, { params: { id } }),

  updateResource: (data: CourseResourceUpdateDTO) =>
    request.post<unknown, boolean>('/course/resource/update', data),

  listResources: (courseId: number, type?: string) =>
    request.get<unknown, CourseResourceVO[]>('/course/resource/list', { params: { courseId, type } }),

  // ---- 题目绑定 ----
  bindProblems: (data: CourseProblemBindDTO) =>
    request.post<unknown, boolean>('/course/problem/bind', data),

  unbindProblem: (courseId: number, problemId: number) =>
    request.post<unknown, boolean>('/course/problem/unbind', null, { params: { courseId, problemId } }),

  listProblems: (courseId: number) =>
    request.get<unknown, ProblemVO[]>('/course/problem/list', { params: { courseId } }),
}
