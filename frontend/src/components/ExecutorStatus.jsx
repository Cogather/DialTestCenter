import React, { useState, useEffect } from 'react';
import { 
  Table, 
  Card, 
  Badge, 
  Button, 
  Input, 
  Space, 
  Drawer, 
  Descriptions, 
  Tag, 
  Statistic, 
  Row, 
  Col,
  Tooltip,
  Progress,
  Typography
} from 'antd';
import { 
  SearchOutlined, 
  ReloadOutlined, 
  DesktopOutlined, 
  MobileOutlined,
  InfoCircleOutlined,
  MonitorOutlined
} from '@ant-design/icons';
import { useTranslation } from '../hooks/useTranslation';

const { Title } = Typography;

// 模拟数据生成器
const generateMockData = () => {
  const executors = [];
  const prefixes = ['138', '139', '135', '136', '137', '150', '151', '152', '157', '158', '159', '182', '183', '187', '188', '130', '131', '132', '155', '156', '185', '186', '133', '153', '180', '181', '189'];
  
  for (let i = 1; i <= 12; i++) {
    const totalUe = Math.floor(Math.random() * 15) + 5; // 5-20个UE
    const onlineUe = Math.floor(Math.random() * (totalUe + 1)); // 0-total个在线
    const isOnline = Math.random() > 0.2; // 80%在线概率
    
    // 为每个执行机分配一个固定的号段，模拟真实的批量卡
    const prefix = prefixes[Math.floor(Math.random() * prefixes.length)];
    const regionCode = Math.floor(Math.random() * 9000 + 1000); // 4位地区码
    
    const ueList = [];
    for (let j = 1; j <= totalUe; j++) {
      ueList.push({
        msisdn: `86${prefix}${regionCode}${j.toString().padStart(4, '0')}`,
        brand: ['Huawei', 'Xiaomi', 'Oppo', 'Vivo', 'Samsung'][Math.floor(Math.random() * 5)],
        model: `Model-${Math.floor(Math.random() * 100)}`,
        os: `Android ${10 + Math.floor(Math.random() * 4)}`,
        status: j <= onlineUe ? 1 : 0, // 前 onlineUe 个为在线
        isRunning: j <= onlineUe && Math.random() > 0.7, // 只有在线的设备才有概率在运行任务
        get taskId() { return this.isRunning ? `TASK-${Date.now()}-${j}` : '-'; },
        battery: Math.floor(Math.random() * 100),
        ip: `192.168.1.${100 + j}`
      });
    }

    executors.push({
      name: `Executor-Node-${i.toString().padStart(2, '0')}`,
      ip: `10.10.50.${i}`,
      status: isOnline ? 1 : 0,
      lastOnlineTime: new Date(Date.now() - Math.floor(Math.random() * 1000000)).toISOString(),
      description: `测试机房 A区 ${i}号机柜`,
      proxy: Math.random() > 0.8 ? 'http://proxy.example.com:8080' : null,
      ueList: ueList,
      onlineUeCount: onlineUe,
      totalUeCount: totalUe
    });
  }
  return executors;
};

const ExecutorStatus = () => {
  const { t } = useTranslation();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState([]);
  const [searchText, setSearchText] = useState('');
  const [selectedExecutor, setSelectedExecutor] = useState(null);
  const [drawerVisible, setDrawerVisible] = useState(false);

  // 加载数据
  const loadData = () => {
    setLoading(true);
    // 模拟API延迟
    setTimeout(() => {
      setData(generateMockData());
      setLoading(false);
    }, 800);
  };

  useEffect(() => {
    loadData();
  }, []);

  // 处理查看详情
  const handleViewDetail = (record) => {
    setSelectedExecutor(record);
    setDrawerVisible(true);
  };

  const handleCloseDrawer = () => {
    setDrawerVisible(false);
    setSelectedExecutor(null);
  };

  // 过滤数据
  const filteredData = data.filter(item => 
    item.name.toLowerCase().includes(searchText.toLowerCase()) || 
    item.ip.includes(searchText)
  );


  const columns = [
    {
      title: t('executorStatus.table.name'),
      dataIndex: 'name',
      key: 'name',
      render: (text, record) => (
        <Space>
          <DesktopOutlined />
          <a onClick={() => handleViewDetail(record)} style={{ fontWeight: 'bold' }}>{text}</a>
        </Space>
      ),
    },
    {
      title: t('executorStatus.table.status'),
      dataIndex: 'status',
      key: 'status',
      render: (status) => (
        <Badge 
          status={status === 1 ? 'success' : 'error'} 
          text={status === 1 ? t('executorStatus.status.online') : t('executorStatus.status.offline')} 
        />
      ),
    },
    {
      title: t('executorStatus.table.ueSummary'),
      key: 'ueSummary',
      render: (_, record) => (
        <span>{record.onlineUeCount}</span>
      )
    },
    {
      title: t('executorStatus.table.lastHeartbeat'),
      dataIndex: 'lastOnlineTime',
      key: 'lastOnlineTime',
      render: (text) => new Date(text).toLocaleString(),
    },
    {
      title: t('executorStatus.table.actions'),
      key: 'actions',
      render: (_, record) => (
        <Button type="link" size="small" onClick={() => handleViewDetail(record)}>
          {t('executorStatus.table.viewDetail')}
        </Button>
      ),
    },
  ];

  const ueColumns = [
    {
      title: t('executorStatus.detail.ueTable.msisdn'),
      dataIndex: 'msisdn',
      key: 'msisdn',
      width: 150,
      fixed: 'left',
    },
    {
      title: t('executorStatus.detail.ueTable.status'),
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => (
        <Badge 
          status={status === 1 ? 'success' : 'default'} 
          text={status === 1 ? t('executorStatus.detail.ueTable.online') : t('executorStatus.detail.ueTable.offline')} 
        />
      ),
    },
    {
      title: t('executorStatus.detail.ueTable.runningStatus'),
      key: 'runningStatus',
      width: 100,
      render: (_, record) => (
        <Badge 
          status={record.isRunning ? 'processing' : 'default'} 
          text={record.isRunning ? t('executorStatus.detail.ueTable.running') : t('executorStatus.detail.ueTable.idle')} 
        />
      ),
    },
    {
      title: t('executorStatus.detail.ueTable.taskId'),
      dataIndex: 'taskId',
      key: 'taskId',
      width: 200,
    },
    {
      title: t('executorStatus.detail.ueTable.brandModel'),
      key: 'brandModel',
      render: (_, record) => `${record.brand} ${record.model}`,
    },
    {
      title: t('executorStatus.detail.ueTable.os'),
      dataIndex: 'os',
      key: 'os',
    },
    {
      title: t('executorStatus.detail.ueTable.battery'),
      dataIndex: 'battery',
      key: 'battery',
      render: (val) => (
        <span style={{ color: val < 20 ? 'red' : 'inherit' }}>
          {val}%
        </span>
      )
    },
  ];

  return (
    <div className="executor-status-page" style={{ padding: 24 }}>
      {/* 页面标题 */}
      <div style={{ marginBottom: 24 }}>
        <Title level={2} style={{ margin: 0, textAlign: 'left' }}>
          <MonitorOutlined style={{ marginRight: '8px' }} />
          {t('executorStatus.title')}
        </Title>
        <p style={{ color: 'rgba(0, 0, 0, 0.45)', marginTop: 8, textAlign: 'left' }}>
          {t('executorStatus.description')}
        </p>
      </div>


      <Card 
        title={t('executorStatus.title')} 
        extra={
          <Space>
             <Input 
               placeholder={t('executorStatus.searchPlaceholder')} 
               prefix={<SearchOutlined />} 
               value={searchText}
               onChange={e => setSearchText(e.target.value)}
               style={{ width: 200 }}
             />
             <Button icon={<ReloadOutlined />} onClick={loadData}>
               {t('executorStatus.refresh')}
             </Button>
          </Space>
        }
      >
        <Table
          columns={columns}
          dataSource={filteredData}
          rowKey="name"
          loading={loading}
          pagination={{
              defaultPageSize: 10,
              showSizeChanger: true,
              showTotal: (total, range) => t('executorStatus.table.pagination', { start: range[0], end: range[1], total })
          }}
        />
      </Card>

      <Drawer
        title={t('executorStatus.detail.title')}
        placement="right"
        width={800}
        onClose={handleCloseDrawer}
        open={drawerVisible}
      >
        {selectedExecutor && (
          <>
            <Descriptions title={t('executorStatus.detail.basicInfo')} bordered column={2}>
              <Descriptions.Item label={t('executorStatus.table.name')}>{selectedExecutor.name}</Descriptions.Item>
              <Descriptions.Item label={t('executorStatus.table.status')}>
                 <Badge 
                  status={selectedExecutor.status === 1 ? 'success' : 'error'} 
                  text={selectedExecutor.status === 1 ? t('executorStatus.status.online') : t('executorStatus.status.offline')} 
                />
              </Descriptions.Item>
              <Descriptions.Item label={t('executorStatus.table.lastHeartbeat')}>
                 {new Date(selectedExecutor.lastOnlineTime).toLocaleString()}
              </Descriptions.Item>
              <Descriptions.Item label={t('executorStatus.table.description')} span={2}>
                {selectedExecutor.description}
              </Descriptions.Item>
            </Descriptions>

            <div style={{ marginTop: 24 }}>
              <h3>
                  {t('executorStatus.detail.ueList')} 
              </h3>
              <Table
                columns={ueColumns}
                dataSource={selectedExecutor.ueList}
                rowKey="msisdn"
                pagination={false}
                size="small"
                scroll={{ y: 400 }}
              />
            </div>
          </>
        )}
      </Drawer>
    </div>
  );
};

export default ExecutorStatus;
