<template>
  <div class="threat-event-page">
    <el-card class="header-card">
      <div class="header-content">
        <div class="header-left">
          <h2>威胁事件管理</h2>
          <el-tag type="info">{{ statistics.totalEvents }} 个事件</el-tag>
        </div>
        <div class="header-stats">
          <el-statistic title="待处理" :value="statistics.openEvents" />
          <el-statistic title="处理中" :value="statistics.inProgressEvents" />
          <el-statistic title="已解决" :value="statistics.resolvedEvents" />
          <el-statistic title="严重事件" :value="statistics.criticalEvents" />
        </div>
      </div>
    </el-card>

    <el-card class="filter-card">
      <el-row :gutter="20">
        <el-col :span="5">
          <el-select v-model="filters.type" placeholder="事件类型" clearable @change="fetchEvents">
            <el-option v-for="type in eventTypes" :key="type" :label="type" :value="type" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="filters.severity" placeholder="严重程度" clearable @change="fetchEvents">
            <el-option label="严重" value="CRITICAL" />
            <el-option label="高危" value="HIGH" />
            <el-option label="中危" value="MEDIUM" />
            <el-option label="低危" value="LOW" />
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-select v-model="filters.status" placeholder="状态" clearable @change="fetchEvents">
            <el-option label="待处理" value="OPEN" />
            <el-option label="处理中" value="IN_PROGRESS" />
            <el-option label="已解决" value="RESOLVED" />
            <el-option label="已关闭" value="CLOSED" />
          </el-select>
        </el-col>
        <el-col :span="7">
          <el-date-picker
            v-model="filters.dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            @change="fetchEvents"
          />
        </el-col>
        <el-col :span="4" class="button-group">
          <el-button type="primary" @click="fetchEvents">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="resetFilters">
            <el-icon><RefreshRight /></el-icon>
            重置
          </el-button>
          <el-button type="success" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="table-card">
      <div class="table-toolbar">
        <div class="toolbar-left">
          <el-button
            type="danger"
            :disabled="selectedEvents.length === 0"
            @click="handleBatchDelete"
          >
            批量删除 ({{ selectedEvents.length }})
          </el-button>
          <el-dropdown
            v-if="selectedEvents.length > 0"
            trigger="click"
            @command="handleBatchStatusUpdate"
          >
            <el-button type="warning">
              批量更新状态
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="OPEN">待处理</el-dropdown-item>
                <el-dropdown-item command="IN_PROGRESS">处理中</el-dropdown-item>
                <el-dropdown-item command="RESOLVED">已解决</el-dropdown-item>
                <el-dropdown-item command="CLOSED">已关闭</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div class="toolbar-right">
          <el-button @click="fetchEvents">
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="events"
        @selection-change="handleSelectionChange"
        style="width: 100%"
        :height="600"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="systemCode" label="事件编号" width="200" fixed />
        <el-table-column prop="name" label="事件名称" min-width="200">
          <template #default="scope">
            <el-tooltip :content="scope.row.description" placement="top" :disabled="!scope.row.description">
              <span class="event-name">{{ scope.row.name }}</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="120" />
        <el-table-column prop="severity" label="严重程度" width="100">
          <template #default="scope">
            <el-tag :type="getSeverityType(scope.row.severity)">
              {{ getSeverityLabel(scope.row.severity) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="attacker" label="攻击者" width="150" />
        <el-table-column prop="victim" label="受害者" width="150" />
        <el-table-column prop="alertCount" label="关联告警" width="100">
          <template #default="scope">
            <el-badge :value="scope.row.alertCount" :max="99" type="warning">
              <span>{{ scope.row.alertCount || 0 }}</span>
            </el-badge>
          </template>
        </el-table-column>
        <el-table-column prop="riskScore" label="风险评分" width="100">
          <template #default="scope">
            <el-progress
              :percentage="(scope.row.riskScore || 0) * 10"
              :color="getRiskColor(scope.row.riskScore)"
              :stroke-width="6"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="scope">
            {{ formatTime(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button size="small" type="primary" @click="handleView(scope.row)">
              查看
            </el-button>
            <el-button size="small" @click="handleEdit(scope.row)">
              编辑
            </el-button>
            <el-button size="small" type="danger" @click="handleDelete(scope.row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="totalItems > 0"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="totalItems"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handlePageChange"
        class="pagination"
      />
    </el-card>

    <!-- 查看/编辑/新增对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="80%"
      :close-on-click-modal="false"
    >
      <el-form
        ref="eventFormRef"
        :model="currentEvent"
        :rules="eventRules"
        label-width="120px"
        :disabled="isViewMode"
      >
        <el-tabs v-model="activeTab">
          <el-tab-pane label="基本信息" name="basic">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="事件名称" prop="name">
                  <el-input v-model="currentEvent.name" placeholder="请输入事件名称" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="事件类型" prop="type">
                  <el-select v-model="currentEvent.type" placeholder="请选择事件类型">
                    <el-option v-for="type in eventTypes" :key="type" :label="type" :value="type" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="事件描述" prop="description">
              <el-input
                v-model="currentEvent.description"
                type="textarea"
                :rows="3"
                placeholder="请输入事件描述"
              />
            </el-form-item>
            <el-row :gutter="20">
              <el-col :span="8">
                <el-form-item label="严重程度" prop="severity">
                  <el-select v-model="currentEvent.severity" placeholder="请选择严重程度">
                    <el-option label="严重" value="CRITICAL" />
                    <el-option label="高危" value="HIGH" />
                    <el-option label="中危" value="MEDIUM" />
                    <el-option label="低危" value="LOW" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="优先级" prop="priority">
                  <el-select v-model="currentEvent.priority" placeholder="请选择优先级">
                    <el-option label="紧急" value="URGENT" />
                    <el-option label="高" value="HIGH" />
                    <el-option label="中" value="MEDIUM" />
                    <el-option label="低" value="LOW" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="状态" prop="status">
                  <el-select v-model="currentEvent.status" placeholder="请选择状态">
                    <el-option label="待处理" value="OPEN" />
                    <el-option label="处理中" value="IN_PROGRESS" />
                    <el-option label="已解决" value="RESOLVED" />
                    <el-option label="已关闭" value="CLOSED" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
          </el-tab-pane>

          <el-tab-pane label="威胁信息" name="threat">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="攻击者">
                  <el-input v-model="currentEvent.attacker" placeholder="请输入攻击者信息" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="受害者">
                  <el-input v-model="currentEvent.victim" placeholder="请输入受害者信息" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="攻击工具">
                  <el-input v-model="currentEvent.attackTool" placeholder="请输入攻击工具" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="MITRE技术">
                  <el-input v-model="currentEvent.mitreTechniqueId" placeholder="请输入MITRE技术ID" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="攻击列表">
              <el-input
                v-model="currentEvent.attackList"
                type="textarea"
                :rows="2"
                placeholder="请输入攻击列表"
              />
            </el-form-item>
            <el-form-item label="影响评估">
              <el-input
                v-model="currentEvent.impactAssessment"
                type="textarea"
                :rows="2"
                placeholder="请输入影响评估"
              />
            </el-form-item>
          </el-tab-pane>

          <el-tab-pane label="时间信息" name="time">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="开始时间">
                  <el-date-picker
                    v-model="currentEvent.startTime"
                    type="datetime"
                    placeholder="选择开始时间"
                    format="YYYY-MM-DD HH:mm:ss"
                    value-format="YYYY-MM-DD HH:mm:ss"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="结束时间">
                  <el-date-picker
                    v-model="currentEvent.endTime"
                    type="datetime"
                    placeholder="选择结束时间"
                    format="YYYY-MM-DD HH:mm:ss"
                    value-format="YYYY-MM-DD HH:mm:ss"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="发现时间">
                  <el-date-picker
                    v-model="currentEvent.foundTime"
                    type="datetime"
                    placeholder="选择发现时间"
                    format="YYYY-MM-DD HH:mm:ss"
                    value-format="YYYY-MM-DD HH:mm:ss"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="首次发现时间">
                  <el-date-picker
                    v-model="currentEvent.firstFoundTime"
                    type="datetime"
                    placeholder="选择首次发现时间"
                    format="YYYY-MM-DD HH:mm:ss"
                    value-format="YYYY-MM-DD HH:mm:ss"
                  />
                </el-form-item>
              </el-col>
            </el-row>
          </el-tab-pane>

          <el-tab-pane label="资产信息" name="assets">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="攻击方IP">
                  <el-select
                    v-model="currentEvent.attackAssetIps"
                    multiple
                    filterable
                    allow-create
                    default-first-option
                    placeholder="请输入攻击方IP地址"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="受害方IP">
                  <el-select
                    v-model="currentEvent.victimAssetIps"
                    multiple
                    filterable
                    allow-create
                    default-first-option
                    placeholder="请输入受害方IP地址"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="攻击方域名">
                  <el-select
                    v-model="currentEvent.attackAssetDomains"
                    multiple
                    filterable
                    allow-create
                    default-first-option
                    placeholder="请输入攻击方域名"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="受害方域名">
                  <el-select
                    v-model="currentEvent.victimAssetDomains"
                    multiple
                    filterable
                    allow-create
                    default-first-option
                    placeholder="请输入受害方域名"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="攻击URL">
                  <el-select
                    v-model="currentEvent.attackUrls"
                    multiple
                    filterable
                    allow-create
                    default-first-option
                    placeholder="请输入攻击URL"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="恶意软件">
                  <el-select
                    v-model="currentEvent.attackMalwares"
                    multiple
                    filterable
                    allow-create
                    default-first-option
                    placeholder="请输入恶意软件"
                  />
                </el-form-item>
              </el-col>
            </el-row>
          </el-tab-pane>

          <el-tab-pane label="评分信息" name="scoring">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="置信度评分">
                  <el-slider
                    v-model="currentEvent.confidenceScore"
                    :min="0"
                    :max="1"
                    :step="0.1"
                    show-input
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="风险评分">
                  <el-slider
                    v-model="currentEvent.riskScore"
                    :min="0"
                    :max="10"
                    :step="0.5"
                    show-input
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="关联告警数">
                  <el-input-number
                    v-model="currentEvent.alertCount"
                    :min="0"
                    :max="9999"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="自动生成">
                  <el-switch v-model="currentEvent.isAutoGenerated" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-tab-pane>
        </el-tabs>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button v-if="!isViewMode" type="primary" @click="handleSave">
            保存
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  RefreshRight,
  Plus,
  Refresh,
  ArrowDown,
  Warning
} from '@element-plus/icons-vue'
import axios from 'axios'

const loading = ref(false)
const events = ref([])
const selectedEvents = ref([])
const totalItems = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const dialogVisible = ref(false)
const dialogTitle = ref('')
const isViewMode = ref(false)
const activeTab = ref('basic')
const eventFormRef = ref(null)
const eventTypes = ref([])

const filters = reactive({
  type: '',
  severity: '',
  status: '',
  dateRange: null
})

const statistics = reactive({
  totalEvents: 0,
  openEvents: 0,
  inProgressEvents: 0,
  resolvedEvents: 0,
  closedEvents: 0,
  criticalEvents: 0,
  highEvents: 0,
  mediumEvents: 0,
  lowEvents: 0
})

const currentEvent = reactive({
  id: null,
  systemCode: '',
  name: '',
  description: '',
  type: '',
  attacker: '',
  victim: '',
  startTime: null,
  endTime: null,
  foundTime: null,
  firstFoundTime: null,
  source: '',
  mitreTechniqueId: '',
  attackList: '',
  attackTool: '',
  priority: '',
  severity: '',
  disposeStatus: '',
  app: '',
  impactAssessment: '',
  mergeAlerts: '',
  threatActors: '',
  organizations: '',
  status: 'OPEN',
  confidenceScore: 0.5,
  riskScore: 5,
  alertCount: 0,
  isAutoGenerated: false,
  attackAssetIps: [],
  victimAssetIps: [],
  attackAssetDomains: [],
  victimAssetDomains: [],
  attackUrls: [],
  attackMalwares: []
})

const eventRules = {
  name: [
    { required: true, message: '请输入事件名称', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择事件类型', trigger: 'change' }
  ],
  severity: [
    { required: true, message: '请选择严重程度', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ]
}

const fetchEvents = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value,
      type: filters.type,
      severity: filters.severity,
      status: filters.status
    }

    if (filters.dateRange && filters.dateRange.length === 2) {
      params.startTime = filters.dateRange[0]
      params.endTime = filters.dateRange[1]
    }

    const response = await axios.get('/api/threat-events', { params })
    events.value = response.data.events || []
    totalItems.value = response.data.totalItems || 0
  } catch (error) {
    ElMessage.error('获取事件列表失败: ' + (error.response?.data?.message || error.message))
  } finally {
    loading.value = false
  }
}

const fetchStatistics = async () => {
  try {
    const response = await axios.get('/api/threat-events/statistics')
    Object.assign(statistics, response.data.data)
  } catch (error) {
    console.error('获取统计数据失败:', error)
  }
}

const fetchEventTypes = async () => {
  try {
    const response = await axios.get('/api/threat-events/types')
    eventTypes.value = response.data.data || []
  } catch (error) {
    console.error('获取事件类型失败:', error)
  }
}

const resetFilters = () => {
  filters.type = ''
  filters.severity = ''
  filters.status = ''
  filters.dateRange = null
  currentPage.value = 1
  fetchEvents()
}

const handleSelectionChange = (val) => {
  selectedEvents.value = val
}

const handlePageChange = (page) => {
  currentPage.value = page
  fetchEvents()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  fetchEvents()
}

const handleAdd = () => {
  dialogTitle.value = '新增威胁事件'
  isViewMode.value = false
  activeTab.value = 'basic'
  resetCurrentEvent()
  dialogVisible.value = true
}

const handleView = (row) => {
  dialogTitle.value = '查看威胁事件'
  isViewMode.value = true
  activeTab.value = 'basic'
  Object.assign(currentEvent, row)
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑威胁事件'
  isViewMode.value = false
  activeTab.value = 'basic'
  Object.assign(currentEvent, row)
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除事件 "${row.name}" 吗？`,
      '确认删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await axios.delete(`/api/threat-events/${row.id}`)
    ElMessage.success('删除成功')
    fetchEvents()
    fetchStatistics()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败: ' + (error.response?.data?.message || error.message))
    }
  }
}

const handleBatchDelete = async () => {
  if (selectedEvents.value.length === 0) return

  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedEvents.value.length} 个事件吗？`,
      '批量删除',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const ids = selectedEvents.value.map(e => e.id)
    await axios.post('/api/threat-events/batch-delete', ids)
    ElMessage.success(`成功删除 ${selectedEvents.value.length} 个事件`)
    selectedEvents.value = []
    fetchEvents()
    fetchStatistics()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量删除失败: ' + (error.response?.data?.message || error.message))
    }
  }
}

const handleBatchStatusUpdate = async (status) => {
  if (selectedEvents.value.length === 0) return

  try {
    const ids = selectedEvents.value.map(e => e.id)
    await axios.post('/api/threat-events/batch-update-status', { ids, status })
    ElMessage.success(`成功更新 ${selectedEvents.value.length} 个事件的状态`)
    selectedEvents.value = []
    fetchEvents()
    fetchStatistics()
  } catch (error) {
    ElMessage.error('批量更新状态失败: ' + (error.response?.data?.message || error.message))
  }
}

const handleSave = async () => {
  try {
    await eventFormRef.value.validate()

    if (currentEvent.id) {
      await axios.put(`/api/threat-events/${currentEvent.id}`, currentEvent)
      ElMessage.success('更新成功')
    } else {
      await axios.post('/api/threat-events', currentEvent)
      ElMessage.success('创建成功')
    }

    dialogVisible.value = false
    fetchEvents()
    fetchStatistics()
  } catch (error) {
    if (error.response) {
      ElMessage.error('保存失败: ' + (error.response?.data?.message || error.message))
    }
  }
}

const resetCurrentEvent = () => {
  Object.assign(currentEvent, {
    id: null,
    systemCode: '',
    name: '',
    description: '',
    type: '',
    attacker: '',
    victim: '',
    startTime: null,
    endTime: null,
    foundTime: null,
    firstFoundTime: null,
    source: '',
    mitreTechniqueId: '',
    attackList: '',
    attackTool: '',
    priority: '',
    severity: '',
    disposeStatus: '',
    app: '',
    impactAssessment: '',
    mergeAlerts: '',
    threatActors: '',
    organizations: '',
    status: 'OPEN',
    confidenceScore: 0.5,
    riskScore: 5,
    alertCount: 0,
    isAutoGenerated: false,
    attackAssetIps: [],
    victimAssetIps: [],
    attackAssetDomains: [],
    victimAssetDomains: [],
    attackUrls: [],
    attackMalwares: []
  })
}

const getSeverityType = (severity) => {
  const types = {
    'CRITICAL': 'danger',
    'HIGH': 'warning',
    'MEDIUM': '',
    'LOW': 'success'
  }
  return types[severity] || 'info'
}

const getSeverityLabel = (severity) => {
  const labels = {
    'CRITICAL': '严重',
    'HIGH': '高危',
    'MEDIUM': '中危',
    'LOW': '低危'
  }
  return labels[severity] || severity
}

const getStatusType = (status) => {
  const types = {
    'OPEN': 'warning',
    'IN_PROGRESS': '',
    'RESOLVED': 'success',
    'CLOSED': 'info'
  }
  return types[status] || 'info'
}

const getStatusLabel = (status) => {
  const labels = {
    'OPEN': '待处理',
    'IN_PROGRESS': '处理中',
    'RESOLVED': '已解决',
    'CLOSED': '已关闭'
  }
  return labels[status] || status
}

const getRiskColor = (score) => {
  if (!score) return '#909399'
  if (score >= 8) return '#F56C6C'
  if (score >= 5) return '#E6A23C'
  if (score >= 3) return '#409EFF'
  return '#67C23A'
}

const formatTime = (time) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN')
}

onMounted(() => {
  fetchEvents()
  fetchStatistics()
  fetchEventTypes()
})
</script>

<style scoped>
.threat-event-page {
  padding: 20px;
  height: 100%;
  overflow: auto;
}

.header-card {
  margin-bottom: 20px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.header-left h2 {
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

.button-group {
  display: flex;
  gap: 10px;
}

.table-card {
  min-height: 400px;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.toolbar-left {
  display: flex;
  gap: 10px;
}

.event-name {
  font-weight: 500;
  color: #303133;
  cursor: pointer;
}

.event-name:hover {
  color: #409EFF;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

:deep(.el-dialog__body) {
  max-height: 65vh;
  overflow-y: auto;
}

:deep(.el-tabs__content) {
  padding: 20px 0;
}
</style>