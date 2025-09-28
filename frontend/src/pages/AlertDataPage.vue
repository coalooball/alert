<template>
  <div class="alert-data-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-icon class="header-icon"><Document /></el-icon>
          <span>告警数据</span>
        </div>
      </template>
      <div class="empty-content">
        <el-empty description="此页面正在开发中..." />
      </div>
    </el-card>
  </div>
</template>

<script>
import { Document } from '@element-plus/icons-vue'

export default {
  name: 'AlertDataPage',
  components: {
    Document
  }
}
</script>

<style scoped>
.alert-data-page {
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