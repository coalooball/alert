<template>
  <div class="data-storage-connections">
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        添加数据库连接
      </el-button>
      <el-button @click="loadConfigs">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <el-table :data="configs" v-loading="loading" stripe border>
      <el-table-column prop="name" label="连接名称" width="180" />
      <el-table-column prop="dbType" label="数据库类型" width="120">
        <template #default="{ row }">
          <el-tag :type="getDbTypeTagType(row.dbType)">
            {{ getDbTypeLabel(row.dbType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="连接信息" min-width="200">
        <template #default="{ row }">
          <div class="connection-info">
            <span>{{ row.host }}:{{ row.port }}/{{ row.databaseName }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.isActive ? 'success' : 'info'">
            {{ row.isActive ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="默认" width="80" align="center">
        <template #default="{ row }">
          <el-icon v-if="row.isDefault" style="color: #67c23a">
            <Check />
          </el-icon>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleTest(row)">
            <el-icon><Connection /></el-icon>
            测试连接
          </el-button>
          <el-button size="small" @click="handleEdit(row)">
            <el-icon><Edit /></el-icon>
            编辑
          </el-button>
          <el-button
            size="small"
            type="danger"
            @click="handleDelete(row)"
            :disabled="row.isDefault"
          >
            <el-icon><Delete /></el-icon>
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Add/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑数据库连接' : '添加数据库连接'"
      width="650px"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
      >
        <el-form-item label="连接名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入连接名称" />
        </el-form-item>

        <el-form-item label="数据库类型" prop="dbType">
          <el-select v-model="form.dbType" placeholder="请选择数据库类型" @change="onDbTypeChange">
            <el-option
              v-for="type in databaseTypes"
              :key="type.value"
              :label="type.label"
              :value="type.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="主机地址" prop="host">
          <el-input v-model="form.host" placeholder="例如: localhost 或 192.168.1.1" />
        </el-form-item>

        <el-form-item label="端口" prop="port">
          <el-input-number
            v-model="form.port"
            :min="1"
            :max="65535"
            placeholder="数据库端口"
          />
        </el-form-item>

        <el-form-item label="数据库名称" prop="databaseName">
          <el-input v-model="form.databaseName" placeholder="要连接的数据库名称" />
        </el-form-item>

        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="数据库用户名" />
        </el-form-item>

        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="数据库密码"
          />
        </el-form-item>

        <el-form-item label="最大连接数">
          <el-input-number
            v-model="form.maxConnections"
            :min="1"
            :max="100"
            placeholder="最大连接数"
          />
        </el-form-item>

        <el-form-item label="连接超时(毫秒)">
          <el-input-number
            v-model="form.connectionTimeout"
            :min="1000"
            :max="120000"
            :step="1000"
            placeholder="连接超时时间"
          />
        </el-form-item>

        <el-form-item label="状态">
          <el-switch
            v-model="form.isActive"
            active-text="启用"
            inactive-text="禁用"
          />
        </el-form-item>

        <el-form-item label="设为默认">
          <el-switch
            v-model="form.isDefault"
            active-text="是"
            inactive-text="否"
          />
        </el-form-item>

        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="连接描述信息"
          />
        </el-form-item>

        <el-form-item label="额外参数">
          <el-input
            v-model="connectionParamsStr"
            type="textarea"
            :rows="3"
            placeholder='JSON格式，例如: {"protocol": "http", "compress": true}'
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="warning" @click="handleTestConnection">测试连接</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Refresh, Edit, Delete, Connection, Check
} from '@element-plus/icons-vue'
import axios from '../utils/axios'

export default {
  name: 'DataStorageConnections',
  components: {
    Plus, Refresh, Edit, Delete, Connection, Check
  },
  setup() {
    const configs = ref([])
    const loading = ref(false)
    const dialogVisible = ref(false)
    const isEdit = ref(false)
    const formRef = ref(null)
    const databaseTypes = ref([])
    const connectionParamsStr = ref('')

    const form = reactive({
      id: null,
      name: '',
      dbType: '',
      host: '',
      port: null,
      databaseName: '',
      username: '',
      password: '',
      connectionParams: {},
      maxConnections: 10,
      connectionTimeout: 30000,
      isActive: true,
      isDefault: false,
      description: ''
    })

    const rules = {
      name: [
        { required: true, message: '请输入连接名称', trigger: 'blur' }
      ],
      dbType: [
        { required: true, message: '请选择数据库类型', trigger: 'change' }
      ],
      host: [
        { required: true, message: '请输入主机地址', trigger: 'blur' }
      ],
      port: [
        { required: true, message: '请输入端口', trigger: 'blur' }
      ],
      databaseName: [
        { required: true, message: '请输入数据库名称', trigger: 'blur' }
      ]
    }

    const getDbTypeLabel = (type) => {
      const typeMap = {
        'POSTGRESQL': 'PostgreSQL',
        'MYSQL': 'MySQL',
        'CLICKHOUSE': 'ClickHouse',
        'MONGODB': 'MongoDB',
        'ELASTICSEARCH': 'Elasticsearch'
      }
      return typeMap[type] || type
    }

    const getDbTypeTagType = (type) => {
      const typeMap = {
        'POSTGRESQL': 'primary',
        'MYSQL': 'success',
        'CLICKHOUSE': 'warning',
        'MONGODB': 'info',
        'ELASTICSEARCH': 'danger'
      }
      return typeMap[type] || ''
    }

    const onDbTypeChange = (value) => {
      // Set default port based on database type
      const defaultPorts = {
        'POSTGRESQL': 5432,
        'MYSQL': 3306,
        'CLICKHOUSE': 8123,
        'MONGODB': 27017,
        'ELASTICSEARCH': 9200
      }
      form.port = defaultPorts[value] || null
    }

    const loadConfigs = async () => {
      loading.value = true
      try {
        const response = await axios.get('/api/data-storage-config')
        if (response.data.success) {
          configs.value = response.data.data
        }
      } catch (error) {
        ElMessage.error('加载数据库连接配置失败')
      } finally {
        loading.value = false
      }
    }

    const loadDatabaseTypes = async () => {
      try {
        const response = await axios.get('/api/data-storage-config/database-types')
        if (response.data.success) {
          databaseTypes.value = response.data.data
        }
      } catch (error) {
        ElMessage.error('加载数据库类型失败')
      }
    }

    const handleAdd = () => {
      isEdit.value = false
      resetForm()
      dialogVisible.value = true
    }

    const handleEdit = (row) => {
      isEdit.value = true
      Object.assign(form, row)
      if (row.connectionParams) {
        connectionParamsStr.value = JSON.stringify(row.connectionParams, null, 2)
      }
      dialogVisible.value = true
    }

    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm(
          `确定要删除数据库连接 "${row.name}" 吗？`,
          '删除确认',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )

        const response = await axios.delete(`/api/data-storage-config/${row.id}`)
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

    const handleTest = async (row) => {
      try {
        const response = await axios.post('/api/data-storage-config/test-connection', row)
        if (response.data.success) {
          ElMessage.success(response.data.message || '连接测试成功')
        } else {
          ElMessage.error(response.data.message || '连接测试失败')
        }
      } catch (error) {
        ElMessage.error('连接测试失败')
      }
    }

    const handleTestConnection = async () => {
      try {
        await formRef.value.validate()

        // Parse connection params
        if (connectionParamsStr.value) {
          try {
            form.connectionParams = JSON.parse(connectionParamsStr.value)
          } catch (e) {
            ElMessage.error('额外参数格式错误，请输入有效的JSON')
            return
          }
        }

        const response = await axios.post('/api/data-storage-config/test-connection', form)
        if (response.data.success) {
          ElMessage.success(response.data.message || '连接测试成功')
        } else {
          ElMessage.error(response.data.message || '连接测试失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('请填写必填项')
        }
      }
    }

    const handleSubmit = async () => {
      try {
        await formRef.value.validate()

        // Parse connection params
        if (connectionParamsStr.value) {
          try {
            form.connectionParams = JSON.parse(connectionParamsStr.value)
          } catch (e) {
            ElMessage.error('额外参数格式错误，请输入有效的JSON')
            return
          }
        }

        const url = isEdit.value
          ? `/api/data-storage-config/${form.id}`
          : '/api/data-storage-config'
        const method = isEdit.value ? 'put' : 'post'

        const response = await axios[method](url, form)
        if (response.data.success) {
          ElMessage.success(response.data.message || '保存成功')
          dialogVisible.value = false
          loadConfigs()
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
      form.name = ''
      form.dbType = ''
      form.host = ''
      form.port = null
      form.databaseName = ''
      form.username = ''
      form.password = ''
      form.connectionParams = {}
      form.maxConnections = 10
      form.connectionTimeout = 30000
      form.isActive = true
      form.isDefault = false
      form.description = ''
      connectionParamsStr.value = ''
      if (formRef.value) {
        formRef.value.clearValidate()
      }
    }

    onMounted(() => {
      loadConfigs()
      loadDatabaseTypes()
    })

    return {
      configs,
      loading,
      dialogVisible,
      isEdit,
      formRef,
      form,
      rules,
      databaseTypes,
      connectionParamsStr,
      getDbTypeLabel,
      getDbTypeTagType,
      onDbTypeChange,
      loadConfigs,
      handleAdd,
      handleEdit,
      handleDelete,
      handleTest,
      handleTestConnection,
      handleSubmit,
      resetForm
    }
  }
}
</script>

<style scoped>
.data-storage-connections {
  padding: 10px;
}

.toolbar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

.connection-info {
  font-family: monospace;
  font-size: 12px;
  color: #606266;
}

.el-table {
  font-size: 14px;
}
</style>