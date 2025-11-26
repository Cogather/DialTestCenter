import React, { useState, useEffect } from 'react';
import { Typography, Table, Button, Modal, Form, Input, Select, Space, Tag, message, Descriptions, Switch, DatePicker, InputNumber, Row, Col } from 'antd';
import { PlusOutlined, ReloadOutlined, EditOutlined, DeleteOutlined, PlayCircleOutlined } from '@ant-design/icons';
import { useTranslation } from '../hooks/useTranslation';
import moment from 'moment';

const { Title, Text } = Typography;
const { Option } = Select;
const { TextArea } = Input;

const DialTaskManagement = () => {
  const { t, language } = useTranslation();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState([]);
  const [isModalVisible, setIsModalVisible] = useState(false);
  const [form] = Form.useForm();
  const [editingId, setEditingId] = useState(null);
  
  // Generate mock data on component mount
  useEffect(() => {
    generateMockData();
  }, [language]);

  const generateMockData = () => {
    setLoading(true);
    // Simulate API call delay
    setTimeout(() => {
      const mockData = Array.from({ length: 5 }).map((_, i) => ({
        key: i,
        id: `TEMPLATE-${1000 + i}`,
        name: `Daily Check ${i + 1}`,
        creator: `Admin ${i + 1}`,
        createTime: `2023-11-25 10:${i < 10 ? '0' + i : i}:00`,
        type: 'VPN_BLOCK',
        schedule: {
          startTime: `2023-11-26 00:00:00`,
          intervalValue: 1,
          intervalUnit: 'DAY'
        },
        config: '{"target": "192.168.1.1", "timeout": 5000}',
        isEnabled: i % 2 === 0
      }));

      setData(mockData);
      setLoading(false);
    }, 500);
  };

  const handleCreate = () => {
    setEditingId(null);
    form.resetFields();
    setIsModalVisible(true);
  };

  const handleEdit = (record) => {
    setEditingId(record.id);
    form.setFieldsValue({
      taskName: record.name,
      type: record.type,
      startTime: moment(record.schedule.startTime),
      intervalValue: record.schedule.intervalValue,
      intervalUnit: record.schedule.intervalUnit,
      config: record.config,
      isEnabled: record.isEnabled
    });
    setIsModalVisible(true);
  };

  const handleDelete = (record) => {
    Modal.confirm({
      title: t('common.delete'),
      content: t('dialTask.confirmDelete') || 'Are you sure you want to delete this template?',
      onOk: () => {
        setLoading(true);
        setTimeout(() => {
            setData(data.filter(item => item.id !== record.id));
            setLoading(false);
            message.success(t('common.deleteSuccess') || 'Deleted successfully');
        }, 500);
      }
    });
  };

  const handleRunNow = (record) => {
    Modal.confirm({
      title: t('dialTask.runNow'),
      content: t('dialTask.confirmRunNow') || 'Are you sure you want to run this task immediately?',
      onOk: () => {
        message.success(t('dialTask.runSuccess') || 'Task started successfully');
      }
    });
  };

  const handleOk = () => {
    form.validateFields().then(values => {
      setLoading(true);
      setTimeout(() => {
        if (editingId) {
           // Edit existing
           const newData = data.map(item => {
             if (item.id === editingId) {
               return {
                 ...item,
                 name: values.taskName,
                 type: values.type,
                 schedule: {
                   startTime: values.startTime.format('YYYY-MM-DD HH:mm:ss'),
                   intervalValue: values.intervalValue,
                   intervalUnit: values.intervalUnit
                 },
                 config: values.config,
                 isEnabled: values.isEnabled
               };
             }
             return item;
           });
           setData(newData);
           message.success(t('common.updateSuccess') || 'Updated successfully');
        } else {
            // Create new
            const newTemplate = {
              key: data.length + Math.random(),
              id: `TEMPLATE-${2000 + Math.floor(Math.random() * 1000)}`,
              name: values.taskName,
              creator: 'CurrentUser',
              createTime: moment().format('YYYY-MM-DD HH:mm:ss'),
              type: values.type,
              schedule: {
                startTime: values.startTime.format('YYYY-MM-DD HH:mm:ss'),
                intervalValue: values.intervalValue,
                intervalUnit: values.intervalUnit
              },
              config: values.config,
              isEnabled: values.isEnabled
            };
            setData([newTemplate, ...data]);
            message.success(t('common.createSuccess') || 'Created successfully');
        }
        
        setLoading(false);
        setIsModalVisible(false);
        form.resetFields();
      }, 500);
    }).catch(info => {
      console.log('Validate Failed:', info);
    });
  };

  const handleCancel = () => {
    setIsModalVisible(false);
    form.resetFields();
    setEditingId(null);
  };

  const columns = [
    {
      title: t('dialTask.table.id') || 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 150,
    },
    {
      title: t('dialTask.table.name') || 'Name',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: t('dialTask.table.type') || 'Type',
      dataIndex: 'type',
      key: 'type',
      render: text => <Tag color="blue">{t(`dialTask.types.${text}`) || text}</Tag>,
    },
    {
      title: t('dialTask.table.startTime') || 'First Start Time',
      key: 'startTime',
      render: (_, record) => (
        <span>{record.schedule.startTime}</span>
      )
    },
    {
      title: t('dialTask.table.schedule') || 'Schedule',
      key: 'schedule',
      render: (_, record) => (
        <span>
            {`${t('dialTask.form.interval') || 'Interval'}: ${record.schedule.intervalValue} ${t(`dialTask.form.units.${record.schedule.intervalUnit}`) || record.schedule.intervalUnit}`}
        </span>
      )
    },
    {
      title: t('dialTask.table.isEnabled') || 'Is Scheduled',
      dataIndex: 'isEnabled',
      key: 'isEnabled',
      render: isEnabled => (
        <Switch checked={isEnabled} disabled />
      )
    },
    {
      title: t('dialTask.table.creator') || 'Creator',
      dataIndex: 'creator',
      key: 'creator',
    },
    {
      title: t('common.actions') || 'Actions',
      key: 'actions',
      render: (_, record) => (
        <Space size="middle">
          <Button 
            type="text" 
            icon={<EditOutlined />} 
            onClick={() => handleEdit(record)}
            title={t('common.edit') || 'Edit'}
          />
          <Button
            type="text"
            icon={<PlayCircleOutlined />}
            onClick={() => handleRunNow(record)}
            title={t('dialTask.runNow') || 'Run Now'}
          />
          <Button 
            type="text" 
            danger
            icon={<DeleteOutlined />} 
            onClick={() => handleDelete(record)}
            title={t('common.delete') || 'Delete'}
          />
        </Space>
      ),
    },
  ];

  return (
    <div className="dial-task-management">
      <div style={{ marginBottom: 24 }}>
        <Title level={3} style={{ marginBottom: 8 }}>{t('navigation.dialTask') || 'Task Configuration'}</Title>
        <Text type="secondary">
          {t('dialTask.configDescription') || 'Manage scheduled task templates and configurations'}
        </Text>
      </div>

      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
          {t('dialTask.newTemplate') || 'New Template'}
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
        title={editingId ? (t('dialTask.editTemplate') || 'Edit Task Template') : (t('dialTask.createTemplate') || 'Create Task Template')}
        visible={isModalVisible}
        onOk={handleOk}
        onCancel={handleCancel}
        destroyOnClose
        width={700}
      >
        <Form
          form={form}
          layout="vertical"
          name="create_template_form"
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
            name="isEnabled"
            label={t('dialTask.form.isScheduled') || 'Is Scheduled'}
            valuePropName="checked"
            initialValue={true}
          >
            <Switch />
          </Form.Item>

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
                      initialValue={1}
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
        </Form>
      </Modal>
    </div>
  );
};

export default DialTaskManagement;
