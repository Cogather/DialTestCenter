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
  for (let i = 1; i <= 12; i++) {
    const totalUe = Math.floor(Math.random() * 15) + 5; // 5-20个UE
    const onlineUe = Math.floor(Math.random() * (totalUe + 1)); // 0-total个在线
    const isOnline = Math.random() > 0.2; // 80%在线概率
    
    const ueList = [];
    for (let j = 1; j <= totalUe; j++) {
      ueList.push({
        msisdn: `861380000${i.toString().padStart(2, '0')}${j.toString().padStart(2, '0')}`,
        brand: ['Huawei', 'Xiaomi', 'Oppo', 'Vivo', 'Samsung'][Math.floor(Math.random() * 5)],
        model: `Model-${Math.floor(Math.random() * 100)}`,
        os: `Android ${10 + Math.floor(Math.random() * 4)}`,
        status: j <= onlineUe ? 1 : 0, // 前 onlineUe 个为在线
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

  // 统计数据
  const stats = {
    total: data.length,
    online: data.filter(d => d.status === 1).length,
    ueOnlineRate: (() => {
        const total = data.reduce((acc, cur) => acc + cur.totalUeCount, 0);
        const online = data.reduce((acc, cur) => acc + cur.onlineUeCount, 0);
        return total === 0 ? 0 : Math.round((online / total) * 100);
    })()
  };

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
      title: t('executorStatus.table.ip'),
      dataIndex: 'ip',
      key: 'ip',
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
      render: (_, record) => {
        const rate = record.totalUeCount > 0 
          ? Math.round((record.onlineUeCount / record.totalUeCount) * 100) 
          : 0;
        let status = 'normal';
        if (rate < 50) status = 'exception';
        else if (rate < 80) status = 'active'; // Antd Progress status naming is a bit specific, 'active' is just blue
        
        // 自定义颜色逻辑
        const strokeColor = rate === 100 ? '#52c41a' : (rate < 50 ? '#ff4d4f' : '#1890ff');

        return (
          <Space direction="vertical" size={0} style={{ width: 120 }}>
             <span style={{ fontSize: '12px' }}>
               {t('executorStatus.table.ueCountFormat', { 
                 online: record.onlineUeCount, 
                 total: record.totalUeCount 
               })}
             </span>
             <Progress percent={rate} size="small" strokeColor={strokeColor} showInfo={false} />
          </Space>
        );
      }
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
        <Tag color={status === 1 ? 'success' : 'default'}>
          {status === 1 ? t('executorStatus.detail.ueTable.online') : t('executorStatus.detail.ueTable.offline')}
        </Tag>
      ),
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
    {
      title: t('executorStatus.detail.ueTable.network'),
      dataIndex: 'ip',
      key: 'ip',
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

      <div style={{ marginBottom: 24 }}>
        <Row gutter={16}>
          <Col span={8}>
            <Card>
              <Statistic
                title={t('executorStatus.status.all')}
                value={stats.total}
                prefix={<DesktopOutlined />}
              />
            </Card>
          </Col>
          <Col span={8}>
            <Card>
              <Statistic
                title={t('executorStatus.status.online')}
                value={stats.online}
                valueStyle={{ color: '#3f8600' }}
                prefix={<DesktopOutlined />}
              />
            </Card>
          </Col>
          <Col span={8}>
            <Card>
              <Statistic
                title="总UE在线率"
                value={stats.ueOnlineRate}
                suffix="%"
                prefix={<MobileOutlined />}
              />
            </Card>
          </Col>
        </Row>
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
              <Descriptions.Item label={t('executorStatus.table.ip')}>{selectedExecutor.ip}</Descriptions.Item>
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
               <Descriptions.Item label={t('executorStatus.table.proxy')} span={2}>
                {selectedExecutor.proxy || '-'}
              </Descriptions.Item>
            </Descriptions>

            <div style={{ marginTop: 24 }}>
              <h3>
                  {t('executorStatus.detail.ueList')} 
                  <Tag style={{ marginLeft: 8 }}>
                      {selectedExecutor.onlineUeCount} / {selectedExecutor.totalUeCount} Online
                  </Tag>
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
