import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Card, Typography, Tag, Space, Button, Descriptions, message, Spin, Divider } from 'antd'
import Editor from '@monaco-editor/react'
import { problemApi } from '../../api/problem'
import { submissionApi } from '../../api/submission'
import type { ProblemVO } from '../../types/api'
import { DEFAULT_JAVA_CODE, getDifficultyMeta } from '../../constants'

const { Title, Text } = Typography

const ProblemDetailPage = () => {
  const { id } = useParams()
  const navigate = useNavigate()
  const [problem, setProblem] = useState<ProblemVO | null>(null)
  const [code, setCode] = useState<string>(DEFAULT_JAVA_CODE)
  const [submitting, setSubmitting] = useState(false)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const pid = Number(id)
    problemApi
      .get(pid)
      .then((p) => setProblem(p))
      .finally(() => setLoading(false))
  }, [id])

  const handleSubmit = async () => {
    if (!problem) return
    setSubmitting(true)
    try {
      const sid = await submissionApi.add({ problemId: problem.id, language: 'java', code })
      message.success('提交成功，正在跳转结果')
      navigate(`/submission/${sid}`)
    } finally {
      setSubmitting(false)
    }
  }

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: 80 }}>
        <Spin />
      </div>
    )
  }
  if (!problem) {
    return <Text type="danger">题目不存在</Text>
  }

  const diff = getDifficultyMeta(problem.difficulty)

  return (
    <Space direction="vertical" size="middle" style={{ width: '100%' }}>
      <Card>
        <Title level={4} style={{ marginBottom: 8 }}>
          {problem.id}. {problem.title} <Tag color={diff.color}>{diff.text}</Tag>
        </Title>
        <Space size={[4, 4]} wrap>
          {(problem.tags ?? []).map((t) => (
            <Tag key={t}>{t}</Tag>
          ))}
        </Space>
        <Divider style={{ margin: '16px 0' }} />
        <div className="problem-content">{problem.content}</div>
        <Descriptions column={1} style={{ marginTop: 16 }}>
          <Descriptions.Item label="时间限制">{problem.timeLimit} ms</Descriptions.Item>
          <Descriptions.Item label="内存限制">{problem.memoryLimit} MB</Descriptions.Item>
          <Descriptions.Item label="输入描述">
            <div className="problem-content">{problem.inputDesc}</div>
          </Descriptions.Item>
          <Descriptions.Item label="输出描述">
            <div className="problem-content">{problem.outputDesc}</div>
          </Descriptions.Item>
          <Descriptions.Item label="样例输入">
            <pre className="code-block">{problem.sampleInput}</pre>
          </Descriptions.Item>
          <Descriptions.Item label="样例输出">
            <pre className="code-block">{problem.sampleOutput}</pre>
          </Descriptions.Item>
        </Descriptions>
      </Card>

      <Card
        title="代码编辑（Java，主类名需为 Main）"
        extra={
          <Button type="primary" loading={submitting} onClick={handleSubmit}>
            提交
          </Button>
        }
      >
        <Editor
          height="400px"
          defaultLanguage="java"
          theme="vs-dark"
          value={code}
          onChange={(v) => setCode(v ?? '')}
        />
      </Card>
    </Space>
  )
}

export default ProblemDetailPage
