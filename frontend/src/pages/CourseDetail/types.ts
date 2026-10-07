import type { CourseDetailVO } from '../../types/api'

export interface TabProps {
  course: CourseDetailVO
  canEdit: boolean
  refresh: () => Promise<void>
}
