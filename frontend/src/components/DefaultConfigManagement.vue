<template>
  <div class="default-config-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-icon><Setting /></el-icon>
          <span>恢复默认配置</span>
        </div>
      </template>

      <div class="config-content">
        <el-alert
          title="警告"
          type="warning"
          :closable="false"
          show-icon
          class="alert-box"
        >
          <p>恢复默认配置将执行以下操作：</p>
          <ul>
            <li>清除所有告警类型、子类型和字段配置</li>
            <li>清除所有告警存储映射配置</li>
            <li>清除所有标签配置</li>
            <li>清除所有过滤规则和标签规则</li>
            <li>清除所有规则执行日志</li>
          </ul>
          <p style="margin-top: 12px; font-weight: bold; color: #e6a23c;">
            ⚠️ 此操作不可逆，请谨慎操作！用户数据不会被删除。
          </p>
        </el-alert>

        <div class="button-container">
          <el-button
            type="danger"
            size="large"
            :loading="restoring"
            @click="confirmRestore"
          >
            <el-icon><RefreshRight /></el-icon>
            恢复默认配置
          </el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { Setting, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from '../utils/axios'

export default {
  name: 'DefaultConfigManagement',
  components: {
    Setting,
    RefreshRight
  },
  data() {
    return {
      restoring: false
    }
  },
  methods: {
    confirmRestore() {
      ElMessageBox.confirm(
        '确定要恢复默认配置吗？此操作将删除所有配置数据并恢复初始状态。',
        '确认恢复',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
          distinguishCancelAndClose: true,
          confirmButtonClass: 'el-button--danger'
        }
      )
        .then(() => {
          this.restoreDefaultConfig()
        })
        .catch(() => {
          ElMessage.info('已取消操作')
        })
    },
    async restoreDefaultConfig() {
      this.restoring = true
      try {
        const response = await axios.post('/api/restore-default-config')
        ElMessage.success(response.data.message || '默认配置恢复成功')
        setTimeout(() => {
          window.location.reload()
        }, 1500)
      } catch (error) {
        console.error('恢复默认配置失败:', error)
        ElMessage.error(
          error.response?.data?.message || '恢复默认配置失败，请查看日志'
        )
      } finally {
        this.restoring = false
      }
    }
  }
}
</script>

<style scoped>
.default-config-management {
  padding: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: bold;
}

.config-content {
  padding: 20px;
}

.alert-box {
  margin-bottom: 30px;
}

.alert-box ul {
  margin: 10px 0;
  padding-left: 20px;
}

.alert-box li {
  margin: 5px 0;
}

.button-container {
  display: flex;
  justify-content: center;
  margin-top: 30px;
}

.button-container .el-button {
  padding: 15px 40px;
  font-size: 16px;
}
</style>