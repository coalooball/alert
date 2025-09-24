<template>
  <div class="alert-data-type-config">
    <el-tabs v-model="activeTab" type="card">
      <el-tab-pane label="告警类型" name="types">
        <div class="tab-content">
          <div class="toolbar">
            <el-button type="primary" @click="showAddTypeDialog">
              <el-icon><Plus /></el-icon>
              新增告警类型
            </el-button>
          </div>

          <el-table :data="alertTypes" border stripe style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="typeName" label="类型名称" />
            <el-table-column prop="typeLabel" label="类型标签" />
            <el-table-column prop="description" label="描述" />
            <el-table-column label="状态" width="100">
              <template #default="scope">
                <el-tag :type="scope.row.isActive ? 'success' : 'info'">
                  {{ scope.row.isActive ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="scope">
                <el-button link type="primary" @click="editType(scope.row)">编辑</el-button>
                <el-button link type="danger" @click="deleteType(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="告警子类型" name="subtypes">
        <div class="tab-content">
          <div class="toolbar">
            <el-select v-model="selectedTypeId" placeholder="请选择告警类型" @change="loadSubtypes">
              <el-option
                v-for="type in alertTypes"
                :key="type.id"
                :label="type.typeLabel"
                :value="type.id"
              />
            </el-select>
            <el-button type="primary" @click="showAddSubtypeDialog" :disabled="!selectedTypeId">
              <el-icon><Plus /></el-icon>
              新增子类型
            </el-button>
          </div>

          <el-table :data="alertSubtypes" border stripe style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="subtypeCode" label="子类型代码" />
            <el-table-column prop="subtypeLabel" label="子类型标签" />
            <el-table-column prop="description" label="描述" />
            <el-table-column label="状态" width="100">
              <template #default="scope">
                <el-tag :type="scope.row.isActive ? 'success' : 'info'">
                  {{ scope.row.isActive ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="scope">
                <el-button link type="primary" @click="editSubtype(scope.row)">编辑</el-button>
                <el-button link type="danger" @click="deleteSubtype(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="告警字段" name="fields">
        <div class="tab-content">
          <div class="toolbar">
            <el-select v-model="selectedTypeIdForFields" placeholder="请选择告警类型" @change="loadFields">
              <el-option
                v-for="type in alertTypes"
                :key="type.id"
                :label="type.typeLabel"
                :value="type.id"
              />
            </el-select>
            <el-button type="primary" @click="showAddFieldDialog" :disabled="!selectedTypeIdForFields">
              <el-icon><Plus /></el-icon>
              新增字段
            </el-button>
          </div>

          <el-table :data="alertFields" border stripe style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="fieldName" label="字段名称" />
            <el-table-column prop="fieldLabel" label="字段标签" />
            <el-table-column prop="fieldType" label="字段类型">
              <template #default="scope">
                <el-tag>{{ scope.row.fieldType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述" />
            <el-table-column label="状态" width="100">
              <template #default="scope">
                <el-tag :type="scope.row.isActive ? 'success' : 'info'">
                  {{ scope.row.isActive ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="scope">
                <el-button link type="primary" @click="editField(scope.row)">编辑</el-button>
                <el-button link type="danger" @click="deleteField(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 告警类型编辑对话框 -->
    <el-dialog
      v-model="typeDialogVisible"
      :title="typeDialogTitle"
      width="500px"
    >
      <el-form :model="typeForm" label-width="100px">
        <el-form-item label="ID" v-if="!isEditType">
          <el-input v-model.number="typeForm.id" />
        </el-form-item>
        <el-form-item label="类型名称">
          <el-input v-model="typeForm.typeName" />
        </el-form-item>
        <el-form-item label="类型标签">
          <el-input v-model="typeForm.typeLabel" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="typeForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="typeForm.isActive" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="typeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveType">保存</el-button>
      </template>
    </el-dialog>

    <!-- 子类型编辑对话框 -->
    <el-dialog
      v-model="subtypeDialogVisible"
      :title="subtypeDialogTitle"
      width="500px"
    >
      <el-form :model="subtypeForm" label-width="100px">
        <el-form-item label="告警类型">
          <el-select v-model="subtypeForm.alertTypeId" :disabled="isEditSubtype">
            <el-option
              v-for="type in alertTypes"
              :key="type.id"
              :label="type.typeLabel"
              :value="type.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="子类型代码">
          <el-input v-model="subtypeForm.subtypeCode" />
        </el-form-item>
        <el-form-item label="子类型标签">
          <el-input v-model="subtypeForm.subtypeLabel" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="subtypeForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="subtypeForm.isActive" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="subtypeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSubtype">保存</el-button>
      </template>
    </el-dialog>

    <!-- 字段编辑对话框 -->
    <el-dialog
      v-model="fieldDialogVisible"
      :title="fieldDialogTitle"
      width="500px"
    >
      <el-form :model="fieldForm" label-width="100px">
        <el-form-item label="告警类型">
          <el-select v-model="fieldForm.alertTypeId" :disabled="isEditField">
            <el-option
              v-for="type in alertTypes"
              :key="type.id"
              :label="type.typeLabel"
              :value="type.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="字段名称">
          <el-input v-model="fieldForm.fieldName" />
        </el-form-item>
        <el-form-item label="字段标签">
          <el-input v-model="fieldForm.fieldLabel" />
        </el-form-item>
        <el-form-item label="字段类型">
          <el-select v-model="fieldForm.fieldType">
            <el-option label="字符串" value="string" />
            <el-option label="数字" value="number" />
            <el-option label="布尔" value="boolean" />
            <el-option label="IP地址" value="ip" />
            <el-option label="端口" value="port" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="fieldForm.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="fieldForm.isActive" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="fieldDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveField">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import axios from 'axios'

export default {
  name: 'AlertDataTypeConfig',
  components: { Plus },
  setup() {
    const activeTab = ref('types')
    const alertTypes = ref([])
    const alertSubtypes = ref([])
    const alertFields = ref([])
    const selectedTypeId = ref(null)
    const selectedTypeIdForFields = ref(null)

    const typeDialogVisible = ref(false)
    const typeDialogTitle = ref('新增告警类型')
    const isEditType = ref(false)
    const typeForm = ref({
      id: null,
      typeName: '',
      typeLabel: '',
      description: '',
      displayOrder: 0,
      isActive: true
    })

    const subtypeDialogVisible = ref(false)
    const subtypeDialogTitle = ref('新增子类型')
    const isEditSubtype = ref(false)
    const subtypeForm = ref({
      alertTypeId: null,
      subtypeCode: '',
      subtypeLabel: '',
      description: '',
      displayOrder: 0,
      isActive: true
    })

    const fieldDialogVisible = ref(false)
    const fieldDialogTitle = ref('新增字段')
    const isEditField = ref(false)
    const fieldForm = ref({
      alertTypeId: null,
      fieldName: '',
      fieldLabel: '',
      fieldType: 'string',
      description: '',
      displayOrder: 0,
      isActive: true
    })

    const loadAlertTypes = async () => {
      try {
        const response = await axios.get('/api/alert-types')
        alertTypes.value = response.data.data || []
      } catch (error) {
        ElMessage.error('加载告警类型失败')
      }
    }

    const loadSubtypes = async () => {
      if (!selectedTypeId.value) {
        alertSubtypes.value = []
        return
      }
      try {
        const response = await axios.get(`/api/alert-types/${selectedTypeId.value}/subtypes`)
        alertSubtypes.value = response.data.data || []
      } catch (error) {
        ElMessage.error('加载子类型失败')
      }
    }

    const loadFields = async () => {
      if (!selectedTypeIdForFields.value) {
        alertFields.value = []
        return
      }
      try {
        const response = await axios.get(`/api/alert-types/${selectedTypeIdForFields.value}/fields`)
        alertFields.value = response.data.data || []
      } catch (error) {
        ElMessage.error('加载字段失败')
      }
    }

    const showAddTypeDialog = () => {
      isEditType.value = false
      typeDialogTitle.value = '新增告警类型'
      typeForm.value = {
        id: null,
        typeName: '',
        typeLabel: '',
        description: '',
        displayOrder: 0,
        isActive: true
      }
      typeDialogVisible.value = true
    }

    const editType = (row) => {
      isEditType.value = true
      typeDialogTitle.value = '编辑告警类型'
      typeForm.value = { ...row }
      typeDialogVisible.value = true
    }

    const saveType = async () => {
      try {
        if (isEditType.value) {
          await axios.put(`/api/alert-types/${typeForm.value.id}`, typeForm.value)
          ElMessage.success('更新成功')
        } else {
          await axios.post('/api/alert-types', typeForm.value)
          ElMessage.success('创建成功')
        }
        typeDialogVisible.value = false
        loadAlertTypes()
      } catch (error) {
        ElMessage.error(error.response?.data?.message || '操作失败')
      }
    }

    const deleteType = async (row) => {
      try {
        await ElMessageBox.confirm(
          `确定要删除告警类型"${row.typeLabel}"吗？这将同时删除其所有子类型和字段。`,
          '警告',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
          }
        )
        await axios.delete(`/api/alert-types/${row.id}`)
        ElMessage.success('删除成功')
        loadAlertTypes()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败')
        }
      }
    }

    const showAddSubtypeDialog = () => {
      isEditSubtype.value = false
      subtypeDialogTitle.value = '新增子类型'
      subtypeForm.value = {
        alertTypeId: selectedTypeId.value,
        subtypeCode: '',
        subtypeLabel: '',
        description: '',
        displayOrder: 0,
        isActive: true
      }
      subtypeDialogVisible.value = true
    }

    const editSubtype = (row) => {
      isEditSubtype.value = true
      subtypeDialogTitle.value = '编辑子类型'
      subtypeForm.value = { ...row }
      subtypeDialogVisible.value = true
    }

    const saveSubtype = async () => {
      try {
        if (isEditSubtype.value) {
          await axios.put(`/api/alert-types/subtypes/${subtypeForm.value.id}`, subtypeForm.value)
          ElMessage.success('更新成功')
        } else {
          await axios.post('/api/alert-types/subtypes', subtypeForm.value)
          ElMessage.success('创建成功')
        }
        subtypeDialogVisible.value = false
        loadSubtypes()
      } catch (error) {
        ElMessage.error(error.response?.data?.message || '操作失败')
      }
    }

    const deleteSubtype = async (row) => {
      try {
        await ElMessageBox.confirm(
          `确定要删除子类型"${row.subtypeLabel}"吗？`,
          '警告',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
          }
        )
        await axios.delete(`/api/alert-types/subtypes/${row.id}`)
        ElMessage.success('删除成功')
        loadSubtypes()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败')
        }
      }
    }

    const showAddFieldDialog = () => {
      isEditField.value = false
      fieldDialogTitle.value = '新增字段'
      fieldForm.value = {
        alertTypeId: selectedTypeIdForFields.value,
        fieldName: '',
        fieldLabel: '',
        fieldType: 'string',
        description: '',
        displayOrder: 0,
        isActive: true
      }
      fieldDialogVisible.value = true
    }

    const editField = (row) => {
      isEditField.value = true
      fieldDialogTitle.value = '编辑字段'
      fieldForm.value = { ...row }
      fieldDialogVisible.value = true
    }

    const saveField = async () => {
      try {
        if (isEditField.value) {
          await axios.put(`/api/alert-types/fields/${fieldForm.value.id}`, fieldForm.value)
          ElMessage.success('更新成功')
        } else {
          await axios.post('/api/alert-types/fields', fieldForm.value)
          ElMessage.success('创建成功')
        }
        fieldDialogVisible.value = false
        loadFields()
      } catch (error) {
        ElMessage.error(error.response?.data?.message || '操作失败')
      }
    }

    const deleteField = async (row) => {
      try {
        await ElMessageBox.confirm(
          `确定要删除字段"${row.fieldLabel}"吗？`,
          '警告',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning',
          }
        )
        await axios.delete(`/api/alert-types/fields/${row.id}`)
        ElMessage.success('删除成功')
        loadFields()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败')
        }
      }
    }

    onMounted(() => {
      loadAlertTypes()
    })

    return {
      activeTab,
      alertTypes,
      alertSubtypes,
      alertFields,
      selectedTypeId,
      selectedTypeIdForFields,
      typeDialogVisible,
      typeDialogTitle,
      isEditType,
      typeForm,
      subtypeDialogVisible,
      subtypeDialogTitle,
      isEditSubtype,
      subtypeForm,
      fieldDialogVisible,
      fieldDialogTitle,
      isEditField,
      fieldForm,
      loadAlertTypes,
      loadSubtypes,
      loadFields,
      showAddTypeDialog,
      editType,
      saveType,
      deleteType,
      showAddSubtypeDialog,
      editSubtype,
      saveSubtype,
      deleteSubtype,
      showAddFieldDialog,
      editField,
      saveField,
      deleteField
    }
  }
}
</script>

<style scoped>
.alert-data-type-config {
  padding: 20px;
}

.tab-content {
  padding: 20px 0;
}

.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.toolbar .el-select {
  width: 200px;
}
</style>