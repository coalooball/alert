<template>
  <div class="associated-rules">
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        添加关联规则
      </el-button>
      <el-button @click="loadRules">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <el-table :data="rules" v-loading="loading" stripe border>
      <el-table-column prop="ruleName" label="规则名称" min-width="150" />
      <el-table-column prop="ruleType" label="规则类型" width="120">
        <template #default="{ row }">
          <el-tag :type="getRuleTypeTagType(row.ruleType)">
            {{ getRuleTypeLabel(row.ruleType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="correlationWindow" label="关联时间窗口" width="120" align="center">
        <template #default="{ row }">
          {{ row.correlationWindow }} 分钟
        </template>
      </el-table-column>
      <el-table-column prop="minAlerts" label="最小告警数" width="100" align="center" />
      <el-table-column prop="confidence" label="置信度" width="100" align="center">
        <template #default="{ row }">
          <el-progress :percentage="row.confidence" :width="60" type="circle" />
        </template>
      </el-table-column>
      <el-table-column prop="matchedCount" label="匹配次数" width="100" align="center">
        <template #default="{ row }">
          <el-badge :value="row.matchedCount" class="item" type="primary">
            <span>{{ row.matchedCount }}</span>
          </el-badge>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-switch v-model="row.isEnabled" @change="toggleRule(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleView(row)">查看</el-button>
          <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Add/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑关联规则' : '添加关联规则'"
      width="800px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="140px">
        <el-form-item label="规则名称" prop="ruleName">
          <el-input v-model="form.ruleName" placeholder="请输入规则名称" />
        </el-form-item>

        <el-form-item label="规则类型" prop="ruleType">
          <el-select v-model="form.ruleType" placeholder="请选择规则类型" style="width: 100%">
            <el-option label="时序关联" value="temporal" />
            <el-option label="空间关联" value="spatial" />
            <el-option label="行为关联" value="behavioral" />
            <el-option label="攻击链关联" value="attack_chain" />
          </el-select>
        </el-form-item>

        <el-form-item label="关联条件" prop="correlationConditions">
          <div class="condition-list">
            <div v-for="(condition, index) in form.correlationConditions" :key="index" class="condition-item">
              <el-select v-model="condition.field" placeholder="选择字段" style="width: 150px">
                <el-option label="源IP" value="src_ip" />
                <el-option label="目标IP" value="dst_ip" />
                <el-option label="告警类型" value="alert_type" />
                <el-option label="威胁等级" value="threat_level" />
              </el-select>
              <el-select v-model="condition.operator" placeholder="操作符" style="width: 120px; margin: 0 10px">
                <el-option label="等于" value="equals" />
                <el-option label="包含" value="contains" />
                <el-option label="大于" value="gt" />
                <el-option label="小于" value="lt" />
              </el-select>
              <el-input v-model="condition.value" placeholder="值" style="width: 200px" />
              <el-button type="danger" size="small" @click="removeCondition(index)" style="margin-left: 10px">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button type="primary" size="small" @click="addCondition">
              <el-icon><Plus /></el-icon>
              添加条件
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="关联时间窗口" prop="correlationWindow">
          <el-input-number
            v-model="form.correlationWindow"
            :min="1"
            :max="1440"
            placeholder="分钟"
            style="width: 200px"
          />
          <span style="margin-left: 10px">分钟</span>
        </el-form-item>

        <el-form-item label="最小告警数" prop="minAlerts">
          <el-input-number
            v-model="form.minAlerts"
            :min="1"
            :max="100"
            placeholder="最小告警数量"
            style="width: 200px"
          />
        </el-form-item>

        <el-form-item label="置信度阈值" prop="confidence">
          <el-slider v-model="form.confidence" :min="0" :max="100" show-input />
        </el-form-item>

        <el-form-item label="输出事件名称" prop="outputEventName">
          <el-input v-model="form.outputEventName" placeholder="关联分析后生成的事件名称" />
        </el-form-item>

        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="规则描述信息"
          />
        </el-form-item>

        <el-form-item label="启用状态">
          <el-switch v-model="form.isEnabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- View Dialog -->
    <el-dialog v-model="viewDialogVisible" title="规则详情" width="700px">
      <el-descriptions :column="1" border v-if="viewData">
        <el-descriptions-item label="规则名称">{{ viewData.ruleName }}</el-descriptions-item>
        <el-descriptions-item label="规则类型">
          <el-tag :type="getRuleTypeTagType(viewData.ruleType)">
            {{ getRuleTypeLabel(viewData.ruleType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="关联时间窗口">{{ viewData.correlationWindow }} 分钟</el-descriptions-item>
        <el-descriptions-item label="最小告警数">{{ viewData.minAlerts }}</el-descriptions-item>
        <el-descriptions-item label="置信度">{{ viewData.confidence }}%</el-descriptions-item>
        <el-descriptions-item label="匹配次数">{{ viewData.matchedCount }}</el-descriptions-item>
        <el-descriptions-item label="输出事件">{{ viewData.outputEventName }}</el-descriptions-item>
        <el-descriptions-item label="描述">{{ viewData.description || '暂无描述' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="viewData.isEnabled ? 'success' : 'info'">
            {{ viewData.isEnabled ? '已启用' : '已禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ viewData.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ viewData.updateTime }}</el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Delete } from '@element-plus/icons-vue'

export default {
  name: 'AssociatedRules',
  components: { Plus, Refresh, Delete },
  setup() {
    const rules = ref([])
    const loading = ref(false)
    const dialogVisible = ref(false)
    const viewDialogVisible = ref(false)
    const isEdit = ref(false)
    const formRef = ref(null)
    const viewData = ref(null)

    const form = reactive({
      id: null,
      ruleName: '',
      ruleType: '',
      correlationConditions: [
        { field: '', operator: '', value: '' }
      ],
      correlationWindow: 30,
      minAlerts: 3,
      confidence: 70,
      outputEventName: '',
      description: '',
      isEnabled: true
    })

    const formRules = {
      ruleName: [
        { required: true, message: '请输入规则名称', trigger: 'blur' }
      ],
      ruleType: [
        { required: true, message: '请选择规则类型', trigger: 'change' }
      ],
      correlationWindow: [
        { required: true, message: '请设置关联时间窗口', trigger: 'blur' }
      ],
      minAlerts: [
        { required: true, message: '请设置最小告警数', trigger: 'blur' }
      ],
      confidence: [
        { required: true, message: '请设置置信度阈值', trigger: 'change' }
      ],
      outputEventName: [
        { required: true, message: '请输入输出事件名称', trigger: 'blur' }
      ]
    }

    const getRuleTypeTagType = (type) => {
      const typeMap = {
        'temporal': 'primary',
        'spatial': 'success',
        'behavioral': 'warning',
        'attack_chain': 'danger'
      }
      return typeMap[type] || 'info'
    }

    const getRuleTypeLabel = (type) => {
      const typeMap = {
        'temporal': '时序关联',
        'spatial': '空间关联',
        'behavioral': '行为关联',
        'attack_chain': '攻击链关联'
      }
      return typeMap[type] || type
    }

    const addCondition = () => {
      form.correlationConditions.push({ field: '', operator: '', value: '' })
    }

    const removeCondition = (index) => {
      if (form.correlationConditions.length > 1) {
        form.correlationConditions.splice(index, 1)
      }
    }

    const loadRules = () => {
      loading.value = true
      // 模拟数据
      setTimeout(() => {
        rules.value = [
          {
            id: 1,
            ruleName: '暴力破解攻击链检测',
            ruleType: 'attack_chain',
            correlationWindow: 30,
            minAlerts: 5,
            confidence: 85,
            matchedCount: 128,
            outputEventName: '暴力破解攻击事件',
            description: '检测同一源IP对多个目标进行暴力破解尝试',
            isEnabled: true,
            createTime: '2024-01-20 10:00:00',
            updateTime: '2024-01-20 10:00:00'
          },
          {
            id: 2,
            ruleName: '横向移动检测',
            ruleType: 'spatial',
            correlationWindow: 60,
            minAlerts: 3,
            confidence: 75,
            matchedCount: 45,
            outputEventName: '内网横向移动事件',
            description: '检测内网中的横向移动行为',
            isEnabled: true,
            createTime: '2024-01-19 15:30:00',
            updateTime: '2024-01-19 15:30:00'
          },
          {
            id: 3,
            ruleName: '数据外泄行为分析',
            ruleType: 'behavioral',
            correlationWindow: 120,
            minAlerts: 2,
            confidence: 90,
            matchedCount: 12,
            outputEventName: '疑似数据外泄事件',
            description: '分析异常的数据传输行为',
            isEnabled: false,
            createTime: '2024-01-18 09:20:00',
            updateTime: '2024-01-18 09:20:00'
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

    const handleView = (row) => {
      viewData.value = row
      viewDialogVisible.value = true
    }

    const handleEdit = (row) => {
      isEdit.value = true
      Object.assign(form, {
        ...row,
        correlationConditions: row.correlationConditions || [{ field: '', operator: '', value: '' }]
      })
      dialogVisible.value = true
    }

    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm(
          `确定要删除规则 "${row.ruleName}" 吗？`,
          '删除确认',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        ElMessage.success('删除成功')
        loadRules()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败')
        }
      }
    }

    const toggleRule = (row) => {
      ElMessage.success(`已${row.isEnabled ? '启用' : '禁用'}规则`)
    }

    const handleSubmit = async () => {
      try {
        await formRef.value.validate()
        ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
        dialogVisible.value = false
        loadRules()
      } catch (error) {
        ElMessage.error('请填写必填项')
      }
    }

    const resetForm = () => {
      form.id = null
      form.ruleName = ''
      form.ruleType = ''
      form.correlationConditions = [{ field: '', operator: '', value: '' }]
      form.correlationWindow = 30
      form.minAlerts = 3
      form.confidence = 70
      form.outputEventName = ''
      form.description = ''
      form.isEnabled = true
      if (formRef.value) {
        formRef.value.clearValidate()
      }
    }

    onMounted(() => {
      loadRules()
    })

    return {
      rules,
      loading,
      dialogVisible,
      viewDialogVisible,
      isEdit,
      formRef,
      viewData,
      form,
      formRules,
      getRuleTypeTagType,
      getRuleTypeLabel,
      addCondition,
      removeCondition,
      loadRules,
      handleAdd,
      handleView,
      handleEdit,
      handleDelete,
      toggleRule,
      handleSubmit,
      resetForm
    }
  }
}
</script>

<style scoped>
.associated-rules {
  padding: 10px;
}

.toolbar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

.condition-list {
  width: 100%;
}

.condition-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.item {
  margin: 0 5px;
}
</style>