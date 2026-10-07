import { useEffect, useState } from 'react'
import { Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { Link } from 'react-router-dom'
import dayjs from 'dayjs'
import { submissionApi } from '../../api/submission'
import type { SubmissionVO } from '../../types/api'
import { getStatusMeta } from '../../constants'

const { Title } = Typography

const SubmissionListPage = () => {
  const [data, setData] = useState<SubmissionVO[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState({ current: 1, pageSize: 10 })

  const fetchData = async (current: number, pageSize: number) => {
    setLoading(true)
    try {
      const res = await submissionApi.myList(current, pageSize)
      setData(res.records)
      setTotal(res.total)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchData(1, 10)
  }, [])

  const columns: ColumnsType<SubmissionVO> = [
    {
      title: '#',
      dataIndex: 'id',
      width: 80,
      render: (v: number) => <Link to={`/submission/${v}`}>{v}</Link>,
    },
    {
      title: '题目',
      dataIndex: 'problemId',
      render: (v: number) => <Link to={`/problem/${v}`}>#{v}</Link>,
    },
    {
      title: '语言',
      dataIndex: 'language',
      width: 80,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 120,
      render: (v: string) => {
        const m = getStatusMeta(v)
        return <Tag color={m.color}>{m.text}</Tag>
      },
    },
    {
      title: '耗时',
      dataIndex: 'timeUsed',
      width: 100,
      render: (v: number) => `${v} ms`,
    },
    {
      title: '提交时间',
      dataIndex: 'createTime',
      render: (v: string) => (v ? dayjs(v).format('YYYY-MM-DD HH:mm:ss') : '-'),
    },
  ]

  return (
    <div style={{ background: '#fff', padding: 24, borderRadius: 8 }}>
      <Title level={4}>我的提交</Title>
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

export default SubmissionListPage
