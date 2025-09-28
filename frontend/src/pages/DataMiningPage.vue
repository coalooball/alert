<template>
  <div class="page-container">
    <el-card class="page-card">
      <template #header>
        <div class="card-header">
          <el-icon class="header-icon"><DataAnalysis /></el-icon>
          <span>告警数据挖掘</span>
        </div>
      </template>
      <div class="content">
        <el-menu
          :default-active="activeSubPage"
          mode="horizontal"
          @select="handleSubPageSelect"
          class="sub-page-menu"
        >
          <el-menu-item index="alertdata">告警数据</el-menu-item>
          <el-menu-item index="threatevent">威胁事件</el-menu-item>
        </el-menu>
        <div class="sub-page-content">
          <component :is="currentSubComponent" />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { DataAnalysis } from '@element-plus/icons-vue'
import AlertDataPage from './AlertDataPage.vue'
import ThreatEventPage from './ThreatEventPage.vue'

export default {
  name: 'DataMiningPage',
  components: {
    DataAnalysis,
    AlertDataPage,
    ThreatEventPage
  },
  data() {
    return {
      activeSubPage: 'alertdata'
    }
  },
  computed: {
    currentSubComponent() {
      const componentMap = {
        'alertdata': 'AlertDataPage',
        'threatevent': 'ThreatEventPage'
      }
      return componentMap[this.activeSubPage] || 'AlertDataPage'
    }
  },
  methods: {
    handleSubPageSelect(index) {
      this.activeSubPage = index
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

.content {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.sub-page-menu {
  border-bottom: 1px solid #e4e7ed;
  margin-bottom: 20px;
}

.sub-page-content {
  flex: 1;
  overflow: auto;
}
</style>