// 与后端 VO / IPage 对齐的类型定义

export interface BaseResponse<T> {
  code: number
  data: T
  message: string
}

export interface UserVO {
  id: number
  username: string
  role: string
  createTime?: string
}

export interface ProblemVO {
  id: number
  title: string
  content?: string
  inputDesc?: string
  outputDesc?: string
  sampleInput?: string
  sampleOutput?: string
  timeLimit: number
  memoryLimit: number
  difficulty?: number
  tags?: string[]
  createUserId?: number
  createTime?: string
}

export interface SubmissionVO {
  id: number
  userId: number
  problemId: number
  language: string
  code: string
  status: string
  timeUsed: number
  memoryUsed: number
  errorMessage?: string
  judgeInfo?: string
  createTime?: string
}

// MyBatis-Plus IPage 序列化结构
export interface PageResult<T> {
  records: T[]
  total: number
  size?: number
  current?: number
  pages?: number
}

// ==================== 课程模块 ====================

export interface CourseVO {
  id: number
  title: string
  description?: string
  coverUrl?: string
  difficulty?: string
  tags?: string[]
  teacherId?: number
  teacherName?: string
  status?: string
  createTime?: string
  updateTime?: string
}

export interface CourseResourceVO {
  id: number
  courseId: number
  resourceType: string
  title?: string
  description?: string
  fileName?: string
  fileUrl?: string
  fileSize?: number
  mimeType?: string
  sortOrder?: number
  createTime?: string
}

export interface CourseDetailVO extends CourseVO {
  videoList?: CourseResourceVO[]
  pptList?: CourseResourceVO[]
  problemList?: ProblemVO[]
}
