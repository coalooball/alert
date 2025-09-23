<template>
  <div id="app">
    <!-- 登录页面 -->
    <LoginPage v-if="!isLoggedIn" @login-success="handleLoginSuccess" />

    <!-- 主应用界面 -->
    <el-container v-else>
      <el-header class="header">
        <div class="header-left">
          <h1>告警数据智能挖掘系统</h1>
        </div>
        <div class="header-center">
          <el-tabs v-model="activeTab" @tab-click="handleTabClick">
            <el-tab-pane label="态势感知" name="situation"></el-tab-pane>
            <el-tab-pane label="告警数据挖掘" name="datamining"></el-tab-pane>
            <el-tab-pane label="自动化流程管理" name="automation"></el-tab-pane>
            <el-tab-pane label="系统配置" name="settings" v-if="isAdmin"></el-tab-pane>
          </el-tabs>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleUserAction">
            <span class="user-info">
              <el-icon><User /></el-icon>
              {{ currentUser?.username }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main-content">
        <component :is="currentComponent" />
      </el-main>
    </el-container>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { User, ArrowDown } from '@element-plus/icons-vue'
import LoginPage from './components/LoginPage.vue'
import SituationPage from './pages/SituationPage.vue'
import DataMiningPage from './pages/DataMiningPage.vue'
import AutomationPage from './pages/AutomationPage.vue'
import SettingsPage from './pages/SettingsPage.vue'

export default {
  name: 'App',
  components: {
    LoginPage,
    SituationPage,
    DataMiningPage,
    AutomationPage,
    SettingsPage,
    User,
    ArrowDown
  },
  data() {
    return {
      activeTab: 'situation',
      currentUser: null,
      isLoggedIn: false
    }
  },
  computed: {
    currentComponent() {
      const componentMap = {
        'situation': 'SituationPage',
        'datamining': 'DataMiningPage',
        'automation': 'AutomationPage',
        'settings': 'SettingsPage'
      }
      return componentMap[this.activeTab]
    },
    isAdmin() {
      return this.currentUser?.role === 'Admin'
    }
  },
  mounted() {
    this.checkAuthStatus()
  },
  methods: {
    async checkAuthStatus() {
      // 检查本地存储的用户信息
      const storedUser = localStorage.getItem('user')
      const token = localStorage.getItem('token')

      if (storedUser && token) {
        try {
          // 验证token是否有效
          const response = await fetch('/api/me', {
            credentials: 'include'
          })

          if (response.ok) {
            this.currentUser = JSON.parse(storedUser)
            this.isLoggedIn = true
          } else {
            this.clearAuthData()
          }
        } catch (error) {
          this.clearAuthData()
        }
      }
    },
    handleLoginSuccess(user) {
      this.currentUser = user
      this.isLoggedIn = true
      ElMessage.success(`欢迎，${user.username}！`)
    },
    async handleUserAction(command) {
      if (command === 'logout') {
        await this.logout()
      }
    },
    async logout() {
      try {
        await fetch('/api/logout', {
          method: 'POST',
          credentials: 'include'
        })
      } catch (error) {
        console.error('Logout request failed:', error)
      }

      this.clearAuthData()
      ElMessage.success('已退出登录')
    },
    clearAuthData() {
      this.currentUser = null
      this.isLoggedIn = false
      this.activeTab = 'situation'
      localStorage.removeItem('user')
      localStorage.removeItem('token')
    },
    handleTabClick(tab) {
      this.activeTab = tab.name
    }
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

#app {
  font-family: 'Microsoft YaHei', Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
  height: 100vh;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 20px;
  height: 60px;
}

.header-left h1 {
  color: #000000;
  font-size: 24px;
  font-weight: bold;
}

.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.user-info:hover {
  background-color: #f5f7fa;
}

.el-tabs {
  width: auto;
}

.el-tabs__header {
  margin: 0;
}

.main-content {
  padding: 20px;
  background: #f5f5f5;
  min-height: calc(100vh - 60px);
}
</style>