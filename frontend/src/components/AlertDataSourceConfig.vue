<template>
  <div class="alert-data-source-config">
    <el-tabs type="border-card" v-model="activeTab">
      <!-- Kafka配置 -->
      <el-tab-pane label="Kafka数据源配置" name="kafka">
        <KafkaDataSourceConfig />
      </el-tab-pane>

      <!-- REST HTTP接口配置 -->
      <el-tab-pane label="REST HTTP接口配置" name="rest-http">
        <RestHttpDataSourceConfig />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import { ref } from 'vue'
import KafkaDataSourceConfig from './KafkaDataSourceConfig.vue'
import RestHttpDataSourceConfig from './RestHttpDataSourceConfig.vue'

export default {
  name: 'AlertDataSourceConfig',
  components: {
    KafkaDataSourceConfig,
    RestHttpDataSourceConfig
  },
  setup() {
    const activeTab = ref('kafka')

    return {
      activeTab
    }
  }
}
</script>

<style scoped>
.alert-data-source-config {
  padding: 0;
}
</style>