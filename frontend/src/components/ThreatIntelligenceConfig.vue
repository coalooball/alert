<template>
  <div class="threat-intelligence-config">
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        添加情报源
      </el-button>
      <el-button @click="loadSources">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
      <el-button @click="testAllConnections">
        <el-icon><Connection /></el-icon>
        测试所有连接
      </el-button>
    </div>

    <el-table :data="sources" v-loading="loading" stripe border>
      <el-table-column prop="sourceName" label="情报源名称" min-width="150" />
      <el-table-column prop="sourceType" label="情报类型" width="120">
        <template #default="{ row }">
          <el-tag :type="getSourceTypeTagType(row.sourceType)">
            {{ getSourceTypeLabel(row.sourceType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="apiUrl" label="API地址" min-width="200" show-overflow-tooltip />
      <el-table-column prop="updateFrequency" label="更新频率" width="120" align="center">
        <template #default="{ row }">
          {{ row.updateFrequency }} 小时
        </template>
      </el-table-column>
      <el-table-column prop="lastUpdate" label="最后更新" width="160" />
      <el-table-column prop="connectionStatus" label="连接状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.connectionStatus === 'connected' ? 'success' : 'danger'">
            {{ row.connectionStatus === 'connected' ? '正常' : '异常' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-switch v-model="row.isEnabled" @change="toggleSource(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleTest(row)">测试</el-button>
          <el-button size="small" @click="handleView(row)">查看</el-button>
          <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Add/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑情报源' : '添加情报源'"
      width="700px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="情报源名称" prop="sourceName">
          <el-input v-model="form.sourceName" placeholder="请输入情报源名称" />
        </el-form-item>

        <el-form-item label="情报类型" prop="sourceType">
          <el-select v-model="form.sourceType" placeholder="请选择情报类型" style="width: 100%">
            <el-option label="IP威胁情报" value="ip_threat" />
            <el-option label="域名威胁情报" value="domain_threat" />
            <el-option label="文件哈希情报" value="file_hash" />
            <el-option label="漏洞情报" value="vulnerability" />
            <el-option label="APT情报" value="apt" />
            <el-option label="综合威胁情报" value="comprehensive" />
          </el-select>
        </el-form-item>

        <el-form-item label="API地址" prop="apiUrl">
          <el-input v-model="form.apiUrl" placeholder="https://api.threatintel.com/v1/feed" />
        </el-form-item>

        <el-form-item label="认证方式" prop="authType">
          <el-radio-group v-model="form.authType">
            <el-radio label="none">无需认证</el-radio>
            <el-radio label="apikey">API Key</el-radio>
            <el-radio label="oauth">OAuth 2.0</el-radio>
            <el-radio label="basic">Basic Auth</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="form.authType === 'apikey'" label="API Key" prop="apiKey">
          <el-input v-model="form.apiKey" type="password" placeholder="请输入API Key" show-password />
        </el-form-item>

        <el-form-item v-if="form.authType === 'basic'" label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>

        <el-form-item v-if="form.authType === 'basic'" label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>

        <el-form-item label="更新频率" prop="updateFrequency">
          <el-input-number
            v-model="form.updateFrequency"
            :min="1"
            :max="168"
            placeholder="小时"
            style="width: 200px"
          />
          <span style="margin-left: 10px">小时</span>
        </el-form-item>

        <el-form-item label="超时时间" prop="timeout">
          <el-input-number
            v-model="form.timeout"
            :min="5"
            :max="300"
            placeholder="秒"
            style="width: 200px"
          />
          <span style="margin-left: 10px">秒</span>
        </el-form-item>

        <el-form-item label="数据格式" prop="dataFormat">
          <el-select v-model="form.dataFormat" placeholder="请选择数据格式" style="width: 100%">
            <el-option label="JSON" value="json" />
            <el-option label="XML" value="xml" />
            <el-option label="CSV" value="csv" />
            <el-option label="STIX" value="stix" />
          </el-select>
        </el-form-item>

        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="情报源描述信息"
          />
        </el-form-item>

        <el-form-item label="启用状态">
          <el-switch v-model="form.isEnabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="warning" @click="handleTestConnection">测试连接</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- View Dialog -->
    <el-dialog v-model="viewDialogVisible" title="情报源详情" width="700px">
      <el-descriptions :column="1" border v-if="viewData">
        <el-descriptions-item label="情报源名称">{{ viewData.sourceName }}</el-descriptions-item>
        <el-descriptions-item label="情报类型">
          <el-tag :type="getSourceTypeTagType(viewData.sourceType)">
            {{ getSourceTypeLabel(viewData.sourceType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="API地址">{{ viewData.apiUrl }}</el-descriptions-item>
        <el-descriptions-item label="认证方式">{{ viewData.authType || '无需认证' }}</el-descriptions-item>
        <el-descriptions-item label="更新频率">{{ viewData.updateFrequency }} 小时</el-descriptions-item>
        <el-descriptions-item label="超时时间">{{ viewData.timeout }} 秒</el-descriptions-item>
        <el-descriptions-item label="数据格式">{{ viewData.dataFormat }}</el-descriptions-item>
        <el-descriptions-item label="最后更新">{{ viewData.lastUpdate }}</el-descriptions-item>
        <el-descriptions-item label="连接状态">
          <el-tag :type="viewData.connectionStatus === 'connected' ? 'success' : 'danger'">
            {{ viewData.connectionStatus === 'connected' ? '正常' : '异常' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="viewData.isEnabled ? 'success' : 'info'">
            {{ viewData.isEnabled ? '已启用' : '已禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="描述">{{ viewData.description || '暂无描述' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ viewData.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ viewData.updateTime }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="viewData && viewData.statistics" style="margin-top: 20px">
        <h4>统计信息</h4>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-statistic title="获取次数" :value="viewData.statistics.fetchCount" />
          </el-col>
          <el-col :span="8">
            <el-statistic title="成功率" :value="viewData.statistics.successRate" suffix="%" />
          </el-col>
          <el-col :span="8">
            <el-statistic title="情报条数" :value="viewData.statistics.totalRecords" />
          </el-col>
        </el-row>
      </div>

      <template #footer>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Connection } from '@element-plus/icons-vue'

export default {
  name: 'ThreatIntelligenceConfig',
  components: { Plus, Refresh, Connection },
  setup() {
    const sources = ref([])
    const loading = ref(false)
    const dialogVisible = ref(false)
    const viewDialogVisible = ref(false)
    const isEdit = ref(false)
    const formRef = ref(null)
    const viewData = ref(null)

    const form = reactive({
      id: null,
      sourceName: '',
      sourceType: '',
      apiUrl: '',
      authType: 'none',
      apiKey: '',
      username: '',
      password: '',
      updateFrequency: 24,
      timeout: 30,
      dataFormat: 'json',
      description: '',
      isEnabled: true
    })

    const rules = {
      sourceName: [
        { required: true, message: '请输入情报源名称', trigger: 'blur' }
      ],
      sourceType: [
        { required: true, message: '请选择情报类型', trigger: 'change' }
      ],
      apiUrl: [
        { required: true, message: '请输入API地址', trigger: 'blur' },
        { type: 'url', message: '请输入有效的URL地址', trigger: 'blur' }
      ],
      updateFrequency: [
        { required: true, message: '请设置更新频率', trigger: 'blur' }
      ],
      timeout: [
        { required: true, message: '请设置超时时间', trigger: 'blur' }
      ],
      dataFormat: [
        { required: true, message: '请选择数据格式', trigger: 'change' }
      ]
    }

    const getSourceTypeTagType = (type) => {
      const typeMap = {
        'ip_threat': 'danger',
        'domain_threat': 'warning',
        'file_hash': 'primary',
        'vulnerability': 'info',
        'apt': 'danger',
        'comprehensive': 'success'
      }
      return typeMap[type] || 'info'
    }

    const getSourceTypeLabel = (type) => {
      const typeMap = {
        'ip_threat': 'IP威胁',
        'domain_threat': '域名威胁',
        'file_hash': '文件哈希',
        'vulnerability': '漏洞情报',
        'apt': 'APT情报',
        'comprehensive': '综合情报'
      }
      return typeMap[type] || type
    }

    const loadSources = () => {
      loading.value = true
      // 模拟数据
      setTimeout(() => {
        sources.value = [
          {
            id: 1,
            sourceName: '国家威胁情报中心',
            sourceType: 'comprehensive',
            apiUrl: 'https://api.threatcenter.cn/v2/feed',
            authType: 'apikey',
            updateFrequency: 12,
            timeout: 30,
            dataFormat: 'json',
            lastUpdate: '2024-01-23 10:00:00',
            connectionStatus: 'connected',
            description: '国家级威胁情报综合数据源',
            isEnabled: true,
            statistics: {
              fetchCount: 1024,
              successRate: 99.8,
              totalRecords: 500000
            },
            createTime: '2024-01-01 10:00:00',
            updateTime: '2024-01-23 10:00:00'
          },
          {
            id: 2,
            sourceName: 'VirusTotal情报源',
            sourceType: 'file_hash',
            apiUrl: 'https://www.virustotal.com/api/v3/files',
            authType: 'apikey',
            updateFrequency: 24,
            timeout: 60,
            dataFormat: 'json',
            lastUpdate: '2024-01-23 08:00:00',
            connectionStatus: 'connected',
            description: '文件哈希威胁检测情报',
            isEnabled: true,
            statistics: {
              fetchCount: 856,
              successRate: 98.5,
              totalRecords: 250000
            },
            createTime: '2024-01-05 14:30:00',
            updateTime: '2024-01-23 08:00:00'
          },
          {
            id: 3,
            sourceName: 'CVE漏洞情报库',
            sourceType: 'vulnerability',
            apiUrl: 'https://services.nvd.nist.gov/rest/json/cves/2.0',
            authType: 'none',
            updateFrequency: 6,
            timeout: 45,
            dataFormat: 'json',
            lastUpdate: '2024-01-23 11:30:00',
            connectionStatus: 'disconnected',
            description: 'CVE漏洞情报数据源',
            isEnabled: false,
            statistics: {
              fetchCount: 2048,
              successRate: 95.2,
              totalRecords: 180000
            },
            createTime: '2023-12-20 09:00:00',
            updateTime: '2024-01-23 11:30:00'
          }
        ]
        loading.value = false
      }, 500)
    }

    const handleAdd = () => {
      isEdit.value = false
      resetForm()
      dialogVisible.value = true
    }

    const handleTest = async (row) => {
      ElMessage.info('正在测试连接...')
      setTimeout(() => {
        if (Math.random() > 0.3) {
          ElMessage.success(`情报源 "${row.sourceName}" 连接测试成功`)
          row.connectionStatus = 'connected'
        } else {
          ElMessage.error(`情报源 "${row.sourceName}" 连接测试失败`)
          row.connectionStatus = 'disconnected'
        }
      }, 1500)
    }

    const handleTestConnection = () => {
      ElMessage.info('正在测试连接...')
      setTimeout(() => {
        if (Math.random() > 0.3) {
          ElMessage.success('连接测试成功')
        } else {
          ElMessage.error('连接测试失败，请检查配置')
        }
      }, 1500)
    }

    const testAllConnections = () => {
      ElMessage.info('正在测试所有连接...')
      setTimeout(() => {
        ElMessage.success('连接测试完成，2个成功，1个失败')
      }, 2000)
    }

    const handleView = (row) => {
      viewData.value = row
      viewDialogVisible.value = true
    }

    const handleEdit = (row) => {
      isEdit.value = true
      Object.assign(form, row)
      dialogVisible.value = true
    }

    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm(
          `确定要删除情报源 "${row.sourceName}" 吗？`,
          '删除确认',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        ElMessage.success('删除成功')
        loadSources()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败')
        }
      }
    }

    const toggleSource = (row) => {
      ElMessage.success(`已${row.isEnabled ? '启用' : '禁用'}情报源`)
    }

    const handleSubmit = async () => {
      try {
        await formRef.value.validate()
        ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
        dialogVisible.value = false
        loadSources()
      } catch (error) {
        ElMessage.error('请填写必填项')
      }
    }

    const resetForm = () => {
      form.id = null
      form.sourceName = ''
      form.sourceType = ''
      form.apiUrl = ''
      form.authType = 'none'
      form.apiKey = ''
      form.username = ''
      form.password = ''
      form.updateFrequency = 24
      form.timeout = 30
      form.dataFormat = 'json'
      form.description = ''
      form.isEnabled = true
      if (formRef.value) {
        formRef.value.clearValidate()
      }
    }

    onMounted(() => {
      loadSources()
    })

    return {
      sources,
      loading,
      dialogVisible,
      viewDialogVisible,
      isEdit,
      formRef,
      viewData,
      form,
      rules,
      getSourceTypeTagType,
      getSourceTypeLabel,
      loadSources,
      handleAdd,
      handleTest,
      handleTestConnection,
      testAllConnections,
      handleView,
      handleEdit,
      handleDelete,
      toggleSource,
      handleSubmit,
      resetForm
    }
  }
}
</script>

<style scoped>
.threat-intelligence-config {
  padding: 10px;
}

.toolbar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

h4 {
  margin-bottom: 15px;
  color: #303133;
  font-size: 16px;
}
</style>