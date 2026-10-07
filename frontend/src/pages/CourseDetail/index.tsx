import { useEffect, useState, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Typography, Spin, Tabs, message } from 'antd'
import { courseApi } from '../../api/course'
import type { CourseDetailVO } from '../../types/api'
import { useUser } from '../../store/user'
import { getCourseStatusMeta, getCourseDifficultyMeta } from '../../constants'
import type { TabProps } from './types'
import InfoTab from './InfoTab'
import ResourceTab from './ResourceTab'
import ProblemTab from './ProblemTab'

const { Title, Text } = Typography

const CourseDetailPage = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useUser()
  const courseId = Number(id)
  const [course, setCourse] = useState<CourseDetailVO | null>(null)
  const [loading, setLoading] = useState(true)

  const refresh = useCallback(async () => {
    if (!courseId) return
    setLoading(true)
    try {
      const data = await courseApi.get(courseId)
      setCourse(data)
    } catch {
      // 拦截器已提示
    } finally {
      setLoading(false)
    }
  }, [courseId])

  useEffect(() => {
    refresh()
  }, [refresh])

  if (loading && !course) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', padding: 80 }}>
        <Spin size="large" />
      </div>
    )
  }

  if (!course) {
    return (
      <div style={{ background: '#fff', padding: 24, borderRadius: 8 }}>
        <Text type="secondary">课程不存在或已删除。</Text>
      </div>
    )
  }

  const canEdit = !!user && (user.role === 'admin' || user.id === course.teacherId)

  const tabProps: TabProps = { course, canEdit, refresh }

  return (
    <div style={{ background: '#fff', padding: 24, borderRadius: 8 }}>
      <Title level={4} style={{ marginBottom: 4 }}>{course.title}</Title>
      <div style={{ marginBottom: 16, color: '#888' }}>
        教师：{course.teacherName ?? '-'}　|　难度：
        {getCourseDifficultyMeta(course.difficulty).text}　|　状态：
        {getCourseStatusMeta(course.status).text}　|　创建时间：
        {course.createTime ?? '-'}
      </div>

      <Tabs
        defaultActiveKey="info"
        items={[
          { key: 'info', label: '基本信息', children: <InfoTab {...tabProps} /> },
          {
            key: 'video',
            label: `视频 (${course.videoList?.length ?? 0})`,
            children: <ResourceTab {...tabProps} type="video" />,
          },
          {
            key: 'ppt',
            label: `课件 (${course.pptList?.length ?? 0})`,
            children: <ResourceTab {...tabProps} type="ppt" />,
          },
          {
            key: 'problem',
            label: `题目 (${course.problemList?.length ?? 0})`,
            children: <ProblemTab {...tabProps} />,
          },
        ]}
      />
    </div>
  )
}

export default CourseDetailPage
