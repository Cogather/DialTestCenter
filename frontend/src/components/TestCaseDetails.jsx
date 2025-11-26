import {
  CheckCircleOutlined,
  FileTextOutlined,
  InfoCircleOutlined,
  ReloadOutlined,
  DownloadOutlined,
} from '@ant-design/icons'
import {
  Alert,
  Button,
  Card,
  Col,
  Descriptions,
  message,
  Modal,
  Row,
  Space,
  Spin,
  Table,
  Tag,
  Badge,
  Typography,
} from 'antd'
import React, { useEffect, useState } from 'react'
import { useTranslation } from '../hooks/useTranslation.js'
import { useI18n } from '../contexts/I18nContext.jsx'
import { usePermission } from '../hooks/usePermission.js'
import testCaseSetService from '../services/testCaseSetService.js'
import { getValidationResultColumns, showTestCaseDetail } from '../utils/validationUtils.js'

const { Text } = Typography

const TestCaseDetails = ({
  visible,
  testCaseSet,
  onCancel,
}) => {
  const [validationResult, setValidationResult] = useState(null)
  const [loading, setLoading] = useState(true) // 初始为true，避免首次渲染时闪烁
  const [exportLoading, setExportLoading] = useState(false)

  const { translateTestCaseSet } = useTranslation()
  const { language } = useI18n()
  const { hasPagePermission } = usePermission()
  // 只有ADMIN和OPERATOR用户才能导出校验结果
  const canExport = hasPagePermission('test-case-set', 'validate')

  // 加载校验结果
  const loadValidationResult = async (forceRefresh = false) => {
    if (!testCaseSet) {
      return
    }

    try {
      setLoading(true)
      const response = await testCaseSetService.getValidationResult(testCaseSet.id, forceRefresh)
      if (response && response.success && response.data && response.data.caseResults) {
        setValidationResult(response.data)
      } else {
        setValidationResult(null)
      }
    } catch (error) {
      console.error('Failed to load validation result:', error)
      setValidationResult(null)
    } finally {
      setLoading(false)
    }
  }

  // 刷新校验结果
  const handleRefresh = () => {
    loadValidationResult(true)
  }

  // 导出Excel
  const handleExport = async () => {
    if (!testCaseSet || !validationResult) {
      message.warning(translateTestCaseSet('validation.noResultToExport'))
      return
    }

    try {
      setExportLoading(true)
      await testCaseSetService.exportValidationResult(testCaseSet.id)
      message.success(translateTestCaseSet('validation.exportSuccess'))
    } catch (error) {
      console.error('Failed to export validation result:', error)
      message.error(translateTestCaseSet('validation.exportFailed'))
    } finally {
      setExportLoading(false)
    }
  }

  useEffect(() => {
    if (visible && testCaseSet) {
      // 立即设置loading状态，避免显示"暂无结果"的闪烁
      setLoading(true)
      loadValidationResult()
    } else {
      setValidationResult(null)
      setLoading(false)
    }
  }, [visible, testCaseSet])

  // 格式化时间
  const formatDateTime = (dateTimeStr) => {
    if (!dateTimeStr) {
      return '-'
    }
    try {
      const date = new Date(dateTimeStr)
      const locale = language === 'en' ? 'en-US' : 'zh-CN'
      return date.toLocaleString(locale, {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
      })
    } catch (e) {
      return dateTimeStr
    }
  }

  // 获取状态标签
  const getStatusTag = (status) => {
    const statusKey = status || 'UNKNOWN'
    const statusText = translateTestCaseSet(`validation.status.${statusKey}`)
    let statusType = 'default';
    if (status === 'COMPLETED') {
      statusType = 'success';
    } else if (status === 'RUNNING') {
      statusType = 'processing';
    } else if (status === 'PENDING') {
      statusType = 'warning';
    } else if (status === 'FAILED') {
      statusType = 'error';
    }
    return <Badge status={statusType} text={statusText} />;
  }

  // 用例详情表格列定义（使用共享工具函数）
  const columns = getValidationResultColumns(
    (record) => showTestCaseDetail(record, translateTestCaseSet),
    translateTestCaseSet
  )

  return (
    <Modal
      title={
        <Space>
          <FileTextOutlined />
          <span>{translateTestCaseSet('details.title')}</span>
          {testCaseSet && (
            <Tag color="blue">{testCaseSet.name} - {testCaseSet.version}</Tag>
          )}
        </Space>
      }
      open={visible}
      onCancel={onCancel}
      width={1400}
      footer={[
        <Button key="close" onClick={onCancel}>
          {translateTestCaseSet('details.close')}
        </Button>,
      ]}
    >
      {testCaseSet && (
        <div>
          {/* 第一部分：最近一次校验任务信息 */}
          {validationResult && validationResult.validationTaskStatus && (
            <Card size="small" style={{ marginBottom: '16px' }}>
              <Descriptions column={3} size="small">
                <Descriptions.Item label={translateTestCaseSet('validation.taskStatus')}>
                  {getStatusTag(validationResult.validationTaskStatus)}
                </Descriptions.Item>
                <Descriptions.Item label={translateTestCaseSet('validation.triggerTime')}>
                  {formatDateTime(validationResult.validationTaskCreatedTime)}
                </Descriptions.Item>
                <Descriptions.Item label={translateTestCaseSet('validation.completedTime')}>
                  {formatDateTime(validationResult.validationTaskCompletedTime)}
                </Descriptions.Item>
              </Descriptions>
            </Card>
          )}

          {/* 第二部分：统计卡片 */}
          {(() => {
            if (loading) {
              return (
                <div style={{ marginBottom: '16px', textAlign: 'center', padding: '20px' }}>
                  <Spin size="large" />
                </div>
              );
            } else if (validationResult) {
              return (
                <Row gutter={16} style={{ marginBottom: '16px' }}>
                  <Col span={8}>
                    <Card size="small">
                      <Space>
                        <InfoCircleOutlined style={{ color: '#1890ff', fontSize: '24px' }} />
                        <div>
                          <div style={{ fontSize: '20px', fontWeight: 'bold' }}>
                            {validationResult.totalCaseCount || 0}
                          </div>
                          <div style={{ color: '#666' }}>{translateTestCaseSet('statistics.totalCases')}</div>
                        </div>
                      </Space>
                    </Card>
                  </Col>
                  <Col span={8}>
                    <Card size="small">
                      <Space>
                        <CheckCircleOutlined style={{ color: '#52c41a', fontSize: '24px' }} />
                        <div>
                          <div style={{ fontSize: '20px', fontWeight: 'bold' }}>
                            {validationResult.passedCaseCount || 0}
                          </div>
                          <div style={{ color: '#666' }}>{translateTestCaseSet('statistics.passedCases')}</div>
                        </div>
                      </Space>
                    </Card>
                  </Col>
                  <Col span={8}>
                    <Card size="small">
                      <Space>
                        <InfoCircleOutlined style={{ color: '#722ed1', fontSize: '24px' }} />
                        <div>
                          <div style={{ fontSize: '20px', fontWeight: 'bold' }}>
                            {validationResult.matchRate ? Number(validationResult.matchRate).toFixed(2) : '0.00'}%
                          </div>
                          <div style={{ color: '#666' }}>{translateTestCaseSet('statistics.matchRate')}</div>
                        </div>
                      </Space>
                    </Card>
                  </Col>
                </Row>
              );
            } else {
              return (
                <Alert
                  message={translateTestCaseSet('validation.noResult')}
                  description={translateTestCaseSet('validation.noResultDescription')}
                  type="info"
                  showIcon
                  style={{ marginBottom: '16px' }}
                />
              );
            }
          })()}

          {/* 操作按钮 */}
          <div style={{ marginBottom: '16px', textAlign: 'right' }}>
            <Space>
              <Button
                icon={<ReloadOutlined />}
                onClick={handleRefresh}
                loading={loading}
              >
                {translateTestCaseSet('validation.refresh')}
              </Button>
              {validationResult && canExport && (
                <Button
                  type="primary"
                  icon={<DownloadOutlined />}
                  onClick={handleExport}
                  loading={exportLoading}
                >
                  {translateTestCaseSet('validation.export')}
                </Button>
              )}
            </Space>
          </div>

          {/* 第三部分：用例详情表格 */}
          {(() => {
            if (loading || (visible && !validationResult)) {
              return (
                <Table
                  columns={columns}
                  dataSource={[]}
                  loading={true}
                  scroll={{ x: 1200 }}
                />
              );
            } else if (validationResult && validationResult.caseResults) {
              return (
                <Table
                  columns={columns}
                  dataSource={validationResult.caseResults || []}
                  rowKey={(record, index) => `${record.caseNumber || index}-${index}`}
                  loading={false}
                  scroll={{ x: 1200 }}
                  pagination={{
                    pageSize: 10,
                    showSizeChanger: true,
                    showQuickJumper: true,
                    showTotal: (total) => translateTestCaseSet('validation.table.paginationTotal', { total }),
                  }}
                />
              );
            } else {
              return (
                <Alert
                  message={translateTestCaseSet('validation.noCaseData')}
                  description={translateTestCaseSet('validation.noCaseDataDescription')}
                  type="warning"
                  showIcon
                />
              );
            }
          })()}
        </div>
      )}
    </Modal>
  )
}

TestCaseDetails.displayName = 'TestCaseDetails'

export default TestCaseDetails
