<template>
  <div class="threat-event-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-icon class="header-icon"><Warning /></el-icon>
          <span>威胁事件</span>
        </div>
      </template>
      <div class="empty-content">
        <el-empty description="此页面正在开发中..." />
      </div>
    </el-card>
  </div>
</template>

<script>
import { Warning } from '@element-plus/icons-vue'

export default {
  name: 'ThreatEventPage',
  components: {
    Warning
  }
}
</script>

<style scoped>
.threat-event-page {
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