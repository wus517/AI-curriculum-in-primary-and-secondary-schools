import { useState } from 'react'
import { Table, Button, Space, Modal, Form, Input, InputNumber, Upload, message, Popconfirm, Typography } from 'antd'
import { UploadOutlined, DeleteOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { UploadProps } from 'antd'
import type { TabProps } from './types'
import { courseApi } from '../../api/course'

interface ResourceTabProps extends TabProps {
  type: 'video' | 'ppt'
}

const { Text } = Typography

const ACCEPT: Record<'video' | 'ppt', string> = {
  video: '.mp4,.mov,.avi,.mkv',
  ppt: '.ppt,.pptx,.pdf',
}

const TYPE_LABEL: Record<'video' | 'ppt', string> = {
  video: '视频',
  ppt: '课件',
}

const ResourceTab = ({ course, canEdit, refresh, type }: ResourceTabProps) => {
  const [uploadOpen, setUploadOpen] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [file, setFile] = useState<File | null>(null)
  const [form] = Form.useForm<{ title: string; description: string; sortOrder: number }>()

  const resources = (type === 'video' ? course.videoList : course.pptList) ?? []

  const uploadProps: UploadProps = {
    accept: ACCEPT[type],
    showUploadList: false,
    maxCount: 1,
    beforeUpload: (f) => {
      setFile(f)
      return false
    },
  }

  const handleUpload = async () => {
    if (!file) {
      message.warning('请先选择文件')
      return
    }
    const values = await form.validateFields().catch(() => null)
    setSubmitting(true)
    try {
      await courseApi.uploadResource(course.id, type, file, {
        title: values?.title,
        description: values?.description,
        sortOrder: values?.sortOrder,
      })
      message.success('上传成功')
      setUploadOpen(false)
      setFile(null)
      form.resetFields()
      await refresh()
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async (id: number) => {
    await courseApi.deleteResource(id)
    message.success('已删除')
    await refresh()
  }

  const columns: ColumnsType<typeof resources[number]> = [
    { title: '#', dataIndex: 'sortOrder', width: 60 },
    { title: '标题', dataIndex: 'title', render: (v: string) => v || '-' },
    { title: '文件名', dataIndex: 'fileName' },
    {
      title: '大小',
      dataIndex: 'fileSize',
      width: 100,
      render: (v?: number) => (v ? `${(v / 1024 / 1024).toFixed(2)} MB` : '-'),
    },
    {
      title: '操作',
      width: 160,
      render: (_, r) => (
        <Space>
          {r.fileUrl && (
            <a href={r.fileUrl} target="_blank" rel="noreferrer">
              {type === 'video' ? '播放/下载' : '下载'}
            </a>
          )}
          {canEdit && (
            <Popconfirm title="确认删除该资源？" onConfirm={() => handleDelete(r.id)}>
              <Button type="link" danger size="small" icon={<DeleteOutlined />}>删除</Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ]

  return (
    <div>
      {canEdit && (
        <Button
          type="primary"
          icon={<UploadOutlined />}
          style={{ marginBottom: 16 }}
          onClick={() => setUploadOpen(true)}
        >
          上传{TYPE_LABEL[type]}
        </Button>
      )}
      {resources.length === 0 ? (
        <Text type="secondary">暂无{TYPE_LABEL[type]}资源</Text>
      ) : (
        <Table rowKey="id" columns={columns} dataSource={resources} pagination={false} size="middle" />
      )}

      <Modal
        title={`上传${TYPE_LABEL[type]}`}
        open={uploadOpen}
        onCancel={() => { setUploadOpen(false); setFile(null); form.resetFields() }}
        confirmLoading={submitting}
        onOk={handleUpload}
        destroyOnClose
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item label="选择文件" required>
            <Space direction="vertical" style={{ width: '100%' }}>
              <Upload {...uploadProps}>
                <Button icon={<UploadOutlined />}>选择{TYPE_LABEL[type]}文件</Button>
              </Upload>
              {file && <Text type="success">已选择：{file.name}</Text>}
            </Space>
          </Form.Item>
          <Form.Item label="标题" name="title">
            <Input placeholder={`给这个${TYPE_LABEL[type]}起个标题`} />
          </Form.Item>
          <Form.Item label="描述" name="description">
            <Input.TextArea rows={2} />
          </Form.Item>
          <Form.Item label="排序" name="sortOrder" initialValue={0}>
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default ResourceTab
