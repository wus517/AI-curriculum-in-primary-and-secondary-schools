import { useEffect, useState } from 'react'
import { Table, Tag, Typography, Space, Input, Select, Button, Modal, Form, message, Popconfirm } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { Link, useNavigate } from 'react-router-dom'
import { courseApi } from '../../api/course'
import type { CourseAddDTO, CourseQueryDTO } from '../../api/course'
import type { CourseVO } from '../../types/api'
import { getCourseStatusMeta, getCourseDifficultyMeta } from '../../constants'
import { useUser } from '../../store/user'

const { Title } = Typography

const CourseListPage = () => {
  const navigate = useNavigate()
  const { user } = useUser()
  const [data, setData] = useState<CourseVO[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState({ current: 1, pageSize: 10 })
  const [query, setQuery] = useState<CourseQueryDTO>({ pageNum: 1, pageSize: 10 })
  const [createOpen, setCreateOpen] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [form] = Form.useForm<CourseAddDTO>()

  const fetchData = async (params: CourseQueryDTO) => {
    setLoading(true)
    try {
      const res = await courseApi.list(params)
      setData(res.records)
      setTotal(res.total)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData({ pageNum: 1, pageSize: 10 })
  }, [])

  const onSearch = () => {
    const params = { ...query, pageNum: 1, pageSize: page.pageSize }
    setQuery(params)
    setPage({ current: 1, pageSize: page.pageSize })
    fetchData(params)
  }

  const handleCreate = async () => {
    const values = await form.validateFields()
    setSubmitting(true)
    try {
      const id = await courseApi.add(values)
      message.success('创建成功')
      setCreateOpen(false)
      form.resetFields()
      navigate(`/course/${id}`)
    } finally {
      setSubmitting(false)
    }
  }

  const columns: ColumnsType<CourseVO> = [
    { title: '#', dataIndex: 'id', width: 70 },
    {
      title: '封面',
      dataIndex: 'coverUrl',
      width: 90,
      render: (url?: string) =>
        url ? (
          <img src={url} alt="cover" style={{ width: 64, height: 40, objectFit: 'cover', borderRadius: 4 }} />
        ) : (
          <div style={{ width: 64, height: 40, background: '#f0f0f0', borderRadius: 4, lineHeight: '40px', textAlign: 'center', color: '#bbb', fontSize: 12 }}>
            无
          </div>
        ),
    },
    {
      title: '标题',
      dataIndex: 'title',
      render: (_, r) => <Link to={`/course/${r.id}`}>{r.title}</Link>,
    },
    {
      title: '难度',
      dataIndex: 'difficulty',
      width: 90,
      render: (v?: string) => {
        const m = getCourseDifficultyMeta(v)
        return <Tag color={m.color}>{m.text}</Tag>
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 90,
      render: (v?: string) => {
        const m = getCourseStatusMeta(v)
        return <Tag color={m.color}>{m.text}</Tag>
      },
    },
    {
      title: '标签',
      dataIndex: 'tags',
      render: (tags?: string[]) => (tags ?? []).map((t) => <Tag key={t}>{t}</Tag>),
    },
    {
      title: '教师',
      dataIndex: 'teacherName',
      width: 100,
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      width: 170,
    },
  ]

  return (
    <div style={{ background: '#fff', padding: 24, borderRadius: 8 }}>
      <Space style={{ justifyContent: 'space-between', width: '100%', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>课程列表</Title>
        <Button type="primary" onClick={() => setCreateOpen(true)}>创建课程</Button>
      </Space>

      <Space style={{ marginBottom: 16 }} wrap>
        <Input.Search
          placeholder="搜索课程标题/描述"
          allowClear
          style={{ width: 240 }}
          value={query.keyword}
          onChange={(e) => setQuery({ ...query, keyword: e.target.value })}
          onSearch={onSearch}
        />
        <Select
          placeholder="难度"
          allowClear
          style={{ width: 120 }}
          value={query.difficulty}
          onChange={(v) => setQuery({ ...query, difficulty: v })}
          options={[
            { value: 'beginner', label: '入门' },
            { value: 'intermediate', label: '进阶' },
            { value: 'advanced', label: '高级' },
          ]}
        />
        <Select
          placeholder="状态"
          allowClear
          style={{ width: 120 }}
          value={query.status}
          onChange={(v) => setQuery({ ...query, status: v })}
          options={[
            { value: 'draft', label: '草稿' },
            { value: 'published', label: '已发布' },
            { value: 'offline', label: '已下架' },
          ]}
        />
        <Button onClick={onSearch}>查询</Button>
      </Space>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={data}
        loading={loading}
        pagination={{
          current: page.current,
          pageSize: page.pageSize,
          total,
          showSizeChanger: true,
          onChange: (current, pageSize) => {
            const params = { ...query, pageNum: current, pageSize }
            setQuery(params)
            setPage({ current, pageSize })
            fetchData(params)
          },
        }}
      />

      <Modal
        title="创建课程"
        open={createOpen}
        onCancel={() => setCreateOpen(false)}
        confirmLoading={submitting}
        onOk={handleCreate}
        destroyOnClose
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item label="标题" name="title" rules={[{ required: true, message: '请输入课程标题' }]}>
            <Input placeholder="请输入课程标题" />
          </Form.Item>
          <Form.Item label="描述" name="description">
            <Input.TextArea rows={3} placeholder="课程简介" />
          </Form.Item>
          <Form.Item label="难度" name="difficulty" initialValue="beginner">
            <Select
              options={[
                { value: 'beginner', label: '入门' },
                { value: 'intermediate', label: '进阶' },
                { value: 'advanced', label: '高级' },
              ]}
            />
          </Form.Item>
          <Form.Item label="标签" name="tags">
            <Select mode="tags" placeholder="输入后回车添加标签" tokenSeparators={[',']} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default CourseListPage
