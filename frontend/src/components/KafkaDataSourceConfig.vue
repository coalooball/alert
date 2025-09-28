<template>
  <div class="kafka-datasource-config">
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        添加Kafka配置
      </el-button>
      <el-button @click="loadConfigs">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <el-table :data="configs" v-loading="loading" stripe border @row-click="handleRowClick">
      <el-table-column prop="configName" label="配置名称" min-width="150" />
      <el-table-column prop="alertTypeName" label="关联告警类型" width="150">
        <template #default="{ row }">
          <el-tag :type="getAlertTypeTagType(row.alertTypeName)">
            {{ row.alertTypeLabel || row.alertTypeName }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="brokers" label="Kafka集群" min-width="200" show-overflow-tooltip />
      <el-table-column prop="topicName" label="Topic" width="150" />
      <el-table-column prop="consumerGroup" label="消费者组" width="120" />
      <el-table-column prop="connectionStatus" label="连接状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.connectionStatus === 'connected' ? 'success' : 'danger'">
            {{ row.connectionStatus === 'connected' ? '正常' : '异常' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-switch v-model="row.isEnabled" @change="toggleConfig(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click.stop="handleTest(row)">测试</el-button>
          <el-button size="small" @click.stop="handleView(row)">查看</el-button>
          <el-button size="small" type="primary" @click.stop="handleEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click.stop="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Add/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑Kafka配置' : '添加Kafka配置'"
      width="800px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="140px">
        <el-form-item label="配置名称" prop="configName">
          <el-input v-model="form.configName" placeholder="请输入配置名称" />
        </el-form-item>

        <el-form-item label="关联告警类型" prop="alertTypeId">
          <el-select v-model="form.alertTypeId" placeholder="请选择告警类型" style="width: 100%" :disabled="isEdit">
            <el-option
              v-for="type in alertTypes"
              :key="type.id"
              :label="type.typeLabel"
              :value="type.id"
              :disabled="isMapped(type.id)"
            >
              <span style="float: left">{{ type.typeLabel }}</span>
              <span v-if="isMapped(type.id)" style="float: right; color: #8492a6; font-size: 12px">
                已配置
              </span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="Kafka集群地址" prop="brokers">
          <el-input
            v-model="form.brokers"
            placeholder="localhost:9092,localhost:9093"
            type="textarea"
            :rows="2"
          />
          <div class="form-tip">多个地址用逗号分隔</div>
        </el-form-item>

        <el-form-item label="Topic名称" prop="topicName">
          <el-input v-model="form.topicName" placeholder="alert-topic" />
        </el-form-item>

        <el-form-item label="消费者组" prop="consumerGroup">
          <el-input v-model="form.consumerGroup" placeholder="alert-consumer-group" />
        </el-form-item>

        <el-form-item label="认证配置">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-select v-model="form.securityProtocol" placeholder="安全协议" style="width: 100%">
                <el-option label="PLAINTEXT" value="PLAINTEXT" />
                <el-option label="SASL_PLAINTEXT" value="SASL_PLAINTEXT" />
                <el-option label="SASL_SSL" value="SASL_SSL" />
                <el-option label="SSL" value="SSL" />
              </el-select>
            </el-col>
            <el-col :span="8">
              <el-select v-model="form.saslMechanism" placeholder="SASL机制" style="width: 100%" :disabled="!form.securityProtocol.includes('SASL')">
                <el-option label="PLAIN" value="PLAIN" />
                <el-option label="SCRAM-SHA-256" value="SCRAM-SHA-256" />
                <el-option label="SCRAM-SHA-512" value="SCRAM-SHA-512" />
              </el-select>
            </el-col>
            <el-col :span="8">
              <el-input v-model="form.username" placeholder="用户名" :disabled="!form.securityProtocol.includes('SASL')" />
            </el-col>
          </el-row>
        </el-form-item>

        <el-form-item label="密码" v-if="form.securityProtocol.includes('SASL')">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>

        <el-form-item label="消费配置">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-input-number
                v-model="form.sessionTimeout"
                :min="1000"
                :max="300000"
                placeholder="会话超时"
                style="width: 100%"
              />
              <div class="form-tip">会话超时(ms)</div>
            </el-col>
            <el-col :span="8">
              <el-input-number
                v-model="form.maxPollRecords"
                :min="1"
                :max="10000"
                placeholder="最大拉取记录数"
                style="width: 100%"
              />
              <div class="form-tip">最大拉取记录数</div>
            </el-col>
            <el-col :span="8">
              <el-select v-model="form.autoOffsetReset" placeholder="偏移量重置" style="width: 100%">
                <el-option label="earliest" value="earliest" />
                <el-option label="latest" value="latest" />
                <el-option label="none" value="none" />
              </el-select>
              <div class="form-tip">偏移量重置策略</div>
            </el-col>
          </el-row>
        </el-form-item>

        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            placeholder="配置描述信息"
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

    <!-- Detail Dialog -->
    <el-dialog v-model="detailDialogVisible" title="Kafka配置详情" width="800px">
      <el-descriptions :column="2" border v-if="detailData">
        <el-descriptions-item label="配置名称" :span="2">{{ detailData.configName }}</el-descriptions-item>
        <el-descriptions-item label="关联告警类型">
          <el-tag :type="getAlertTypeTagType(detailData.alertTypeName)">
            {{ detailData.alertTypeLabel || detailData.alertTypeName }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Topic名称">{{ detailData.topicName }}</el-descriptions-item>
        <el-descriptions-item label="消费者组">{{ detailData.consumerGroup }}</el-descriptions-item>
        <el-descriptions-item label="安全协议">{{ detailData.securityProtocol }}</el-descriptions-item>
        <el-descriptions-item label="会话超时">{{ detailData.sessionTimeout }}ms</el-descriptions-item>
        <el-descriptions-item label="最大拉取记录数">{{ detailData.maxPollRecords }}</el-descriptions-item>
        <el-descriptions-item label="偏移量重置">{{ detailData.autoOffsetReset }}</el-descriptions-item>
        <el-descriptions-item label="连接状态">
          <el-tag :type="detailData.connectionStatus === 'connected' ? 'success' : 'danger'">
            {{ detailData.connectionStatus === 'connected' ? '正常' : '异常' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detailData.isEnabled ? 'success' : 'info'">
            {{ detailData.isEnabled ? '已启用' : '已禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="Kafka集群地址" :span="2">{{ detailData.brokers }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ detailData.description || '暂无描述' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailData.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detailData.updateTime }}</el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleEditFromDetail">编辑</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Right, Delete } from '@element-plus/icons-vue'
import axios from 'axios'

export default {
  name: 'KafkaDataSourceConfig',
  components: { Plus, Refresh, Right, Delete },
  setup() {
    const configs = ref([])
    const loading = ref(false)
    const dialogVisible = ref(false)
    const detailDialogVisible = ref(false)
    const isEdit = ref(false)
    const formRef = ref(null)
    const detailData = ref(null)
    const alertTypes = ref([])
    const alertFields = ref({})

    const form = reactive({
      id: null,
      configName: '',
      alertTypeId: null,
      brokers: '',
      topicName: '',
      consumerGroup: '',
      securityProtocol: 'PLAINTEXT',
      saslMechanism: 'PLAIN',
      username: '',
      password: '',
      sessionTimeout: 30000,
      maxPollRecords: 500,
      autoOffsetReset: 'latest',
      description: '',
      isEnabled: true
    })

    const rules = {
      configName: [
        { required: true, message: '请输入配置名称', trigger: 'blur' }
      ],
      alertTypeId: [
        { required: true, message: '请选择告警类型', trigger: 'change' }
      ],
      brokers: [
        { required: true, message: '请输入Kafka集群地址', trigger: 'blur' }
      ],
      topicName: [
        { required: true, message: '请输入Topic名称', trigger: 'blur' }
      ],
      consumerGroup: [
        { required: true, message: '请输入消费者组', trigger: 'blur' }
      ],
    }

    const getAlertTypeTagType = (typeName) => {
      const typeMap = {
        'network_attack': 'danger',
        'malicious_sample': 'warning',
        'host_behavior': 'primary'
      }
      return typeMap[typeName] || 'info'
    }

    const isMapped = (alertTypeId) => {
      return configs.value.some(c => c.alertTypeId === alertTypeId && c.id !== form.id)
    }

    const loadConfigs = async () => {
      loading.value = true
      try {
        const response = await axios.get('/api/kafka-datasource-config')
        if (response.data.success) {
          configs.value = response.data.data
          testAllConnections()
        }
      } catch (error) {
        ElMessage.error('加载配置失败: ' + (error.response?.data?.message || error.message))
      } finally {
        loading.value = false
      }
    }

    const testAllConnections = async () => {
      for (const config of configs.value) {
        if (!config.brokers || !config.topicName) {
          config.connectionStatus = 'disconnected'
          continue
        }

        try {
          const testData = {
            brokers: config.brokers,
            topicName: config.topicName,
            securityProtocol: config.securityProtocol || 'PLAINTEXT',
            saslMechanism: config.saslMechanism || 'PLAIN',
            username: config.username || '',
            password: config.password || ''
          }
          const response = await axios.post('/api/kafka-datasource-config/test-connection', testData)
          config.connectionStatus = response.data.success ? 'connected' : 'disconnected'
        } catch (error) {
          config.connectionStatus = 'disconnected'
        }
      }
    }

    const loadAlertTypes = async () => {
      try {
        const response = await axios.get('/api/alert-types')
        if (response.data.success) {
          alertTypes.value = response.data.data
        }
      } catch (error) {
        console.error('Failed to load alert types:', error)
      }
    }

    const loadAlertFields = async () => {
      try {
        const response = await axios.get('/api/alert-metadata/fields')
        if (response.data.success) {
          alertFields.value = response.data.data
        }
      } catch (error) {
        console.error('Failed to load alert fields:', error)
      }
    }

    const handleAdd = () => {
      isEdit.value = false
      resetForm()
      dialogVisible.value = true
    }

    const handleRowClick = (row) => {
      detailData.value = row
      detailDialogVisible.value = true
    }

    const handleView = (row) => {
      detailData.value = row
      detailDialogVisible.value = true
    }

    const handleEdit = (row) => {
      isEdit.value = true
      Object.assign(form, row)
      dialogVisible.value = true
    }

    const handleEditFromDetail = () => {
      if (detailData.value) {
        detailDialogVisible.value = false
        handleEdit(detailData.value)
      }
    }

    const handleTest = async (row) => {
      if (!row.brokers || !row.topicName) {
        ElMessage.error('配置数据不完整，无法测试连接')
        return
      }

      try {
        const response = await axios.post('/api/kafka-datasource-config/test-connection', {
          brokers: row.brokers,
          topicName: row.topicName,
          securityProtocol: row.securityProtocol || 'PLAINTEXT',
          saslMechanism: row.saslMechanism || 'PLAIN',
          username: row.username || '',
          password: row.password || ''
        })

        if (response.data.success) {
          ElMessage.success(`Kafka连接 "${row.configName}" 测试成功`)
          row.connectionStatus = 'connected'
        } else {
          ElMessage.error(`Kafka连接 "${row.configName}" 测试失败: ${response.data.message}`)
          row.connectionStatus = 'disconnected'
        }
      } catch (error) {
        ElMessage.error(`Kafka连接 "${row.configName}" 测试失败: ${error.response?.data?.message || error.message}`)
        row.connectionStatus = 'disconnected'
      }
    }

    const handleTestConnection = async () => {
      try {
        await formRef.value.validate()

        ElMessage.info('正在测试连接...')
        const response = await axios.post('/api/kafka-datasource-config/test-connection', {
          brokers: form.brokers,
          topicName: form.topicName,
          securityProtocol: form.securityProtocol,
          saslMechanism: form.saslMechanism,
          username: form.username,
          password: form.password
        })

        if (response.data.success) {
          ElMessage.success('Kafka连接测试成功')
        } else {
          ElMessage.error(response.data.message || '连接测试失败，请检查配置')
        }
      } catch (error) {
        if (error.message) {
          console.error('Validation error:', error)
        } else {
          ElMessage.error('连接测试失败，请检查配置')
        }
      }
    }


    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm(
          `确定要删除配置 "${row.configName}" 吗？`,
          '删除确认',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )

        const response = await axios.delete(`/api/kafka-datasource-config/${row.id}`)
        if (response.data.success) {
          ElMessage.success('删除成功')
          loadConfigs()
        } else {
          ElMessage.error(response.data.message || '删除失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败')
        }
      }
    }

    const toggleConfig = (row) => {
      ElMessage.success(`已${row.isEnabled ? '启用' : '禁用'}配置`)
    }

    const handleSubmit = async () => {
      if (!formRef.value) return

      try {
        await formRef.value.validate()

        const url = isEdit.value
          ? `/api/kafka-datasource-config/${form.id}`
          : '/api/kafka-datasource-config'
        const method = isEdit.value ? 'put' : 'post'

        const response = await axios[method](url, form)
        if (response.data.success) {
          ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
          dialogVisible.value = false
          loadConfigs()
        } else {
          ElMessage.error(response.data.message || '保存失败')
        }
      } catch (error) {
        if (error.response) {
          ElMessage.error(error.response.data?.message || '保存失败')
        } else if (error instanceof Error) {
          console.error('Validation error:', error)
        } else {
          ElMessage.error('保存失败，请检查网络连接')
        }
      }
    }

    const resetForm = () => {
      form.id = null
      form.configName = ''
      form.alertTypeId = null
      form.brokers = ''
      form.topicName = ''
      form.consumerGroup = ''
      form.securityProtocol = 'PLAINTEXT'
      form.saslMechanism = 'PLAIN'
      form.username = ''
      form.password = ''
      form.sessionTimeout = 30000
      form.maxPollRecords = 500
      form.autoOffsetReset = 'latest'
      form.description = ''
      form.isEnabled = true
      if (formRef.value) {
        formRef.value.clearValidate()
      }
    }

    onMounted(() => {
      loadConfigs()
      loadAlertTypes()
      loadAlertFields()
    })

    return {
      configs,
      loading,
      dialogVisible,
      detailDialogVisible,
      isEdit,
      formRef,
      detailData,
      alertTypes,
      form,
      rules,
      getAlertTypeTagType,
      isMapped,
      loadConfigs,
      handleAdd,
      handleRowClick,
      handleView,
      handleEdit,
      handleEditFromDetail,
      handleTest,
      handleTestConnection,
      handleDelete,
      toggleConfig,
      handleSubmit,
      resetForm
    }
  }
}
</script>

<style scoped>
.kafka-datasource-config {
  padding: 10px;
}

.toolbar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}

</style>