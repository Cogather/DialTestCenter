import React, { useState, useEffect } from 'react';
import { 
  Table, 
  Card, 
  Button, 
  Input, 
  Space, 
  Tag, 
  Typography,
  Badge
} from 'antd';
import { 
  SearchOutlined, 
  ReloadOutlined, 
  MonitorOutlined
} from '@ant-design/icons';
import { useTranslation } from '../hooks/useTranslation';

const { Title } = Typography;

// 模拟数据生成器 - 与 ExecutorStatus 保持一致但扁平化
const generateMockData = () => {
  const ues = [];
  const executorsCount = 12;
  const prefixes = ['138', '139', '135', '136', '137', '150', '151', '152', '157', '158', '159', '182', '183', '187', '188', '130', '131', '132', '155', '156', '185', '186', '133', '153', '180', '181', '189'];
  
  for (let i = 1; i <= executorsCount; i++) {
    const totalUe = Math.floor(Math.random() * 15) + 5; // 5-20个UE
    const onlineUe = Math.floor(Math.random() * (totalUe + 1)); // 0-total个在线
    const executorName = `Executor-Node-${i.toString().padStart(2, '0')}`;
    
    // 为每个执行机分配一个固定的号段
    const prefix = prefixes[Math.floor(Math.random() * prefixes.length)];
    const regionCode = Math.floor(Math.random() * 9000 + 1000);
    
    for (let j = 1; j <= totalUe; j++) {
        const isRunning = j <= onlineUe && Math.random() > 0.7;
        ues.push({
            msisdn: `86${prefix}${regionCode}${j.toString().padStart(4, '0')}`,
            brand: ['Huawei', 'Xiaomi', 'Oppo', 'Vivo', 'Samsung'][Math.floor(Math.random() * 5)],
            model: `Model-${Math.floor(Math.random() * 100)}`,
            os: `Android ${10 + Math.floor(Math.random() * 4)}`,
            status: j <= onlineUe ? 1 : 0,
            isRunning: isRunning,
            taskId: isRunning ? `TASK-${Date.now()}-${j}` : '-',
            battery: Math.floor(Math.random() * 100),
            executorName: executorName
        });
    }
  }
  return ues;
};

const UeStatus = () => {
  const { t } = useTranslation();
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState([]);
  const [searchText, setSearchText] = useState('');

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

  // 过滤数据
  const filteredData = data.filter(item => 
    item.msisdn.toLowerCase().includes(searchText.toLowerCase()) || 
    item.executorName.toLowerCase().includes(searchText.toLowerCase())
  );

  const columns = [
    {
      title: t('ueStatus.table.msisdn'),
      dataIndex: 'msisdn',
      key: 'msisdn',
      width: 150,
      fixed: 'left',
    },
    {
      title: t('ueStatus.table.executor'),
      dataIndex: 'executorName',
      key: 'executorName',
      width: 180,
    },
    {
      title: t('ueStatus.table.status'),
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status) => (
        <Badge status={status === 1 ? 'success' : 'default'} text={status === 1 ? t('executorStatus.detail.ueTable.online') : t('executorStatus.detail.ueTable.offline')} />
      ),
    },
    {
      title: t('ueStatus.table.runningStatus'),
      key: 'runningStatus',
      width: 100,
      render: (_, record) => (
        <Badge status={record.isRunning ? 'processing' : 'default'} text={record.isRunning ? t('executorStatus.detail.ueTable.running') : t('executorStatus.detail.ueTable.idle')} />
      ),
    },
    {
      title: t('ueStatus.table.taskId'),
      dataIndex: 'taskId',
      key: 'taskId',
      width: 200,
    },
    {
      title: t('ueStatus.table.brandModel'),
      key: 'brandModel',
      render: (_, record) => `${record.brand} ${record.model}`,
    },
    {
      title: t('ueStatus.table.os'),
      dataIndex: 'os',
      key: 'os',
    },
    {
      title: t('ueStatus.table.battery'),
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
    <div className="ue-status-page" style={{ padding: 24 }}>
      {/* 页面标题 */}
      <div style={{ marginBottom: 24 }}>
        <Title level={2} style={{ margin: 0, textAlign: 'left' }}>
          <MonitorOutlined style={{ marginRight: '8px' }} />
          {t('ueStatus.title')}
        </Title>
        <p style={{ color: 'rgba(0, 0, 0, 0.45)', marginTop: 8, textAlign: 'left' }}>
          {t('ueStatus.description')}
        </p>
      </div>

      <Card 
        title={t('ueStatus.title')} 
        extra={
          <Space>
             <Input 
               placeholder={t('ueStatus.searchPlaceholder')} 
               prefix={<SearchOutlined />} 
               value={searchText}
               onChange={e => setSearchText(e.target.value)}
               style={{ width: 300 }}
             />
             <Button icon={<ReloadOutlined />} onClick={loadData}>
               {t('ueStatus.refresh')}
             </Button>
          </Space>
        }
      >
        <Table
          columns={columns}
          dataSource={filteredData}
          rowKey="msisdn"
          loading={loading}
          pagination={{
              defaultPageSize: 10,
              showSizeChanger: true,
              showTotal: (total, range) => t('ueStatus.table.pagination', { start: range[0], end: range[1], total })
          }}
        />
      </Card>
    </div>
  );
};

export default UeStatus;
