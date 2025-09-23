<template>
  <div class="page-container">
    <el-card class="page-card">
      <template #header>
        <div class="card-header">
          <el-icon class="header-icon"><DataAnalysis /></el-icon>
          <span>告警数据挖掘</span>
        </div>
      </template>
      <div class="empty-content">
        <el-empty description="此页面正在开发中..." />
      </div>
    </el-card>
  </div>
</template>

<script>
import { DataAnalysis } from '@element-plus/icons-vue'

export default {
  name: 'DataMiningPage',
  components: {
    DataAnalysis
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

.empty-content {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}
</style>