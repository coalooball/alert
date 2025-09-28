<template>
  <div class="page-container">
    <el-card class="page-card">
      <template #header>
        <div class="card-header">
          <el-icon class="header-icon"><Tools /></el-icon>
          <span>系统配置</span>
        </div>
      </template>

      <div class="settings-content">
        <el-tabs v-model="activeTab" tab-position="left" class="settings-tabs">
          <el-tab-pane label="用户管理" name="users">
            <UserManagement />
          </el-tab-pane>
          <el-tab-pane label="系统基本信息" name="system">
            <SystemInfo />
          </el-tab-pane>
          <el-tab-pane label="计算引擎" name="compute">
            <ComputeEngine />
          </el-tab-pane>
          <el-tab-pane label="告警数据类型配置" name="alertDataType">
            <AlertDataTypeConfig />
          </el-tab-pane>
          <el-tab-pane label="告警数据存储配置" name="dataStorage">
            <DataStorageConfigTabs />
          </el-tab-pane>
          <el-tab-pane label="告警数据来源配置" name="dataSource">
            <AlertDataSourceConfig />
          </el-tab-pane>
          <el-tab-pane label="恢复默认配置" name="defaultConfig">
            <DefaultConfigManagement />
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-card>
  </div>
</template>

<script>
import { Tools } from '@element-plus/icons-vue'
import UserManagement from '../components/UserManagement.vue'
import SystemInfo from '../components/SystemInfo.vue'
import ComputeEngine from '../views/ComputeEngine.vue'
import AlertDataTypeConfig from '../components/AlertDataTypeConfig.vue'
import DataStorageConfigTabs from '../components/DataStorageConfigTabs.vue'
import AlertDataSourceConfig from '../components/AlertDataSourceConfig.vue'
import DefaultConfigManagement from '../components/DefaultConfigManagement.vue'

export default {
  name: 'SettingsPage',
  components: {
    Tools,
    UserManagement,
    SystemInfo,
    ComputeEngine,
    AlertDataTypeConfig,
    DataStorageConfigTabs,
    AlertDataSourceConfig,
    DefaultConfigManagement
  },
  data() {
    return {
      activeTab: 'users'
    }
  }
}
</script>

<style scoped>
.page-container {
  height: 100%;
}

.page-card {
  height: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: bold;
  font-size: 16px;
}

.header-icon {
  color: #409eff;
}

.settings-content {
  padding: 20px 0;
}

.settings-tabs {
  min-height: 500px;
}

.settings-tabs .el-tabs__content {
  padding-left: 20px;
}
</style>