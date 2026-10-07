import { Layout, Menu, Dropdown, Space, message, Button } from 'antd'
import { Link, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useMemo } from 'react'
import type { MenuProps } from 'antd'
import { useUser } from '../store/user'
import { userApi } from '../api/user'

const { Header, Content } = Layout

const AppLayout = () => {
  const location = useLocation()
  const navigate = useNavigate()
  const { user, setUser } = useUser()

  // 当前选中的导航项
  const selectedKey = useMemo(() => {
    if (location.pathname.startsWith('/submission')) return '/submissions'
    if (location.pathname.startsWith('/problem')) return '/problem'
    if (location.pathname.startsWith('/course')) return '/course'
    return location.pathname
  }, [location.pathname])

  const handleLogout = async () => {
    try {
      await userApi.logout()
      setUser(null)
      message.success('已退出登录')
      navigate('/login', { replace: true })
    } catch {
      // 拦截器已提示
    }
  }

  const userMenuItems: MenuProps['items'] = [
    {
      key: 'logout',
      label: '退出登录',
      onClick: handleLogout,
    },
  ]

  const navItems: MenuProps['items'] = [
    { key: '/problem', label: <Link to="/problem">题目</Link> },
    { key: '/course', label: <Link to="/course">课程</Link> },
    { key: '/submissions', label: <Link to="/submissions">我的提交</Link> },
  ]

  return (
    <Layout style={{ minHeight: '100%' }}>
      <Header
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0 24px',
        }}
      >
        <Space size="large">
          <div style={{ color: '#fff', fontSize: 18, fontWeight: 600 }}>OJ 在线判题</div>
          <Menu
            theme="dark"
            mode="horizontal"
            selectedKeys={[selectedKey]}
            items={navItems}
            style={{ minWidth: 240, background: 'transparent' }}
          />
        </Space>
        {user ? (
          <Dropdown menu={{ items: userMenuItems }} placement="bottomRight">
            <span style={{ color: '#fff', cursor: 'pointer' }}>
              {user.username}
              {user.role === 'admin' ? '（管理员）' : ''}
            </span>
          </Dropdown>
        ) : (
          <Button type="primary" onClick={() => navigate('/login')}>
            登录
          </Button>
        )}
      </Header>
      <Content style={{ padding: 24 }}>
        <Outlet />
      </Content>
    </Layout>
  )
}

export default AppLayout
