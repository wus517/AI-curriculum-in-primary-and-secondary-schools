import { useEffect, useState } from 'react'
import { Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { Link } from 'react-router-dom'
import { problemApi } from '../../api/problem'
import type { ProblemVO } from '../../types/api'
import { getDifficultyMeta } from '../../constants'

const { Title } = Typography

const ProblemListPage = () => {
  const [data, setData] = useState<ProblemVO[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState({ current: 1, pageSize: 10 })

  const fetchData = async (current: number, pageSize: number) => {
    setLoading(true)
    try {
      const res = await problemApi.list(current, pageSize)
      setData(res.records)
      setTotal(res.total)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData(1, 10)
  }, [])

  const columns: ColumnsType<ProblemVO> = [
    {
      title: '#',
      dataIndex: 'id',
      width: 80,
    },
    {
      title: '标题',
      dataIndex: 'title',
      render: (_, r) => <Link to={`/problem/${r.id}`}>{r.title}</Link>,
    },
    {
      title: '难度',
      dataIndex: 'difficulty',
      width: 100,
      render: (v: number) => {
        const m = getDifficultyMeta(v)
        return <Tag color={m.color}>{m.text}</Tag>
      },
    },
    {
      title: '标签',
      dataIndex: 'tags',
      render: (tags?: string[]) => (tags ?? []).map((t) => <Tag key={t}>{t}</Tag>),
    },
    {
      title: '限制',
      width: 160,
      render: (_, r) => (
        <span>
          {r.timeLimit}ms / {r.memoryLimit}MB
        </span>
      ),
    },
  ]

  return (
    <div style={{ background: '#fff', padding: 24, borderRadius: 8 }}>
      <Title level={4}>题目列表</Title>
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
            setPage({ current, pageSize })
            fetchData(current, pageSize)
          },
        }}
      />
    </div>
  )
}

export default ProblemListPage
