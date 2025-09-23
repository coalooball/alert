<template>
  <div class="page-container">
    <el-card class="page-card">
      <template #header>
        <div class="card-header">
          <el-icon class="header-icon"><Setting /></el-icon>
          <span>自动化流程管理</span>
        </div>
      </template>
      <div class="automation-content">
        <el-tabs v-model="activeTab" tab-position="left" class="automation-tabs">
          <el-tab-pane label="告警数据汇聚融合" name="data-aggregation">
            <div class="content-panel">
              <h3>告警数据汇聚融合</h3>
              <div class="section-content">
                <el-row :gutter="20">
                  <el-col :span="12">
                    <div class="info-card">
                      <h4>数据源管理</h4>
                      <el-table :data="dataSources" stripe style="width: 100%">
                        <el-table-column prop="name" label="数据源名称" />
                        <el-table-column prop="type" label="类型" />
                        <el-table-column prop="status" label="状态">
                          <template #default="scope">
                            <el-tag :type="scope.row.status === '在线' ? 'success' : 'danger'">
                              {{ scope.row.status }}
                            </el-tag>
                          </template>
                        </el-table-column>
                      </el-table>
                    </div>
                  </el-col>
                  <el-col :span="12">
                    <div class="info-card">
                      <h4>融合规则配置</h4>
                      <el-form label-width="100px">
                        <el-form-item label="时间窗口">
                          <el-input-number v-model="timeWindow" :min="1" :max="60" /> 分钟
                        </el-form-item>
                        <el-form-item label="相似度阈值">
                          <el-slider v-model="similarityThreshold" :min="0" :max="100" show-input />
                        </el-form-item>
                        <el-form-item>
                          <el-button type="primary">保存配置</el-button>
                        </el-form-item>
                      </el-form>
                    </div>
                  </el-col>
                </el-row>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="告警数据智能处理" name="intelligent-processing">
            <div class="content-panel">
              <h3>告警数据智能处理</h3>
              <div class="section-content">
                <el-row :gutter="20">
                  <el-col :span="24">
                    <div class="info-card">
                      <h4>处理策略配置</h4>
                      <el-table :data="processingStrategies" stripe>
                        <el-table-column prop="name" label="策略名称" />
                        <el-table-column prop="type" label="处理类型" />
                        <el-table-column prop="priority" label="优先级">
                          <template #default="scope">
                            <el-tag :type="getPriorityType(scope.row.priority)">
                              {{ scope.row.priority }}
                            </el-tag>
                          </template>
                        </el-table-column>
                        <el-table-column label="操作" width="200">
                          <template #default>
                            <el-button size="small" type="primary">编辑</el-button>
                            <el-button size="small" type="danger">删除</el-button>
                          </template>
                        </el-table-column>
                      </el-table>
                      <el-button type="primary" style="margin-top: 20px">新增处理策略</el-button>
                    </div>
                  </el-col>
                </el-row>
                <el-row :gutter="20" style="margin-top: 20px">
                  <el-col :span="12">
                    <div class="info-card">
                      <h4>自动分类规则</h4>
                      <el-form label-width="120px">
                        <el-form-item label="启用AI分类">
                          <el-switch v-model="enableAIClassification" />
                        </el-form-item>
                        <el-form-item label="分类算法">
                          <el-select v-model="classificationAlgorithm" placeholder="选择算法">
                            <el-option label="深度学习" value="deep-learning" />
                            <el-option label="规则引擎" value="rule-engine" />
                            <el-option label="混合模式" value="hybrid" />
                          </el-select>
                        </el-form-item>
                      </el-form>
                    </div>
                  </el-col>
                  <el-col :span="12">
                    <div class="info-card">
                      <h4>处理性能监控</h4>
                      <div class="stat-item">
                        <span>处理速率：</span>
                        <strong>{{ processingRate }} 条/秒</strong>
                      </div>
                      <div class="stat-item">
                        <span>平均延迟：</span>
                        <strong>{{ avgLatency }} ms</strong>
                      </div>
                      <div class="stat-item">
                        <span>成功率：</span>
                        <el-progress :percentage="successRate" :color="getProgressColor" />
                      </div>
                    </div>
                  </el-col>
                </el-row>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="告警数据智能分析" name="intelligent-analysis">
            <div class="content-panel">
              <h3>告警数据智能分析</h3>
              <div class="section-content">
                <el-row :gutter="20">
                  <el-col :span="24">
                    <div class="info-card">
                      <h4>分析模型配置</h4>
                      <el-tabs type="border-card">
                        <el-tab-pane label="关联分析">
                          <el-form label-width="120px">
                            <el-form-item label="关联时间窗口">
                              <el-input-number v-model="correlationWindow" :min="1" :max="1440" /> 分钟
                            </el-form-item>
                            <el-form-item label="最小关联度">
                              <el-slider v-model="minCorrelation" :min="0" :max="1" :step="0.1" show-input />
                            </el-form-item>
                            <el-form-item>
                              <el-button type="primary">应用配置</el-button>
                            </el-form-item>
                          </el-form>
                        </el-tab-pane>
                        <el-tab-pane label="趋势分析">
                          <el-form label-width="120px">
                            <el-form-item label="分析周期">
                              <el-radio-group v-model="trendPeriod">
                                <el-radio label="daily">每日</el-radio>
                                <el-radio label="weekly">每周</el-radio>
                                <el-radio label="monthly">每月</el-radio>
                              </el-radio-group>
                            </el-form-item>
                            <el-form-item label="预测模型">
                              <el-select v-model="predictionModel" placeholder="选择模型">
                                <el-option label="ARIMA" value="arima" />
                                <el-option label="LSTM" value="lstm" />
                                <el-option label="Prophet" value="prophet" />
                              </el-select>
                            </el-form-item>
                            <el-form-item>
                              <el-button type="primary">开始分析</el-button>
                            </el-form-item>
                          </el-form>
                        </el-tab-pane>
                        <el-tab-pane label="异常检测">
                          <el-form label-width="120px">
                            <el-form-item label="检测算法">
                              <el-select v-model="anomalyAlgorithm" placeholder="选择算法">
                                <el-option label="孤立森林" value="isolation-forest" />
                                <el-option label="LOF" value="lof" />
                                <el-option label="自编码器" value="autoencoder" />
                              </el-select>
                            </el-form-item>
                            <el-form-item label="敏感度">
                              <el-slider v-model="anomalySensitivity" :min="1" :max="10" show-input />
                            </el-form-item>
                            <el-form-item>
                              <el-button type="primary">启动检测</el-button>
                            </el-form-item>
                          </el-form>
                        </el-tab-pane>
                      </el-tabs>
                    </div>
                  </el-col>
                </el-row>
                <el-row :gutter="20" style="margin-top: 20px">
                  <el-col :span="24">
                    <div class="info-card">
                      <h4>分析结果展示</h4>
                      <el-empty description="暂无分析结果" />
                    </div>
                  </el-col>
                </el-row>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="威胁事件审核" name="threat-review">
            <div class="content-panel">
              <h3>威胁事件审核</h3>
              <div class="section-content">
                <el-row :gutter="20">
                  <el-col :span="24">
                    <div class="info-card">
                      <h4>待审核事件列表</h4>
                      <div class="filter-bar">
                        <el-form :inline="true">
                          <el-form-item label="威胁等级">
                            <el-select v-model="threatLevelFilter" placeholder="全部">
                              <el-option label="全部" value="" />
                              <el-option label="严重" value="critical" />
                              <el-option label="高危" value="high" />
                              <el-option label="中危" value="medium" />
                              <el-option label="低危" value="low" />
                            </el-select>
                          </el-form-item>
                          <el-form-item label="状态">
                            <el-select v-model="reviewStatusFilter" placeholder="全部">
                              <el-option label="全部" value="" />
                              <el-option label="待审核" value="pending" />
                              <el-option label="审核中" value="reviewing" />
                              <el-option label="已确认" value="confirmed" />
                              <el-option label="已忽略" value="ignored" />
                            </el-select>
                          </el-form-item>
                          <el-form-item>
                            <el-button type="primary">搜索</el-button>
                            <el-button>重置</el-button>
                          </el-form-item>
                        </el-form>
                      </div>
                      <el-table :data="threatEvents" stripe>
                        <el-table-column type="selection" width="55" />
                        <el-table-column prop="id" label="事件ID" width="100" />
                        <el-table-column prop="name" label="事件名称" />
                        <el-table-column prop="type" label="威胁类型" />
                        <el-table-column prop="level" label="威胁等级" width="100">
                          <template #default="scope">
                            <el-tag :type="getThreatLevelType(scope.row.level)">
                              {{ scope.row.level }}
                            </el-tag>
                          </template>
                        </el-table-column>
                        <el-table-column prop="source" label="来源" />
                        <el-table-column prop="time" label="发生时间" width="160" />
                        <el-table-column prop="status" label="审核状态" width="100">
                          <template #default="scope">
                            <el-tag :type="getReviewStatusType(scope.row.status)">
                              {{ scope.row.status }}
                            </el-tag>
                          </template>
                        </el-table-column>
                        <el-table-column label="操作" width="200" fixed="right">
                          <template #default>
                            <el-button size="small" type="primary">审核</el-button>
                            <el-button size="small" type="success">确认</el-button>
                            <el-button size="small" type="warning">忽略</el-button>
                          </template>
                        </el-table-column>
                      </el-table>
                      <div class="batch-operations">
                        <el-button type="primary">批量确认</el-button>
                        <el-button type="warning">批量忽略</el-button>
                        <el-button>导出报告</el-button>
                      </div>
                    </div>
                  </el-col>
                </el-row>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-card>
  </div>
</template>

<script>
import { Setting, Document, Operation, List } from '@element-plus/icons-vue'

export default {
  name: 'AutomationPage',
  components: {
    Setting,
    Document,
    Operation,
    List
  },
  data() {
    return {
      activeTab: 'data-aggregation',
      // 告警数据汇聚融合
      dataSources: [
        { name: 'IDS系统', type: '入侵检测', status: '在线' },
        { name: 'WAF防火墙', type: 'Web防护', status: '在线' },
        { name: '终端防护', type: '端点安全', status: '离线' },
        { name: '日志采集器', type: '日志分析', status: '在线' }
      ],
      timeWindow: 5,
      similarityThreshold: 80,

      // 告警数据智能处理
      processingStrategies: [
        { name: '高危威胁优先', type: '优先级排序', priority: '高' },
        { name: '重复告警合并', type: '去重处理', priority: '中' },
        { name: '误报过滤', type: '智能过滤', priority: '高' },
        { name: '关联事件聚合', type: '事件聚合', priority: '中' }
      ],
      enableAIClassification: true,
      classificationAlgorithm: 'deep-learning',
      processingRate: 1250,
      avgLatency: 23,
      successRate: 98.5,

      // 告警数据智能分析
      correlationWindow: 30,
      minCorrelation: 0.7,
      trendPeriod: 'daily',
      predictionModel: 'lstm',
      anomalyAlgorithm: 'isolation-forest',
      anomalySensitivity: 7,

      // 威胁事件审核
      threatLevelFilter: '',
      reviewStatusFilter: '',
      threatEvents: [
        {
          id: 'THR001',
          name: 'SQL注入攻击',
          type: 'Web攻击',
          level: '严重',
          source: 'WAF防火墙',
          time: '2024-01-23 10:30:45',
          status: '待审核'
        },
        {
          id: 'THR002',
          name: '暴力破解尝试',
          type: '认证攻击',
          level: '高危',
          source: 'IDS系统',
          time: '2024-01-23 10:25:12',
          status: '审核中'
        },
        {
          id: 'THR003',
          name: '异常流量检测',
          type: 'DDoS攻击',
          level: '中危',
          source: '日志采集器',
          time: '2024-01-23 10:20:08',
          status: '已确认'
        },
        {
          id: 'THR004',
          name: '恶意文件上传',
          type: '文件攻击',
          level: '高危',
          source: '终端防护',
          time: '2024-01-23 10:15:33',
          status: '待审核'
        }
      ]
    }
  },
  methods: {
    getPriorityType(priority) {
      const typeMap = {
        '高': 'danger',
        '中': 'warning',
        '低': 'info'
      }
      return typeMap[priority] || 'info'
    },
    getThreatLevelType(level) {
      const typeMap = {
        '严重': 'danger',
        '高危': 'warning',
        '中危': 'primary',
        '低危': 'info'
      }
      return typeMap[level] || 'info'
    },
    getReviewStatusType(status) {
      const typeMap = {
        '待审核': 'warning',
        '审核中': 'primary',
        '已确认': 'success',
        '已忽略': 'info'
      }
      return typeMap[status] || 'info'
    },
    getProgressColor(percentage) {
      if (percentage < 60) return '#f56c6c'
      if (percentage < 80) return '#e6a23c'
      return '#67c23a'
    }
  }
}
</script>

<style scoped>
.page-container {
  height: 100%;
}

.page-card {
  height: 100%;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: bold;
  font-size: 16px;
}

.header-icon {
  color: #409eff;
}

.automation-content {
  padding: 20px 0;
}

.automation-tabs {
  min-height: 500px;
}

.automation-tabs .el-tabs__content {
  padding-left: 20px;
}

.content-panel {
  background: white;
  padding: 20px;
  border-radius: 6px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.content-panel h3 {
  margin-bottom: 20px;
  color: #303133;
  font-size: 18px;
  font-weight: 600;
}

.content-panel h4 {
  margin: 20px 0 10px 0;
  color: #606266;
  font-size: 16px;
  font-weight: 500;
}

.section-content {
  padding: 10px 0;
}

.info-card {
  background: #f9f9f9;
  padding: 20px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.info-card h4 {
  margin-top: 0;
  margin-bottom: 15px;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 2px solid #409eff;
  padding-bottom: 8px;
}

.stat-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px solid #e4e7ed;
}

.stat-item:last-child {
  border-bottom: none;
}

.stat-item span {
  color: #606266;
  font-size: 14px;
}

.stat-item strong {
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.filter-bar {
  margin-bottom: 20px;
  padding: 15px;
  background: #fafafa;
  border-radius: 4px;
}

.batch-operations {
  margin-top: 20px;
  padding-top: 15px;
  border-top: 1px solid #e4e7ed;
}

.el-tabs--border-card {
  box-shadow: none;
  border: 1px solid #e4e7ed;
}

.automation-tabs .el-tabs__item {
  min-width: 150px;
  text-align: left;
  padding: 12px 20px !important;
}

.automation-tabs .el-tabs__item.is-active {
  background: #409eff;
  color: white;
}
</style>