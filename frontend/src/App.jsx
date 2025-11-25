import React from 'react'
import { Redirect, Route, Switch } from 'react-router-dom'
import './App.css'
import Layout from './components/Layout'
import { I18nProvider } from './contexts/I18nContext'
import { PermissionProvider } from './hooks/usePermission'
import TestCaseSetManagementPage from './pages/TestCaseSetManagement'
import UserRoleManagementPage from './pages/UserRoleManagement'
import UserManagementPage from './pages/UserManagement'
import SoftwarePackageManagementPage from './pages/SoftwarePackageManagement'
import OperationLogManagementPage from './pages/OperationLogManagement'
import PreprocessRuleManagementPage from './pages/PreprocessRuleManagement'
import DialTaskManagementPage from './pages/DialTaskManagementPage'
import ExecutorStatusPage from './pages/ExecutorStatusPage'

function App() {
  return (
    <I18nProvider>
      <PermissionProvider>
        <Layout>
          <Switch>
            <Route path="/users" component={UserManagementPage} />
            <Route path="/user-roles" component={UserRoleManagementPage} />
            <Route path="/test-case-sets" component={TestCaseSetManagementPage} />
            <Route path="/software-packages" component={SoftwarePackageManagementPage} />
            <Route path="/operation-logs" component={OperationLogManagementPage} />
            <Route path="/preprocess-rules" component={PreprocessRuleManagementPage} />
            <Route path="/dial-tasks" component={DialTaskManagementPage} />
            <Route path="/executor-status" component={ExecutorStatusPage} />
            <Route path="/" exact>
              <Redirect to="/users" />
            </Route>
          </Switch>
        </Layout>
      </PermissionProvider>
    </I18nProvider>
  )
}

export default App
