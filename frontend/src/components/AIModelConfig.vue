<template>
  <div class="ai-model-config">
    <div class="toolbar">
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>
        添加模型配置
      </el-button>
      <el-button @click="loadModels">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
      <el-button @click="trainAllModels">
        <el-icon><VideoPlay /></el-icon>
        批量训练
      </el-button>
    </div>

    <el-table :data="models" v-loading="loading" stripe border>
      <el-table-column prop="modelName" label="模型名称" min-width="150" />
      <el-table-column prop="modelType" label="模型类型" width="130">
        <template #default="{ row }">
          <el-tag :type="getModelTypeTagType(row.modelType)">
            {{ getModelTypeLabel(row.modelType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="algorithm" label="算法" width="120">
        <template #default="{ row }">
          <el-tag type="info">{{ row.algorithm }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="accuracy" label="准确率" width="120" align="center">
        <template #default="{ row }">
          <el-progress :percentage="row.accuracy" :color="getAccuracyColor(row.accuracy)" />
        </template>
      </el-table-column>
      <el-table-column prop="trainingStatus" label="训练状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="getTrainingStatusType(row.trainingStatus)">
            {{ getTrainingStatusLabel(row.trainingStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastTrainTime" label="最后训练时间" width="160" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-switch v-model="row.isEnabled" @change="toggleModel(row)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleTrain(row)">训练</el-button>
          <el-button size="small" @click="handleEvaluate(row)">评估</el-button>
          <el-button size="small" @click="handleView(row)">查看</el-button>
          <el-button size="small" type="primary" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Add/Edit Dialog -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑模型配置' : '添加模型配置'"
      width="800px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="140px">
        <el-form-item label="模型名称" prop="modelName">
          <el-input v-model="form.modelName" placeholder="请输入模型名称" />
        </el-form-item>

        <el-form-item label="模型类型" prop="modelType">
          <el-select v-model="form.modelType" placeholder="请选择模型类型" style="width: 100%" @change="onModelTypeChange">
            <el-option label="异常检测" value="anomaly_detection" />
            <el-option label="威胁分类" value="threat_classification" />
            <el-option label="攻击预测" value="attack_prediction" />
            <el-option label="行为分析" value="behavior_analysis" />
            <el-option label="风险评分" value="risk_scoring" />
          </el-select>
        </el-form-item>

        <el-form-item label="算法选择" prop="algorithm">
          <el-select v-model="form.algorithm" placeholder="请选择算法" style="width: 100%">
            <el-option-group label="监督学习">
              <el-option label="随机森林" value="random_forest" />
              <el-option label="XGBoost" value="xgboost" />
              <el-option label="神经网络" value="neural_network" />
              <el-option label="SVM" value="svm" />
            </el-option-group>
            <el-option-group label="无监督学习">
              <el-option label="孤立森林" value="isolation_forest" />
              <el-option label="DBSCAN" value="dbscan" />
              <el-option label="K-Means" value="kmeans" />
            </el-option-group>
            <el-option-group label="深度学习">
              <el-option label="LSTM" value="lstm" />
              <el-option label="Transformer" value="transformer" />
              <el-option label="CNN" value="cnn" />
            </el-option-group>
          </el-select>
        </el-form-item>

        <el-form-item label="训练数据集" prop="datasetConfig">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-select v-model="form.datasetConfig.source" placeholder="数据源" style="width: 100%">
                <el-option label="历史告警数据" value="historical_alerts" />
                <el-option label="实时流数据" value="realtime_stream" />
                <el-option label="外部数据集" value="external_dataset" />
              </el-select>
            </el-col>
            <el-col :span="12">
              <el-input-number
                v-model="form.datasetConfig.sampleSize"
                :min="1000"
                :max="1000000"
                placeholder="样本数量"
                style="width: 100%"
              />
            </el-col>
          </el-row>
        </el-form-item>

        <el-form-item label="特征工程" prop="features">
          <el-checkbox-group v-model="form.features">
            <el-checkbox label="ip_features">IP特征</el-checkbox>
            <el-checkbox label="time_features">时间特征</el-checkbox>
            <el-checkbox label="behavior_features">行为特征</el-checkbox>
            <el-checkbox label="statistical_features">统计特征</el-checkbox>
            <el-checkbox label="network_features">网络特征</el-checkbox>
            <el-checkbox label="content_features">内容特征</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="模型参数">
          <el-input
            v-model="form.parameters"
            type="textarea"
            :rows="4"
            placeholder="JSON格式的模型参数，例如：{&quot;max_depth&quot;: 10, &quot;n_estimators&quot;: 100}"
          />
        </el-form-item>

        <el-form-item label="训练计划" prop="trainingSchedule">
          <el-radio-group v-model="form.trainingSchedule">
            <el-radio label="manual">手动训练</el-radio>
            <el-radio label="daily">每日训练</el-radio>
            <el-radio label="weekly">每周训练</el-radio>
            <el-radio label="monthly">每月训练</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="评估指标" prop="evaluationMetrics">
          <el-checkbox-group v-model="form.evaluationMetrics">
            <el-checkbox label="accuracy">准确率</el-checkbox>
            <el-checkbox label="precision">精确率</el-checkbox>
            <el-checkbox label="recall">召回率</el-checkbox>
            <el-checkbox label="f1_score">F1分数</el-checkbox>
            <el-checkbox label="auc_roc">AUC-ROC</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <el-form-item label="阈值设置" prop="threshold">
          <el-slider v-model="form.threshold" :min="0" :max="1" :step="0.01" show-input />
        </el-form-item>

        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="2"
            placeholder="模型描述信息"
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
    <el-dialog v-model="viewDialogVisible" title="模型详情" width="800px">
      <el-descriptions :column="2" border v-if="viewData">
        <el-descriptions-item label="模型名称" :span="2">{{ viewData.modelName }}</el-descriptions-item>
        <el-descriptions-item label="模型类型">
          <el-tag :type="getModelTypeTagType(viewData.modelType)">
            {{ getModelTypeLabel(viewData.modelType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="算法">
          <el-tag type="info">{{ viewData.algorithm }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="准确率">{{ viewData.accuracy }}%</el-descriptions-item>
        <el-descriptions-item label="训练状态">
          <el-tag :type="getTrainingStatusType(viewData.trainingStatus)">
            {{ getTrainingStatusLabel(viewData.trainingStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="训练计划">{{ viewData.trainingSchedule }}</el-descriptions-item>
        <el-descriptions-item label="阈值">{{ viewData.threshold }}</el-descriptions-item>
        <el-descriptions-item label="最后训练时间">{{ viewData.lastTrainTime }}</el-descriptions-item>
        <el-descriptions-item label="下次训练时间">{{ viewData.nextTrainTime }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="viewData.isEnabled ? 'success' : 'info'">
            {{ viewData.isEnabled ? '已启用' : '已禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ viewData.description || '暂无描述' }}</el-descriptions-item>
      </el-descriptions>

      <div v-if="viewData && viewData.metrics" style="margin-top: 20px">
        <h4>性能指标</h4>
        <el-row :gutter="20">
          <el-col :span="6">
            <el-statistic title="准确率" :value="viewData.metrics.accuracy" suffix="%" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="精确率" :value="viewData.metrics.precision" suffix="%" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="召回率" :value="viewData.metrics.recall" suffix="%" />
          </el-col>
          <el-col :span="6">
            <el-statistic title="F1分数" :value="viewData.metrics.f1Score" :precision="3" />
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
import { Plus, Refresh, VideoPlay } from '@element-plus/icons-vue'

export default {
  name: 'AIModelConfig',
  components: { Plus, Refresh, VideoPlay },
  setup() {
    const models = ref([])
    const loading = ref(false)
    const dialogVisible = ref(false)
    const viewDialogVisible = ref(false)
    const isEdit = ref(false)
    const formRef = ref(null)
    const viewData = ref(null)

    const form = reactive({
      id: null,
      modelName: '',
      modelType: '',
      algorithm: '',
      datasetConfig: {
        source: 'historical_alerts',
        sampleSize: 10000
      },
      features: [],
      parameters: '',
      trainingSchedule: 'manual',
      evaluationMetrics: ['accuracy', 'precision', 'recall'],
      threshold: 0.5,
      description: '',
      isEnabled: true
    })

    const rules = {
      modelName: [
        { required: true, message: '请输入模型名称', trigger: 'blur' }
      ],
      modelType: [
        { required: true, message: '请选择模型类型', trigger: 'change' }
      ],
      algorithm: [
        { required: true, message: '请选择算法', trigger: 'change' }
      ],
      features: [
        { type: 'array', required: true, message: '请选择至少一个特征', trigger: 'change' }
      ],
      evaluationMetrics: [
        { type: 'array', required: true, message: '请选择评估指标', trigger: 'change' }
      ]
    }

    const getModelTypeTagType = (type) => {
      const typeMap = {
        'anomaly_detection': 'warning',
        'threat_classification': 'danger',
        'attack_prediction': 'primary',
        'behavior_analysis': 'success',
        'risk_scoring': 'info'
      }
      return typeMap[type] || 'info'
    }

    const getModelTypeLabel = (type) => {
      const typeMap = {
        'anomaly_detection': '异常检测',
        'threat_classification': '威胁分类',
        'attack_prediction': '攻击预测',
        'behavior_analysis': '行为分析',
        'risk_scoring': '风险评分'
      }
      return typeMap[type] || type
    }

    const getTrainingStatusType = (status) => {
      const typeMap = {
        'trained': 'success',
        'training': 'warning',
        'failed': 'danger',
        'pending': 'info'
      }
      return typeMap[status] || 'info'
    }

    const getTrainingStatusLabel = (status) => {
      const typeMap = {
        'trained': '已训练',
        'training': '训练中',
        'failed': '训练失败',
        'pending': '待训练'
      }
      return typeMap[status] || status
    }

    const getAccuracyColor = (percentage) => {
      if (percentage >= 90) return '#67c23a'
      if (percentage >= 70) return '#e6a23c'
      return '#f56c6c'
    }

    const onModelTypeChange = () => {
      // 根据模型类型推荐算法
      const recommendations = {
        'anomaly_detection': 'isolation_forest',
        'threat_classification': 'random_forest',
        'attack_prediction': 'lstm',
        'behavior_analysis': 'dbscan',
        'risk_scoring': 'xgboost'
      }
      if (!form.algorithm && form.modelType) {
        form.algorithm = recommendations[form.modelType] || ''
      }
    }

    const loadModels = () => {
      loading.value = true
      // 模拟数据
      setTimeout(() => {
        models.value = [
          {
            id: 1,
            modelName: 'DDoS异常检测模型',
            modelType: 'anomaly_detection',
            algorithm: 'isolation_forest',
            accuracy: 92,
            trainingStatus: 'trained',
            lastTrainTime: '2024-01-23 10:00:00',
            nextTrainTime: '2024-01-24 10:00:00',
            trainingSchedule: 'daily',
            threshold: 0.85,
            description: '用于检测DDoS攻击的异常流量模型',
            isEnabled: true,
            metrics: {
              accuracy: 92,
              precision: 89,
              recall: 94,
              f1Score: 0.915
            },
            createTime: '2024-01-01 10:00:00',
            updateTime: '2024-01-23 10:00:00'
          },
          {
            id: 2,
            modelName: '恶意软件分类器',
            modelType: 'threat_classification',
            algorithm: 'xgboost',
            accuracy: 88,
            trainingStatus: 'training',
            lastTrainTime: '2024-01-22 15:30:00',
            nextTrainTime: '2024-01-29 15:30:00',
            trainingSchedule: 'weekly',
            threshold: 0.75,
            description: '多类别恶意软件分类模型',
            isEnabled: true,
            metrics: {
              accuracy: 88,
              precision: 86,
              recall: 90,
              f1Score: 0.88
            },
            createTime: '2024-01-10 14:00:00',
            updateTime: '2024-01-22 15:30:00'
          },
          {
            id: 3,
            modelName: 'APT攻击预测',
            modelType: 'attack_prediction',
            algorithm: 'lstm',
            accuracy: 78,
            trainingStatus: 'pending',
            lastTrainTime: '2024-01-20 08:00:00',
            nextTrainTime: '2024-02-20 08:00:00',
            trainingSchedule: 'monthly',
            threshold: 0.6,
            description: '基于LSTM的APT攻击序列预测',
            isEnabled: false,
            metrics: {
              accuracy: 78,
              precision: 75,
              recall: 82,
              f1Score: 0.784
            },
            createTime: '2023-12-15 09:00:00',
            updateTime: '2024-01-20 08:00:00'
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

    const handleTrain = async (row) => {
      ElMessage.info(`正在训练模型 "${row.modelName}"...`)
      row.trainingStatus = 'training'
      setTimeout(() => {
        row.trainingStatus = 'trained'
        row.accuracy = Math.floor(Math.random() * 15) + 80
        row.lastTrainTime = new Date().toLocaleString('zh-CN')
        ElMessage.success(`模型 "${row.modelName}" 训练完成，准确率: ${row.accuracy}%`)
      }, 3000)
    }

    const handleEvaluate = (row) => {
      ElMessage.info(`正在评估模型 "${row.modelName}"...`)
      setTimeout(() => {
        ElMessage.success(`模型评估完成，F1分数: 0.${Math.floor(Math.random() * 100) + 800}`)
      }, 2000)
    }

    const trainAllModels = () => {
      ElMessage.info('开始批量训练模型...')
      models.value.forEach(model => {
        if (model.isEnabled) {
          model.trainingStatus = 'training'
        }
      })
      setTimeout(() => {
        models.value.forEach(model => {
          if (model.isEnabled) {
            model.trainingStatus = 'trained'
            model.accuracy = Math.floor(Math.random() * 15) + 80
            model.lastTrainTime = new Date().toLocaleString('zh-CN')
          }
        })
        ElMessage.success('批量训练完成')
      }, 5000)
    }

    const handleView = (row) => {
      viewData.value = row
      viewDialogVisible.value = true
    }

    const handleEdit = (row) => {
      isEdit.value = true
      Object.assign(form, {
        ...row,
        datasetConfig: row.datasetConfig || { source: 'historical_alerts', sampleSize: 10000 },
        features: row.features || [],
        evaluationMetrics: row.evaluationMetrics || ['accuracy']
      })
      dialogVisible.value = true
    }

    const handleDelete = async (row) => {
      try {
        await ElMessageBox.confirm(
          `确定要删除模型 "${row.modelName}" 吗？`,
          '删除确认',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        ElMessage.success('删除成功')
        loadModels()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败')
        }
      }
    }

    const toggleModel = (row) => {
      ElMessage.success(`已${row.isEnabled ? '启用' : '禁用'}模型`)
    }

    const handleSubmit = async () => {
      try {
        await formRef.value.validate()
        ElMessage.success(isEdit.value ? '更新成功' : '添加成功')
        dialogVisible.value = false
        loadModels()
      } catch (error) {
        ElMessage.error('请填写必填项')
      }
    }

    const resetForm = () => {
      form.id = null
      form.modelName = ''
      form.modelType = ''
      form.algorithm = ''
      form.datasetConfig = {
        source: 'historical_alerts',
        sampleSize: 10000
      }
      form.features = []
      form.parameters = ''
      form.trainingSchedule = 'manual'
      form.evaluationMetrics = ['accuracy', 'precision', 'recall']
      form.threshold = 0.5
      form.description = ''
      form.isEnabled = true
      if (formRef.value) {
        formRef.value.clearValidate()
      }
    }

    onMounted(() => {
      loadModels()
    })

    return {
      models,
      loading,
      dialogVisible,
      viewDialogVisible,
      isEdit,
      formRef,
      viewData,
      form,
      rules,
      getModelTypeTagType,
      getModelTypeLabel,
      getTrainingStatusType,
      getTrainingStatusLabel,
      getAccuracyColor,
      onModelTypeChange,
      loadModels,
      handleAdd,
      handleTrain,
      handleEvaluate,
      trainAllModels,
      handleView,
      handleEdit,
      handleDelete,
      toggleModel,
      handleSubmit,
      resetForm
    }
  }
}
</script>

<style scoped>
.ai-model-config {
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