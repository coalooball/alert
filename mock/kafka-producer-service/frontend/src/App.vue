<template>
  <div class="app-container">
    <el-container>
      <!-- 头部 -->
      <el-header class="header">
        <div class="header-content">
          <div class="logo">
            <el-icon :size="32" color="#409EFF"><Platform /></el-icon>
            <span class="title">Kafka Producer Service</span>
          </div>
          <div class="status">
            <el-tag :type="serviceStatus === 'online' ? 'success' : 'danger'" size="large">
              {{ serviceStatus === 'online' ? '服务在线' : '服务离线' }}
            </el-tag>
          </div>
        </div>
      </el-header>

      <!-- 主体内容 -->
      <el-main class="main-content">
        <el-tabs v-model="activeTab" type="border-card" class="main-tabs">
          
          <!-- 从服务器文件发送 -->
          <el-tab-pane label="从服务器文件发送" name="fromFile">
            <div class="tab-content">
              <el-form :model="fileForm" label-width="140px" class="form-container">
                <el-form-item label="Kafka Broker">
                  <el-input 
                    v-model="fileForm.brokerAddress" 
                    placeholder="例如: localhost:9092"
                    clearable>
                    <template #prepend>
                      <el-icon><Connection /></el-icon>
                    </template>
                    <template #append>
                      <el-button 
                        @click="testKafkaConnection" 
                        :loading="testingConnection"
                        :icon="testConnectionResult?.success ? 'SuccessFilled' : 'Connection'">
                        {{ testingConnection ? '测试中...' : '测试连接' }}
                      </el-button>
                    </template>
                  </el-input>
                </el-form-item>

                <!-- 连接测试结果 -->
                <el-alert
                  v-if="testConnectionResult"
                  :title="testConnectionResult.message"
                  :type="testConnectionResult.success ? 'success' : 'error'"
                  :closable="true"
                  @close="testConnectionResult = null"
                  style="margin-bottom: 20px"
                  show-icon>
                  <template #default>
                    <div v-if="testConnectionResult.success">
                      <p>Broker: {{ testConnectionResult.brokerAddress }}</p>
                      <p>响应时间: {{ testConnectionResult.responseTime }}</p>
                    </div>
                    <div v-else>
                      <p>{{ testConnectionResult.error }}</p>
                    </div>
                  </template>
                </el-alert>

                <el-form-item label="Topic 名称">
                  <el-input 
                    v-model="fileForm.topic" 
                    placeholder="例如: network-attack-alerts"
                    clearable>
                    <template #prepend>
                      <el-icon><Document /></el-icon>
                    </template>
                  </el-input>
                </el-form-item>

                <el-form-item label="选择文件">
                  <el-select 
                    v-model="fileForm.jsonFilePath" 
                    placeholder="请选择JSON文件"
                    style="width: 100%"
                    @focus="loadFileList">
                    <el-option
                      v-for="file in fileList"
                      :key="file"
                      :label="file"
                      :value="file">
                      <el-icon><Document /></el-icon>
                      <span style="margin-left: 8px">{{ file }}</span>
                    </el-option>
                  </el-select>
                  <el-button 
                    @click="loadFileList" 
                    :icon="Refresh" 
                    circle 
                    style="margin-left: 8px"
                    size="small" />
                </el-form-item>

                <el-form-item label="限制数量">
                  <el-input-number 
                    v-model="fileForm.limit" 
                    :min="1" 
                    :max="100000"
                    placeholder="不填则全部发送"
                    controls-position="right"
                    style="width: 100%" />
                </el-form-item>

                <el-form-item label="延迟时间(毫秒)">
                  <el-input-number 
                    v-model="fileForm.delayMillis" 
                    :min="0" 
                    :max="10000"
                    controls-position="right"
                    style="width: 100%" />
                </el-form-item>

                <el-form-item label="高级选项">
                  <el-collapse>
                    <el-collapse-item title="点击展开高级配置" name="advanced">
                      <el-form-item label="消息Key字段">
                        <el-input v-model="fileForm.messageKeyField" />
                      </el-form-item>
                      <el-form-item label="启用压缩">
                        <el-switch v-model="fileForm.compressionEnabled" />
                      </el-form-item>
                      <el-form-item label="ACK模式">
                        <el-select v-model="fileForm.acks">
                          <el-option label="all (最安全)" value="all" />
                          <el-option label="1 (Leader确认)" value="1" />
                          <el-option label="0 (不等待)" value="0" />
                        </el-select>
                      </el-form-item>
                      <el-form-item label="重试次数">
                        <el-input-number v-model="fileForm.retries" :min="0" :max="10" />
                      </el-form-item>
                    </el-collapse-item>
                  </el-collapse>
                </el-form-item>

                <el-form-item>
                  <el-button 
                    type="primary" 
                    @click="sendFromFile" 
                    :loading="sending"
                    :icon="Upload"
                    size="large">
                    {{ sending ? '发送中...' : '开始发送' }}
                  </el-button>
                  <el-button @click="resetFileForm" :icon="RefreshLeft">重置</el-button>
                </el-form-item>
              </el-form>

              <!-- 结果显示 -->
              <el-card v-if="result" class="result-card" shadow="hover">
                <template #header>
                  <div class="card-header">
                    <span>发送结果</span>
                    <el-button text @click="result = null" :icon="Close">关闭</el-button>
                  </div>
                </template>
                <div v-if="result.success" class="result-success">
                  <el-result icon="success" title="发送成功">
                    <template #sub-title>
                      <el-descriptions :column="2" border>
                        <el-descriptions-item label="总消息数">
                          {{ result.statistics.total }}
                        </el-descriptions-item>
                        <el-descriptions-item label="成功数">
                          <el-tag type="success">{{ result.statistics.success }}</el-tag>
                        </el-descriptions-item>
                        <el-descriptions-item label="失败数">
                          <el-tag type="danger">{{ result.statistics.failed }}</el-tag>
                        </el-descriptions-item>
                        <el-descriptions-item label="成功率">
                          {{ result.statistics.successRate.toFixed(2) }}%
                        </el-descriptions-item>
                        <el-descriptions-item label="吞吐量">
                          {{ result.statistics.throughput.toFixed(2) }} msg/s
                        </el-descriptions-item>
                        <el-descriptions-item label="耗时">
                          {{ result.durationSeconds.toFixed(2) }} 秒
                        </el-descriptions-item>
                      </el-descriptions>
                    </template>
                  </el-result>
                </div>
                <div v-else class="result-error">
                  <el-result icon="error" title="发送失败" :sub-title="result.errorDetails" />
                </div>
              </el-card>
            </div>
          </el-tab-pane>

          <!-- 文件上传 -->
          <el-tab-pane label="文件上传" name="fromUpload">
            <div class="tab-content">
              <el-form label-width="140px" class="form-container">
                <el-form-item label="上传文件">
                  <el-upload
                    class="upload-demo"
                    drag
                    :auto-upload="false"
                    :on-change="handleFileChange"
                    :file-list="uploadFileList"
                    :limit="1"
                    accept=".json">
                    <el-icon class="el-icon--upload"><upload-filled /></el-icon>
                    <div class="el-upload__text">
                      拖拽文件到此处 或 <em>点击上传</em>
                    </div>
                    <template #tip>
                      <div class="el-upload__tip">
                        只支持 .json 文件，上传后将保存到服务器数据目录
                      </div>
                    </template>
                  </el-upload>
                </el-form-item>

                <el-form-item>
                  <el-button 
                    type="primary" 
                    @click="uploadFileOnly" 
                    :loading="uploading"
                    :icon="Upload"
                    :disabled="!uploadFile"
                    size="large">
                    {{ uploading ? '上传中...' : '上传' }}
                  </el-button>
                  <el-button @click="resetUploadForm" :icon="RefreshLeft">重置</el-button>
                </el-form-item>
              </el-form>

              <!-- 上传结果显示 -->
              <el-card v-if="uploadResult" class="result-card" shadow="hover">
                <template #header>
                  <div class="card-header">
                    <span>上传结果</span>
                    <el-button text @click="uploadResult = null" :icon="Close">关闭</el-button>
                  </div>
                </template>
                <div v-if="uploadResult.success" class="result-success">
                  <el-result icon="success" title="上传成功">
                    <template #sub-title>
                      <el-descriptions :column="2" border>
                        <el-descriptions-item label="文件名">
                          {{ uploadResult.fileName }}
                        </el-descriptions-item>
                        <el-descriptions-item label="保存路径">
                          {{ uploadResult.savedPath }}
                        </el-descriptions-item>
                        <el-descriptions-item label="文件大小">
                          {{ formatFileSize(uploadResult.fileSize) }}
                        </el-descriptions-item>
                        <el-descriptions-item label="状态">
                          <el-tag type="success">{{ uploadResult.message }}</el-tag>
                        </el-descriptions-item>
                      </el-descriptions>
                    </template>
                  </el-result>
                </div>
                <div v-else class="result-error">
                  <el-result icon="error" title="上传失败" :sub-title="uploadResult.errorDetails" />
                </div>
              </el-card>
            </div>
          </el-tab-pane>

        </el-tabs>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Upload, 
  Refresh, 
  RefreshLeft, 
  Close, 
  Connection, 
  Document,
  Platform,
  UploadFilled
} from '@element-plus/icons-vue'
import axios from 'axios'

// 状态
const serviceStatus = ref('offline')
const activeTab = ref('fromFile')
const sending = ref(false)
const uploading = ref(false)
const testingConnection = ref(false)
const result = ref(null)
const uploadResult = ref(null)
const testConnectionResult = ref(null)
const fileList = ref([])
const uploadFile = ref(null)
const uploadFileList = ref([])

// 从文件发送表单
const fileForm = ref({
  brokerAddress: 'localhost:9092',
  topic: 'network-attack-alerts',
  jsonFilePath: '',
  limit: null,
  delayMillis: 100,
  messageKeyField: 'alarm_id',
  compressionEnabled: true,
  acks: 'all',
  retries: 3
})

// 检查服务状态
const checkHealth = async () => {
  try {
    const response = await axios.get('/api/kafka/health')
    if (response.data.code === 200) {
      serviceStatus.value = 'online'
    }
  } catch (error) {
    serviceStatus.value = 'offline'
  }
}

// 加载文件列表
const loadFileList = async () => {
  try {
    const response = await axios.get('/api/kafka/list-files')
    if (response.data.code === 200) {
      fileList.value = response.data.data
    }
  } catch (error) {
    ElMessage.error('获取文件列表失败: ' + error.message)
  }
}

// 测试Kafka连接
const testKafkaConnection = async () => {
  if (!fileForm.value.brokerAddress) {
    ElMessage.warning('请输入Kafka Broker地址')
    return
  }

  testingConnection.value = true
  testConnectionResult.value = null

  try {
    const formData = new FormData()
    formData.append('brokerAddress', fileForm.value.brokerAddress)

    const response = await axios.post('/api/kafka/test-connection', formData)
    
    if (response.data.code === 200) {
      testConnectionResult.value = response.data.data
      if (testConnectionResult.value.success) {
        ElMessage.success('Kafka连接测试成功')
      } else {
        ElMessage.error('Kafka连接测试失败')
      }
    } else {
      ElMessage.error(response.data.message)
      testConnectionResult.value = {
        success: false,
        message: response.data.message,
        error: '连接失败'
      }
    }
  } catch (error) {
    ElMessage.error('测试连接失败: ' + error.message)
    testConnectionResult.value = {
      success: false,
      message: '测试连接失败',
      error: error.message
    }
  } finally {
    testingConnection.value = false
  }
}

// 从文件发送
const sendFromFile = async () => {
  if (!fileForm.value.jsonFilePath) {
    ElMessage.warning('请选择JSON文件')
    return
  }

  sending.value = true
  result.value = null

  try {
    const response = await axios.post('/api/kafka/send-from-file', fileForm.value)
    
    if (response.data.code === 200) {
      result.value = response.data.data
      ElMessage.success('发送完成')
    } else {
      ElMessage.error(response.data.message)
    }
  } catch (error) {
    ElMessage.error('发送失败: ' + error.message)
  } finally {
    sending.value = false
  }
}

// 处理文件变化
const handleFileChange = (file) => {
  uploadFile.value = file.raw
  uploadFileList.value = [file]
}

// 上传文件（不发送到Kafka）
const uploadFileOnly = async () => {
  if (!uploadFile.value) {
    ElMessage.warning('请先选择JSON文件')
    return
  }

  uploading.value = true
  uploadResult.value = null

  try {
    const formData = new FormData()
    formData.append('file', uploadFile.value)

    const response = await axios.post('/api/kafka/upload-file', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })

    if (response.data.code === 200) {
      uploadResult.value = response.data.data
      ElMessage.success('文件上传成功')
      // 刷新文件列表
      await loadFileList()
    } else {
      ElMessage.error(response.data.message)
    }
  } catch (error) {
    ElMessage.error('上传失败: ' + error.message)
  } finally {
    uploading.value = false
  }
}

// 重置表单
const resetFileForm = () => {
  fileForm.value = {
    brokerAddress: 'localhost:9092',
    topic: 'network-attack-alerts',
    jsonFilePath: '',
    limit: null,
    delayMillis: 100,
    messageKeyField: 'alarm_id',
    compressionEnabled: true,
    acks: 'all',
    retries: 3
  }
  result.value = null
}

const resetUploadForm = () => {
  uploadFile.value = null
  uploadFileList.value = []
  uploadResult.value = null
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

// 初始化
onMounted(() => {
  checkHealth()
  loadFileList()
  // 定期检查服务状态
  setInterval(checkHealth, 30000)
})
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
}

.el-container {
  background: white;
  border-radius: 10px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
  max-width: 1400px;
  margin: 0 auto;
  min-height: calc(100vh - 40px);
}

.header {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  border-radius: 10px 10px 0 0;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title {
  font-size: 24px;
  font-weight: bold;
}

.main-content {
  padding: 20px;
}

.main-tabs {
  border: none;
  box-shadow: none;
}

.tab-content {
  padding: 20px;
}

.form-container {
  max-width: 800px;
  margin: 0 auto;
}

.result-card {
  margin-top: 30px;
  max-width: 900px;
  margin-left: auto;
  margin-right: auto;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.upload-demo {
  width: 100%;
}

code {
  background: #f5f7fa;
  padding: 4px 8px;
  border-radius: 4px;
  margin-left: 8px;
  font-family: monospace;
}

:deep(.el-upload-dragger) {
  width: 100%;
}
</style>

