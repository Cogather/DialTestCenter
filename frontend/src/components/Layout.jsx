import {
    FileZipOutlined,
    FileTextOutlined,
    MenuFoldOutlined,
    MenuUnfoldOutlined,
    SettingOutlined,
    AppstoreOutlined,
    TeamOutlined,
    DesktopOutlined,
    SafetyCertificateOutlined,
    ToolOutlined,
    HistoryOutlined,
    HomeOutlined,
    CaretDownFilled,
    ScheduleOutlined,
    ExperimentOutlined,
    MonitorOutlined,
    MobileOutlined,
} from '@ant-design/icons'
import { Layout as AntLayout, Button, Menu, Typography, Dropdown } from 'antd'
import React, { useState, useEffect } from 'react'
import { useLocation, useHistory } from 'react-router-dom'
import { useLanguage, useTranslation } from '../hooks/useTranslation.js'
import { clearUsernameCache, createApiRequestConfig } from '../utils/apiUtils.js'

const { Header, Sider, Content, Footer } = AntLayout
const { Text } = Typography

const Layout = ({ children }) => {
  // 从 localStorage 读取菜单折叠状态，如果不存在则默认为 false
  const [collapsed, setCollapsed] = useState(() => {
    const savedCollapsed = localStorage.getItem('menuCollapsed')
    return savedCollapsed === 'true'
  })

  // 从 localStorage 读取展开的子菜单，如果不存在则默认为 ['user']
  const [openKeys, setOpenKeys] = useState(() => {
    const savedOpenKeys = localStorage.getItem('menuOpenKeys')
    if (savedOpenKeys) {
      try {
        return JSON.parse(savedOpenKeys)
      } catch (e) {
        return ['user']
      }
    }
    return ['user']
  })

  // 当 collapsed 状态改变时，保存到 localStorage
  useEffect(() => {
    localStorage.setItem('menuCollapsed', collapsed.toString())
    // 当菜单栏折叠时，清空 openKeys；展开时恢复
    if (collapsed) {
      setOpenKeys([])
    } else {
      // 恢复之前保存的 openKeys
      const savedOpenKeys = localStorage.getItem('menuOpenKeys')
      if (savedOpenKeys) {
        try {
          setOpenKeys(JSON.parse(savedOpenKeys))
        } catch (e) {
          setOpenKeys(['user'])
        }
      } else {
        setOpenKeys(['user'])
      }
    }
  }, [collapsed])

  // 当 openKeys 状态改变时，保存到 localStorage（仅在菜单展开时保存）
  useEffect(() => {
    if (!collapsed) {
      localStorage.setItem('menuOpenKeys', JSON.stringify(openKeys))
    }
  }, [openKeys, collapsed])

  // 处理子菜单展开/收起
  const handleOpenChange = (keys) => {
    setOpenKeys(keys)
  }
  const history = useHistory()
  const location = useLocation()
  const { translateNavigation, translateFooter, translate } = useTranslation()
  const translateHeader = (key) => translate('header', key)
  const { currentLanguage, setLanguage } = useLanguage()
  const currentYear = new Date().getFullYear()

  const menuItems = [
    {
      key: 'task',
      icon: <ScheduleOutlined />,
      label: translateNavigation('taskManagement'),
      children: [
        {
          key: '/task-records',
          icon: <FileTextOutlined />,
          label: translateNavigation('taskRecords'),
        },
        {
          key: '/dial-tasks',
          icon: <ExperimentOutlined />,
          label: translateNavigation('dialTask'),
        },
        {
          key: '/ue-status',
          icon: <MobileOutlined />,
          label: translateNavigation('ueStatus'),
        },
        {
          key: '/executor-status',
          icon: <MonitorOutlined />,
          label: translateNavigation('executorStatus'),
        },
      ],
    },
    {
      key: 'user',
      icon: <TeamOutlined />,
      label: translateNavigation('userManagement'),
      children: [
        {
          key: '/users',
          icon: <DesktopOutlined />,
          label: translateNavigation('userList'),
        },
        {
          key: '/user-roles',
          icon: <SafetyCertificateOutlined />,
          label: translateNavigation('roleManagement'),
        },
      ],
    },
    {
      key: 'config',
      icon: <SettingOutlined />,
      label: translateNavigation('configManagement'),
      children: [
        {
          key: '/test-case-sets',
          icon: <FileZipOutlined />,
          label: translateNavigation('testCaseSetManagement'),
        },
        {
          key: '/software-packages',
          icon: <AppstoreOutlined />,
          label: translateNavigation('softwarePackageManagement'),
        },
        {
          key: '/preprocess-rules',
          icon: <FileTextOutlined />,
          label: translateNavigation('preprocessRuleManagement'),
        },
      ],
    },
    {
      key: 'operation',
      icon: <ToolOutlined />,
      label: translateNavigation('operationManagement'),
      children: [
        {
          key: '/operation-logs',
          icon: <HistoryOutlined />,
          label: translateNavigation('operationLogManagement'),
        },
      ],
    },
  ]

  // 语言切换菜单项
  const handleMenuClick = ({ key }) => {
    history.push(key)
  }

  return (
    <AntLayout style={{ minHeight: '100vh' }}>
      <Sider 
        trigger={null} 
        collapsible 
        collapsed={collapsed}
        style={{ position: 'relative' }}
      >
        <div style={{ 
          height: 32, 
          margin: 16, 
          background: 'transparent',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          borderRadius: '4px'
        }}>
            <img 
              src="data:image/webp;base64,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"
              alt="Dial Test Center" 
              style={{ 
                width: collapsed ? '20px' : '24px', 
                height: collapsed ? '20px' : '24px',
                backgroundColor: 'transparent'
              }} 
            />
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          openKeys={openKeys}
          onOpenChange={handleOpenChange}
          items={menuItems}
          onClick={handleMenuClick}
        />
        {/* 收起按钮 - 放在左下角 */}
        <div style={{
          position: 'absolute',
          bottom: 16,
          left: 16,
          right: 16,
          display: 'flex',
          justifyContent: 'center'
        }}>
          <Button
            type="text"
            icon={collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
            onClick={() => setCollapsed(!collapsed)}
            style={{
              color: 'white',
              fontSize: '16px',
              width: collapsed ? '100%' : '100%',
              height: 40,
              border: '1px solid rgba(255, 255, 255, 0.2)',
              borderRadius: '4px',
              backgroundColor: 'rgba(255, 255, 255, 0.1)',
              transition: 'all 0.3s ease'
            }}
          />
        </div>
      </Sider>
      <AntLayout>
        <Header style={{ padding: 0, background: '#001529', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <span style={{ fontSize: '18px', fontWeight: 'bold', marginLeft: '16px', color: 'white' }}>
             
            </span>
          </div>
        <div style={{ marginRight: '24px', display: 'flex', alignItems: 'center' }}>
          <HomeOutlined 
            onClick={() => {
              sessionStorage.clear()
              window.open('https://www.example.com', '_blank')
            }}
            style={{ color: 'white', fontSize: '18px', marginRight: '12px', cursor: 'pointer' }}
            title={translateHeader('home')}
          />
          <Dropdown 
            menu={{
              items: [
                { key: 'settings', label: translateHeader('settings') },
                { key: 'logout', label: translateHeader('logout') },
              ],
                onClick: async ({ key }) => {
                  if (key === 'settings') {
                    history.push('/settings')
                  } else if (key === 'logout') {
                    try {
                      // 清除缓存的用户信息
                      clearUsernameCache()
                      
                      // 清除权限缓存
                      sessionStorage.removeItem('userPermission')
                      sessionStorage.removeItem('userPermissionTime')
                      sessionStorage.removeItem('userPermissionUsername')
                      
                      const response = await fetch('https://api.example.com/logout', createApiRequestConfig('POST', {}, false))
                      const result = await response.json()
                      if (result.success) {
                        window.location.href = '/login'
                      } else {
                        console.error('Logout failed:', result.message)
                      }
                    } catch (error) {
                      console.error('Logout request failed:', error)
                    }
                  }
                },
            }}
          >
            <CaretDownFilled style={{ color: 'white', fontSize: '12px', marginRight: '16px', cursor: 'pointer' }} />
          </Dropdown>
          <span 
            onClick={() => setLanguage('zh')}
            style={{ 
              color: currentLanguage === 'zh' ? 'white' : 'rgba(255, 255, 255, 0.6)',
              fontSize: '14px',
              fontWeight: 'bold',
              padding: '8px 12px',
              cursor: 'pointer',
              transition: 'all 0.3s ease'
            }}
          >
            中
          </span>
          <span style={{ color: 'rgba(255, 255, 255, 0.4)', margin: '0 2px' }}>|</span>
          <span 
            onClick={() => setLanguage('en')}
            style={{ 
              color: currentLanguage === 'en' ? 'white' : 'rgba(255, 255, 255, 0.6)',
              fontSize: '14px',
              fontWeight: 'bold',
              padding: '8px 12px',
              cursor: 'pointer',
              transition: 'all 0.3s ease'
            }}
          >
            EN
          </span>
        </div>
        </Header>
        <Content
          style={{
            margin: '24px 16px',
            padding: 24,
            minHeight: 280,
            overflow: 'auto',
            height: 'calc(100vh - 64px - 48px - 60px)',
          }}
        >
          {children}
        </Content>
        <Footer
          style={{
            textAlign: 'center',
            backgroundColor: '#f0f2f5',
            borderTop: '1px solid #d9d9d9',
            padding: '12px 24px',
            height: '60px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <Text type="secondary" style={{ fontSize: '14px', color: '#000000' }}>
            {translateFooter('copyright', { year: currentYear }) || `© ${currentYear} Dial Test Center. 版权所有.`}
          </Text>
        </Footer>
      </AntLayout>
    </AntLayout>
  )
}

Layout.displayName = 'Layout'

export default Layout
