<template>
  <div class="user-management">
    <div class="toolbar">
      <el-button type="primary" @click="showCreateDialog" :icon="Plus">
        创建用户
      </el-button>
      <el-button @click="refreshUsers" :icon="Refresh">
        刷新
      </el-button>
    </div>

    <el-table
      :data="users"
      style="width: 100%"
      v-loading="loading"
      border
    >
      <el-table-column prop="username" label="用户名" width="150" />
      <el-table-column prop="department" label="部门" width="200" />
      <el-table-column prop="role" label="角色" width="120">
        <template #default="scope">
          <el-tag :type="scope.row.role === 'ADMIN' ? 'danger' : 'primary'">
            {{ scope.row.role === 'ADMIN' ? '管理员' : '普通用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="scope">
          {{ formatDate(scope.row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="scope">
          <el-button
            size="small"
            type="danger"
            @click="deleteUser(scope.row)"
            :disabled="scope.row.role === 'ADMIN'"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 创建用户对话框 -->
    <el-dialog
      v-model="createDialogVisible"
      title="创建用户"
      width="500px"
    >
      <el-form
        :model="createForm"
        :rules="rules"
        ref="createFormRef"
        label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="createForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="部门" prop="department">
          <el-input v-model="createForm.department" placeholder="请输入部门" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="createForm.password"
            type="password"
            placeholder="请输入密码"
          />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="createForm.role" placeholder="请选择角色">
            <el-option label="普通用户" value="USER" />
            <el-option label="管理员" value="ADMIN" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="createDialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            @click="createUser"
            :loading="creating"
          >
            创建
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'

export default {
  name: 'UserManagement',
  components: {
    Plus,
    Refresh
  },
  data() {
    return {
      users: [],
      loading: false,
      createDialogVisible: false,
      creating: false,
      createForm: {
        username: '',
        department: '',
        password: '',
        role: 'USER'
      },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
        ],
        department: [
          { required: true, message: '请输入部门', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, message: '密码长度至少为 6 个字符', trigger: 'blur' }
        ],
        role: [
          { required: true, message: '请选择角色', trigger: 'change' }
        ]
      }
    }
  },
  mounted() {
    this.fetchUsers()
  },
  methods: {
    async fetchUsers() {
      this.loading = true
      try {
        const response = await fetch('/api/users', {
          credentials: 'include'
        })
        if (response.ok) {
          this.users = await response.json()
        } else {
          ElMessage.error('获取用户列表失败')
        }
      } catch (error) {
        ElMessage.error('网络错误')
      }
      this.loading = false
    },
    refreshUsers() {
      this.fetchUsers()
    },
    showCreateDialog() {
      this.createDialogVisible = true
      this.resetForm()
    },
    resetForm() {
      this.createForm = {
        username: '',
        department: '',
        password: '',
        role: 'USER'
      }
      if (this.$refs.createFormRef) {
        this.$refs.createFormRef.clearValidate()
      }
    },
    async createUser() {
      if (!this.$refs.createFormRef) return

      const valid = await this.$refs.createFormRef.validate().catch(() => false)
      if (!valid) return

      this.creating = true
      try {
        const response = await fetch('/api/users', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          credentials: 'include',
          body: JSON.stringify(this.createForm)
        })

        if (response.ok) {
          ElMessage.success('用户创建成功')
          this.createDialogVisible = false
          this.fetchUsers()
        } else {
          const error = await response.json()
          ElMessage.error(error.error || '创建用户失败')
        }
      } catch (error) {
        ElMessage.error('网络错误')
      }
      this.creating = false
    },
    async deleteUser(user) {
      try {
        await ElMessageBox.confirm(
          `确定要删除用户 "${user.username}" 吗？`,
          '确认删除',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )

        const response = await fetch(`/api/users/${user.id}`, {
          method: 'DELETE',
          credentials: 'include'
        })

        if (response.ok) {
          ElMessage.success('用户删除成功')
          this.fetchUsers()
        } else {
          const error = await response.json()
          ElMessage.error(error.error || '删除用户失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('删除用户失败')
        }
      }
    },
    formatDate(dateString) {
      if (!dateString) return '-'
      try {
        const date = new Date(dateString)
        if (isNaN(date.getTime())) return '-'
        return date.toLocaleString('zh-CN', {
          year: 'numeric',
          month: '2-digit',
          day: '2-digit',
          hour: '2-digit',
          minute: '2-digit',
          second: '2-digit'
        })
      } catch {
        return '-'
      }
    }
  }
}
</script>

<style scoped>
.user-management {
  padding: 20px;
}

.toolbar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>