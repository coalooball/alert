<template>
  <div class="filter-rules">
    <div class="page-header">
      <h3>告警过滤规则</h3>
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
      <el-table-column prop="matchField" label="匹配字段" width="120">
        <template #default="scope">
          {{ getFieldLabel(scope.row.alertType, scope.row.matchField) }}
        </template>
      </el-table-column>
      <el-table-column prop="matchType" label="匹配类型" width="100">
        <template #default="scope">
          <el-tag size="small" :type="scope.row.matchType === 'regex' ? 'warning' : 'success'">
            {{ getMatchTypeName(scope.row.matchType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="matchValue" label="匹配值" min-width="150" show-overflow-tooltip />
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

    <!-- 创建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '添加过滤规则' : '编辑过滤规则'"
      width="600px"
    >
      <el-form
        :model="ruleForm"
        :rules="formRules"
        ref="ruleFormRef"
        label-width="100px"
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
        <el-form-item label="匹配字段" prop="matchField">
          <el-select
            v-model="ruleForm.matchField"
            placeholder="请先选择告警类型"
            filterable
            :disabled="!ruleForm.alertType"
          >
            <el-option
              v-for="option in currentFieldOptions"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="匹配类型" prop="matchType">
          <el-select v-model="ruleForm.matchType" placeholder="请选择匹配类型">
            <el-option label="精确匹配" value="exact" />
            <el-option label="正则匹配" value="regex" />
          </el-select>
        </el-form-item>
        <el-form-item label="匹配值" prop="matchValue">
          <el-input
            v-model="ruleForm.matchValue"
            type="textarea"
            :rows="2"
            placeholder="匹配的值或正则表达式"
          />
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
  name: 'AlertFilterRules',
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
      ruleForm: {
        ruleName: '',
        ruleDescription: '',
        alertType: null,
        alertSubtype: '',
        matchField: '',
        matchType: '',
        matchValue: '',
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
        matchField: [
          { required: true, message: '请选择匹配字段', trigger: 'change' }
        ],
        matchType: [
          { required: true, message: '请选择匹配类型', trigger: 'change' }
        ],
        matchValue: [
          { required: true, message: '请输入匹配值', trigger: 'blur' }
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

          // 转换子类型数据格式 - 显示 subtype_code
          const subtypes = metadata.subtypes || {}
          this.subtypeOptions = {}
          for (const typeId in subtypes) {
            this.subtypeOptions[typeId] = subtypes[typeId].map(subtype => ({
              value: subtype.subtypeCode,
              label: `${subtype.subtypeLabel} (${subtype.subtypeCode})`
            }))
          }

          // 转换字段数据格式 - 显示英文名字和数据类型
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
      this.ruleForm.matchField = ''
    },
    async loadRules() {
      this.loading = true
      try {
        let url = '/api/alert/filter-rules'
        if (this.filterType) {
          url = `/api/alert/filter-rules/type/${this.filterType}`
        }
        const response = await fetch(url, {
          credentials: 'include'
        })
        if (response.ok) {
          this.rules = await response.json()
        } else {
          ElMessage.error('获取规则列表失败')
        }
      } catch (error) {
        ElMessage.error('网络错误')
      }
      this.loading = false
    },
    async searchRules() {
      if (!this.searchKeyword.trim()) {
        this.loadRules()
        return
      }
      this.loading = true
      try {
        const response = await fetch(`/api/alert/filter-rules/search?keyword=${encodeURIComponent(this.searchKeyword)}`, {
          credentials: 'include'
        })
        if (response.ok) {
          this.rules = await response.json()
        } else {
          ElMessage.error('搜索失败')
        }
      } catch (error) {
        ElMessage.error('网络错误')
      }
      this.loading = false
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
        matchField: rule.matchField,
        matchType: rule.matchType,
        matchValue: rule.matchValue,
        priority: rule.priority,
        isEnabled: rule.isEnabled
      }
      this.dialogVisible = true
    },
    resetForm() {
      this.ruleForm = {
        ruleName: '',
        ruleDescription: '',
        alertType: null,
        alertSubtype: '',
        matchField: '',
        matchType: '',
        matchValue: '',
        priority: 0,
        isEnabled: true
      }
      if (this.$refs.ruleFormRef) {
        this.$refs.ruleFormRef.clearValidate()
      }
    },
    async saveRule() {
      if (!this.$refs.ruleFormRef) return

      const valid = await this.$refs.ruleFormRef.validate().catch(() => false)
      if (!valid) return

      this.saving = true
      try {
        const method = this.dialogMode === 'create' ? 'POST' : 'PUT'
        const url = this.dialogMode === 'create'
          ? '/api/alert/filter-rules'
          : `/api/alert/filter-rules/${this.currentRuleId}`

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
          ElMessage.error(error.error || '操作失败')
        }
      } catch (error) {
        ElMessage.error('网络错误')
      }
      this.saving = false
    },
    async toggleRule(rule) {
      try {
        const response = await fetch(`/api/alert/filter-rules/${rule.id}`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json'
          },
          credentials: 'include',
          body: JSON.stringify({
            ...rule,
            isEnabled: rule.isEnabled
          })
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

        const response = await fetch(`/api/alert/filter-rules/${rule.id}`, {
          method: 'DELETE',
          credentials: 'include'
        })

        if (response.ok) {
          ElMessage.success('规则删除成功')
          this.loadRules()
        } else {
          const error = await response.json()
          ElMessage.error(error.error || '删除失败')
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
    getMatchTypeName(type) {
      const types = {
        'exact': '精确匹配',
        'regex': '正则匹配'
      }
      return types[type] || type
    },
    getSubtypeLabel(alertTypeId, subtypeCode) {
      if (!alertTypeId || !subtypeCode) return subtypeCode || '-'
      const options = this.subtypeOptions[alertTypeId]
      if (!options) return subtypeCode
      const found = options.find(opt => opt.value === subtypeCode)
      return found ? found.label : subtypeCode
    },
    getFieldLabel(alertTypeId, fieldName) {
      if (!alertTypeId || !fieldName) return fieldName
      const options = this.fieldOptions[alertTypeId]
      if (!options) return fieldName
      const found = options.find(opt => opt.value === fieldName)
      return found ? found.label : fieldName
    }
  }
}
</script>

<style scoped>
.filter-rules {
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
</style>