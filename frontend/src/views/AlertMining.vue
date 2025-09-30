<template>
  <div class="alert-mining">
    <el-card class="header-card">
      <div class="header-content">
        <h2>告警数据挖掘</h2>
        <div class="header-stats">
          <el-statistic title="总告警数" :value="statistics.totalAlerts" />
          <el-statistic title="24小时" :value="statistics.last24HoursCount" />
          <el-statistic title="7天" :value="statistics.last7DaysCount" />
          <el-statistic title="自动标记" :value="statistics.autoTaggedAlerts" />
        </div>
      </div>
    </el-card>

    <el-card class="filter-card">
      <el-row :gutter="20">
        <el-col :span="6">
          <el-select v-model="filters.alertType" placeholder="选择告警类型" clearable @change="fetchAlerts">
            <el-option label="网络攻击" value="1" />
            <el-option label="恶意样本" value="2" />
            <el-option label="主机行为" value="3" />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-select v-model="filters.severity" placeholder="选择严重级别" clearable @change="fetchAlerts">
            <el-option label="严重" value="CRITICAL" />
            <el-option label="高危" value="HIGH" />
            <el-option label="中危" value="MEDIUM" />
            <el-option label="低危" value="LOW" />
          </el-select>
        </el-col>
        <el-col :span="8">
          <el-date-picker
            v-model="filters.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            @change="fetchAlerts"
          />
        </el-col>
        <el-col :span="4">
          <el-button type="primary" @click="fetchAlerts">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="resetFilters">
            <el-icon><RefreshRight /></el-icon>
            重置
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="alert-list">
      <div v-loading="loading">
        <div v-for="alert in alerts" :key="alert.id" class="alert-item">
          <el-row class="alert-header">
            <el-col :span="16">
              <span class="alert-id">{{ alert.alertUuid }}</span>
              <el-tag :type="getSeverityType(alert.severity)" size="small" class="severity-tag">
                {{ alert.severity }}
              </el-tag>
              <el-tag type="info" size="small">{{ alert.priority }}</el-tag>
              <el-tag type="warning" size="small">{{ alert.status }}</el-tag>
            </el-col>
            <el-col :span="8" class="text-right">
              <span class="alert-time">{{ formatTime(alert.alertTime) }}</span>
            </el-col>
          </el-row>

          <div class="alert-title">{{ alert.title }}</div>
          <div class="alert-description">{{ alert.description }}</div>

          <el-descriptions :column="2" border size="small" class="alert-details">
            <el-descriptions-item label="告警类型">{{ alert.alertType }}</el-descriptions-item>
            <el-descriptions-item label="子类型">{{ alert.alertSubtype }}</el-descriptions-item>
            <el-descriptions-item label="源IP" v-if="alert.sourceIp">
              {{ alert.sourceIp }}{{ alert.sourcePort ? ':' + alert.sourcePort : '' }}
            </el-descriptions-item>
            <el-descriptions-item label="目标IP" v-if="alert.destIp">
              {{ alert.destIp }}{{ alert.destPort ? ':' + alert.destPort : '' }}
            </el-descriptions-item>
            <el-descriptions-item label="存储表">{{ alert.clickhouseTable }}</el-descriptions-item>
            <el-descriptions-item label="是否过滤">
              <el-tag :type="alert.isFiltered ? 'danger' : 'success'" size="small">
                {{ alert.isFiltered ? '已过滤' : '未过滤' }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>

          <div v-if="alert.rawData && Object.keys(alert.rawData).length > 0" class="data-section">
            <div class="section-title">原始数据</div>
            <el-descriptions :column="3" size="small">
              <el-descriptions-item
                v-for="(value, key) in alert.rawData"
                :key="key"
                :label="key"
              >
                {{ value }}
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <div v-if="alert.parsedData && Object.keys(alert.parsedData).length > 0" class="data-section">
            <div class="section-title">解析数据</div>
            <el-descriptions :column="3" size="small">
              <el-descriptions-item
                v-for="(value, key) in alert.parsedData"
                :key="key"
                :label="key"
              >
                {{ value }}
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <div v-if="alert.tags && alert.tags.length > 0" class="tags-section">
            <div class="section-title">关联标签</div>
            <div class="tag-list">
              <el-tooltip
                v-for="tag in alert.tags"
                :key="tag.id"
                effect="dark"
                placement="top"
              >
                <template #content>
                  <div>类型: {{ tag.tagType }}</div>
                  <div>描述: {{ tag.description }}</div>
                  <div>置信度: {{ (tag.confidenceScore * 100).toFixed(1) }}%</div>
                  <div>标记: {{ tag.isAutoTagged ? '自动' : '手动' }}</div>
                </template>
                <el-tag
                  :style="{ backgroundColor: tag.color + '20', borderColor: tag.color, color: tag.color }"
                  class="custom-tag"
                >
                  <el-icon v-if="tag.isAutoTagged" size="12"><MagicStick /></el-icon>
                  {{ tag.tagName }}
                  <span class="confidence">{{ (tag.confidenceScore * 100).toFixed(0) }}%</span>
                </el-tag>
              </el-tooltip>
            </div>
          </div>
        </div>

        <el-empty v-if="!loading && alerts.length === 0" description="暂无告警数据" />

        <el-pagination
          v-if="alerts.length > 0"
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="totalItems"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
          class="pagination"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, RefreshRight, MagicStick } from '@element-plus/icons-vue'
import axios from 'axios'

const loading = ref(false)
const alerts = ref([])
const statistics = ref({
  totalAlerts: 0,
  last24HoursCount: 0,
  last7DaysCount: 0,
  last30DaysCount: 0,
  autoTaggedAlerts: 0,
  filteredAlertsLast24Hours: 0
})

const filters = ref({
  alertType: '',
  severity: '',
  dateRange: null
})

const currentPage = ref(1)
const pageSize = ref(20)
const totalItems = ref(0)

const fetchAlerts = async () => {
  loading.value = true
  try {
    let url = `/api/alert-mining/alerts?page=${currentPage.value - 1}&size=${pageSize.value}`

    if (filters.value.alertType) {
      url = `/api/alert-mining/alerts/by-type/${filters.value.alertType}?page=${currentPage.value - 1}&size=${pageSize.value}`
    } else if (filters.value.dateRange) {
      const [startTime, endTime] = filters.value.dateRange
      url = `/api/alert-mining/alerts/by-time-range?startTime=${startTime}&endTime=${endTime}&page=${currentPage.value - 1}&size=${pageSize.value}`
    }

    const response = await axios.get(url)
    alerts.value = response.data.alerts || []
    totalItems.value = response.data.totalItems || 0

    // 过滤严重级别（前端过滤）
    if (filters.value.severity) {
      alerts.value = alerts.value.filter(alert => alert.severity === filters.value.severity)
    }
  } catch (error) {
    ElMessage.error('获取告警数据失败: ' + (error.response?.data?.message || error.message))
  } finally {
    loading.value = false
  }
}

const fetchStatistics = async () => {
  try {
    const response = await axios.get('/api/alert-mining/statistics')
    statistics.value = response.data
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

const resetFilters = () => {
  filters.value = {
    alertType: '',
    severity: '',
    dateRange: null
  }
  currentPage.value = 1
  fetchAlerts()
}

const handlePageChange = (page) => {
  currentPage.value = page
  fetchAlerts()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  fetchAlerts()
}

const getSeverityType = (severity) => {
  const types = {
    'CRITICAL': 'danger',
    'HIGH': 'warning',
    'MEDIUM': 'info',
    'LOW': 'success'
  }
  return types[severity] || 'info'
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchAlerts()
  fetchStatistics()
})
</script>

<style scoped>
.alert-mining {
  padding: 20px;
}

.header-card {
  margin-bottom: 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-content h2 {
  margin: 0;
  color: #303133;
}

.header-stats {
  display: flex;
  gap: 40px;
}

.filter-card {
  margin-bottom: 20px;
}

.alert-list {
  min-height: 400px;
}

.alert-item {
  padding: 20px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  margin-bottom: 20px;
  transition: all 0.3s;
}

.alert-item:hover {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.alert-header {
  margin-bottom: 15px;
  align-items: center;
}

.alert-id {
  font-weight: bold;
  font-size: 16px;
  color: #303133;
  margin-right: 10px;
}

.severity-tag {
  margin-right: 8px;
}

.alert-time {
  color: #909399;
  font-size: 14px;
}

.alert-title {
  font-size: 18px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 8px;
}

.alert-description {
  color: #606266;
  margin-bottom: 15px;
  line-height: 1.6;
}

.alert-details {
  margin-bottom: 15px;
}

.data-section {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #ebeef5;
}

.section-title {
  font-weight: 500;
  color: #606266;
  margin-bottom: 10px;
}

.tags-section {
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #ebeef5;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.custom-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  font-size: 13px;
  border-width: 1px;
  border-style: solid;
}

.confidence {
  font-size: 11px;
  opacity: 0.8;
  margin-left: 4px;
}

.text-right {
  text-align: right;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}
</style>