<template>
  <div class="compute-engine">
    <div class="header">
      <h2>计算引擎配置</h2>
      <el-button type="primary" @click="showAddDialog" v-if="userStore.isAdmin">
        <el-icon><Plus /></el-icon>
        添加配置
      </el-button>
    </div>

    <el-card class="config-card">
      <el-table :data="configs" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="name" label="配置名称" width="180" />
        <el-table-column prop="job_manager_url" label="Job Manager URL" min-width="200">
          <template #default="{ row }">
            {{ row.job_manager_url }}:{{ row.port }}
          </template>
        </el-table-column>
        <el-table-column prop="is_active" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.is_active ? 'success' : 'info'">
              {{ row.is_active ? '激活' : '未激活' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="is_default" label="默认" width="80" align="center">
          <template #default="{ row }">
            <el-icon v-if="row.is_default" class="default-icon">
              <CircleCheck />
            </el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="last_test_status" label="测试状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.last_test_status"
                    :type="row.last_test_status === 'SUCCESS' ? 'success' : 'danger'">
              {{ row.last_test_status }}
            </el-tag>
            <span v-else class="no-test">未测试</span>
          </template>
        </el-table-column>
        <el-table-column prop="last_test_time" label="最后测试时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.last_test_time) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="testConnection(row)">
              测试连接
            </el-button>
            <el-button type="warning" size="small" @click="showEditDialog(row)" v-if="userStore.isAdmin">
              编辑
            </el-button>
            <el-popconfirm
              title="确定要删除这个配置吗？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="deleteConfig(row.id)"
              v-if="userStore.isAdmin"
            >
              <template #reference>
                <el-button type="danger" size="small">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="配置名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入配置名称" />
        </el-form-item>
        <el-form-item label="Job Manager" prop="jobManagerUrl">
          <el-input v-model="form.jobManagerUrl" placeholder="例如: http://localhost 或 192.168.1.100" />
        </el-form-item>
        <el-form-item label="端口" prop="port">
          <el-input-number v-model="form.port" :min="1" :max="65535" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            placeholder="请输入描述信息"
          />
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="可选，如需认证请输入" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="可选，如需认证请输入" show-password />
        </el-form-item>
        <el-form-item label="连接超时">
          <el-input-number v-model="form.connectionTimeout" :min="1000" :max="60000" :step="1000" />
          <span class="timeout-unit">毫秒</span>
        </el-form-item>
        <el-form-item label="读取超时">
          <el-input-number v-model="form.readTimeout" :min="1000" :max="120000" :step="1000" />
          <span class="timeout-unit">毫秒</span>
        </el-form-item>
        <el-form-item label="设为激活">
          <el-switch v-model="form.isActive" />
          <span class="switch-desc">激活后其他配置将被停用</span>
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" />
          <span class="switch-desc">新任务将默认使用此配置</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="testFormConnection" :loading="testing">测试连接</el-button>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitting">
            {{ isEdit ? '更新' : '添加' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 测试结果对话框 -->
    <el-dialog
      v-model="testResultVisible"
      title="连接测试结果"
      width="600px"
    >
      <div class="test-result" v-if="testResult">
        <el-result
          :icon="testResult.success ? 'success' : 'error'"
          :title="testResult.success ? '连接成功' : '连接失败'"
        >
          <template #sub-title>
            <p>{{ testResult.message }}</p>
            <p v-if="testResult.error" class="error-detail">
              错误: {{ testResult.error }}
              <span v-if="testResult.error_detail">
                - {{ testResult.error_detail }}
              </span>
            </p>
          </template>
          <template #extra v-if="testResult.success">
            <el-descriptions :column="2" border class="test-info">
              <el-descriptions-item label="响应时间">
                {{ testResult.response_time_ms }}ms
              </el-descriptions-item>
              <el-descriptions-item label="Flink版本">
                {{ testResult.flink_version || 'N/A' }}
              </el-descriptions-item>
              <el-descriptions-item label="Task Managers">
                {{ testResult.task_managers || 0 }}
              </el-descriptions-item>
              <el-descriptions-item label="总插槽数">
                {{ testResult.total_task_slots || 0 }}
              </el-descriptions-item>
              <el-descriptions-item label="可用插槽">
                {{ testResult.available_task_slots || 0 }}
              </el-descriptions-item>
              <el-descriptions-item label="运行作业">
                {{ testResult.running_jobs || 0 }}
              </el-descriptions-item>
            </el-descriptions>
          </template>
        </el-result>
      </div>
      <template #footer>
        <el-button type="primary" @click="testResultVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, FormInstance, FormRules } from 'element-plus'
import { Plus, CircleCheck } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'
import axios from '../utils/axios'
import dayjs from 'dayjs'

interface FlinkConfig {
  id: string
  name: string
  job_manager_url: string
  port: number
  description?: string
  is_active: boolean
  is_default: boolean
  username?: string
  password?: string
  connection_timeout: number
  read_timeout: number
  created_at: string
  updated_at: string
  last_test_time?: string
  last_test_status?: string
  last_test_message?: string
}

interface TestResult {
  success: boolean
  message: string
  test_time?: string
  response_time_ms?: number
  flink_version?: string
  cluster_id?: string
  job_manager_address?: string
  task_managers?: number
  total_task_slots?: number
  available_task_slots?: number
  running_jobs?: number
  additional_info?: Record<string, any>
  error?: string
  error_detail?: string
}

const userStore = useUserStore()

const configs = ref<FlinkConfig[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref('')
const submitting = ref(false)
const testing = ref(false)
const testResultVisible = ref(false)
const testResult = ref<TestResult | null>(null)

const formRef = ref<FormInstance>()
const form = reactive({
  name: '',
  jobManagerUrl: '',
  port: 8081,
  description: '',
  username: '',
  password: '',
  connectionTimeout: 5000,
  readTimeout: 10000,
  isActive: false,
  isDefault: false
})

const rules = reactive<FormRules>({
  name: [
    { required: true, message: '请输入配置名称', trigger: 'blur' },
    { min: 1, max: 100, message: '长度在 1 到 100 个字符', trigger: 'blur' }
  ],
  jobManagerUrl: [
    { required: true, message: '请输入 Job Manager URL', trigger: 'blur' },
    { max: 255, message: '长度不能超过 255 个字符', trigger: 'blur' }
  ],
  port: [
    { required: true, message: '请输入端口', trigger: 'change' },
    { type: 'number', min: 1, max: 65535, message: '端口必须在 1-65535 之间', trigger: 'change' }
  ]
})

const dialogTitle = computed(() => isEdit.value ? '编辑 Flink 配置' : '添加 Flink 配置')

const formatDate = (date?: string) => {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

const fetchConfigs = async () => {
  loading.value = true
  try {
    const response = await axios.get('/api/flink-configs')
    configs.value = response.data
  } catch (error: any) {
    ElMessage.error('获取配置列表失败: ' + (error.response?.data?.error || error.message))
  } finally {
    loading.value = false
  }
}

const showAddDialog = () => {
  isEdit.value = false
  dialogVisible.value = true
  resetForm()
}

const showEditDialog = (row: FlinkConfig) => {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, {
    name: row.name,
    jobManagerUrl: row.job_manager_url,
    port: row.port,
    description: row.description || '',
    username: row.username || '',
    password: '', // Don't show existing password
    connectionTimeout: row.connection_timeout,
    readTimeout: row.read_timeout,
    isActive: row.is_active,
    isDefault: row.is_default
  })
  dialogVisible.value = true
}

const resetForm = () => {
  form.name = ''
  form.jobManagerUrl = ''
  form.port = 8081
  form.description = ''
  form.username = ''
  form.password = ''
  form.connectionTimeout = 5000
  form.readTimeout = 10000
  form.isActive = false
  form.isDefault = false
  formRef.value?.clearValidate()
}

const submitForm = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      const data = {
        name: form.name,
        jobManagerUrl: form.jobManagerUrl,
        port: form.port,
        description: form.description || null,
        username: form.username || null,
        password: form.password || null,
        connectionTimeout: form.connectionTimeout,
        readTimeout: form.readTimeout,
        isActive: form.isActive,
        isDefault: form.isDefault
      }

      if (isEdit.value) {
        await axios.put(`/api/flink-configs/${editId.value}`, data)
        ElMessage.success('配置更新成功')
      } else {
        await axios.post('/api/flink-configs', data)
        ElMessage.success('配置添加成功')
      }

      dialogVisible.value = false
      fetchConfigs()
    } catch (error: any) {
      ElMessage.error('操作失败: ' + (error.response?.data?.error || error.message))
    } finally {
      submitting.value = false
    }
  })
}

const deleteConfig = async (id: string) => {
  try {
    await axios.delete(`/api/flink-configs/${id}`)
    ElMessage.success('配置删除成功')
    fetchConfigs()
  } catch (error: any) {
    ElMessage.error('删除失败: ' + (error.response?.data?.error || error.message))
  }
}

const testConnection = async (config: FlinkConfig) => {
  loading.value = true
  try {
    const response = await axios.post(`/api/flink-configs/${config.id}/test`)
    testResult.value = response.data
    testResultVisible.value = true
    // Refresh to show updated test status
    fetchConfigs()
  } catch (error: any) {
    ElMessage.error('测试失败: ' + (error.response?.data?.error || error.message))
  } finally {
    loading.value = false
  }
}

const testFormConnection = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    testing.value = true
    try {
      const data = {
        name: form.name,
        jobManagerUrl: form.jobManagerUrl,
        port: form.port,
        username: form.username || null,
        password: form.password || null,
        connectionTimeout: form.connectionTimeout,
        readTimeout: form.readTimeout,
        isActive: false,
        isDefault: false
      }

      const response = await axios.post('/api/flink-configs/test', data)
      testResult.value = response.data
      testResultVisible.value = true
    } catch (error: any) {
      ElMessage.error('测试失败: ' + (error.response?.data?.error || error.message))
    } finally {
      testing.value = false
    }
  })
}

onMounted(() => {
  fetchConfigs()
})
</script>

<style scoped lang="scss">
.compute-engine {
  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    h2 {
      margin: 0;
      color: var(--el-text-color-primary);
    }
  }

  .config-card {
    .default-icon {
      color: var(--el-color-primary);
      font-size: 20px;
    }

    .no-test {
      color: var(--el-text-color-secondary);
      font-size: 12px;
    }
  }

  .timeout-unit {
    margin-left: 10px;
    color: var(--el-text-color-secondary);
  }

  .switch-desc {
    margin-left: 10px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  .test-result {
    .error-detail {
      color: var(--el-color-danger);
      margin-top: 10px;
      font-size: 14px;
    }

    .test-info {
      margin-top: 20px;
      width: 100%;
    }
  }

  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
  }
}
</style>