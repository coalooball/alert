<template>
  <div class="associated-playbooks">
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        添加剧本关联
      </el-button>
      <el-button @click="loadPlaybooks">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <el-table :data="playbooks" v-loading="loading" stripe border>
      <el-table-column prop="playbookName" label="剧本名称" min-width="150" />
      <el-table-column prop="alertType" label="告警类型" width="120">
        <template #default="{ row }">
          <el-tag :type="getAlertTypeTagType(row.alertType)">
            {{ row.alertTypeLabel || row.alertType }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="triggerCondition" label="触发条件" min-width="200" show-overflow-tooltip />
      <el-table-column prop="actionType" label="动作类型" width="120">
        <template #default="{ row }">
          <el-tag type="info">{{ row.actionType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="priority" label="优先级" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getPriorityType(row.priority)">
            {{ row.priority }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-switch v-model="row.isEnabled" @change="togglePlaybook(row)" />
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
      :title="isEdit ? '编辑剧本关联' : '添加剧本关联'"
      width="700px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="剧本名称" prop="playbookName">
          <el-input v-model="form.playbookName" placeholder="请输入剧本名称" />
        </el-form-item>

        <el-form-item label="告警类型" prop="alertType">
          <el-select v-model="form.alertType" placeholder="请选择告警类型" style="width: 100%">
            <el-option label="网络攻击" value="network_attack" />
            <el-option label="恶意样本" value="malicious_sample" />
            <el-option label="主机行为" value="host_behavior" />
            <el-option label="全部类型" value="all" />
          </el-select>
        </el-form-item>

        <el-form-item label="触发条件" prop="triggerCondition">
          <el-input
            v-model="form.triggerCondition"
            type="textarea"
            :rows="3"
            placeholder="描述触发该剧本的条件"
          />
        </el-form-item>

        <el-form-item label="动作类型" prop="actionType">
          <el-select v-model="form.actionType" placeholder="请选择动作类型" style="width: 100%">
            <el-option label="自动响应" value="auto_response" />
            <el-option label="通知告警" value="notification" />
            <el-option label="隔离处置" value="isolation" />
            <el-option label="证据收集" value="evidence_collection" />
            <el-option label="自定义脚本" value="custom_script" />
          </el-select>
        </el-form-item>

        <el-form-item label="执行脚本" v-if="form.actionType === 'custom_script'">
          <el-input
            v-model="form.scriptContent"
            type="textarea"
            :rows="6"
            placeholder="输入要执行的脚本内容"
          />
        </el-form-item>

        <el-form-item label="优先级" prop="priority">
          <el-radio-group v-model="form.priority">
            <el-radio label="高">高优先级</el-radio>
            <el-radio label="中">中优先级</el-radio>
            <el-radio label="低">低优先级</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            placeholder="剧本描述信息"
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
    <el-dialog v-model="viewDialogVisible" title="剧本详情" width="700px">
      <el-descriptions :column="1" border v-if="viewData">
        <el-descriptions-item label="剧本名称">{{ viewData.playbookName }}</el-descriptions-item>
        <el-descriptions-item label="告警类型">
          <el-tag :type="getAlertTypeTagType(viewData.alertType)">
            {{ viewData.alertTypeLabel || viewData.alertType }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="触发条件">{{ viewData.triggerCondition }}</el-descriptions-item>
        <el-descriptions-item label="动作类型">
          <el-tag type="info">{{ viewData.actionType }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="优先级">
          <el-tag :type="getPriorityType(viewData.priority)">{{ viewData.priority }}</el-tag>
        </el-descriptions-item>
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
import { Plus, Refresh } from '@element-plus/icons-vue'

export default {
  name: 'AssociatedPlaybooks',
  components: { Plus, Refresh },
  setup() {
    const playbooks = ref([])
    const loading = ref(false)
    const dialogVisible = ref(false)
    const viewDialogVisible = ref(false)
    const isEdit = ref(false)
    const formRef = ref(null)
    const viewData = ref(null)

    const form = reactive({
      id: null,
      playbookName: '',
      alertType: '',
      triggerCondition: '',
      actionType: '',
      scriptContent: '',
      priority: '中',
      description: '',
      isEnabled: true
    })

    const rules = {
      playbookName: [
        { required: true, message: '请输入剧本名称', trigger: 'blur' }
      ],
      alertType: [
        { required: true, message: '请选择告警类型', trigger: 'change' }
      ],
      triggerCondition: [
        { required: true, message: '请输入触发条件', trigger: 'blur' }
      ],
      actionType: [
        { required: true, message: '请选择动作类型', trigger: 'change' }
      ],
      priority: [
        { required: true, message: '请选择优先级', trigger: 'change' }
      ]
    }

    const getAlertTypeTagType = (type) => {
      const typeMap = {
        'network_attack': 'danger',
        'malicious_sample': 'warning',
        'host_behavior': 'primary',
        'all': 'info'
      }
      return typeMap[type] || 'info'
    }

    const getPriorityType = (priority) => {
      const typeMap = {
        '高': 'danger',
        '中': 'warning',
        '低': 'info'
      }
      return typeMap[priority] || 'info'
    }

    const loadPlaybooks = () => {
      loading.value = true
      // 模拟数据
      setTimeout(() => {
        playbooks.value = [
          {
            id: 1,
            playbookName: 'DDoS攻击自动响应',
            alertType: 'network_attack',
            alertTypeLabel: '网络攻击',
            triggerCondition: '检测到来自同一源IP的大量请求，且QPS超过1000',
            actionType: 'auto_response',
            priority: '高',
            description: '自动触发DDoS防护措施，包括流量清洗和IP封禁',
            isEnabled: true,
            createTime: '2024-01-20 10:00:00',
            updateTime: '2024-01-20 10:00:00'
          },
          {
            id: 2,
            playbookName: '恶意文件隔离处置',
            alertType: 'malicious_sample',
            alertTypeLabel: '恶意样本',
            triggerCondition: '检测到恶意文件且威胁等级>=高危',
            actionType: 'isolation',
            priority: '高',
            description: '自动隔离恶意文件并生成处置报告',
            isEnabled: true,
            createTime: '2024-01-19 15:30:00',
            updateTime: '2024-01-19 15:30:00'
          },
          {
            id: 3,
            playbookName: '异常登录告警通知',
            alertType: 'host_behavior',
            alertTypeLabel: '主机行为',
            triggerCondition: '检测到非常规地点登录或异常时间登录',
            actionType: 'notification',
            priority: '中',
            description: '发送告警通知到安全运营中心',
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
      Object.assign(form, row)
      dialogVisible.value = true
    }

    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm(
          `确定要删除剧本 "${row.playbookName}" 吗？`,
          '删除确认',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        ElMessage.success('删除成功')
        loadPlaybooks()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败')
        }
      }
    }

    const togglePlaybook = (row) => {
      ElMessage.success(`已${row.isEnabled ? '启用' : '禁用'}剧本`)
    }

    const handleSubmit = async () => {
      try {
        await formRef.value.validate()
        ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
        dialogVisible.value = false
        loadPlaybooks()
      } catch (error) {
        ElMessage.error('请填写必填项')
      }
    }

    const resetForm = () => {
      form.id = null
      form.playbookName = ''
      form.alertType = ''
      form.triggerCondition = ''
      form.actionType = ''
      form.scriptContent = ''
      form.priority = '中'
      form.description = ''
      form.isEnabled = true
      if (formRef.value) {
        formRef.value.clearValidate()
      }
    }

    onMounted(() => {
      loadPlaybooks()
    })

    return {
      playbooks,
      loading,
      dialogVisible,
      viewDialogVisible,
      isEdit,
      formRef,
      viewData,
      form,
      rules,
      getAlertTypeTagType,
      getPriorityType,
      loadPlaybooks,
      handleAdd,
      handleView,
      handleEdit,
      handleDelete,
      togglePlaybook,
      handleSubmit,
      resetForm
    }
  }
}
</script>

<style scoped>
.associated-playbooks {
  padding: 10px;
}

.toolbar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}
</style>