<template>
  <div class="alert-storage-mappings">
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        添加映射
      </el-button>
      <el-button @click="loadMappings">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <el-table
      :data="mappings"
      v-loading="loading"
      stripe
      border
      @row-click="handleRowClick"
      style="cursor: pointer"
    >
      <el-table-column label="告警类型" width="150">
        <template #default="{ row }">
          <el-tag :type="getAlertTypeTagType(row.alertTypeName)">
            {{ row.alertTypeLabel || row.alertTypeName }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="存储配置" min-width="200">
        <template #default="{ row }">
          <div v-if="row.storageConfigName" class="storage-info">
            <el-icon><Coin /></el-icon>
            <span>{{ row.storageConfigName }}</span>
            <el-tag size="small" type="info">{{ row.storageDatabase }}</el-tag>
          </div>
          <span v-else class="text-gray">未配置</span>
        </template>
      </el-table-column>
      <el-table-column prop="tableName" label="目标表名" width="180" />
      <el-table-column prop="retentionDays" label="保留天数" width="100" align="center">
        <template #default="{ row }">
          {{ row.retentionDays }} 天
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleEdit(row)">
            <el-icon><Edit /></el-icon>
            编辑
          </el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">
            <el-icon><Delete /></el-icon>
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Add/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑告警存储映射' : '添加告警存储映射'"
      width="600px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
      >
        <el-form-item label="告警类型" prop="alertTypeId">
          <el-select
            v-model="form.alertTypeId"
            placeholder="请选择告警类型"
            @change="onAlertTypeChange"
            :disabled="isEdit"
            style="width: 100%"
          >
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

        <el-form-item label="存储配置" prop="storageConfigId">
          <el-select
            v-model="form.storageConfigId"
            placeholder="请选择数据库连接"
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="config in activeStorageConfigs"
              :key="config.id"
              :label="`${config.name} (${config.databaseName})`"
              :value="config.id"
            >
              <span style="float: left">{{ config.name }}</span>
              <span style="float: right; color: #8492a6; font-size: 13px">
                {{ config.host }}:{{ config.port }}
              </span>
            </el-option>
          </el-select>
        </el-form-item>

        <el-form-item label="目标表名" prop="tableName">
          <el-input
            v-model="form.tableName"
            placeholder="存储到数据库的表名"
          />
        </el-form-item>

        <el-form-item label="保留天数" prop="retentionDays">
          <el-input-number
            v-model="form.retentionDays"
            :min="1"
            :max="3650"
            placeholder="数据保留天数"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="映射描述信息"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- Detail Dialog -->
    <el-dialog
      v-model="detailDialogVisible"
      title="告警存储映射详情"
      width="600px"
    >
      <el-descriptions :column="1" border v-if="detailData">
        <el-descriptions-item label="告警类型">
          <el-tag :type="getAlertTypeTagType(detailData.alertTypeName)">
            {{ detailData.alertTypeLabel || detailData.alertTypeName }}
          </el-tag>
        </el-descriptions-item>

        <el-descriptions-item label="存储配置">
          <div v-if="detailData.storageConfigName" class="storage-info">
            <el-icon><Coin /></el-icon>
            <span>{{ detailData.storageConfigName }}</span>
          </div>
          <span v-else class="text-gray">未配置</span>
        </el-descriptions-item>

        <el-descriptions-item label="数据库类型">
          <el-tag type="info">{{ detailData.storageDatabase || '未配置' }}</el-tag>
        </el-descriptions-item>

        <el-descriptions-item label="目标表名">
          {{ detailData.tableName }}
        </el-descriptions-item>

        <el-descriptions-item label="保留天数">
          {{ detailData.retentionDays }} 天
        </el-descriptions-item>

        <el-descriptions-item label="描述">
          {{ detailData.description || '暂无描述' }}
        </el-descriptions-item>

        <el-descriptions-item label="创建时间" v-if="detailData.createdAt">
          {{ formatDateTime(detailData.createdAt) }}
        </el-descriptions-item>

        <el-descriptions-item label="更新时间" v-if="detailData.updatedAt">
          {{ formatDateTime(detailData.updatedAt) }}
        </el-descriptions-item>

        <el-descriptions-item label="数据表状态">
          <div class="table-status">
            <el-icon v-if="checkingTable"><Loading /></el-icon>
            <el-tag v-else-if="tableExists" type="success">
              <el-icon><Check /></el-icon>
              已创建
            </el-tag>
            <el-tag v-else type="info">
              <el-icon><Warning /></el-icon>
              未创建
            </el-tag>
          </div>
        </el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleEditFromDetail">编辑</el-button>
        <el-button
          type="success"
          @click="handleCreateTable"
          :disabled="tableExists || creatingTable"
          :loading="creatingTable"
        >
          <el-icon><CirclePlus /></el-icon>
          {{ tableExists ? '表已存在' : (creatingTable ? '创建中...' : '生成数据存储表') }}
        </el-button>
        <el-button type="info" @click="handleViewDDL">
          <el-icon><View /></el-icon>
          查看表DDL
        </el-button>
      </template>
    </el-dialog>

    <!-- DDL Dialog -->
    <el-dialog
      v-model="ddlDialogVisible"
      title="表DDL"
      width="800px"
      :close-on-click-modal="false"
    >
      <div class="ddl-container">
        <el-alert
          v-if="ddlData"
          title="以下是根据告警类型字段生成的DDL语句"
          type="info"
          :closable="false"
          style="margin-bottom: 20px"
        />
        <pre class="ddl-code">{{ ddlData }}</pre>
        <div class="ddl-actions">
          <el-button @click="copyDDL" type="primary" size="small">
            <el-icon><DocumentCopy /></el-icon>
            复制DDL
          </el-button>
        </div>
      </div>

      <template #footer>
        <el-button @click="ddlDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Refresh, Edit, Delete, Coin, CirclePlus, View, DocumentCopy, Loading, Check, Warning
} from '@element-plus/icons-vue'
import axios from 'axios'

export default {
  name: 'AlertStorageMappings',
  components: {
    Plus, Refresh, Edit, Delete, Coin, CirclePlus, View, DocumentCopy
  },
  setup() {
    const mappings = ref([])
    const loading = ref(false)
    const dialogVisible = ref(false)
    const detailDialogVisible = ref(false)
    const detailData = ref(null)
    const ddlDialogVisible = ref(false)
    const ddlData = ref('')
    const tableExists = ref(false)
    const checkingTable = ref(false)
    const creatingTable = ref(false)
    const isEdit = ref(false)
    const formRef = ref(null)
    const alertTypes = ref([])
    const activeStorageConfigs = ref([])

    const form = reactive({
      id: null,
      alertTypeId: null,
      storageConfigId: null,
      tableName: '',
      retentionDays: 90,
      description: ''
    })

    const rules = {
      alertTypeId: [
        { required: true, message: '请选择告警类型', trigger: 'change' }
      ],
      tableName: [
        { required: true, message: '请输入目标表名', trigger: 'blur' }
      ],
      retentionDays: [
        { required: true, message: '请设置保留天数', trigger: 'blur' }
      ]
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
      return mappings.value.some(m => m.alertTypeId === alertTypeId && m.id !== form.id)
    }

    const onAlertTypeChange = (value) => {
      const selectedType = alertTypes.value.find(t => t.id === value)
      if (selectedType && !form.tableName) {
        form.tableName = `alert_${selectedType.typeName.replace(/_/g, '_')}`
      }
    }

    const loadMappings = async () => {
      loading.value = true
      try {
        const response = await axios.get('/api/alert-storage-mapping')
        if (response.data.success) {
          mappings.value = response.data.data
        }
      } catch (error) {
        ElMessage.error('加载告警存储映射失败')
      } finally {
        loading.value = false
      }
    }

    const loadAlertTypes = async () => {
      try {
        const response = await axios.get('/api/alert-types')
        if (response.data.success) {
          alertTypes.value = response.data.data
        }
      } catch (error) {
        ElMessage.error('加载告警类型失败')
      }
    }

    const loadStorageConfigs = async () => {
      try {
        const response = await axios.get('/api/data-storage-config/active')
        if (response.data.success) {
          activeStorageConfigs.value = response.data.data
        }
      } catch (error) {
        ElMessage.error('加载存储配置失败')
      }
    }

    const handleAdd = () => {
      isEdit.value = false
      resetForm()
      dialogVisible.value = true
    }

    const handleRowClick = async (row, column, event) => {
      // Skip if clicking on action buttons
      if (column && column.label === '操作') {
        return
      }
      detailData.value = row
      detailDialogVisible.value = true

      // Check if table exists when dialog opens
      await checkTableExists(row.id)
    }

    const checkTableExists = async (mappingId) => {
      if (!mappingId) return

      try {
        checkingTable.value = true
        const response = await axios.get(`/api/alert-storage-mapping/${mappingId}/table-exists`)
        if (response.data.success) {
          tableExists.value = response.data.data.exists
        }
      } catch (error) {
        console.error('Failed to check table existence:', error)
        tableExists.value = false
      } finally {
        checkingTable.value = false
      }
    }

    const handleEdit = (row) => {
      isEdit.value = true
      Object.assign(form, {
        id: row.id,
        alertTypeId: row.alertTypeId,
        storageConfigId: row.storageConfigId,
        tableName: row.tableName,
        retentionDays: row.retentionDays,
        description: row.description
      })
      dialogVisible.value = true
    }

    const handleEditFromDetail = () => {
      if (detailData.value) {
        detailDialogVisible.value = false
        handleEdit(detailData.value)
      }
    }

    const formatDateTime = (dateString) => {
      if (!dateString) return ''
      const date = new Date(dateString)
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      })
    }

    const handleCreateTable = async () => {
      if (!detailData.value) return

      // Check if table already exists
      if (tableExists.value) {
        ElMessage.warning('数据表已存在，无需重复创建')
        return
      }

      try {
        await ElMessageBox.confirm(
          `确定要在 "${detailData.value.storageConfigName || '默认数据库'}" 中创建表 "${detailData.value.tableName}" 吗？`,
          '创建数据表',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )

        creatingTable.value = true
        const response = await axios.post(`/api/alert-storage-mapping/${detailData.value.id}/create-table`)

        if (response.data.success) {
          const created = response.data.data?.created
          if (created) {
            ElMessage.success('数据表创建成功')
            tableExists.value = true // Update local status
          } else {
            ElMessage.info('数据表已存在')
            tableExists.value = true // Update local status
          }
        } else {
          ElMessage.error(response.data.message || '创建失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error(error.response?.data?.message || '创建数据表失败')
        }
      } finally {
        creatingTable.value = false
      }
    }

    const handleViewDDL = async () => {
      if (!detailData.value) return

      try {
        const response = await axios.get(`/api/alert-storage-mapping/${detailData.value.id}/ddl`)
        if (response.data.success) {
          ddlData.value = response.data.data
          ddlDialogVisible.value = true
        } else {
          ElMessage.error(response.data.message || '获取DDL失败')
        }
      } catch (error) {
        ElMessage.error(error.response?.data?.message || '获取DDL失败')
      }
    }

    const copyDDL = () => {
      if (!ddlData.value) return

      // 创建临时文本区域
      const textarea = document.createElement('textarea')
      textarea.value = ddlData.value
      document.body.appendChild(textarea)
      textarea.select()

      try {
        document.execCommand('copy')
        ElMessage.success('DDL已复制到剪贴板')
      } catch (err) {
        ElMessage.error('复制失败，请手动选择复制')
      }

      document.body.removeChild(textarea)
    }

    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm(
          `确定要删除告警类型 "${row.alertTypeLabel}" 的存储映射吗？`,
          '删除确认',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )

        const response = await axios.delete(`/api/alert-storage-mapping/${row.id}`)
        if (response.data.success) {
          ElMessage.success('删除成功')
          loadMappings()
        } else {
          ElMessage.error(response.data.message || '删除失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败')
        }
      }
    }

    const handleSubmit = async () => {
      try {
        await formRef.value.validate()

        const url = isEdit.value
          ? `/api/alert-storage-mapping/${form.id}`
          : '/api/alert-storage-mapping'
        const method = isEdit.value ? 'put' : 'post'

        const response = await axios[method](url, form)
        if (response.data.success) {
          ElMessage.success(response.data.message || '保存成功')
          dialogVisible.value = false
          loadMappings()
        } else {
          ElMessage.error(response.data.message || '保存失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('请填写必填项')
        }
      }
    }

    const resetForm = () => {
      form.id = null
      form.alertTypeId = null
      form.storageConfigId = null
      form.tableName = ''
      form.retentionDays = 90
      form.description = ''
      if (formRef.value) {
        formRef.value.clearValidate()
      }
    }

    onMounted(() => {
      loadMappings()
      loadAlertTypes()
      loadStorageConfigs()
    })

    return {
      mappings,
      loading,
      dialogVisible,
      detailDialogVisible,
      detailData,
      ddlDialogVisible,
      ddlData,
      tableExists,
      checkingTable,
      creatingTable,
      isEdit,
      formRef,
      form,
      rules,
      alertTypes,
      activeStorageConfigs,
      getAlertTypeTagType,
      isMapped,
      onAlertTypeChange,
      loadMappings,
      handleAdd,
      handleRowClick,
      handleEdit,
      handleEditFromDetail,
      handleDelete,
      handleSubmit,
      resetForm,
      formatDateTime,
      checkTableExists,
      handleCreateTable,
      handleViewDDL,
      copyDDL
    }
  }
}
</script>

<style scoped>
.alert-storage-mappings {
  padding: 10px;
}

.toolbar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

.storage-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.text-gray {
  color: #909399;
}

.el-table {
  font-size: 14px;
}

.ddl-container {
  position: relative;
}

.ddl-code {
  background-color: #f5f7fa;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 15px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 400px;
  overflow-y: auto;
}

.ddl-actions {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 10;
}

.table-status {
  margin-left: 8px;
  font-size: 12px;
  color: #67c23a;
  font-weight: 500;
}
</style>