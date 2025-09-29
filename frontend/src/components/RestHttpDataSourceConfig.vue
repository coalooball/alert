<template>
  <div class="rest-http-datasource-config">
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        添加HTTP接口配置
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
      <el-table-column prop="endpointPath" label="接口路径" min-width="250" show-overflow-tooltip />
      <el-table-column prop="method" label="请求方法" width="100">
        <template #default="{ row }">
          <el-tag :type="getMethodTagType(row.method)">{{ row.method }}</el-tag>
        </template>
      </el-table-column>
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
      :title="isEdit ? '编辑HTTP接口配置' : '添加HTTP接口配置'"
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

        <el-form-item label="接口路径" prop="endpointPath">
          <el-input v-model="form.endpointPath" placeholder="/api/alerts/network-attack">
            <template #prepend>{{ baseUrl }}</template>
          </el-input>
          <div class="form-tip">完整接口地址: {{ baseUrl }}{{ form.endpointPath }}</div>
        </el-form-item>

        <el-form-item label="请求方法">
          <el-input value="POST" disabled style="width: 200px">
            <template #prepend>HTTP</template>
          </el-input>
          <div class="form-tip">固定为POST方法，用于接收告警数据</div>
        </el-form-item>

        <el-form-item label="字段映射" v-if="form.alertTypeId">
          <div class="field-mapping">
            <div class="form-tip" style="margin-bottom: 10px">
              配置接收到的JSON数据字段与告警字段的映射关系
            </div>
            <div v-for="(mapping, index) in form.fieldMapping" :key="index" class="mapping-item">
              <el-input v-model="mapping.sourceField" placeholder="JSON字段路径 (如: data.src_ip)" style="width: 250px" />
              <el-icon class="mapping-arrow"><Right /></el-icon>
              <el-select v-model="mapping.targetField" placeholder="目标字段" style="width: 200px">
                <el-option
                  v-for="field in currentAlertFields"
                  :key="field.value"
                  :label="field.label"
                  :value="field.value"
                />
              </el-select>
              <el-button type="danger" size="small" @click="removeMapping(index)" style="margin-left: 10px">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button type="primary" size="small" @click="addMapping">
              <el-icon><Plus /></el-icon>
              添加映射
            </el-button>
          </div>
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
    <el-dialog v-model="detailDialogVisible" title="HTTP接口配置详情" width="800px">
      <el-descriptions :column="2" border v-if="detailData">
        <el-descriptions-item label="配置名称" :span="2">{{ detailData.configName }}</el-descriptions-item>
        <el-descriptions-item label="关联告警类型">
          <el-tag :type="getAlertTypeTagType(detailData.alertTypeName)">
            {{ detailData.alertTypeLabel || detailData.alertTypeName }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="请求方法">
          <el-tag :type="getMethodTagType(detailData.method)">{{ detailData.method }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="接口地址" :span="2">{{ detailData.endpointPath }}</el-descriptions-item>
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
        <el-descriptions-item label="描述" :span="2">{{ detailData.description || '暂无描述' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detailData.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detailData.updateTime }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="detailData && detailData.fieldMapping && detailData.fieldMapping.length" style="margin-top: 20px">
        <h4>字段映射</h4>
        <el-table :data="detailData.fieldMapping" border size="small">
          <el-table-column prop="sourceField" label="响应字段" />
          <el-table-column prop="targetField" label="目标字段" />
        </el-table>
      </div>

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
import axios from '../utils/axios'

export default {
  name: 'RestHttpDataSourceConfig',
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

    const baseUrl = ref('http://localhost:3000')

    const form = reactive({
      id: null,
      configName: '',
      alertTypeId: null,
      endpointPath: '',
      method: 'POST',
      authType: 'none',
      fieldMapping: [{ sourceField: '', targetField: '' }],
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
      endpointPath: [
        { required: true, message: '请输入接口路径', trigger: 'blur' },
        { pattern: /^\//, message: '接口路径必须以/开头', trigger: 'blur' }
      ]
    }

    const currentAlertFields = computed(() => {
      if (!form.alertTypeId || !alertFields.value[form.alertTypeId]) {
        return []
      }
      return alertFields.value[form.alertTypeId].map(field => ({
        label: `${field.fieldLabel} (${field.fieldName})`,
        value: field.fieldName
      }))
    })

    const getAlertTypeTagType = (typeName) => {
      const typeMap = {
        'network_attack': 'danger',
        'malicious_sample': 'warning',
        'host_behavior': 'primary'
      }
      return typeMap[typeName] || 'info'
    }

    const getMethodTagType = (method) => {
      const typeMap = {
        'GET': 'success',
        'POST': 'primary',
        'PUT': 'warning',
        'DELETE': 'danger'
      }
      return typeMap[method] || 'info'
    }

    const isMapped = (alertTypeId) => {
      return configs.value.some(c => c.alertTypeId === alertTypeId && c.id !== form.id)
    }

    const addMapping = () => {
      form.fieldMapping.push({ sourceField: '', targetField: '' })
    }

    const removeMapping = (index) => {
      if (form.fieldMapping.length > 1) {
        form.fieldMapping.splice(index, 1)
      }
    }

    const loadConfigs = async () => {
      loading.value = true
      try {
        const response = await axios.get('/api/rest-http-datasource-config')
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
        if (!config.endpointPath) {
          config.connectionStatus = 'disconnected'
          continue
        }

        try {
          const fullUrl = baseUrl.value + config.endpointPath
          const response = await axios.post('/api/rest-http-datasource-config/test-connection', {
            endpointPath: fullUrl,
            method: config.method || 'POST',
            authType: config.authType || 'none'
          })
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
      Object.assign(form, {
        ...row,
        fieldMapping: row.fieldMapping || [{ sourceField: '', targetField: '' }]
      })
      dialogVisible.value = true
    }

    const handleEditFromDetail = () => {
      if (detailData.value) {
        detailDialogVisible.value = false
        handleEdit(detailData.value)
      }
    }

    const handleTest = async (row) => {
      if (!row.endpointPath) {
        ElMessage.error('配置数据不完整，无法测试连接')
        return
      }

      try {
        const fullUrl = baseUrl.value + row.endpointPath
        const response = await axios.post('/api/rest-http-datasource-config/test-connection', {
          endpointPath: fullUrl,
          method: row.method || 'POST',
          authType: row.authType || 'none'
        })

        if (response.data.success) {
          ElMessage.success(`HTTP接口 "${row.configName}" 测试成功`)
          row.connectionStatus = 'connected'
        } else {
          ElMessage.error(`HTTP接口 "${row.configName}" 测试失败: ${response.data.message}`)
          row.connectionStatus = 'disconnected'
        }
      } catch (error) {
        ElMessage.error(`HTTP接口 "${row.configName}" 测试失败: ${error.response?.data?.message || error.message}`)
        row.connectionStatus = 'disconnected'
      }
    }

    const handleTestConnection = async () => {
      if (!formRef.value) return

      try {
        await formRef.value.validate()

        ElMessage.info('正在测试连接...')
        const fullUrl = baseUrl + form.endpointPath
        const response = await axios.post('/api/rest-http-datasource-config/test-connection', {
          endpointPath: fullUrl,
          method: form.method,
          authType: 'none'
        })

        if (response.data.success) {
          ElMessage.success('HTTP接口连接测试成功')
        } else {
          ElMessage.error(response.data.message || '连接测试失败，请检查配置')
        }
      } catch (error) {
        if (error.response) {
          ElMessage.error(error.response.data?.message || '连接测试失败')
        } else if (error instanceof Error) {
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

        const response = await axios.delete(`/api/rest-http-datasource-config/${row.id}`)
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
          ? `/api/rest-http-datasource-config/${form.id}`
          : '/api/rest-http-datasource-config'
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
      form.endpointPath = ''
      form.method = 'POST'
      form.fieldMapping = [{ sourceField: '', targetField: '' }]
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
      baseUrl,
      form,
      rules,
      currentAlertFields,
      getAlertTypeTagType,
      getMethodTagType,
      isMapped,
      addMapping,
      removeMapping,
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
.rest-http-datasource-config {
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

.headers-config, .field-mapping {
  width: 100%;
}

.header-item, .mapping-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.mapping-arrow {
  margin: 0 10px;
  color: #409eff;
}

h4 {
  margin-bottom: 15px;
  color: #303133;
  font-size: 16px;
}
</style>