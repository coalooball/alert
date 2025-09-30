<template>
  <div class="alert-data-page">
    <!-- 告警类型选项卡 -->
    <el-tabs v-model="activeAlertType" @tab-click="handleTabClick" class="alert-tabs">
      <el-tab-pane
        v-for="alertType in alertTypes"
        :key="alertType?.id || alertType"
        :label="alertType?.typeLabel || '未知类型'"
        :name="String(alertType?.id || '')"
      >
        <!-- 统计信息卡片 -->
        <el-row :gutter="20" class="stats-row">
          <el-col :span="6">
            <el-card class="stat-card">
              <el-statistic title="总告警数" :value="statistics[alertType?.id]?.total || 0" />
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card">
              <el-statistic title="今日新增" :value="statistics[alertType?.id]?.today || 0" />
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card">
              <el-statistic title="未处理" :value="statistics[alertType?.id]?.pending || 0" />
            </el-card>
          </el-col>
          <el-col :span="6">
            <el-card class="stat-card">
              <el-statistic title="已标记" :value="statistics[alertType?.id]?.tagged || 0" />
            </el-card>
          </el-col>
        </el-row>

        <!-- 筛选条件 -->
        <el-card class="filter-card">
          <el-form :inline="true" class="filter-form">
            <el-form-item label="时间范围">
              <el-date-picker
                v-model="filters.dateRange"
                type="datetimerange"
                range-separator="至"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
              />
            </el-form-item>
            <el-form-item label="严重级别">
              <el-select v-model="filters.severity" placeholder="请选择" clearable>
                <el-option label="严重" value="CRITICAL" />
                <el-option label="高危" value="HIGH" />
                <el-option label="中危" value="MEDIUM" />
                <el-option label="低危" value="LOW" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="filters.status" placeholder="请选择" clearable>
                <el-option label="新告警" value="NEW" />
                <el-option label="处理中" value="IN_PROGRESS" />
                <el-option label="已解决" value="RESOLVED" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="fetchAlertData">查询</el-button>
              <el-button @click="resetFilters">重置</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <!-- 告警数据表格 -->
        <el-card class="table-card">
          <el-table
            v-loading="loading"
            :data="alertData"
            style="width: 100%"
            @row-click="handleRowClick"
            :row-class-name="getRowClassName"
          >
            <!-- 动态列 - 根据 alert_fields 配置 -->
            <el-table-column
              v-for="field in currentFields"
              :key="field.fieldName"
              :prop="field.fieldName"
              :label="field.fieldLabel"
              :min-width="getColumnWidth(field)"
              :sortable="field.isSortable"
              :show-overflow-tooltip="true"
            >
              <template #default="scope">
                <span v-if="field.fieldName === 'alarm_severity'">
                  <el-tag :type="getSeverityType(scope.row[field.fieldName])">
                    {{ getSeverityLabel(scope.row[field.fieldName]) }}
                  </el-tag>
                </span>
                <span v-else-if="field.fieldName === 'alarm_date'">
                  {{ formatDate(scope.row[field.fieldName]) }}
                </span>
                <span v-else-if="field.fieldName === 'tags'">
                  <el-tag
                    v-for="tag in scope.row.tags"
                    :key="tag.id"
                    :style="{ marginRight: '5px', backgroundColor: tag.color + '20', borderColor: tag.color, color: tag.color }"
                    size="small"
                  >
                    {{ tag.tagName }}
                  </el-tag>
                </span>
                <span v-else>
                  {{ formatFieldValue(scope.row[field.fieldName], field.fieldType) }}
                </span>
              </template>
            </el-table-column>

            <!-- 操作列 -->
            <el-table-column fixed="right" label="操作" width="120">
              <template #default="scope">
                <el-button link type="primary" size="small" @click.stop="viewDetail(scope.row)">
                  详情
                </el-button>
                <el-button link type="primary" size="small" @click.stop="addTag(scope.row)">
                  标记
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <el-pagination
            v-model:current-page="pagination.currentPage"
            v-model:page-size="pagination.pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="pagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
            class="pagination"
          />
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="告警详情"
      width="70%"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item
          v-for="field in currentFields"
          :key="field.fieldName"
          :label="field.fieldLabel"
        >
          <span v-if="field.fieldName === 'alarm_severity'">
            <el-tag :type="getSeverityType(currentAlert[field.fieldName])">
              {{ getSeverityLabel(currentAlert[field.fieldName]) }}
            </el-tag>
          </span>
          <span v-else-if="field.fieldName === 'alarm_date'">
            {{ formatDate(currentAlert[field.fieldName]) }}
          </span>
          <span v-else>
            {{ formatFieldValue(currentAlert[field.fieldName], field.fieldType) }}
          </span>
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="currentAlert.tags && currentAlert.tags.length > 0" class="tags-section">
        <h4>关联标签</h4>
        <el-tag
          v-for="tag in currentAlert.tags"
          :key="tag.id"
          :style="{ marginRight: '10px', backgroundColor: tag.color + '20', borderColor: tag.color, color: tag.color }"
        >
          {{ tag.tagName }} ({{ (tag.confidenceScore * 100).toFixed(0) }}%)
        </el-tag>
      </div>

      <div v-if="currentAlert.rawData" class="raw-data-section">
        <h4>原始数据</h4>
        <pre>{{ JSON.stringify(currentAlert.rawData, null, 2) }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import axios from 'axios'

export default {
  name: 'AlertDataPage',
  data() {
    return {
      activeAlertType: '1',
      alertTypes: [],
      alertFields: {},
      currentFields: [],
      alertData: [],
      loading: false,
      filters: {
        dateRange: null,
        severity: '',
        status: ''
      },
      pagination: {
        currentPage: 1,
        pageSize: 20,
        total: 0
      },
      statistics: {},
      detailDialogVisible: false,
      currentAlert: {}
    }
  },
  watch: {
    // 监听activeAlertType变化，确保数据同步更新
    activeAlertType(newVal, oldVal) {
      if (newVal !== oldVal && newVal) {
        this.updateCurrentFields()
        this.pagination.currentPage = 1
        this.fetchAlertData()
      }
    }
  },
  async mounted() {
    await this.fetchAlertTypes()
    await this.fetchAlertFields()
    await this.fetchAlertData()
    await this.fetchStatistics()
  },
  methods: {
    async fetchAlertTypes() {
      try {
        const response = await axios.get('/api/alert-types')
        // Handle ApiResponse wrapper
        const data = response.data.data || response.data || []
        // Filter out null values and ensure we have valid alert types
        this.alertTypes = Array.isArray(data) ? data.filter(type => type && type.id) : []

        if (this.alertTypes.length > 0) {
          this.activeAlertType = String(this.alertTypes[0].id)
        }
      } catch (error) {
        ElMessage.error('获取告警类型失败: ' + error.message)
        this.alertTypes = [] // Set empty array on error
      }
    },

    async fetchAlertFields() {
      try {
        const response = await axios.get('/api/alert-metadata/fields')

        // 处理嵌套的响应格式
        let fieldsData = response.data
        if (fieldsData.data && typeof fieldsData.data === 'object') {
          // 如果响应是 {data: {1: [...], 2: [...], ...}, success: true} 格式
          this.alertFields = {}
          Object.keys(fieldsData.data).forEach(typeId => {
            const fields = fieldsData.data[typeId]
            if (Array.isArray(fields)) {
              this.alertFields[typeId] = fields.sort((a, b) => a.displayOrder - b.displayOrder)
            }
          })
        } else if (Array.isArray(response.data)) {
          // 如果响应是数组格式（向后兼容）
          const fields = response.data
          // 按告警类型分组
          this.alertFields = {}
          fields.forEach(field => {
            if (!this.alertFields[field.alertTypeId]) {
              this.alertFields[field.alertTypeId] = []
            }
            this.alertFields[field.alertTypeId].push(field)
          })

          // 为每个类型的字段排序
          Object.keys(this.alertFields).forEach(typeId => {
            this.alertFields[typeId].sort((a, b) => a.displayOrder - b.displayOrder)
          })
        }

        // 设置当前字段
        this.updateCurrentFields()
      } catch (error) {
        ElMessage.error('获取字段配置失败: ' + error.message)
      }
    },

    updateCurrentFields() {
      const typeId = parseInt(this.activeAlertType)
      this.currentFields = this.alertFields[typeId] || []

      // 添加标签字段
      if (!this.currentFields.find(f => f.fieldName === 'tags')) {
        this.currentFields.push({
          fieldName: 'tags',
          fieldLabel: '标签',
          fieldType: 'tags',
          displayOrder: 999
        })
      }
    },

    async fetchAlertData() {
      this.loading = true
      try {
        const params = {
          page: this.pagination.currentPage - 1,
          size: this.pagination.pageSize
        }

        // 使用新的API端点
        const response = await axios.get(`/api/alert-mining/alerts/by-type/${this.activeAlertType}`, { params })

        // 转换数据格式以匹配字段配置
        this.alertData = response.data.alerts.map(alert => {
          // 解析parsedData字段
          let parsedData = {}
          if (alert.parsedData) {
            if (typeof alert.parsedData === 'string') {
              try {
                parsedData = JSON.parse(alert.parsedData)
              } catch (e) {
                console.error('Failed to parse parsedData:', e)
              }
            } else {
              parsedData = alert.parsedData
            }
          }

          // 合并所有数据源，优先使用parsedData中的字段
          const formattedAlert = {
            id: alert.id,
            alarm_id: parsedData.alarm_id || alert.alertUuid,
            alarm_date: parsedData.alarm_date || alert.alertTime,
            alarm_severity: parsedData.alarm_severity || this.mapSeverity(alert.severity),
            alarm_name: parsedData.alarm_name || alert.title,
            alarm_description: parsedData.alarm_description || alert.description,
            alarm_type: parsedData.alarm_type || alert.alertType,
            alarm_subtype: parsedData.alarm_subtype || alert.alertSubtype,
            tags: alert.tags,
            rawData: alert.rawData,
            parsedDataObj: parsedData,
            ...parsedData // 展开parsedData中的所有字段
          }

          // 确保所有网络相关字段从parsedData中获取
          if (parsedData.src_ip !== undefined) formattedAlert.src_ip = parsedData.src_ip
          if (parsedData.dst_ip !== undefined) formattedAlert.dst_ip = parsedData.dst_ip
          if (parsedData.src_port !== undefined) formattedAlert.src_port = parsedData.src_port
          if (parsedData.dst_port !== undefined) formattedAlert.dst_port = parsedData.dst_port

          return formattedAlert
        })

        this.pagination.total = response.data.totalItems || 0
      } catch (error) {
        ElMessage.error('获取告警数据失败: ' + error.message)
      } finally {
        this.loading = false
      }
    },

    async fetchStatistics() {
      try {
        const response = await axios.get('/api/alert-mining/statistics')
        // 模拟按类型的统计数据
        this.statistics = {
          1: {
            total: response.data.totalAlerts || 0,
            today: response.data.last24HoursCount || 0,
            pending: Math.floor((response.data.totalAlerts || 0) * 0.3),
            tagged: response.data.autoTaggedAlerts || 0
          },
          2: {
            total: Math.floor((response.data.totalAlerts || 0) * 0.4),
            today: Math.floor((response.data.last24HoursCount || 0) * 0.4),
            pending: Math.floor((response.data.totalAlerts || 0) * 0.2),
            tagged: Math.floor((response.data.autoTaggedAlerts || 0) * 0.4)
          },
          3: {
            total: Math.floor((response.data.totalAlerts || 0) * 0.3),
            today: Math.floor((response.data.last24HoursCount || 0) * 0.3),
            pending: Math.floor((response.data.totalAlerts || 0) * 0.1),
            tagged: Math.floor((response.data.autoTaggedAlerts || 0) * 0.3)
          }
        }
      } catch (error) {
        console.error('获取统计信息失败:', error)
      }
    },

    handleTabClick(tab) {
      // watch会自动处理activeAlertType变化后的更新
      // 这里只需要确保值正确设置即可
      if (tab && tab.props && tab.props.name) {
        this.activeAlertType = tab.props.name
      }
    },

    handlePageChange() {
      this.fetchAlertData()
    },

    handleSizeChange() {
      this.pagination.currentPage = 1
      this.fetchAlertData()
    },

    handleRowClick(row) {
      this.viewDetail(row)
    },

    viewDetail(row) {
      this.currentAlert = row
      this.detailDialogVisible = true
    },

    addTag(row) {
      ElMessage.info('标记功能开发中...')
    },

    resetFilters() {
      this.filters = {
        dateRange: null,
        severity: '',
        status: ''
      }
      this.fetchAlertData()
    },

    getColumnWidth(field) {
      const widthMap = {
        'alarm_id': 120,
        'alarm_date': 160,
        'alarm_severity': 100,
        'alarm_name': 200,
        'alarm_description': 300,
        'source_ip': 130,
        'dest_ip': 130,
        'tags': 200
      }
      return widthMap[field.fieldName] || 120
    },

    getRowClassName({ row }) {
      const severity = row.alarm_severity
      if (severity === 4) return 'critical-row'
      if (severity === 3) return 'high-row'
      if (severity === 2) return 'medium-row'
      return ''
    },

    mapSeverity(severity) {
      const map = {
        'CRITICAL': 4,
        'HIGH': 3,
        'MEDIUM': 2,
        'LOW': 1
      }
      return map[severity] || 0
    },

    getSeverityType(level) {
      const types = {
        4: 'danger',
        3: 'warning',
        2: 'info',
        1: 'success',
        0: 'info'
      }
      return types[level] || 'info'
    },

    getSeverityLabel(level) {
      const labels = {
        4: '严重',
        3: '高危',
        2: '中危',
        1: '低危',
        0: '未知'
      }
      return labels[level] || '未知'
    },

    formatDate(value) {
      if (!value) return '-'
      const date = new Date(value)
      return date.toLocaleString('zh-CN')
    },

    formatFieldValue(value, fieldType) {
      if (value === null || value === undefined) return '-'

      if (fieldType === 'number') {
        return String(value)
      }

      if (fieldType === 'json' && typeof value === 'object') {
        return JSON.stringify(value)
      }

      return String(value)
    }
  }
}
</script>

<style scoped>
.alert-data-page {
  height: 100%;
}

.alert-tabs {
  height: 100%;
}

.stats-row {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-form {
  padding: 10px 0;
}

.table-card {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.tags-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.tags-section h4 {
  margin-bottom: 10px;
}

.raw-data-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.raw-data-section h4 {
  margin-bottom: 10px;
}

.raw-data-section pre {
  background: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
  overflow-x: auto;
}

/* 行高亮样式 */
:deep(.el-table .critical-row) {
  background-color: #fef0f0;
}

:deep(.el-table .high-row) {
  background-color: #fdf6ec;
}

:deep(.el-table .medium-row) {
  background-color: #f0f9ff;
}

:deep(.el-table tbody tr:hover > td) {
  background-color: #f5f7fa !important;
}
</style>