import React, { useState, useEffect } from 'react';
import { Typography, Table, Button, Modal, Form, Input, Select, Space, Tag, message, Descriptions, Tabs, Badge, Divider, Switch, DatePicker, InputNumber, Row, Col } from 'antd';
import { PlusOutlined, ReloadOutlined, StopOutlined, EyeOutlined, CheckCircleOutlined, SyncOutlined, CloseCircleOutlined, ClockCircleOutlined } from '@ant-design/icons';
import { useTranslation } from '../hooks/useTranslation';
import moment from 'moment';

const { Title } = Typography;
const { Option } = Select;
const { TextArea } = Input;
const { TabPane } = Tabs;

const DialTaskManagement = () => {
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
        id: `TASK-${20231100 + i}`,
        creator: `User ${i + 1}`,
        startTime: `2023-11-25 10:${i < 10 ? '0' + i : i}:00`,
        endTime: `2023-11-25 10:${i < 10 ? '0' + i : i}:30`,
        type: 'VPN_BLOCK',
        status: ['PENDING', 'RUNNING', 'COMPLETED', 'FAILED'][i % 4],
        result: i % 4 === 2 ? 'Success' : (i % 4 === 3 ? 'Timeout' : '-'),
      }));

      const scheduledTasks = Array.from({ length: 3 }).map((_, i) => ({
        key: 10 + i,
        id: `TASK-${20231110 + i}`,
        creator: t('dialTask.scheduledTask') || 'Scheduled Task',
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
          id: `TASK-${20231100 + data.length}`,
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

      // Mock Subtasks based on design doc flow (Validation -> Training -> Model -> Replay -> ...)
      const mockSteps = [
        { step: "Validation Check", type: "DialingTestTask(Validation)", executor: "Executor-Group-A", latency: "1.2s" },
        { step: "Training Collection", type: "DialingTestTask(Training)", executor: "Executor-Group-A", latency: "15.5s" },
        { step: "Model Training", type: "ModelTrainTask(Async)", executor: "Fine-tuning Center", latency: "120s" },
        { step: "Model Replay", type: "ReplayTask(Model)", executor: "Executor-Group-B", latency: "4.5s" },
        { step: "Gray Validation", type: "GrayValidationTask", executor: "Executor-Group-A", latency: "2.1s" },
        { step: "Full Replay", type: "ReplayTask(Full)", executor: "Executor-Group-All", latency: "8.3s" },
        { step: "Full Release", type: "FullReleaseTask", executor: "System", latency: "0.5s" }
      ];

      const mockSubTasks = mockSteps.map((step, i) => ({
        key: i,
        id: `${record.id}-0${i + 1}`,
        stepName: step.step,
        executor: step.executor,
        startTime: record.startTime,
        endTime: record.endTime,
        status: 'COMPLETED',
        result: 'Success',
        latency: step.latency
      }));
      
      // Simulate different scenarios based on record status
      if (record.status === 'FAILED') {
        // Scenario: Failed at Model Replay
        mockSubTasks[3].status = 'FAILED';
        mockSubTasks[3].result = 'Accuracy < 80%';
        // Remove subsequent steps
        mockSubTasks.splice(4);
      } else if (record.status === 'RUNNING') {
        // Scenario: Running at Model Training
        mockSubTasks[2].status = 'RUNNING';
        mockSubTasks[2].result = '-';
        mockSubTasks[2].latency = '-';
        // Pending subsequent steps
        for (let j = 3; j < mockSubTasks.length; j++) {
          mockSubTasks[j].status = 'PENDING';
          mockSubTasks[j].result = '-';
          mockSubTasks[j].latency = '-';
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

  const columns = [
    {
      title: t('dialTask.table.id') || 'Task ID',
      dataIndex: 'id',
      key: 'id',
    },
    {
      title: t('dialTask.table.creator') || 'Creator',
      dataIndex: 'creator',
      key: 'creator',
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
      render: text => <Tag color="blue">{t(`dialTask.types.${text}`) || text}</Tag>,
    },
    {
      title: t('dialTask.table.status') || 'Status',
      dataIndex: 'status',
      key: 'status',
      render: status => {
        let color = 'default';
        let text = status;
        if (status === 'PENDING') {
            color = 'orange';
            text = t('dialTask.status.PENDING') || 'Pending';
        } else if (status === 'RUNNING') {
            color = 'blue';
            text = t('dialTask.status.RUNNING') || 'Running';
        } else if (status === 'COMPLETED') {
            color = 'green';
            text = t('dialTask.status.COMPLETED') || 'Completed';
        } else if (status === 'FAILED') {
            color = 'red';
            text = t('dialTask.status.FAILED') || 'Failed';
        }
        return <Tag color={color}>{text}</Tag>;
      }
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
        </Space>
      ),
    },
  ];

  const subTaskColumns = [
    { title: t('dialTask.detail.table.id') || 'Subtask ID', dataIndex: 'id', key: 'id', width: 120 },
    { title: t('dialTask.detail.table.stepName') || 'Task Step', dataIndex: 'stepName', key: 'stepName', width: 180 },
    { title: t('dialTask.detail.table.executor') || 'Executor', dataIndex: 'executor', key: 'executor' },
    { title: t('dialTask.detail.table.status') || 'Status', dataIndex: 'status', key: 'status', 
      render: status => {
        let color = 'default';
        if (status === 'PENDING') color = 'orange';
        else if (status === 'RUNNING') color = 'blue';
        else if (status === 'COMPLETED') color = 'green';
        else if (status === 'FAILED') color = 'red';
        return <Tag color={color}>{status}</Tag>;
      }
    },
    { title: t('dialTask.detail.table.latency') || 'Latency', dataIndex: 'latency', key: 'latency' },
    { title: t('dialTask.detail.table.result') || 'Result', dataIndex: 'result', key: 'result' },
  ];

  const renderStatusBadge = (status) => {
     if (status === 'RUNNING') return <Badge status="processing" text="Running" />;
     if (status === 'COMPLETED') return <Badge status="success" text="Completed" />;
     if (status === 'FAILED') return <Badge status="error" text="Failed" />;
     return <Badge status="warning" text="Pending" />;
  };

  return (
    <div className="dial-task-management">
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Title level={3} style={{ margin: 0 }}>{t('dialTask.title') || 'Dial Task Management'}</Title>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={generateMockData}>
            {t('common.refresh') || 'Refresh'}
          </Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            {t('dialTask.newTask') || 'New Task'}
          </Button>
        </Space>
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
          <Row gutter={24}>
            <Col span={12}>
              <Form.Item
                name="taskName"
                label={t('dialTask.form.taskName') || 'Task Name'}
                rules={[{ required: true, message: t('dialTask.form.taskNamePlaceholder') || 'Please enter task name' }]}
              >
                <Input placeholder={t('dialTask.form.taskNamePlaceholder') || 'Please enter task name'} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="type"
                label={t('dialTask.form.type') || 'Task Type'}
                rules={[{ required: true, message: t('dialTask.form.typePlaceholder') || 'Please select task type' }]}
              >
                <Select placeholder={t('dialTask.form.typePlaceholder') || 'Please select task type'}>
                  <Option value="VPN_BLOCK">{t('dialTask.types.VPN_BLOCK') || 'VPN Block'}</Option>
                </Select>
              </Form.Item>
            </Col>
          </Row>

          <Row gutter={24}>
            <Col span={12}>
              <Form.Item
                name="target"
                label={t('dialTask.form.target') || 'Test Target'}
                rules={[{ required: true, message: t('dialTask.form.targetPlaceholder') || 'Please enter test target' }]}
              >
                <Input placeholder={t('dialTask.form.targetPlaceholder') || 'Please enter test target (e.g. URL or IP)'} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="preprocessRule"
                label={t('dialTask.form.preprocessRule') || 'Preprocess Rule'}
              >
                 <Select placeholder={t('dialTask.form.preprocessRulePlaceholder') || 'Select preprocess rule'}>
                    <Option value="rule1">Rule_Douyin_Live</Option>
                    <Option value="rule2">Rule_Kuaishou_Video</Option>
                    <Option value="rule3">Rule_Custom_Game_A</Option>
                 </Select>
              </Form.Item>
            </Col>
          </Row>

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

          <Form.Item
            name="config"
            label={t('dialTask.form.config') || 'Task Config'}
          >
            <TextArea rows={4} placeholder={t('dialTask.form.configPlaceholder') || 'Please enter configuration content'} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Task Detail Modal */}
      <Modal
        title={t('dialTask.detail.title') || 'Task Details'}
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
              <Descriptions.Item label={t('dialTask.table.id') || "Task ID"}>{currentTask.id}</Descriptions.Item>
              <Descriptions.Item label={t('dialTask.table.creator') || "Creator"}>{currentTask.creator}</Descriptions.Item>
              <Descriptions.Item label={t('dialTask.table.startTime') || "Start Time"}>{currentTask.startTime}</Descriptions.Item>
              <Descriptions.Item label={t('dialTask.table.endTime') || "End Time"}>{currentTask.endTime}</Descriptions.Item>
              <Descriptions.Item label={t('dialTask.table.status') || "Status"}>{renderStatusBadge(currentTask.status)}</Descriptions.Item>
              <Descriptions.Item label={t('dialTask.table.type') || "Type"}>{currentTask.type}</Descriptions.Item>
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

            {/* 3. Configuration (Input) */}
            <div>
              <Title level={5}>{t('dialTask.detail.config') || "Configuration (Input)"}</Title>
              <TextArea 
                readOnly 
                value={currentTask.input} 
                autoSize={{ minRows: 3, maxRows: 10 }} 
                style={{ fontFamily: 'monospace', backgroundColor: '#f5f5f5' }} 
              />
            </div>

            {/* 4. Result (Output) */}
            <div>
               <Title level={5}>{t('dialTask.detail.output') || "Result (Output)"}</Title>
               {currentTask.output ? (
                 <TextArea 
                  readOnly 
                  value={currentTask.output} 
                  autoSize={{ minRows: 3, maxRows: 10 }} 
                  style={{ fontFamily: 'monospace', backgroundColor: '#f5f5f5' }} 
                />
               ) : <span style={{ color: '#999' }}>{t('dialTask.detail.noOutput') || "No output available"}</span>}
            </div>
          </Space>
        )}
      </Modal>
    </div>
  );
};

export default DialTaskManagement;
