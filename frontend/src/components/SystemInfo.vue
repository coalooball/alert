<template>
  <div class="system-info-container">
    <el-card class="info-card">
      <template #header>
        <div class="card-header">
          <el-icon class="header-icon"><Monitor /></el-icon>
          <span>服务器基本信息</span>
          <el-button
            type="primary"
            size="small"
            @click="refreshSystemInfo"
            :loading="loading"
            class="refresh-btn"
          >
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </template>

      <el-row :gutter="20" v-loading="loading">
        <el-col :span="12">
          <div class="info-section">
            <h3 class="section-title">
              <el-icon><DataLine /></el-icon>
              系统信息
            </h3>
            <el-descriptions :column="1" border class="info-descriptions">
              <el-descriptions-item label="系统名称">
                {{ systemInfo.system_name || '未知' }}
              </el-descriptions-item>
              <el-descriptions-item label="系统版本">
                {{ systemInfo.system_version || '未知' }}
              </el-descriptions-item>
              <el-descriptions-item label="内核版本">
                {{ systemInfo.kernel_version || '未知' }}
              </el-descriptions-item>
              <el-descriptions-item label="主机名">
                {{ systemInfo.hostname || '未知' }}
              </el-descriptions-item>
              <el-descriptions-item label="架构">
                {{ systemInfo.architecture || '未知' }}
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>

        <el-col :span="12">
          <div class="info-section">
            <h3 class="section-title">
              <el-icon><Cpu /></el-icon>
              硬件信息
            </h3>
            <el-descriptions :column="1" border class="info-descriptions">
              <el-descriptions-item label="CPU核心数">
                {{ systemInfo.cpu_cores || '未知' }}
              </el-descriptions-item>
              <el-descriptions-item label="总内存">
                {{ formatBytes(systemInfo.total_memory) }}
              </el-descriptions-item>
              <el-descriptions-item label="可用内存">
                {{ formatBytes(systemInfo.available_memory) }}
              </el-descriptions-item>
              <el-descriptions-item label="内存使用率">
                <el-progress
                  :percentage="memoryUsagePercentage"
                  :color="getMemoryColor(memoryUsagePercentage)"
                  class="memory-progress"
                />
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>
      </el-row>

    </el-card>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { Monitor, Refresh, DataLine, Cpu } from '@element-plus/icons-vue'

export default {
  name: 'SystemInfo',
  components: {
    Monitor,
    Refresh,
    DataLine,
    Cpu
  },
  data() {
    return {
      loading: false,
      systemInfo: {
        system_name: '',
        system_version: '',
        kernel_version: '',
        hostname: '',
        architecture: '',
        cpu_cores: 0,
        total_memory: 0,
        available_memory: 0
      }
    }
  },
  computed: {
    memoryUsagePercentage() {
      if (!this.systemInfo.total_memory || !this.systemInfo.available_memory) {
        return 0
      }
      const used = this.systemInfo.total_memory - this.systemInfo.available_memory
      return Math.round((used / this.systemInfo.total_memory) * 100)
    }
  },
  mounted() {
    this.fetchSystemInfo()
  },
  methods: {
    async fetchSystemInfo() {
      this.loading = true
      try {
        const response = await fetch('/api/system-info', {
          credentials: 'include'
        })

        if (response.ok) {
          this.systemInfo = await response.json()
        } else {
          throw new Error('Failed to fetch system info')
        }
      } catch (error) {
        console.error('Error fetching system info:', error)
        ElMessage.error('获取系统信息失败')
      } finally {
        this.loading = false
      }
    },
    async refreshSystemInfo() {
      await this.fetchSystemInfo()
      ElMessage.success('系统信息已刷新')
    },
    formatBytes(bytes) {
      if (!bytes || bytes === 0) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    },
    getMemoryColor(percentage) {
      if (percentage < 60) return '#67c23a'
      if (percentage < 80) return '#e6a23c'
      return '#f56c6c'
    }
  }
}
</script>

<style scoped>
.system-info-container {
  padding: 10px;
}

.info-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-weight: bold;
  font-size: 16px;
}

.card-header .el-icon {
  color: #409eff;
}

.refresh-btn {
  margin-left: auto;
}

.info-section {
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 15px;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.section-title .el-icon {
  color: #409eff;
}

.info-descriptions {
  box-shadow: none;
}

.memory-progress {
  width: 100%;
}

.memory-progress :deep(.el-progress__text) {
  font-size: 12px;
}
</style>