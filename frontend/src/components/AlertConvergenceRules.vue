<template>
  <div class="convergence-rules">
    <div class="page-header">
      <h3>收敛规则管理</h3>
      <el-button type="primary" @click="showCreateDialog" :icon="Plus">
        添加规则
      </el-button>
    </div>

    <div class="filter-bar">
      <el-select v-model="filterType" placeholder="选择告警类型" clearable @change="loadRules">
        <el-option
          v-for="type in alertTypes"
          :key="type.id"
          :label="type.typeLabel"
          :value="type.id"
        />
      </el-select>
      <el-input
        v-model="searchKeyword"
        placeholder="搜索规则名称或描述"
        clearable
        style="width: 300px; margin-left: 10px"
        @keyup.enter="searchRules"
      >
        <template #append>
          <el-button :icon="Search" @click="searchRules" />
        </template>
      </el-input>
      <el-button @click="loadRules" :icon="Refresh" style="margin-left: 10px">
        刷新
      </el-button>
    </div>

    <el-table
      :data="rules"
      style="width: 100%"
      v-loading="loading"
      border
    >
      <el-table-column prop="ruleName" label="规则名称" min-width="150" />
      <el-table-column prop="alertType" label="告警类型" width="100">
        <template #default="scope">
          <el-tag :type="getAlertTypeTag(scope.row.alertType)">
            {{ getAlertTypeName(scope.row.alertType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="alertSubtype" label="子类型" width="120">
        <template #default="scope">
          {{ getSubtypeLabel(scope.row.alertType, scope.row.alertSubtype) }}
        </template>
      </el-table-column>
      <el-table-column prop="timeWindow" label="时间窗口" width="100">
        <template #default="scope">
          {{ formatTimeWindow(scope.row.timeWindow) }}
        </template>
      </el-table-column>
      <el-table-column prop="minCount" label="最小数量" width="100" align="center" />
      <el-table-column prop="priority" label="优先级" width="80" align="center" />
      <el-table-column prop="isEnabled" label="状态" width="80" align="center">
        <template #default="scope">
          <el-switch
            v-model="scope.row.isEnabled"
            @change="toggleRule(scope.row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="scope">
          <el-button
            size="small"
            @click="editRule(scope.row)"
          >
            编辑
          </el-button>
          <el-button
            size="small"
            type="danger"
            @click="deleteRule(scope.row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      v-if="total > 0"
      v-model:current-page="currentPage"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      style="margin-top: 20px; text-align: right"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
    />

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '添加收敛规则' : '编辑收敛规则'"
      width="800px"
    >
      <el-form
        :model="ruleForm"
        :rules="formRules"
        ref="ruleFormRef"
        label-width="120px"
      >
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="ruleForm.ruleName" placeholder="请输入规则名称" />
        </el-form-item>
        <el-form-item label="规则描述" prop="ruleDescription">
          <el-input
            v-model="ruleForm.ruleDescription"
            type="textarea"
            :rows="3"
            placeholder="请输入规则描述"
          />
        </el-form-item>
        <el-form-item label="告警类型" prop="alertType">
          <el-select v-model="ruleForm.alertType" placeholder="请选择告警类型" @change="onAlertTypeChange">
            <el-option
              v-for="type in alertTypes"
              :key="type.id"
              :label="type.typeLabel"
              :value="type.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="子类型" prop="alertSubtype">
          <el-select
            v-model="ruleForm.alertSubtype"
            placeholder="请先选择告警类型"
            clearable
            :disabled="!ruleForm.alertType"
          >
            <el-option
              v-for="option in currentSubtypeOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <!-- 计算引擎字段配置 -->
        <el-form-item label="计算引擎字段">
          <div class="field-config-container">
            <el-card class="field-config-card">
              <template #header>
                <div class="field-config-header">
                  <span>系统内部计算引擎处理的字段列表</span>
                  <el-button type="primary" size="small" @click="addEngineField" :icon="Plus">
                    添加字段
                  </el-button>
                </div>
              </template>
              <div v-if="ruleForm.engineFields && ruleForm.engineFields.length > 0" class="field-list">
                <el-tag
                  v-for="(field, index) in ruleForm.engineFields"
                  :key="index"
                  closable
                  size="large"
                  @close="removeEngineField(index)"
                  style="margin: 5px"
                >
                  {{ getFieldDisplayName(field) }}
                </el-tag>
              </div>
              <el-empty v-else description="暂无配置字段" :image-size="50" />
            </el-card>
          </div>
        </el-form-item>

        <!-- 机器学习字段配置 -->
        <el-form-item label="机器学习字段">
          <div class="field-config-container">
            <el-card class="field-config-card">
              <template #header>
                <div class="field-config-header">
                  <span>机器学习模型处理的字段列表</span>
                  <el-button type="primary" size="small" @click="addMlField" :icon="Plus">
                    添加字段
                  </el-button>
                </div>
              </template>
              <div v-if="ruleForm.mlFields && ruleForm.mlFields.length > 0" class="field-list">
                <el-tag
                  v-for="(field, index) in ruleForm.mlFields"
                  :key="index"
                  closable
                  size="large"
                  type="warning"
                  @close="removeMlField(index)"
                  style="margin: 5px"
                >
                  {{ getFieldDisplayName(field) }}
                </el-tag>
              </div>
              <el-empty v-else description="暂无配置字段" :image-size="50" />
            </el-card>
          </div>
        </el-form-item>

        <!-- 机器学习模型配置 -->
        <el-form-item label="使用ML模型">
          <el-switch v-model="ruleForm.useMlModel" />
        </el-form-item>
        <el-form-item label="ML模型名称" v-if="ruleForm.useMlModel">
          <el-input v-model="ruleForm.mlModelName" placeholder="请输入机器学习模型名称" />
        </el-form-item>
        <el-form-item label="ML模型配置" v-if="ruleForm.useMlModel">
          <el-input
            v-model="mlModelConfigString"
            type="textarea"
            :rows="3"
            placeholder='请输入JSON格式配置，例如：{"threshold": 0.8, "algorithm": "kmeans"}'
            @blur="parseMlModelConfig"
          />
        </el-form-item>

        <el-form-item label="时间窗口" prop="timeWindow">
          <el-input-number v-model="ruleForm.timeWindow" :min="60" :max="86400" :step="60" />
          <span style="margin-left: 10px; color: #909399;">秒（1分钟-24小时）</span>
        </el-form-item>
        <el-form-item label="最小数量" prop="minCount">
          <el-input-number v-model="ruleForm.minCount" :min="2" :max="1000" />
          <span style="margin-left: 10px; color: #909399;">触发收敛的最小告警数量</span>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="ruleForm.priority" :min="0" :max="999" />
          <span style="margin-left: 10px; color: #909399;">数值越大优先级越高</span>
        </el-form-item>
        <el-form-item label="启用" prop="isEnabled">
          <el-switch v-model="ruleForm.isEnabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            @click="saveRule"
            :loading="saving"
          >
            保存
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

export default {
  name: 'AlertConvergenceRules',
  components: {
    Plus,
    Refresh,
    Search
  },
  data() {
    return {
      rules: [],
      loading: false,
      filterType: null,
      searchKeyword: '',
      dialogVisible: false,
      dialogMode: 'create',
      saving: false,
      currentRuleId: null,
      currentPage: 1,
      pageSize: 10,
      total: 0,
      mlModelConfigString: '',
      fieldSelectorVisible: false,
      fieldSelectorType: '',
      selectedField: '',
      ruleForm: {
        ruleName: '',
        ruleDescription: '',
        alertType: null,
        alertSubtype: '',
        convergenceType: 'field_match',
        engineFields: [],
        mlFields: [],
        useMlModel: false,
        mlModelName: '',
        mlModelConfig: {},
        convergenceConfig: {},
        timeWindow: 3600,
        minCount: 2,
        priority: 0,
        isEnabled: true
      },
      formRules: {
        ruleName: [
          { required: true, message: '请输入规则名称', trigger: 'blur' }
        ],
        alertType: [
          { required: true, message: '请选择告警类型', trigger: 'change' }
        ],
        alertSubtype: [
          { required: true, message: '请选择子类型', trigger: 'change' }
        ],
        timeWindow: [
          { required: true, message: '请设置时间窗口', trigger: 'blur' }
        ],
        minCount: [
          { required: true, message: '请设置最小数量', trigger: 'blur' }
        ]
      },
      // 从后端获取的元数据
      alertTypes: [],
      subtypeOptions: {},
      fieldOptions: {}
    }
  },
  computed: {
    currentSubtypeOptions() {
      return this.ruleForm.alertType ? this.subtypeOptions[this.ruleForm.alertType] || [] : []
    },
    currentFieldOptions() {
      return this.ruleForm.alertType ? this.fieldOptions[this.ruleForm.alertType] || [] : []
    }
  },
  mounted() {
    this.loadMetadata()
    this.loadRules()
  },
  methods: {
    async loadMetadata() {
      try {
        const response = await fetch('/api/alert-metadata/all', {
          credentials: 'include'
        })
        if (response.ok) {
          const metadata = await response.json()
          this.alertTypes = metadata.types || []

          // 转换子类型数据格式
          const subtypes = metadata.subtypes || {}
          this.subtypeOptions = {}
          for (const typeId in subtypes) {
            this.subtypeOptions[typeId] = subtypes[typeId].map(subtype => ({
              value: subtype.subtypeCode,
              label: `${subtype.subtypeLabel} (${subtype.subtypeCode})`
            }))
          }

          // 转换字段数据格式
          const fields = metadata.fields || {}
          this.fieldOptions = {}
          for (const typeId in fields) {
            this.fieldOptions[typeId] = fields[typeId].map(field => ({
              value: field.fieldName,
              label: `${field.fieldLabel} (${field.fieldName}, ${field.fieldType})`
            }))
          }
        } else {
          ElMessage.error('获取告警元数据失败')
        }
      } catch (error) {
        ElMessage.error('网络错误：无法获取告警元数据')
      }
    },
    onAlertTypeChange() {
      // 当告警类型改变时，清空子类型和字段选择
      this.ruleForm.alertSubtype = ''
      this.ruleForm.engineFields = []
      this.ruleForm.mlFields = []
    },
    async loadRules() {
      this.loading = true
      try {
        const params = new URLSearchParams({
          page: this.currentPage - 1,
          size: this.pageSize,
          sortBy: 'createdAt',
          sortDir: 'desc'
        })

        if (this.filterType) {
          params.append('alertType', this.filterType)
        }
        if (this.searchKeyword.trim()) {
          params.append('ruleName', this.searchKeyword.trim())
        }

        const response = await fetch(`/api/alert/convergence-rules?${params}`, {
          credentials: 'include'
        })
        if (response.ok) {
          const data = await response.json()
          this.rules = data.records || []
          this.total = data.total || 0
        } else {
          ElMessage.error('获取规则列表失败')
        }
      } catch (error) {
        ElMessage.error('网络错误')
      }
      this.loading = false
    },
    async searchRules() {
      this.currentPage = 1
      this.loadRules()
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
      this.loadRules()
    },
    handleCurrentChange(page) {
      this.currentPage = page
      this.loadRules()
    },
    showCreateDialog() {
      this.dialogMode = 'create'
      this.currentRuleId = null
      this.resetForm()
      this.dialogVisible = true
    },
    editRule(rule) {
      this.dialogMode = 'edit'
      this.currentRuleId = rule.id
      this.ruleForm = {
        ruleName: rule.ruleName,
        ruleDescription: rule.ruleDescription,
        alertType: rule.alertType,
        alertSubtype: rule.alertSubtype,
        convergenceType: rule.convergenceType,
        engineFields: rule.engineFields || [],
        mlFields: rule.mlFields || [],
        useMlModel: rule.useMlModel || false,
        mlModelName: rule.mlModelName || '',
        mlModelConfig: rule.mlModelConfig || {},
        convergenceConfig: rule.convergenceConfig || {},
        timeWindow: rule.timeWindow,
        minCount: rule.minCount,
        priority: rule.priority,
        isEnabled: rule.isEnabled
      }
      // 将 ML 配置转换为字符串以便编辑
      if (rule.mlModelConfig && typeof rule.mlModelConfig === 'object') {
        this.mlModelConfigString = JSON.stringify(rule.mlModelConfig, null, 2)
      } else {
        this.mlModelConfigString = ''
      }
      this.dialogVisible = true
    },
    resetForm() {
      this.ruleForm = {
        ruleName: '',
        ruleDescription: '',
        alertType: null,
        alertSubtype: '',
        convergenceType: 'field_match',
        engineFields: [],
        mlFields: [],
        useMlModel: false,
        mlModelName: '',
        mlModelConfig: {},
        convergenceConfig: {},
        timeWindow: 3600,
        minCount: 2,
        priority: 0,
        isEnabled: true
      }
      this.mlModelConfigString = ''
      if (this.$refs.ruleFormRef) {
        this.$refs.ruleFormRef.clearValidate()
      }
    },
    async saveRule() {
      if (!this.$refs.ruleFormRef) return

      const valid = await this.$refs.ruleFormRef.validate().catch(() => false)
      if (!valid) return

      // 在保存前解析ML配置
      if (this.ruleForm.useMlModel) {
        this.parseMlModelConfig()
      }

      this.saving = true
      try {
        const method = this.dialogMode === 'create' ? 'POST' : 'PUT'
        const url = this.dialogMode === 'create'
          ? '/api/alert/convergence-rules'
          : `/api/alert/convergence-rules/${this.currentRuleId}`

        const response = await fetch(url, {
          method,
          headers: {
            'Content-Type': 'application/json'
          },
          credentials: 'include',
          body: JSON.stringify(this.ruleForm)
        })

        if (response.ok) {
          ElMessage.success(this.dialogMode === 'create' ? '规则创建成功' : '规则更新成功')
          this.dialogVisible = false
          this.loadRules()
        } else {
          const error = await response.json()
          ElMessage.error(error.message || '操作失败')
        }
      } catch (error) {
        ElMessage.error('网络错误')
      }
      this.saving = false
    },
    async toggleRule(rule) {
      try {
        const response = await fetch(`/api/alert/convergence-rules/${rule.id}/toggle`, {
          method: 'PATCH',
          credentials: 'include'
        })

        if (response.ok) {
          ElMessage.success(rule.isEnabled ? '规则已启用' : '规则已禁用')
        } else {
          rule.isEnabled = !rule.isEnabled
          ElMessage.error('操作失败')
        }
      } catch (error) {
        rule.isEnabled = !rule.isEnabled
        ElMessage.error('网络错误')
      }
    },
    async deleteRule(rule) {
      try {
        await ElMessageBox.confirm(
          `确定要删除规则 "${rule.ruleName}" 吗？`,
          '确认删除',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )

        const response = await fetch(`/api/alert/convergence-rules/${rule.id}`, {
          method: 'DELETE',
          credentials: 'include'
        })

        if (response.ok) {
          ElMessage.success('规则删除成功')
          this.loadRules()
        } else {
          const error = await response.json()
          ElMessage.error(error.message || '删除失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败')
        }
      }
    },
    getAlertTypeName(typeId) {
      const type = this.alertTypes.find(t => t.id === typeId)
      return type ? type.typeLabel : '未知'
    },
    getAlertTypeTag(type) {
      const tags = {
        1: 'danger',
        2: 'warning',
        3: 'info'
      }
      return tags[type] || 'info'
    },
    formatTimeWindow(seconds) {
      if (seconds >= 3600) {
        return `${Math.floor(seconds / 3600)}小时`
      } else if (seconds >= 60) {
        return `${Math.floor(seconds / 60)}分钟`
      } else {
        return `${seconds}秒`
      }
    },
    getSubtypeLabel(alertTypeId, subtypeCode) {
      if (!alertTypeId || !subtypeCode) return subtypeCode || '-'
      const options = this.subtypeOptions[alertTypeId]
      if (!options) return subtypeCode
      const found = options.find(opt => opt.value === subtypeCode)
      return found ? found.label : subtypeCode
    },
    getFieldDisplayName(fieldName) {
      if (!fieldName) return ''
      if (!this.ruleForm.alertType) return fieldName
      const options = this.fieldOptions[this.ruleForm.alertType]
      if (!options) return fieldName
      const found = options.find(opt => opt.value === fieldName)
      return found ? found.label : fieldName
    },
    addEngineField() {
      this.fieldSelectorType = 'engine'
      this.selectedField = ''
      this.showFieldSelector()
    },
    addMlField() {
      this.fieldSelectorType = 'ml'
      this.selectedField = ''
      this.showFieldSelector()
    },
    showFieldSelector() {
      if (!this.ruleForm.alertType) {
        ElMessage.warning('请先选择告警类型')
        return
      }
      ElMessageBox.prompt('请选择或输入字段名称', '添加字段', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入字段名称',
        inputValue: '',
        customClass: 'field-selector-dialog',
        beforeClose: (action, instance, done) => {
          if (action === 'confirm') {
            const fieldName = instance.inputValue
            if (!fieldName || !fieldName.trim()) {
              ElMessage.error('字段名称不能为空')
              return
            }
            if (this.fieldSelectorType === 'engine') {
              if (!this.ruleForm.engineFields) {
                this.ruleForm.engineFields = []
              }
              if (!this.ruleForm.engineFields.includes(fieldName.trim())) {
                this.ruleForm.engineFields.push(fieldName.trim())
              } else {
                ElMessage.warning('该字段已存在')
              }
            } else if (this.fieldSelectorType === 'ml') {
              if (!this.ruleForm.mlFields) {
                this.ruleForm.mlFields = []
              }
              if (!this.ruleForm.mlFields.includes(fieldName.trim())) {
                this.ruleForm.mlFields.push(fieldName.trim())
              } else {
                ElMessage.warning('该字段已存在')
              }
            }
          }
          done()
        }
      }).then(({ value }) => {
        // 已在 beforeClose 中处理
      }).catch(() => {
        // 用户取消
      })
    },
    removeEngineField(index) {
      this.ruleForm.engineFields.splice(index, 1)
    },
    removeMlField(index) {
      this.ruleForm.mlFields.splice(index, 1)
    },
    parseMlModelConfig() {
      if (this.mlModelConfigString && this.mlModelConfigString.trim()) {
        try {
          this.ruleForm.mlModelConfig = JSON.parse(this.mlModelConfigString)
        } catch (e) {
          ElMessage.error('ML模型配置格式错误，请输入有效的JSON格式')
        }
      } else {
        this.ruleForm.mlModelConfig = {}
      }
    }
  }
}
</script>

<style scoped>
.convergence-rules {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h3 {
  margin: 0;
  font-size: 18px;
}

.filter-bar {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.field-config-container {
  width: 100%;
}

.field-config-card {
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
}

.field-config-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.field-list {
  min-height: 60px;
  padding: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.field-config-card :deep(.el-card__header) {
  padding: 12px 20px;
  background: #fafafa;
  border-bottom: 1px solid #e4e7ed;
}

.field-config-card :deep(.el-card__body) {
  padding: 15px;
}

:deep(.field-selector-dialog) .el-message-box__input {
  margin-top: 10px;
}
</style>