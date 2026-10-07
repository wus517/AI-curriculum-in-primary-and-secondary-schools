import { useState } from 'react'
import { Descriptions, Typography, Space, Button, Modal, Form, Input, Select, Upload, message, Tag, Popconfirm } from 'antd'
import { UploadOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import type { UploadProps } from 'antd'
import type { TabProps } from './types'
import { courseApi } from '../../api/course'
import type { CourseUpdateDTO } from '../../api/course'
import { getCourseDifficultyMeta, getCourseStatusMeta } from '../../constants'

const { Paragraph } = Typography

const InfoTab = ({ course, canEdit, refresh }: TabProps) => {
  const [editOpen, setEditOpen] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [form] = Form.useForm<CourseUpdateDTO>()

  const uploadProps: UploadProps = {
    accept: '.jpg,.jpeg,.png,.webp',
    showUploadList: false,
    maxCount: 1,
    customRequest: async (options) => {
      const { file, onSuccess, onError } = options
      try {
        await courseApi.uploadCover(course.id, file as File)
        message.success('封面上传成功')
        await refresh()
        onSuccess?.({}, new XMLHttpRequest())
      } catch (e) {
        onError?.(e as Error)
      }
    },
  }

  const openEdit = () => {
    form.setFieldsValue({
      id: course.id,
      title: course.title,
      description: course.description,
      difficulty: course.difficulty,
      tags: course.tags,
      status: course.status,
    })
    setEditOpen(true)
  }

  const handleEdit = async () => {
    const values = await form.validateFields()
    setSubmitting(true)
    try {
      await courseApi.update({ ...values, id: course.id })
      message.success('修改成功')
      setEditOpen(false)
      await refresh()
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async () => {
    await courseApi.delete(course.id)
    message.success('已删除')
    refresh().catch(() => {})
  }

  return (
    <div>
      <Space style={{ marginBottom: 16 }}>
        {canEdit && (
          <>
            <Button icon={<EditOutlined />} onClick={openEdit}>编辑信息</Button>
            <Popconfirm
              title="确认删除该课程？"
              description="将同时删除其资源与题目关联，且不可恢复。"
              okText="删除"
              cancelText="取消"
              okButtonProps={{ danger: true }}
              onConfirm={handleDelete}
            >
              <Button danger icon={<DeleteOutlined />}>删除课程</Button>
            </Popconfirm>
          </>
        )}
      </Space>

      <Descriptions column={2} bordered>
        <Descriptions.Item label="标题" span={2}>{course.title}</Descriptions.Item>
        <Descriptions.Item label="难度">
          <Tag color={getCourseDifficultyMeta(course.difficulty).color}>
            {getCourseDifficultyMeta(course.difficulty).text}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label="状态">
          <Tag color={getCourseStatusMeta(course.status).color}>
            {getCourseStatusMeta(course.status).text}
          </Tag>
        </Descriptions.Item>
        <Descriptions.Item label="标签" span={2}>
          {(course.tags ?? []).map((t) => <Tag key={t}>{t}</Tag>)}
        </Descriptions.Item>
        <Descriptions.Item label="封面" span={2}>
          {course.coverUrl ? (
            <img src={course.coverUrl} alt="cover" style={{ maxWidth: '100%', maxHeight: 240, borderRadius: 8 }} />
          ) : (
            <span style={{ color: '#bbb' }}>暂无封面</span>
          )}
          {canEdit && (
            <div style={{ marginTop: 8 }}>
              <Upload {...uploadProps}>
                <Button icon={<UploadOutlined />}>上传/更换封面</Button>
              </Upload>
            </div>
          )}
        </Descriptions.Item>
        <Descriptions.Item label="描述" span={2}>
          <Paragraph style={{ marginBottom: 0, whiteSpace: 'pre-wrap' }}>
            {course.description || <span style={{ color: '#bbb' }}>暂无描述</span>}
          </Paragraph>
        </Descriptions.Item>
      </Descriptions>

      <Modal
        title="编辑课程"
        open={editOpen}
        onCancel={() => setEditOpen(false)}
        confirmLoading={submitting}
        onOk={handleEdit}
        destroyOnClose
      >
        <Form form={form} layout="vertical" preserve={false}>
          <Form.Item label="标题" name="title" rules={[{ required: true, message: '请输入标题' }]}>
            <Input />
          </Form.Item>
          <Form.Item label="描述" name="description">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item label="难度" name="difficulty">
            <Select
              options={[
                { value: 'beginner', label: '入门' },
                { value: 'intermediate', label: '进阶' },
                { value: 'advanced', label: '高级' },
              ]}
            />
          </Form.Item>
          <Form.Item label="状态" name="status">
            <Select
              options={[
                { value: 'draft', label: '草稿' },
                { value: 'published', label: '已发布' },
                { value: 'offline', label: '已下架' },
              ]}
            />
          </Form.Item>
          <Form.Item label="标签" name="tags">
            <Select mode="tags" placeholder="输入后回车" tokenSeparators={[',']} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default InfoTab
