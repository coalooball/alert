<template>
  <div class="model-management">
    <div class="page-header">
      <h3>智能模型管理</h3>
      <el-button type="primary" @click="showCreateDialog" :icon="Plus">
        添加模型
      </el-button>
    </div>

    <div class="filter-bar">
      <el-select v-model="filterModelType" placeholder="选择模型类型" clearable @change="loadModels">
        <el-option label="聚类模型" value="clustering" />
        <el-option label="分类模型" value="classification" />
        <el-option label="异常检测" value="anomaly" />
        <el-option label="时序预测" value="timeseries" />
        <el-option label="深度学习" value="deeplearning" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="选择状态" clearable @change="loadModels" style="margin-left: 10px">
        <el-option label="训练中" value="training" />
        <el-option label="就绪" value="ready" />
        <el-option label="部署中" value="deploying" />
        <el-option label="已部署" value="deployed" />
        <el-option label="已停用" value="disabled" />
      </el-select>
      <el-input
        v-model="searchKeyword"
        placeholder="搜索模型名称或描述"
        clearable
        style="width: 300px; margin-left: 10px"
        @keyup.enter="searchModels"
      >
        <template #append>
          <el-button :icon="Search" @click="searchModels" />
        </template>
      </el-input>
      <el-button @click="loadModels" :icon="Refresh" style="margin-left: 10px">
        刷新
      </el-button>
    </div>

    <el-table
      :data="models"
      style="width: 100%"
      v-loading="loading"
      border
    >
      <el-table-column prop="modelName" label="模型名称" min-width="150" />
      <el-table-column prop="modelType" label="模型类型" width="120">
        <template #default="scope">
          <el-tag :type="getModelTypeTag(scope.row.modelType)">
            {{ getModelTypeName(scope.row.modelType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="algorithm" label="算法" width="150">
        <template #default="scope">
          {{ getAlgorithmName(scope.row.algorithm) }}
        </template>
      </el-table-column>
      <el-table-column prop="version" label="版本" width="80" align="center">
        <template #default="scope">
          v{{ scope.row.version || '1.0' }}
        </template>
      </el-table-column>
      <el-table-column prop="accuracy" label="准确率" width="100" align="center">
        <template #default="scope">
          <el-progress
            :percentage="scope.row.accuracy"
            :color="getAccuracyColor(scope.row.accuracy)"
            :stroke-width="8"
          />
        </template>
      </el-table-column>
      <el-table-column prop="lastTrainTime" label="最后训练时间" width="180">
        <template #default="scope">
          {{ formatDateTime(scope.row.lastTrainTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="120" align="center">
        <template #default="scope">
          <el-tag :type="getStatusTag(scope.row.status)" effect="dark">
            {{ getStatusName(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="isEnabled" label="启用" width="80" align="center">
        <template #default="scope">
          <el-switch
            v-model="scope.row.isEnabled"
            @change="toggleModel(scope.row)"
            :disabled="scope.row.status !== 'deployed' && scope.row.status !== 'disabled'"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="scope">
          <el-button
            size="small"
            @click="viewModelDetails(scope.row)"
          >
            详情
          </el-button>
          <el-button
            size="small"
            type="primary"
            @click="trainModel(scope.row)"
            :disabled="scope.row.status === 'training'"
          >
            训练
          </el-button>
          <el-button
            size="small"
            type="success"
            @click="deployModel(scope.row)"
            :disabled="scope.row.status !== 'ready'"
          >
            部署
          </el-button>
          <el-button
            size="small"
            type="danger"
            @click="deleteModel(scope.row)"
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
      :title="dialogMode === 'create' ? '添加智能模型' : '编辑智能模型'"
      width="900px"
    >
      <el-form
        :model="modelForm"
        :rules="formRules"
        ref="modelFormRef"
        label-width="140px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="模型名称" prop="modelName">
              <el-input v-model="modelForm.modelName" placeholder="请输入模型名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模型版本" prop="version">
              <el-input v-model="modelForm.version" placeholder="例如: 1.0.0" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="模型描述" prop="description">
          <el-input
            v-model="modelForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入模型描述"
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="模型类型" prop="modelType">
              <el-select v-model="modelForm.modelType" placeholder="请选择模型类型" style="width: 100%" @change="onModelTypeChange">
                <el-option label="聚类模型" value="clustering" />
                <el-option label="分类模型" value="classification" />
                <el-option label="异常检测" value="anomaly" />
                <el-option label="时序预测" value="timeseries" />
                <el-option label="深度学习" value="deeplearning" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="算法选择" prop="algorithm">
              <el-select v-model="modelForm.algorithm" placeholder="请选择算法" style="width: 100%" :disabled="!modelForm.modelType">
                <el-option
                  v-for="alg in currentAlgorithmOptions"
                  :key="alg.value"
                  :label="alg.label"
                  :value="alg.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 模型参数配置 -->
        <el-form-item label="模型参数">
          <el-card class="parameter-card">
            <template #header>
              <span>算法参数配置</span>
            </template>
            <el-form-item label="超参数设置">
              <el-input
                v-model="hyperparametersString"
                type="textarea"
                :rows="4"
                placeholder='请输入JSON格式的超参数，例如：{"n_clusters": 5, "max_iter": 100}'
                @blur="parseHyperparameters"
              />
            </el-form-item>
            <el-form-item label="特征工程">
              <el-checkbox-group v-model="modelForm.featureEngineering">
                <el-checkbox label="standardization">标准化</el-checkbox>
                <el-checkbox label="normalization">归一化</el-checkbox>
                <el-checkbox label="pca">主成分分析</el-checkbox>
                <el-checkbox label="feature_selection">特征选择</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-card>
        </el-form-item>

        <!-- 训练配置 -->
        <el-form-item label="训练配置">
          <el-card class="parameter-card">
            <template #header>
              <span>训练数据与策略配置</span>
            </template>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="数据源">
                  <el-select v-model="modelForm.dataSource" placeholder="选择数据源" style="width: 100%">
                    <el-option label="历史告警数据" value="historical_alerts" />
                    <el-option label="实时流数据" value="realtime_stream" />
                    <el-option label="外部数据集" value="external_dataset" />
                    <el-option label="混合数据源" value="hybrid" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="训练数据量">
                  <el-input-number v-model="modelForm.trainDataSize" :min="100" :max="1000000" :step="100" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="训练批次大小">
                  <el-input-number v-model="modelForm.batchSize" :min="16" :max="512" :step="16" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="训练轮次">
                  <el-input-number v-model="modelForm.epochs" :min="1" :max="1000" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="评估指标">
              <el-checkbox-group v-model="modelForm.evaluationMetrics">
                <el-checkbox label="accuracy">准确率</el-checkbox>
                <el-checkbox label="precision">精确率</el-checkbox>
                <el-checkbox label="recall">召回率</el-checkbox>
                <el-checkbox label="f1_score">F1分数</el-checkbox>
                <el-checkbox label="auc_roc">AUC-ROC</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
          </el-card>
        </el-form-item>

        <!-- 部署配置 -->
        <el-form-item label="部署配置">
          <el-card class="parameter-card">
            <template #header>
              <span>模型部署设置</span>
            </template>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="部署环境">
                  <el-select v-model="modelForm.deploymentEnv" placeholder="选择部署环境" style="width: 100%">
                    <el-option label="开发环境" value="development" />
                    <el-option label="测试环境" value="testing" />
                    <el-option label="预生产环境" value="staging" />
                    <el-option label="生产环境" value="production" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="资源限制">
                  <el-select v-model="modelForm.resourceLimit" placeholder="选择资源限制" style="width: 100%">
                    <el-option label="小型 (1CPU, 2GB)" value="small" />
                    <el-option label="中型 (2CPU, 4GB)" value="medium" />
                    <el-option label="大型 (4CPU, 8GB)" value="large" />
                    <el-option label="超大型 (8CPU, 16GB)" value="xlarge" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="自动重训练">
              <el-switch v-model="modelForm.autoRetrain" />
              <span style="margin-left: 10px; color: #909399;">定期使用新数据自动重训练模型</span>
            </el-form-item>
            <el-form-item label="重训练周期" v-if="modelForm.autoRetrain">
              <el-select v-model="modelForm.retrainPeriod" placeholder="选择重训练周期" style="width: 200px">
                <el-option label="每天" value="daily" />
                <el-option label="每周" value="weekly" />
                <el-option label="每月" value="monthly" />
                <el-option label="每季度" value="quarterly" />
              </el-select>
            </el-form-item>
          </el-card>
        </el-form-item>

        <el-form-item label="启用" prop="isEnabled">
          <el-switch v-model="modelForm.isEnabled" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            @click="saveModel"
            :loading="saving"
          >
            保存
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 模型详情对话框 -->
    <el-dialog
      v-model="detailsDialogVisible"
      title="模型详情"
      width="900px"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="模型名称">{{ currentModelDetails.modelName }}</el-descriptions-item>
        <el-descriptions-item label="版本">v{{ currentModelDetails.version }}</el-descriptions-item>
        <el-descriptions-item label="模型类型">{{ getModelTypeName(currentModelDetails.modelType) }}</el-descriptions-item>
        <el-descriptions-item label="算法">{{ getAlgorithmName(currentModelDetails.algorithm) }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusTag(currentModelDetails.status)">
            {{ getStatusName(currentModelDetails.status) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="准确率">{{ currentModelDetails.accuracy }}%</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(currentModelDetails.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="最后训练时间">{{ formatDateTime(currentModelDetails.lastTrainTime) }}</el-descriptions-item>
        <el-descriptions-item label="描述" :span="2">{{ currentModelDetails.description }}</el-descriptions-item>
      </el-descriptions>

      <!-- 性能指标 -->
      <el-card style="margin-top: 20px">
        <template #header>
          <span>性能指标</span>
        </template>
        <el-row :gutter="20">
          <el-col :span="6">
            <div class="metric-item">
              <div class="metric-label">准确率</div>
              <div class="metric-value">{{ currentModelDetails.metrics?.accuracy || 0 }}%</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="metric-item">
              <div class="metric-label">精确率</div>
              <div class="metric-value">{{ currentModelDetails.metrics?.precision || 0 }}%</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="metric-item">
              <div class="metric-label">召回率</div>
              <div class="metric-value">{{ currentModelDetails.metrics?.recall || 0 }}%</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="metric-item">
              <div class="metric-label">F1分数</div>
              <div class="metric-value">{{ currentModelDetails.metrics?.f1Score || 0 }}%</div>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <!-- 训练历史 -->
      <el-card style="margin-top: 20px">
        <template #header>
          <span>训练历史</span>
        </template>
        <el-table :data="currentModelDetails.trainingHistory || []" max-height="300">
          <el-table-column prop="trainTime" label="训练时间" width="180">
            <template #default="scope">
              {{ formatDateTime(scope.row.trainTime) }}
            </template>
          </el-table-column>
          <el-table-column prop="dataSize" label="数据量" />
          <el-table-column prop="epochs" label="轮次" />
          <el-table-column prop="accuracy" label="准确率">
            <template #default="scope">
              {{ scope.row.accuracy }}%
            </template>
          </el-table-column>
          <el-table-column prop="loss" label="损失值" />
          <el-table-column prop="duration" label="训练时长" />
        </el-table>
      </el-card>

      <template #footer>
        <el-button @click="detailsDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search } from '@element-plus/icons-vue'

export default {
  name: 'IntelligentModelManagement',
  components: {
    Plus,
    Refresh,
    Search
  },
  data() {
    return {
      models: [],
      loading: false,
      filterModelType: null,
      filterStatus: null,
      searchKeyword: '',
      dialogVisible: false,
      detailsDialogVisible: false,
      dialogMode: 'create',
      saving: false,
      currentModelId: null,
      currentPage: 1,
      pageSize: 10,
      total: 0,
      hyperparametersString: '',
      currentModelDetails: {},
      modelForm: {
        modelName: '',
        version: '1.0.0',
        description: '',
        modelType: '',
        algorithm: '',
        hyperparameters: {},
        featureEngineering: [],
        dataSource: 'historical_alerts',
        trainDataSize: 10000,
        batchSize: 32,
        epochs: 100,
        evaluationMetrics: ['accuracy'],
        deploymentEnv: 'development',
        resourceLimit: 'medium',
        autoRetrain: false,
        retrainPeriod: 'weekly',
        isEnabled: true
      },
      formRules: {
        modelName: [
          { required: true, message: '请输入模型名称', trigger: 'blur' }
        ],
        version: [
          { required: true, message: '请输入模型版本', trigger: 'blur' }
        ],
        modelType: [
          { required: true, message: '请选择模型类型', trigger: 'change' }
        ],
        algorithm: [
          { required: true, message: '请选择算法', trigger: 'change' }
        ]
      },
      algorithmOptions: {
        clustering: [
          { value: 'kmeans', label: 'K-Means聚类' },
          { value: 'dbscan', label: 'DBSCAN密度聚类' },
          { value: 'hierarchical', label: '层次聚类' },
          { value: 'gaussian_mixture', label: '高斯混合模型' }
        ],
        classification: [
          { value: 'random_forest', label: '随机森林' },
          { value: 'xgboost', label: 'XGBoost' },
          { value: 'svm', label: '支持向量机' },
          { value: 'naive_bayes', label: '朴素贝叶斯' },
          { value: 'logistic_regression', label: '逻辑回归' }
        ],
        anomaly: [
          { value: 'isolation_forest', label: '孤立森林' },
          { value: 'lof', label: '局部异常因子' },
          { value: 'one_class_svm', label: '单类SVM' },
          { value: 'autoencoder', label: '自编码器' }
        ],
        timeseries: [
          { value: 'arima', label: 'ARIMA' },
          { value: 'lstm', label: 'LSTM' },
          { value: 'prophet', label: 'Prophet' },
          { value: 'gru', label: 'GRU' }
        ],
        deeplearning: [
          { value: 'cnn', label: '卷积神经网络' },
          { value: 'rnn', label: '循环神经网络' },
          { value: 'transformer', label: 'Transformer' },
          { value: 'gan', label: '生成对抗网络' }
        ]
      }
    }
  },
  computed: {
    currentAlgorithmOptions() {
      return this.modelForm.modelType ? this.algorithmOptions[this.modelForm.modelType] || [] : []
    }
  },
  mounted() {
    this.loadModels()
  },
  methods: {
    async loadModels() {
      this.loading = true
      try {
        // 模拟数据，实际应从后端获取
        this.models = [
          {
            id: '1',
            modelName: '告警聚类模型',
            modelType: 'clustering',
            algorithm: 'kmeans',
            version: '2.1.0',
            accuracy: 92,
            lastTrainTime: '2024-01-23 10:30:00',
            status: 'deployed',
            isEnabled: true,
            description: '基于K-Means的告警自动聚类模型'
          },
          {
            id: '2',
            modelName: '威胁分类器',
            modelType: 'classification',
            algorithm: 'xgboost',
            version: '1.5.0',
            accuracy: 88,
            lastTrainTime: '2024-01-22 15:45:00',
            status: 'ready',
            isEnabled: false,
            description: '使用XGBoost进行威胁等级分类'
          },
          {
            id: '3',
            modelName: '异常检测引擎',
            modelType: 'anomaly',
            algorithm: 'isolation_forest',
            version: '3.0.0',
            accuracy: 85,
            lastTrainTime: '2024-01-21 08:20:00',
            status: 'training',
            isEnabled: false,
            description: '基于孤立森林的异常行为检测'
          }
        ]
        this.total = this.models.length
      } catch (error) {
        ElMessage.error('加载模型列表失败')
      }
      this.loading = false
    },
    async searchModels() {
      this.currentPage = 1
      this.loadModels()
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.currentPage = 1
      this.loadModels()
    },
    handleCurrentChange(page) {
      this.currentPage = page
      this.loadModels()
    },
    showCreateDialog() {
      this.dialogMode = 'create'
      this.currentModelId = null
      this.resetForm()
      this.dialogVisible = true
    },
    editModel(model) {
      this.dialogMode = 'edit'
      this.currentModelId = model.id
      // 填充表单数据
      this.modelForm = { ...model }
      if (model.hyperparameters && typeof model.hyperparameters === 'object') {
        this.hyperparametersString = JSON.stringify(model.hyperparameters, null, 2)
      }
      this.dialogVisible = true
    },
    viewModelDetails(model) {
      this.currentModelDetails = {
        ...model,
        metrics: {
          accuracy: 92,
          precision: 89,
          recall: 87,
          f1Score: 88
        },
        trainingHistory: [
          {
            trainTime: '2024-01-23 10:30:00',
            dataSize: 50000,
            epochs: 100,
            accuracy: 92,
            loss: 0.08,
            duration: '2小时15分'
          },
          {
            trainTime: '2024-01-20 14:20:00',
            dataSize: 45000,
            epochs: 80,
            accuracy: 89,
            loss: 0.12,
            duration: '1小时50分'
          }
        ]
      }
      this.detailsDialogVisible = true
    },
    async trainModel(model) {
      try {
        await ElMessageBox.confirm(
          `确定要训练模型 "${model.modelName}" 吗？训练过程可能需要较长时间。`,
          '确认训练',
          {
            confirmButtonText: '开始训练',
            cancelButtonText: '取消',
            type: 'info'
          }
        )

        ElMessage.success('模型训练已启动，请在训练完成后查看结果')
        model.status = 'training'
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('启动训练失败')
        }
      }
    },
    async deployModel(model) {
      try {
        await ElMessageBox.confirm(
          `确定要部署模型 "${model.modelName}" 吗？`,
          '确认部署',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'info'
          }
        )

        ElMessage.success('模型部署成功')
        model.status = 'deployed'
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('部署失败')
        }
      }
    },
    resetForm() {
      this.modelForm = {
        modelName: '',
        version: '1.0.0',
        description: '',
        modelType: '',
        algorithm: '',
        hyperparameters: {},
        featureEngineering: [],
        dataSource: 'historical_alerts',
        trainDataSize: 10000,
        batchSize: 32,
        epochs: 100,
        evaluationMetrics: ['accuracy'],
        deploymentEnv: 'development',
        resourceLimit: 'medium',
        autoRetrain: false,
        retrainPeriod: 'weekly',
        isEnabled: true
      }
      this.hyperparametersString = ''
      if (this.$refs.modelFormRef) {
        this.$refs.modelFormRef.clearValidate()
      }
    },
    async saveModel() {
      if (!this.$refs.modelFormRef) return

      const valid = await this.$refs.modelFormRef.validate().catch(() => false)
      if (!valid) return

      // 解析超参数
      if (this.hyperparametersString) {
        this.parseHyperparameters()
      }

      this.saving = true
      try {
        // 这里应该调用后端API保存
        ElMessage.success(this.dialogMode === 'create' ? '模型创建成功' : '模型更新成功')
        this.dialogVisible = false
        this.loadModels()
      } catch (error) {
        ElMessage.error('保存失败')
      }
      this.saving = false
    },
    async toggleModel(model) {
      try {
        ElMessage.success(model.isEnabled ? '模型已启用' : '模型已禁用')
      } catch (error) {
        model.isEnabled = !model.isEnabled
        ElMessage.error('操作失败')
      }
    },
    async deleteModel(model) {
      try {
        await ElMessageBox.confirm(
          `确定要删除模型 "${model.modelName}" 吗？删除后不可恢复。`,
          '确认删除',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )

        ElMessage.success('模型删除成功')
        this.loadModels()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除失败')
        }
      }
    },
    onModelTypeChange() {
      this.modelForm.algorithm = ''
    },
    parseHyperparameters() {
      if (this.hyperparametersString && this.hyperparametersString.trim()) {
        try {
          this.modelForm.hyperparameters = JSON.parse(this.hyperparametersString)
        } catch (e) {
          ElMessage.error('超参数格式错误，请输入有效的JSON格式')
        }
      } else {
        this.modelForm.hyperparameters = {}
      }
    },
    getModelTypeName(type) {
      const typeMap = {
        clustering: '聚类模型',
        classification: '分类模型',
        anomaly: '异常检测',
        timeseries: '时序预测',
        deeplearning: '深度学习'
      }
      return typeMap[type] || type
    },
    getModelTypeTag(type) {
      const tags = {
        clustering: 'primary',
        classification: 'success',
        anomaly: 'warning',
        timeseries: 'info',
        deeplearning: 'danger'
      }
      return tags[type] || 'info'
    },
    getAlgorithmName(algorithm) {
      const algorithms = {
        kmeans: 'K-Means聚类',
        dbscan: 'DBSCAN密度聚类',
        hierarchical: '层次聚类',
        gaussian_mixture: '高斯混合模型',
        random_forest: '随机森林',
        xgboost: 'XGBoost',
        svm: '支持向量机',
        naive_bayes: '朴素贝叶斯',
        logistic_regression: '逻辑回归',
        isolation_forest: '孤立森林',
        lof: '局部异常因子',
        one_class_svm: '单类SVM',
        autoencoder: '自编码器',
        arima: 'ARIMA',
        lstm: 'LSTM',
        prophet: 'Prophet',
        gru: 'GRU',
        cnn: '卷积神经网络',
        rnn: '循环神经网络',
        transformer: 'Transformer',
        gan: '生成对抗网络'
      }
      return algorithms[algorithm] || algorithm
    },
    getStatusName(status) {
      const statusMap = {
        training: '训练中',
        ready: '就绪',
        deploying: '部署中',
        deployed: '已部署',
        disabled: '已停用'
      }
      return statusMap[status] || status
    },
    getStatusTag(status) {
      const tags = {
        training: 'warning',
        ready: 'success',
        deploying: 'primary',
        deployed: 'success',
        disabled: 'info'
      }
      return tags[status] || 'info'
    },
    getAccuracyColor(accuracy) {
      if (accuracy >= 90) return '#67c23a'
      if (accuracy >= 80) return '#409eff'
      if (accuracy >= 70) return '#e6a23c'
      return '#f56c6c'
    },
    formatDateTime(dateTime) {
      if (!dateTime) return '-'
      return dateTime
    }
  }
}
</script>

<style scoped>
.model-management {
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

.parameter-card {
  background: #f5f7fa;
  border: 1px solid #e4e7ed;
}

.parameter-card :deep(.el-card__header) {
  padding: 12px 20px;
  background: #fafafa;
  border-bottom: 1px solid #e4e7ed;
  font-weight: 500;
}

.parameter-card :deep(.el-card__body) {
  padding: 15px;
}

.metric-item {
  text-align: center;
  padding: 15px;
}

.metric-label {
  color: #909399;
  font-size: 14px;
  margin-bottom: 8px;
}

.metric-value {
  color: #303133;
  font-size: 24px;
  font-weight: 600;
}

.el-descriptions {
  margin-top: 20px;
}

:deep(.el-progress__text) {
  font-size: 12px !important;
}
</style>