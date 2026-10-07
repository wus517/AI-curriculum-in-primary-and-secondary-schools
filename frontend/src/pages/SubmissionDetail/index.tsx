import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { Card, Typography, Tag, Descriptions, Spin, Alert, Space } from 'antd'
import Editor from '@monaco-editor/react'
import dayjs from 'dayjs'
import { submissionApi } from '../../api/submission'
import type { SubmissionVO } from '../../types/api'
import { getStatusMeta } from '../../constants'

const { Title, Text, Paragraph } = Typography

interface JudgeInfo {
  total?: number
  timeUsed?: number
  memoryUsed?: number
  message?: string
}

const SubmissionDetailPage = () => {
  const { id } = useParams()
  const [data, setData] = useState<SubmissionVO | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    submissionApi
      .get(Number(id))
      .then(setData)
      .finally(() => setLoading(false))
  }, [id])

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: 80 }}>
        <Spin />
      </div>
    )
  }
  if (!data) {
    return <Text type="danger">提交记录不存在</Text>
  }

  const m = getStatusMeta(data.status)

  let judgeInfo: JudgeInfo | null = null
  try {
    judgeInfo = data.judgeInfo ? (JSON.parse(data.judgeInfo) as JudgeInfo) : null
  } catch {
    judgeInfo = null
  }

  const errorType = data.status === 'COMPILE_ERROR' ? 'warning' : 'error'

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Card>
        <Title level={4} style={{ marginBottom: 16 }}>
          提交 #{data.id} <Tag color={m.color}>{m.text}</Tag>
        </Title>
        <Descriptions column={2}>
          <Descriptions.Item label="题目">
            <Link to={`/problem/${data.problemId}`}>#{data.problemId}</Link>
          </Descriptions.Item>
          <Descriptions.Item label="语言">{data.language}</Descriptions.Item>
          <Descriptions.Item label="耗时">{data.timeUsed} ms</Descriptions.Item>
          <Descriptions.Item label="内存">{data.memoryUsed} KB</Descriptions.Item>
          <Descriptions.Item label="提交时间">
            {data.createTime ? dayjs(data.createTime).format('YYYY-MM-DD HH:mm:ss') : '-'}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      {data.errorMessage && (
        <Alert
          type={errorType}
          message="详细信息"
          description={<pre className="code-block">{data.errorMessage}</pre>}
          showIcon
        />
      )}

      {judgeInfo && (
        <Card title="判题信息">
          <Paragraph>
            {judgeInfo.total !== undefined && <Text>用例总数：{judgeInfo.total}　</Text>}
            {judgeInfo.timeUsed !== undefined && <Text>耗时：{judgeInfo.timeUsed} ms　</Text>}
            {judgeInfo.message && <Text>结果：{judgeInfo.message}</Text>}
          </Paragraph>
        </Card>
      )}

      <Card title="提交代码">
        <Editor
          height="360px"
          defaultLanguage="java"
          theme="vs-dark"
          value={data.code}
          options={{ readOnly: true }}
        />
      </Card>
    </Space>
  )
}

export default SubmissionDetailPage
