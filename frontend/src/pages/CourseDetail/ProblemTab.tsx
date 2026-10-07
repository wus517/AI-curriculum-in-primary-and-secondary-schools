import { useEffect, useState } from 'react'
import { Table, Button, Space, Modal, message, Popconfirm, Typography, Select, Tag } from 'antd'
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { Link } from 'react-router-dom'
import type { TabProps } from './types'
import { courseApi } from '../../api/course'
import { problemApi } from '../../api/problem'
import type { ProblemVO } from '../../types/api'
import { getDifficultyMeta } from '../../constants'

const { Text } = Typography

const ProblemTab = ({ course, canEdit, refresh }: TabProps) => {
  const [bindOpen, setBindOpen] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [allProblems, setAllProblems] = useState<ProblemVO[]>([])
  const [selectedIds, setSelectedIds] = useState<number[]>([])

  const problems = course.problemList ?? []

  // 已绑定题目 id 集合
  const boundIds = new Set(problems.map((p) => p.id))

  // 打开绑定弹窗时拉取所有题目
  useEffect(() => {
    if (bindOpen) {
      problemApi.list(1, 200).then((res) => setAllProblems(res.records)).catch(() => {})
      setSelectedIds([])
    }
  }, [bindOpen])

  const handleBind = async () => {
    if (selectedIds.length === 0) {
      message.warning('请至少选择一个题目')
      return
    }
    setSubmitting(true)
    try {
      await courseApi.bindProblems({ courseId: course.id, problemIdList: selectedIds })
      message.success('绑定成功')
      setBindOpen(false)
      await refresh()
    } finally {
      setSubmitting(false)
    }
  }

  const handleUnbind = async (problemId: number) => {
    await courseApi.unbindProblem(course.id, problemId)
    message.success('已解绑')
    await refresh()
  }

  const columns: ColumnsType<ProblemVO> = [
    { title: '#', dataIndex: 'id', width: 70 },
    {
      title: '标题',
      dataIndex: 'title',
      render: (_, r) => <Link to={`/problem/${r.id}`}>{r.title}</Link>,
    },
    {
      title: '难度',
      dataIndex: 'difficulty',
      width: 90,
      render: (v?: number) => {
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
      title: '操作',
      width: 100,
      render: (_, r) =>
        canEdit ? (
          <Popconfirm title="确认解绑该题目？" onConfirm={() => handleUnbind(r.id)}>
            <Button type="link" danger size="small" icon={<DeleteOutlined />}>解绑</Button>
          </Popconfirm>
        ) : null,
    },
  ]

  return (
    <div>
      {canEdit && (
        <Button
          type="primary"
          icon={<PlusOutlined />}
          style={{ marginBottom: 16 }}
          onClick={() => setBindOpen(true)}
        >
          绑定题目
        </Button>
      )}
      {problems.length === 0 ? (
        <Text type="secondary">暂未绑定题目</Text>
      ) : (
        <Table rowKey="id" columns={columns} dataSource={problems} pagination={false} size="middle" />
      )}

      <Modal
        title="绑定题目"
        open={bindOpen}
        onCancel={() => setBindOpen(false)}
        confirmLoading={submitting}
        onOk={handleBind}
        destroyOnClose
        width={560}
      >
        <Text type="secondary" style={{ display: 'block', marginBottom: 8 }}>
          已绑定 {boundIds.size} 个，下方列表可多选追加（已绑定项不可重复选择）。
        </Text>
        <Select
          mode="multiple"
          placeholder="选择要绑定的题目"
          style={{ width: '100%' }}
          value={selectedIds}
          onChange={setSelectedIds}
          options={allProblems.map((p) => ({
            value: p.id,
            label: `#${p.id} ${p.title}${boundIds.has(p.id) ? '（已绑定）' : ''}`,
            disabled: boundIds.has(p.id),
          }))}
          optionFilterProp="label"
        />
      </Modal>
    </div>
  )
}

export default ProblemTab
