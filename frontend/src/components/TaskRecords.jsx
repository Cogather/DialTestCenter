import React, { useState, useEffect } from 'react';
import { Typography, Table, Button, Modal, Form, Input, Select, Space, Tag, message, Descriptions, Tabs, Badge, Divider, Switch, DatePicker, InputNumber, Row, Col } from 'antd';
import { PlusOutlined, ReloadOutlined, StopOutlined, EyeOutlined, CheckCircleOutlined, SyncOutlined, CloseCircleOutlined, ClockCircleOutlined, DownloadOutlined } from '@ant-design/icons';
import { useTranslation } from '../hooks/useTranslation';
import moment from 'moment';

const { Title, Text } = Typography;
const { Option } = Select;
const { TextArea } = Input;
const { TabPane } = Tabs;

const TaskRecords = () => {
  const { t, language } = useTranslation();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState([]);
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [form] = Form.useForm();
  const [isScheduled, setIsScheduled] = useState(false);
  
  // Detail Modal State
  const [detailVisible, setDetailVisible] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [currentTask, setCurrentTask] = useState(null);
  const [subTasks, setSubTasks] = useState([]);

  // Dialing Detail Modal State
  const [dialingDetailVisible, setDialingDetailVisible] = useState(false);
  const [dialingDetailData, setDialingDetailData] = useState([]);

  // ... (existing code)

  const handleViewDialingDetail = (subTask) => {
    // Mock data for dialing detail
    const mockDialingData = Array.from({ length: 5 }).map((_, i) => ({
      key: i,
      id: `${subTask.id}-D${i + 1}`,
      target: 'http://example.com/api/test',
      ueInfo: `UE-${100 + i} (IMSI: 46000...)`,
      status: i === 3 ? 'FAILED' : 'COMPLETED',
      latency: `${100 + Math.floor(Math.random() * 50)}ms`,
      result: i === 3 ? 'Timeout' : '200 OK'
    }));
    setDialingDetailData(mockDialingData);
    setDialingDetailVisible(true);
  };

  const dialingDetailColumns = [
    { title: t('dialTask.detail.dialing.id') || 'ID', dataIndex: 'id', key: 'id' },
    { title: t('dialTask.detail.dialing.ueInfo') || 'UE Info', dataIndex: 'ueInfo', key: 'ueInfo' },
    { title: t('dialTask.detail.dialing.target') || 'Target', dataIndex: 'target', key: 'target' },
    { title: t('dialTask.detail.dialing.status') || 'Status', dataIndex: 'status', key: 'status', render: renderStatusTag },
    { title: t('dialTask.detail.dialing.latency') || 'Latency', dataIndex: 'latency', key: 'latency' },
    { title: t('dialTask.detail.dialing.result') || 'Result', dataIndex: 'result', key: 'result' },
  ];

  // Generate mock data on component mount
  useEffect(() => {
    generateMockData();
  }, [language]);

  const generateMockData = () => {
    setLoading(true);
    // Simulate API call delay
    setTimeout(() => {
      const mockData = Array.from({ length: 7 }).map((_, i) => ({
        key: i,
        id: `ONCE-${1000 + i}`,
        taskName: `Task-${1000 + i}`,
        creator: `User ${i + 1}`,
        isScheduled: false,
        startTime: `2023-11-25 10:${i < 10 ? '0' + i : i}:00`,
        endTime: `2023-11-25 10:${i < 10 ? '0' + i : i}:30`,
        type: 'VPN_BLOCK',
        status: ['PENDING', 'RUNNING', 'COMPLETED', 'FAILED'][i % 4],
        result: i % 4 === 2 ? 'Success' : (i % 4 === 3 ? 'Timeout' : '-'),
      }));

      const scheduledTasks = Array.from({ length: 3 }).map((_, i) => ({
        key: 10 + i,
        id: `SCHD-${2000 + i}`,
        taskName: `AutoTask-${2000 + i}`,
        creator: `User ${i + 10}`,
        isScheduled: true,
        startTime: `2023-11-25 12:0${i}:00`,
        endTime: `2023-11-25 12:0${i}:30`,
        type: 'VPN_BLOCK',
        status: 'COMPLETED',
        result: 'Success',
      }));

      setData([...mockData, ...scheduledTasks]);
      setLoading(false);
    }, 500);
  };

  const handleCreate = () => {
    setIsModalVisible(true);
  };

  const handleOk = () => {
    form.validateFields().then(values => {
      console.log('Received values of form: ', values);
      setLoading(true);
      // Simulate API call
      setTimeout(() => {
        const newTask = {
          key: data.length,
          id: `${values.isScheduled ? 'SCHD' : 'ONCE'}-${20231100 + data.length}`,
          taskName: values.taskName,
          creator: 'CurrentUser',
          startTime: new Date().toLocaleString(),
          endTime: '-',
          type: values.type,
          status: 'PENDING',
          result: '-',
        };
        setData([newTask, ...data]);
        setLoading(false);
        setIsModalVisible(false);
        form.resetFields();
        message.success(t('dialTask.createSuccess') || 'Task created successfully');
      }, 500);
    }).catch(info => {
      console.log('Validate Failed:', info);
    });
  };

  const handleCancel = () => {
    setIsModalVisible(false);
    form.resetFields();
    setIsScheduled(false);
  };

  const handleViewDetails = (record) => {
    setDetailLoading(true);
    setDetailVisible(true);
    
    // Simulate API call to fetch task details and subtasks
    setTimeout(() => {
      // Mock Task Detail Enhancement
      const detailedTask = {
        ...record,
        templateId: `TPL-${record.id.split('-')[1] || '001'}`,
        templateName: '标准拨测模板 (Standard)',
        input: JSON.stringify({
          target_url: "http://example.com/api/test",
          method: "POST",
          headers: { "Content-Type": "application/json" },
          timeout: 5000,
          retry: 3
        }, null, 2),
        output: record.status === 'COMPLETED' ? JSON.stringify({
          status_code: 200,
          response_time: "124ms",
          body_size: "1.2KB",
          success: true
        }, null, 2) : null,
        context: JSON.stringify({
          executor_group: "group-alpha",
          trace_id: "trace-abc-123"
        }, null, 2)
      };
      setCurrentTask(detailedTask);

      // Mock Subtasks based on design doc flow
      const stepNames = {
        preparation: t('dialTask.steps.preparation') || '拨测准备',
        execution: t('dialTask.steps.execution') || '拨测执行',
        preprocessing: t('dialTask.steps.preprocessing') || '数据预处理',
        training: t('dialTask.steps.training') || '训练',
        grayValidation: t('dialTask.steps.grayValidation') || '灰度验证',
        release: t('dialTask.steps.release') || '发布'
      };

      let mockSubTasks = [];
      let stepIndex = 1;

      // 1. Preparation (Always success)
      mockSubTasks.push({
        key: stepIndex++,
        id: `${record.id}-01`,
        stepName: stepNames.preparation,
        startTime: record.startTime,
        endTime: moment(record.startTime).add(5, 'seconds').format('YYYY-MM-DD HH:mm:ss'),
        status: 'COMPLETED',
        result: 'Success',
        latency: '0.5s'
      });

      // Helper to add steps
      const addStep = (name, status, result, errorReason, latency) => {
        mockSubTasks.push({
            key: stepIndex++,
            id: `${record.id}-0${stepIndex}`,
            stepName: name,
            startTime: moment(record.startTime).add(10 * stepIndex, 'seconds').format('YYYY-MM-DD HH:mm:ss'),
            endTime: moment(record.startTime).add(10 * stepIndex + 5, 'seconds').format('YYYY-MM-DD HH:mm:ss'),
            status,
            result,
            errorReason,
            latency
        });
      };

      if (record.status === 'COMPLETED') {
        // Scenario: Success with some retries
        // Round 1 Execution Failed
        addStep(stepNames.execution, 'FAILED', 'Failed', 'Network Timeout', '15.0s');
        // Round 2 Execution Success
        addStep(stepNames.execution, 'COMPLETED', 'Success', null, '14.2s');
        
        // Preprocessing Success
        addStep(stepNames.preprocessing, 'COMPLETED', 'Success', null, '2.1s');
        
        // Round 1 Training Failed
        addStep(stepNames.training, 'FAILED', 'Failed', 'Loss > 0.1', '120s');
        // Round 2 Training Success
        addStep(stepNames.training, 'COMPLETED', 'Success', null, '115s');
        
        // Gray Validation Success
        addStep(stepNames.grayValidation, 'COMPLETED', 'Success', null, '3.5s');
        // Release Success
        addStep(stepNames.release, 'COMPLETED', 'Success', null, '1.0s');

      } else if (record.status === 'FAILED') {
        // Scenario: Failed after retries
        // Execution Success
        addStep(stepNames.execution, 'COMPLETED', 'Success', null, '14.5s');
        // Preprocessing Success
        addStep(stepNames.preprocessing, 'COMPLETED', 'Success', null, '2.0s');
        
        // Training Failed 3 times
        addStep(stepNames.training, 'FAILED', 'Failed', 'OOM Error', '45s');
        addStep(stepNames.training, 'FAILED', 'Failed', 'OOM Error', '48s');
        addStep(stepNames.training, 'FAILED', 'Failed', 'OOM Error', '50s');
      } else {
         // Default simple flow for running/pending
         addStep(stepNames.execution, 'COMPLETED', 'Success', null, '15.5s');
         addStep(stepNames.preprocessing, 'COMPLETED', 'Success', null, '2.1s');
         
         if (record.status === 'RUNNING') {
             addStep(stepNames.training, 'RUNNING', '-', null, '-');
         } else {
             addStep(stepNames.training, 'PENDING', '-', null, '-');
         }
      }
      
      setSubTasks(mockSubTasks);
      setDetailLoading(false);
    }, 600);
  };

  const closeDetailModal = () => {
    setDetailVisible(false);
    setCurrentTask(null);
    setSubTasks([]);
  };

  const renderStatusTag = (status) => {
    let statusType = 'default';
    let text = status;
    if (status === 'PENDING') {
        statusType = 'warning';
        text = t('dialTask.status.PENDING') || 'Pending';
    } else if (status === 'RUNNING') {
        statusType = 'processing';
        text = t('dialTask.status.RUNNING') || 'Running';
    } else if (status === 'COMPLETED') {
        statusType = 'success';
        text = t('dialTask.status.COMPLETED') || 'Completed';
    } else if (status === 'FAILED') {
        statusType = 'error';
        text = t('dialTask.status.FAILED') || 'Failed';
    }
    return <Badge status={statusType} text={text} />;
  };

  const renderTypeTag = (text) => <Tag color="blue">{t(`dialTask.types.${text}`) || text}</Tag>;

  const columns = [
    {
      title: t('dialTask.table.taskId') || 'Task ID',
      dataIndex: 'id',
      key: 'id',
    },
    {
      title: t('dialTask.table.taskName') || 'Task Name',
      dataIndex: 'taskName',
      key: 'taskName',
    },
    {
      title: t('dialTask.table.creator') || 'Creator',
      dataIndex: 'creator',
      key: 'creator',
    },
    {
      title: t('dialTask.table.executionMode') || '执行模式',
      dataIndex: 'isScheduled',
      key: 'isScheduled',
      render: isScheduled => (
        isScheduled ? 
        <Tag icon={<ClockCircleOutlined />} color="processing">{t('dialTask.mode.scheduled') || '定时'}</Tag> : 
        <Tag icon={<PlusOutlined />} color="default">{t('dialTask.mode.once') || '单次'}</Tag>
      )
    },
    {
      title: t('dialTask.table.startTime') || 'Start Time',
      dataIndex: 'startTime',
      key: 'startTime',
    },
    {
      title: t('dialTask.table.endTime') || 'End Time',
      dataIndex: 'endTime',
      key: 'endTime',
    },
    {
      title: t('dialTask.table.type') || 'Task Type',
      dataIndex: 'type',
      key: 'type',
      render: renderTypeTag,
    },
    {
      title: t('dialTask.table.status') || 'Status',
      dataIndex: 'status',
      key: 'status',
      render: renderStatusTag,
    },
    {
      title: t('dialTask.table.actions') || 'Actions',
      key: 'actions',
      render: (_, record) => (
        <Space size="middle">
          <Button 
            type="text" 
            icon={<EyeOutlined />} 
            onClick={() => handleViewDetails(record)}
            title={t('dialTask.table.viewDetails') || 'View Details'}
          />
          <Button 
            type="text" 
            danger
            icon={<StopOutlined />} 
            onClick={() => message.info(`Stop task ${record.id}`)}
            title={t('dialTask.stopTask') || 'Stop Task'}
            disabled={record.status === 'COMPLETED' || record.status === 'FAILED'}
          />
          <Button
            type="text"
            icon={<DownloadOutlined />}
            onClick={() => message.success(`Download report for ${record.id}`)}
            title={t('dialTask.downloadReport') || 'Download Report'}
            disabled={record.status !== 'COMPLETED'}
          />
        </Space>
      ),
    },
  ];

  const subTaskColumns = [
    { title: t('dialTask.detail.table.id') || 'Subtask ID', dataIndex: 'id', key: 'id', width: 120 },
    { title: t('dialTask.detail.table.stepName') || 'Task Step', dataIndex: 'stepName', key: 'stepName', width: 180,
      render: (text, record) => {
        if (text === (t('dialTask.steps.execution') || '拨测执行')) {
          return (
            <Space>
              {text}
              <Button 
                type="link" 
                size="small" 
                icon={<EyeOutlined />}
                onClick={() => handleViewDialingDetail(record)}
              >
                {t('common.view') || 'View'}
              </Button>
            </Space>
          );
        }
        return text;
      }
    },
    { title: t('dialTask.detail.table.status') || 'Status', dataIndex: 'status', key: 'status', 
      render: renderStatusTag
    },
    { title: t('dialTask.detail.table.latency') || 'Latency', dataIndex: 'latency', key: 'latency' },
    { title: t('dialTask.detail.table.result') || 'Result', dataIndex: 'result', key: 'result' },
    { title: t('dialTask.detail.table.errorReason') || 'Error Reason', dataIndex: 'errorReason', key: 'errorReason', render: text => text || '-' },
  ];

  return (
    <div className="task-records">
      <div style={{ marginBottom: 24 }}>
        <Title level={3} style={{ marginBottom: 8 }}>{t('navigation.taskRecords') || 'Task Records'}</Title>
        <Text type="secondary">
          {t('dialTask.description') || 'Manage dial testing tasks, support creating, viewing task status and results'}
        </Text>
      </div>

      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          {t('dialTask.newTask') || 'New Task'}
        </Button>
        <Button icon={<ReloadOutlined />} onClick={generateMockData}>
          {t('common.refresh') || 'Refresh'}
        </Button>
      </div>

      <Table
        columns={columns}
        dataSource={data}
        loading={loading}
        pagination={{
          total: data.length,
          showTotal: (total, range) => t('dialTask.table.pagination', { start: range[0], end: range[1], total }) || `${range[0]}-${range[1]} of ${total}`,
        }}
      />

      <Modal
        title={t('dialTask.createTask') || 'Create Dial Task'}
        visible={isModalVisible}
        onOk={handleOk}
        onCancel={handleCancel}
        destroyOnClose
        width={800}
      >
        <Form
          form={form}
          layout="vertical"
          name="create_task_form"
        >
          <Form.Item
            name="taskName"
            label={t('dialTask.form.taskName') || 'Task Name'}
            rules={[{ required: true, message: t('dialTask.form.taskNamePlaceholder') || 'Please enter task name' }]}
          >
            <Input placeholder={t('dialTask.form.taskNamePlaceholder') || 'Please enter task name'} />
          </Form.Item>

          <Form.Item
            name="type"
            label={t('dialTask.form.type') || 'Task Type'}
            rules={[{ required: true, message: t('dialTask.form.typePlaceholder') || 'Please select task type' }]}
          >
            <Select placeholder={t('dialTask.form.typePlaceholder') || 'Please select task type'}>
              <Option value="VPN_BLOCK">{t('dialTask.types.VPN_BLOCK') || 'VPN Block'}</Option>
            </Select>
          </Form.Item>

          <Form.Item
            name="isScheduled"
            label={t('dialTask.form.isScheduled') || 'Scheduled Execution'}
            valuePropName="checked"
          >
            <Switch onChange={(checked) => setIsScheduled(checked)} />
          </Form.Item>

          {isScheduled && (
            <div style={{ background: '#f5f5f5', padding: '16px', borderRadius: '8px', marginBottom: '24px' }}>
              <Title level={5} style={{ marginTop: 0 }}>{t('dialTask.form.scheduleConfig') || 'Schedule Config'}</Title>
              <Row gutter={16}>
                <Col span={12}>
                  <Form.Item
                    name="startTime"
                    label={t('dialTask.form.startTime') || 'First Start Time'}
                    rules={[{ required: true, message: t('dialTask.form.startTimePlaceholder') || 'Select start time' }]}
                    initialValue={moment()}
                  >
                    <DatePicker showTime style={{ width: '100%' }} format="YYYY-MM-DD HH:mm:ss" />
                  </Form.Item>
                </Col>
                <Col span={12}>
                  <Form.Item label={t('dialTask.form.interval') || 'Interval'}>
                    <Input.Group compact>
                      <Form.Item
                        name="intervalValue"
                        noStyle
                        rules={[{ required: true, message: t('dialTask.form.intervalPlaceholder') || 'Enter value' }]}
                      >
                        <InputNumber style={{ width: '60%' }} min={1} placeholder="1" />
                      </Form.Item>
                      <Form.Item
                        name="intervalUnit"
                        noStyle
                        rules={[{ required: true, message: t('dialTask.form.intervalUnitPlaceholder') || 'Select unit' }]}
                        initialValue="HOUR"
                      >
                        <Select style={{ width: '40%' }}>
                          <Option value="HOUR">{t('dialTask.form.units.HOUR') || 'Hour'}</Option>
                          <Option value="DAY">{t('dialTask.form.units.DAY') || 'Day'}</Option>
                        </Select>
                      </Form.Item>
                    </Input.Group>
                  </Form.Item>
                </Col>
              </Row>
            </div>
          )}
        </Form>
      </Modal>

      {/* Task Detail Modal */}
      <Modal
        title={t('dialTask.detail.title') || '任务执行详情'}
        visible={detailVisible}
        onCancel={closeDetailModal}
        footer={[
          <Button key="close" onClick={closeDetailModal}>
            {t('common.close') || 'Close'}
          </Button>
        ]}
        width={1000}
        bodyStyle={{ maxHeight: '80vh', overflowY: 'auto' }}
      >
        {currentTask && (
          <Space direction="vertical" size="large" style={{ width: '100%' }}>
            {/* 1. Basic Information */}
            <Descriptions title={t('dialTask.detail.basicInfo') || "Basic Information"} bordered size="small" column={2}>
              <Descriptions.Item label={t('dialTask.table.taskId') || "Task ID"}>{currentTask.id}</Descriptions.Item>
              <Descriptions.Item label={t('dialTask.table.templateId') || "模板ID"}>{currentTask.templateId}</Descriptions.Item>
              <Descriptions.Item label={t('dialTask.table.taskName') || "Task Name"}>{currentTask.taskName}</Descriptions.Item>
              <Descriptions.Item label={t('dialTask.table.templateName') || "模板名称"}>{currentTask.templateName}</Descriptions.Item>
              <Descriptions.Item label={t('dialTask.table.creator') || "Creator"}>{currentTask.creator}</Descriptions.Item>
              <Descriptions.Item label={t('dialTask.table.type') || "Type"}>{renderTypeTag(currentTask.type)}</Descriptions.Item>
              <Descriptions.Item label={t('dialTask.table.startTime') || "Start Time"}>{currentTask.startTime}</Descriptions.Item>
              <Descriptions.Item label={t('dialTask.table.endTime') || "End Time"}>{currentTask.endTime}</Descriptions.Item>
              <Descriptions.Item label={t('dialTask.table.status') || "Status"}>{renderStatusTag(currentTask.status)}</Descriptions.Item>
              <Descriptions.Item label={t('dialTask.table.report') || "Report"}>
                {currentTask.status === 'COMPLETED' ? (
                  <Button
                    type="link"
                    icon={<DownloadOutlined />}
                    onClick={() => message.success(`Download report for ${currentTask.id}`)}
                    style={{ padding: 0 }}
                  >
                    {t('dialTask.downloadReport') || '报告下载'}
                  </Button>
                ) : '-'}
              </Descriptions.Item>
            </Descriptions>
            
            {/* 2. Subtasks */}
            <div>
              <Title level={5}>{t('dialTask.detail.subtasks') || "Subtasks Execution"}</Title>
              <Table 
                dataSource={subTasks} 
                columns={subTaskColumns} 
                pagination={false} 
                size="small" 
                loading={detailLoading}
                bordered
              />
            </div>
          </Space>
        )}
      </Modal>

      {/* Dialing Detail Modal */}
      <Modal
        title={t('dialTask.detail.dialing.title') || '拨测执行详情'}
        visible={dialingDetailVisible}
        onCancel={() => setDialingDetailVisible(false)}
        footer={[
          <Button key="close" onClick={() => setDialingDetailVisible(false)}>
            {t('common.close') || 'Close'}
          </Button>
        ]}
        width={900}
      >
        <Table
          dataSource={dialingDetailData}
          columns={dialingDetailColumns}
          pagination={false}
          size="small"
          bordered
        />
      </Modal>
    </div>
  );
};

export default TaskRecords;
