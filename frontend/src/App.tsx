import { useEffect, type ReactNode } from 'react'
import { Routes, Route, Navigate, useNavigate, useLocation } from 'react-router-dom'
import { ConfigProvider, Spin } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import { UserProvider, useUser } from './store/user'
import { userApi } from './api/user'
import AppLayout from './components/AppLayout'
import LoginPage from './pages/Login'
import RegisterPage from './pages/Register'
import ProblemListPage from './pages/ProblemList'
import ProblemDetailPage from './pages/ProblemDetail'
import SubmissionListPage from './pages/SubmissionList'
import SubmissionDetailPage from './pages/SubmissionDetail'
import CourseListPage from './pages/CourseList'
import CourseDetailPage from './pages/CourseDetail'

/** 需要登录的路由守卫 */
const RequireAuth = ({ children }: { children: ReactNode }) => {
  const { user } = useUser()
  const location = useLocation()
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return <>{children}</>
}

const AppInner = () => {
  const { setUser, setInitializing, initializing } = useUser()
  const navigate = useNavigate()

  // 首次加载拉取登录态
  useEffect(() => {
    userApi
      .current()
      .then((u) => setUser(u))
      .catch(() => setUser(null))
      .finally(() => setInitializing(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // 监听未登录事件，跳转到登录页
  useEffect(() => {
    const handler = () => {
      setUser(null)
      navigate('/login', { replace: true })
    }
    window.addEventListener('app:unauthorized', handler)
    return () => window.removeEventListener('app:unauthorized', handler)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  if (initializing) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <Spin size="large" />
      </div>
    )
  }

  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route
        path="/"
        element={
          <RequireAuth>
            <AppLayout />
          </RequireAuth>
        }
      >
        <Route index element={<Navigate to="/problem" replace />} />
        <Route path="problem" element={<ProblemListPage />} />
        <Route path="problem/:id" element={<ProblemDetailPage />} />
        <Route path="submissions" element={<SubmissionListPage />} />
        <Route path="submission/:id" element={<SubmissionDetailPage />} />
        <Route path="course" element={<CourseListPage />} />
        <Route path="course/:id" element={<CourseDetailPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

const App = () => (
  <ConfigProvider locale={zhCN} theme={{ token: { colorPrimary: '#1677ff' } }}>
    <UserProvider>
      <AppInner />
    </UserProvider>
  </ConfigProvider>
)

export default App
