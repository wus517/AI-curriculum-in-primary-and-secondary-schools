import { useEffect } from 'react'
import { Card, Form, Input, Button, Typography, Space } from 'antd'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { userApi } from '../../api/user'
import { useUser } from '../../store/user'

const { Title } = Typography

interface LocationState {
  from?: string
}

const LoginPage = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const { user, setUser } = useUser()

  // 已登录则跳走
  useEffect(() => {
    if (user) {
      const from = (location.state as LocationState)?.from || '/problem'
      navigate(from, { replace: true })
    }
  }, [user, location.state, navigate])

  const onFinish = async (values: { username: string; password: string }) => {
    const u = await userApi.login(values)
    setUser(u)
    navigate('/problem', { replace: true })
  }

  return (
    <div
      style={{
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        minHeight: '100vh',
        background: '#f5f5f5',
      }}
    >
      <Card style={{ width: 380 }}>
        <Title level={3} style={{ textAlign: 'center', marginBottom: 24 }}>
          OJ 登录
        </Title>
        <Form layout="vertical" onFinish={onFinish} initialValues={{ username: 'admin', password: '123456' }}>
          <Form.Item label="用户名" name="username" rules={[{ required: true, message: '请输入用户名' }]}>
            <Input placeholder="请输入用户名" />
          </Form.Item>
          <Form.Item label="密码" name="password" rules={[{ required: true, message: '请输入密码' }]}>
            <Input.Password placeholder="请输入密码" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block>
              登录
            </Button>
          </Form.Item>
        </Form>
        <Space style={{ width: '100%', justifyContent: 'center' }}>
          没有账号？<Link to="/register">去注册</Link>
        </Space>
      </Card>
    </div>
  )
}

export default LoginPage
