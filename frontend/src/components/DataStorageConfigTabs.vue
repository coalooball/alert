<template>
  <div class="data-storage-config-tabs">
    <el-tabs v-model="activeTab" type="card">
      <el-tab-pane label="数据库连接管理" name="connections">
        <DataStorageConnections />
      </el-tab-pane>
      <el-tab-pane label="告警数据存储映射" name="mappings">
        <AlertStorageMappings />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import DataStorageConnections from './DataStorageConnections.vue'
import AlertStorageMappings from './AlertStorageMappings.vue'

export default {
  name: 'DataStorageConfigTabs',
  components: {
    DataStorageConnections,
    AlertStorageMappings
  },
  data() {
    return {
      activeTab: 'connections'
    }
  }
}
</script>

<style scoped>
.data-storage-config-tabs {
  height: 100%;
}

.el-tabs {
  height: 100%;
}

:deep(.el-tabs__content) {
  height: calc(100% - 55px);
  overflow: auto;
}
</style>