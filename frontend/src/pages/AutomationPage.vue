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
          <el-tab-pane label="告警过滤规则" name="filter-rules">
            <div class="content-panel">
              <h3>告警过滤规则</h3>
              <div class="section-content">
                <AlertFilterRules />
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="自动添加标签规则" name="tagging-rules">
            <div class="content-panel">
              <h3>自动添加标签规则</h3>
              <div class="section-content">
                <div class="rule-section">
                  <div class="rule-header">
                    <h4>自动添加标签规则管理</h4>
                    <el-button type="primary" @click="handleAddTaggingRule">
                      <el-icon><Plus /></el-icon>
                      新增标签规则
                    </el-button>
                  </div>

                  <!-- 搜索过滤 -->
                  <div class="filter-bar">
                    <el-form :inline="true">
                      <el-form-item label="规则名称">
                        <el-input v-model="taggingSearch.ruleName" placeholder="搜索规则名称" clearable />
                      </el-form-item>
                      <el-form-item label="告警类型">
                        <el-select v-model="taggingSearch.alertType" placeholder="选择告警类型" clearable>
                          <el-option label="全部类型" :value="null" />
                          <el-option label="网络攻击" :value="1" />
                          <el-option label="恶意样本" :value="2" />
                          <el-option label="主机行为" :value="3" />
                        </el-select>
                      </el-form-item>
                      <el-form-item label="状态">
                        <el-select v-model="taggingSearch.enabled" placeholder="选择状态" clearable>
                          <el-option label="启用" :value="true" />
                          <el-option label="禁用" :value="false" />
                        </el-select>
                      </el-form-item>
                      <el-form-item>
                        <el-button type="primary" @click="searchTaggingRules">搜索</el-button>
                        <el-button @click="resetTaggingSearch">重置</el-button>
                      </el-form-item>
                    </el-form>
                  </div>

                  <!-- 标签规则表格 -->
                  <el-table :data="taggingRules" stripe v-loading="taggingLoading">
                    <el-table-column prop="ruleName" label="规则名称" />
                    <el-table-column prop="ruleDescription" label="规则描述" show-overflow-tooltip />
                    <el-table-column prop="alertType" label="告警类型" width="120">
                      <template #default="scope">
                        {{ scope.row.alertType ? getAlertTypeName(scope.row.alertType) : '全部类型' }}
                      </template>
                    </el-table-column>
                    <el-table-column prop="alertSubtype" label="告警子类" width="120" />
                    <el-table-column prop="matchField" label="匹配字段" width="120" />
                    <el-table-column prop="matchType" label="匹配类型" width="100">
                      <template #default="scope">
                        <el-tag :type="scope.row.matchType === 'regex' ? 'warning' : 'primary'" size="small">
                          {{ scope.row.matchType === 'regex' ? '正则匹配' : '精确匹配' }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="matchValue" label="匹配值" width="150" show-overflow-tooltip />
                    <el-table-column prop="tags" label="标签" width="180">
                      <template #default="scope">
                        <el-tag
                          v-for="tag in scope.row.tags.slice(0, 3)"
                          :key="tag"
                          size="small"
                          style="margin-right: 5px; margin-bottom: 5px"
                        >
                          {{ tag }}
                        </el-tag>
                        <el-tag v-if="scope.row.tags.length > 3" size="small" type="info">
                          +{{ scope.row.tags.length - 3 }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="priority" label="优先级" width="100">
                      <template #default="scope">
                        <el-tag :type="getPriorityType(scope.row.priority)">
                          {{ scope.row.priority }}
                        </el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="isEnabled" label="状态" width="80">
                      <template #default="scope">
                        <el-switch
                          v-model="scope.row.isEnabled"
                          @change="toggleTaggingRule(scope.row)"
                        />
                      </template>
                    </el-table-column>
                    <el-table-column label="操作" width="200" fixed="right">
                      <template #default="scope">
                        <el-button size="small" type="primary" @click="handleEditTaggingRule(scope.row)">
                          编辑
                        </el-button>
                        <el-button size="small" type="danger" @click="handleDeleteTaggingRule(scope.row)">
                          删除
                        </el-button>
                      </template>
                    </el-table-column>
                  </el-table>

                  <!-- 分页 -->
                  <el-pagination
                    v-model:current-page="taggingPagination.currentPage"
                    v-model:page-size="taggingPagination.pageSize"
                    :page-sizes="[10, 20, 50, 100]"
                    :total="taggingPagination.total"
                    layout="total, sizes, prev, pager, next, jumper"
                    @size-change="handleTaggingPageSizeChange"
                    @current-change="handleTaggingPageChange"
                    style="margin-top: 20px; justify-content: center"
                  />
                </div>
              </div>
            </div>
          </el-tab-pane>


          <el-tab-pane label="标签管理" name="tag-management">
            <div class="content-panel">
              <h3>标签管理</h3>
              <div class="section-content">
                <el-row :gutter="20">
                  <el-col :span="24">
                    <div class="info-card">
                      <div class="rule-header">
                        <h4>系统标签库</h4>
                        <el-button type="primary" @click="handleAddTag">
                          <el-icon><Plus /></el-icon>
                          新增标签
                        </el-button>
                      </div>

                      <!-- 搜索过滤 -->
                      <div class="filter-bar">
                        <el-form :inline="true">
                          <el-form-item label="标签名称">
                            <el-input v-model="tagSearch.tagName" placeholder="搜索标签名称" clearable />
                          </el-form-item>
                          <el-form-item label="标签类型">
                            <el-select v-model="tagSearch.tagType" placeholder="选择标签类型" clearable>
                              <el-option label="威胁级别" value="threat-level" />
                              <el-option label="攻击类型" value="attack-type" />
                              <el-option label="来源系统" value="source-system" />
                              <el-option label="处理状态" value="status" />
                              <el-option label="业务分类" value="business" />
                              <el-option label="自定义" value="custom" />
                            </el-select>
                          </el-form-item>
                          <el-form-item label="状态">
                            <el-select v-model="tagSearch.enabled" placeholder="选择状态" clearable>
                              <el-option label="启用" :value="true" />
                              <el-option label="禁用" :value="false" />
                            </el-select>
                          </el-form-item>
                          <el-form-item>
                            <el-button type="primary" @click="searchTags">搜索</el-button>
                            <el-button @click="resetTagSearch">重置</el-button>
                          </el-form-item>
                        </el-form>
                      </div>

                      <!-- 标签管理表格 -->
                      <el-table :data="tags" stripe v-loading="tagLoading">
                        <el-table-column prop="tagName" label="标签名称" width="150">
                          <template #default="scope">
                            <span>{{ scope.row.tagName }}</span>
                          </template>
                        </el-table-column>
                        <el-table-column prop="description" label="标签描述" show-overflow-tooltip />
                        <el-table-column prop="tagType" label="标签类型" width="120">
                          <template #default="scope">
                            <el-tag type="info" size="small">
                              {{ getTagTypeName(scope.row.tagType) }}
                            </el-tag>
                          </template>
                        </el-table-column>
                        <el-table-column prop="color" label="标签颜色" width="100">
                          <template #default="scope">
                            <div class="color-display" :style="{ backgroundColor: scope.row.color }"></div>
                          </template>
                        </el-table-column>
                        <el-table-column prop="createdBy" label="创建用户" width="120" />
                        <el-table-column prop="createTime" label="创建时间" width="160" />
                        <el-table-column prop="isEnabled" label="状态" width="80">
                          <template #default="scope">
                            <el-switch
                              v-model="scope.row.isEnabled"
                              @change="toggleTag(scope.row)"
                            />
                          </template>
                        </el-table-column>
                        <el-table-column label="操作" width="200" fixed="right">
                          <template #default="scope">
                            <el-button size="small" type="primary" @click="handleEditTag(scope.row)">
                              编辑
                            </el-button>
                            <el-button size="small" type="danger" @click="handleDeleteTag(scope.row)">
                              删除
                            </el-button>
                          </template>
                        </el-table-column>
                      </el-table>

                      <!-- 分页 -->
                      <el-pagination
                        v-model:current-page="tagPagination.currentPage"
                        v-model:page-size="tagPagination.pageSize"
                        :page-sizes="[10, 20, 50, 100]"
                        :total="tagPagination.total"
                        layout="total, sizes, prev, pager, next, jumper"
                        @size-change="handleTagPageSizeChange"
                        @current-change="handleTagPageChange"
                        style="margin-top: 20px; justify-content: center"
                      />
                    </div>
                  </el-col>
                </el-row>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-card>

    <!-- 新增/编辑标签对话框 -->
    <el-dialog
      v-model="tagDialogVisible"
      :title="tagDialogMode === 'add' ? '新增标签' : '编辑标签'"
      width="600px"
      :before-close="handleTagDialogClose">
      <el-form
        ref="tagFormRef"
        :model="tagForm"
        :rules="tagFormRules"
        label-width="100px"
        @submit.prevent>
        <el-form-item label="标签名称" prop="tagName">
          <el-input
            v-model="tagForm.tagName"
            placeholder="请输入标签名称"
            maxlength="100"
            show-word-limit />
        </el-form-item>

        <el-form-item label="标签描述" prop="description">
          <el-input
            v-model="tagForm.description"
            type="textarea"
            placeholder="请输入标签描述"
            :rows="3"
            maxlength="500"
            show-word-limit />
        </el-form-item>

        <el-form-item label="标签类型" prop="tagType">
          <el-select v-model="tagForm.tagType" placeholder="请选择标签类型" style="width: 100%">
            <el-option label="威胁级别" value="threat-level" />
            <el-option label="攻击类型" value="attack-type" />
            <el-option label="来源系统" value="source-system" />
            <el-option label="处理状态" value="status" />
            <el-option label="业务分类" value="business" />
            <el-option label="自定义" value="custom" />
          </el-select>
        </el-form-item>

        <el-form-item label="标签颜色" prop="color">
          <div class="color-picker-container">
            <el-color-picker
              v-model="tagForm.color"
              show-alpha
              :predefine="predefinedColors" />
            <el-input
              v-model="tagForm.color"
              placeholder="或手动输入颜色值"
              style="width: 150px; margin-left: 15px" />
          </div>
        </el-form-item>

        <el-form-item label="启用状态">
          <el-switch
            v-model="tagForm.isEnabled"
            active-text="启用"
            inactive-text="禁用" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleTagDialogClose">取消</el-button>
          <el-button type="primary" @click="submitTagForm" :loading="tagSubmitting">
            {{ tagDialogMode === 'add' ? '创建' : '更新' }}
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 新增/编辑标签规则对话框 -->
    <el-dialog
      v-model="taggingRuleDialogVisible"
      :title="taggingRuleDialogMode === 'add' ? '新增标签规则' : '编辑标签规则'"
      width="800px"
      :before-close="handleTaggingRuleDialogClose">
      <el-form
        ref="taggingRuleFormRef"
        :model="taggingRuleForm"
        :rules="taggingRuleFormRules"
        label-width="120px"
        @submit.prevent>
        <el-form-item label="规则名称" prop="ruleName">
          <el-input
            v-model="taggingRuleForm.ruleName"
            placeholder="请输入规则名称"
            maxlength="200"
            show-word-limit />
        </el-form-item>

        <el-form-item label="规则描述" prop="ruleDescription">
          <el-input
            v-model="taggingRuleForm.ruleDescription"
            type="textarea"
            placeholder="请输入规则描述"
            :rows="3"
            maxlength="500"
            show-word-limit />
        </el-form-item>

        <el-form-item label="告警类型" prop="alertType">
          <el-select
            v-model="taggingRuleForm.alertType"
            placeholder="请选择告警类型"
            style="width: 100%"
            @change="onTaggingAlertTypeChange">
            <el-option
              v-for="type in alertTypes"
              :key="type.id"
              :label="type.typeLabel"
              :value="type.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="告警子类型" prop="alertSubtype">
          <el-select
            v-model="taggingRuleForm.alertSubtype"
            placeholder="请选择告警子类型"
            style="width: 100%"
            clearable>
            <el-option
              v-for="subtype in currentTaggingSubtypeOptions"
              :key="subtype.value"
              :label="subtype.label"
              :value="subtype.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="匹配字段" prop="matchField">
          <el-select
            v-model="taggingRuleForm.matchField"
            placeholder="请选择匹配字段"
            style="width: 100%">
            <el-option
              v-for="field in currentTaggingFieldOptions"
              :key="field.value"
              :label="field.label"
              :value="field.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="匹配类型" prop="matchType">
          <el-radio-group v-model="taggingRuleForm.matchType">
            <el-radio label="exact">精确匹配</el-radio>
            <el-radio label="regex">正则匹配</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="匹配值" prop="matchValue">
          <el-input
            v-model="taggingRuleForm.matchValue"
            type="textarea"
            :rows="2"
            placeholder="请输入匹配值"
            maxlength="500"
            show-word-limit />
        </el-form-item>

        <el-form-item label="添加标签" prop="tags">
          <div class="tags-container">
            <el-select
              v-model="taggingRuleForm.tags"
              multiple
              filterable
              allow-create
              default-first-option
              placeholder="请选择或输入标签"
              style="width: 100%">
              <el-option
                v-for="tag in availableTags"
                :key="tag.id"
                :label="tag.tagName"
                :value="tag.tagName">
                <span style="float: left">{{ tag.tagName }}</span>
                <span style="float: right; color: #8492a6; font-size: 13px">{{ getTagTypeName(tag.tagType) }}</span>
              </el-option>
            </el-select>
          </div>
        </el-form-item>

        <el-form-item label="优先级" prop="priority">
          <el-input-number
            v-model="taggingRuleForm.priority"
            :min="0"
            :max="100"
            placeholder="规则优先级（数字越大优先级越高）" />
        </el-form-item>

        <el-form-item label="启用状态">
          <el-switch
            v-model="taggingRuleForm.isEnabled"
            active-text="启用"
            inactive-text="禁用" />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleTaggingRuleDialogClose">取消</el-button>
          <el-button type="primary" @click="submitTaggingRuleForm" :loading="taggingRuleSubmitting">
            {{ taggingRuleDialogMode === 'add' ? '创建' : '更新' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { Setting, Document, Operation, List, Plus, Delete } from '@element-plus/icons-vue'
import AlertFilterRules from '../components/AlertFilterRules.vue'

export default {
  name: 'AutomationPage',
  components: {
    Setting,
    Document,
    Operation,
    List,
    Plus,
    Delete,
    AlertFilterRules
  },
  data() {
    return {
      activeTab: 'filter-rules',
      dataSources: [
        { name: 'IDS系统', type: '入侵检测', status: '在线' },
        { name: 'WAF防火墙', type: 'Web防护', status: '在线' },
        { name: '终端防护', type: '端点安全', status: '离线' },
        { name: '日志采集器', type: '日志分析', status: '在线' }
      ],
      timeWindow: 5,
      similarityThreshold: 80,

      // 标签规则相关
      taggingRules: [],
      taggingLoading: false,
      taggingSearch: {
        ruleName: '',
        alertType: null,
        enabled: null
      },
      taggingPagination: {
        currentPage: 1,
        pageSize: 10,
        total: 0
      },

      // 标签规则对话框相关
      taggingRuleDialogVisible: false,
      taggingRuleDialogMode: 'add', // 'add' | 'edit'
      taggingRuleSubmitting: false,
      taggingRuleForm: {
        ruleName: '',
        ruleDescription: '',
        alertType: null,
        alertSubtype: '',
        matchField: '',
        matchType: 'exact',
        matchValue: '',
        tags: [],
        priority: 0,
        isEnabled: true
      },
      taggingRuleFormRules: {
        ruleName: [
          { required: true, message: '请输入规则名称', trigger: 'blur' },
          { min: 1, max: 200, message: '规则名称长度应在 1 到 200 个字符', trigger: 'blur' }
        ],
        alertType: [
          { required: true, message: '请选择告警类型', trigger: 'change' }
        ],
        alertSubtype: [
          { required: true, message: '请选择告警子类型', trigger: 'change' }
        ],
        matchField: [
          { required: true, message: '请选择匹配字段', trigger: 'change' }
        ],
        matchType: [
          { required: true, message: '请选择匹配类型', trigger: 'change' }
        ],
        matchValue: [
          { required: true, message: '请输入匹配值', trigger: 'blur' }
        ],
        tags: [
          { required: true, message: '请至少选择一个标签', trigger: 'change' },
          { type: 'array', min: 1, message: '请至少选择一个标签', trigger: 'change' }
        ]
      },
      currentEditingTaggingRule: null,
      availableTags: [],
      // 告警元数据
      alertTypes: [],
      subtypeOptions: {},
      fieldOptions: {},

      // 标签管理相关
      tags: [],
      tagLoading: false,
      tagSearch: {
        tagName: '',
        tagType: null,
        enabled: null
      },
      tagPagination: {
        currentPage: 1,
        pageSize: 10,
        total: 0
      },

      // 标签对话框相关
      tagDialogVisible: false,
      tagDialogMode: 'add', // 'add' | 'edit'
      tagSubmitting: false,
      tagForm: {
        tagName: '',
        description: '',
        tagType: '',
        color: '#409eff',
        isEnabled: true
      },
      tagFormRules: {
        tagName: [
          { required: true, message: '请输入标签名称', trigger: 'blur' },
          { min: 1, max: 100, message: '标签名称长度应在 1 到 100 个字符', trigger: 'blur' }
        ],
        tagType: [
          { required: true, message: '请选择标签类型', trigger: 'change' }
        ],
        color: [
          { required: true, message: '请输入标签颜色', trigger: 'blur' },
          {
            validator: (rule, value, callback) => {
              if (!value) {
                callback(new Error('请输入标签颜色'))
                return
              }

              // 检查是否是有效的十六进制格式
              if (value.match(/^#[0-9a-fA-F]{3,8}$/)) {
                callback()
                return
              }

              // 检查是否是有效的 RGB/RGBA 格式
              if (value.match(/^rgb\w*\(\s*\d+\s*,\s*\d+\s*,\s*\d+(\s*,\s*[\d.]+)?\s*\)$/)) {
                callback()
                return
              }

              callback(new Error('请输入有效的颜色值，如 #FF0000 或 rgb(255, 0, 0)'))
            },
            trigger: 'blur'
          }
        ]
      },
      predefinedColors: [
        '#f56c6c', '#e6a23c', '#409eff', '#67c23a', '#909399',
        '#c0c4cc', '#ff4d4f', '#ff7a45', '#ffa940', '#ffec3d',
        '#bae637', '#73d13d', '#40a9ff', '#36cfc9', '#b37feb'
      ],
      currentEditingTag: null
    }
  },
  computed: {
    currentTaggingSubtypeOptions() {
      return this.taggingRuleForm.alertType ? this.subtypeOptions[this.taggingRuleForm.alertType] || [] : []
    },
    currentTaggingFieldOptions() {
      return this.taggingRuleForm.alertType ? this.fieldOptions[this.taggingRuleForm.alertType] || [] : []
    }
  },
  methods: {
    // 将颜色值转换为十六进制格式
    convertColorToHex(color) {
      if (!color) return '#000000'

      // 如果已经是十六进制格式，直接返回
      if (color.startsWith('#')) {
        return color
      }

      // 处理 rgb() 和 rgba() 格式
      if (color.startsWith('rgb')) {
        const match = color.match(/rgb\w*\(([^)]+)\)/)
        if (match) {
          const values = match[1].split(',').map(v => {
            const val = v.trim()
            // 处理透明度值（忽略第4个参数的小数）
            return parseInt(val) || 0
          })
          const r = Math.max(0, Math.min(255, values[0] || 0))
          const g = Math.max(0, Math.min(255, values[1] || 0))
          const b = Math.max(0, Math.min(255, values[2] || 0))

          // 转换为十六进制
          const toHex = (n) => {
            const hex = n.toString(16)
            return hex.length === 1 ? '0' + hex : hex
          }

          return `#${toHex(r)}${toHex(g)}${toHex(b)}`
        }
      }

      // 如果都不是，返回默认黑色
      return '#000000'
    },

    // 根据背景颜色计算合适的字体颜色
    getContrastColor(color) {
      // 先转换为十六进制格式
      const hexColor = this.convertColorToHex(color)

      if (!hexColor || !hexColor.startsWith('#')) {
        return '#000000' // 默认黑色
      }

      // 移除 # 号并处理不同长度的颜色值
      let hex = hexColor.slice(1)

      // 处理3位颜色值 (#fff -> #ffffff)
      if (hex.length === 3) {
        hex = hex.split('').map(char => char + char).join('')
      }

      // 处理8位颜色值 (#rrggbbaa -> #rrggbb)
      if (hex.length === 8) {
        hex = hex.slice(0, 6)
      }

      // 确保是6位有效颜色值
      if (hex.length !== 6) {
        return '#000000'
      }

      // 计算亮度
      const r = parseInt(hex.slice(0, 2), 16)
      const g = parseInt(hex.slice(2, 4), 16)
      const b = parseInt(hex.slice(4, 6), 16)

      // 使用相对亮度公式 (ITU-R BT.709)
      const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255

      // 亮度大于0.5使用黑色字体，否则使用白色字体
      return luminance > 0.5 ? '#000000' : '#ffffff'
    },

    getPriorityType(priority) {
      const typeMap = {
        '高': 'danger',
        '中': 'warning',
        '低': 'info'
      }
      return typeMap[priority] || 'info'
    },
    getProgressColor(percentage) {
      if (percentage < 60) return '#f56c6c'
      if (percentage < 80) return '#e6a23c'
      return '#67c23a'
    },

    // 通用方法
    getAlertTypeName(type) {
      const typeMap = {
        1: '网络攻击',
        2: '恶意样本',
        3: '主机行为'
      }
      return typeMap[type] || '未知类型'
    },


    // 标签规则相关方法
    async loadTaggingRules() {
      this.taggingLoading = true
      try {
        const params = new URLSearchParams({
          page: this.taggingPagination.currentPage - 1,
          size: this.taggingPagination.pageSize
        })

        if (this.taggingSearch.ruleName) {
          params.append('ruleName', this.taggingSearch.ruleName)
        }
        if (this.taggingSearch.alertType !== null) {
          params.append('alertType', this.taggingSearch.alertType)
        }
        if (this.taggingSearch.enabled !== null) {
          params.append('isEnabled', this.taggingSearch.enabled)
        }

        const response = await fetch(`/api/alert/tagging-rules?${params.toString()}`, {
          credentials: 'include'
        })

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`)
        }

        const data = await response.json()
        this.taggingRules = data.records || []
        this.taggingPagination.total = data.total || 0
      } catch (error) {
        console.error('Failed to load tagging rules:', error)
        this.$message.error('加载标签规则失败: ' + error.message)
        // 使用模拟数据作为fallback
        this.taggingRules = [
          {
            id: '1',
            ruleName: '高危IP标签',
            ruleDescription: '为来自高危IP的告警添加威胁标签',
            alertType: 1,
            alertSubtype: 'network_attack',
            matchConditions: [
              { field: 'src_ip', operator: 'in', value: 'threat_ip_list' }
            ],
            tags: ['高危IP', '网络威胁', '需关注'],
            priority: 5,
            isEnabled: true
          },
          {
            id: '2',
            ruleName: '恶意域名标签',
            ruleDescription: '为访问恶意域名的告警添加标签',
            alertType: null,
            alertSubtype: null,
            matchConditions: [
              { field: 'domain', operator: 'regex', value: '.*\\.malware\\..*' }
            ],
            tags: ['恶意域名', '可疑行为'],
            priority: 3,
            isEnabled: true
          }
        ]
        this.taggingPagination.total = this.taggingRules.length
      } finally {
        this.taggingLoading = false
      }
    },

    searchTaggingRules() {
      this.taggingPagination.currentPage = 1
      this.loadTaggingRules()
    },

    resetTaggingSearch() {
      this.taggingSearch = {
        ruleName: '',
        alertType: null,
        enabled: null
      }
      this.searchTaggingRules()
    },

    handleTaggingPageChange(page) {
      this.taggingPagination.currentPage = page
      this.loadTaggingRules()
    },

    handleTaggingPageSizeChange(size) {
      this.taggingPagination.pageSize = size
      this.taggingPagination.currentPage = 1
      this.loadTaggingRules()
    },

    handleAddTaggingRule() {
      this.taggingRuleDialogMode = 'add'
      this.resetTaggingRuleForm()
      this.loadAvailableTags()
      this.loadAlertMetadata()
      this.taggingRuleDialogVisible = true
    },

    handleEditTaggingRule(rule) {
      this.taggingRuleDialogMode = 'edit'
      this.currentEditingTaggingRule = rule
      this.taggingRuleForm = {
        ruleName: rule.ruleName,
        ruleDescription: rule.ruleDescription || '',
        alertType: rule.alertType,
        alertSubtype: rule.alertSubtype,
        matchField: rule.matchField,
        matchType: rule.matchType || 'exact',
        matchValue: rule.matchValue || '',
        tags: rule.tags ? [...rule.tags] : [],
        priority: rule.priority || 0,
        isEnabled: rule.isEnabled !== undefined ? rule.isEnabled : true
      }
      this.loadAvailableTags()
      this.loadAlertMetadata()
      this.taggingRuleDialogVisible = true
    },

    handleDeleteTaggingRule(rule) {
      this.$confirm(`确定要删除标签规则 "${rule.ruleName}" 吗？`, '确认删除', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const response = await fetch(`/api/alert/tagging-rules/${rule.id}`, {
            method: 'DELETE',
            credentials: 'include'
          })

          if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`)
          }

          this.$message.success('删除成功')
          this.loadTaggingRules()
        } catch (error) {
          console.error('Failed to delete tagging rule:', error)
          this.$message.error('删除失败: ' + error.message)
        }
      }).catch(() => {
        this.$message.info('已取消删除')
      })
    },

    async toggleTaggingRule(rule) {
      const originalState = rule.isEnabled
      try {
        const response = await fetch(`/api/alert/tagging-rules/${rule.id}/toggle`, {
          method: 'PATCH',
          credentials: 'include'
        })

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`)
        }

        const result = await response.json()
        if (result.success) {
          this.$message.success(`已${rule.isEnabled ? '启用' : '禁用'}规则`)
        } else {
          throw new Error(result.message || 'Toggle failed')
        }
      } catch (error) {
        console.error('Failed to toggle tagging rule:', error)
        this.$message.error('更新状态失败: ' + error.message)
        // 回退状态
        rule.isEnabled = originalState
      }
    },

    // 标签管理相关方法
    getTagTypeName(type) {
      const typeMap = {
        'threat-level': '威胁级别',
        'attack-type': '攻击类型',
        'source-system': '来源系统',
        'status': '处理状态',
        'business': '业务分类',
        'custom': '自定义'
      }
      return typeMap[type] || '未知类型'
    },

    async loadTags() {
      this.tagLoading = true
      try {
        const params = new URLSearchParams({
          page: this.tagPagination.currentPage - 1,
          size: this.tagPagination.pageSize
        })

        if (this.tagSearch.tagName) {
          params.append('tagName', this.tagSearch.tagName)
        }
        if (this.tagSearch.tagType) {
          params.append('tagType', this.tagSearch.tagType)
        }
        if (this.tagSearch.enabled !== null) {
          params.append('isEnabled', this.tagSearch.enabled)
        }

        const response = await fetch(`/api/tags?${params.toString()}`, {
          credentials: 'include'
        })

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`)
        }

        const data = await response.json()
        this.tags = data.records || []
        this.tagPagination.total = data.total || 0
      } catch (error) {
        console.error('Failed to load tags:', error)
        this.$message.error('加载标签失败: ' + error.message)
      } finally {
        this.tagLoading = false
      }
    },

    searchTags() {
      this.tagPagination.currentPage = 1
      this.loadTags()
    },

    resetTagSearch() {
      this.tagSearch = {
        tagName: '',
        tagType: null,
        enabled: null
      }
      this.searchTags()
    },

    handleTagPageChange(page) {
      this.tagPagination.currentPage = page
      this.loadTags()
    },

    handleTagPageSizeChange(size) {
      this.tagPagination.pageSize = size
      this.tagPagination.currentPage = 1
      this.loadTags()
    },

    handleAddTag() {
      this.tagDialogMode = 'add'
      this.resetTagForm()
      this.tagDialogVisible = true
    },

    handleEditTag(tag) {
      this.tagDialogMode = 'edit'
      this.currentEditingTag = tag
      this.tagForm = {
        tagName: tag.tagName,
        description: tag.description || '',
        tagType: tag.tagType,
        color: this.convertColorToHex(tag.color), // 确保编辑时颜色也是十六进制格式
        isEnabled: tag.isEnabled
      }
      this.tagDialogVisible = true
    },

    handleDeleteTag(tag) {
      this.$confirm(`确定要删除标签 "${tag.tagName}" 吗？`, '确认删除', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const response = await fetch(`/api/tags/${tag.id}`, {
            method: 'DELETE',
            credentials: 'include'
          })

          if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`)
          }

          this.$message.success('删除成功')
          this.loadTags()
        } catch (error) {
          console.error('Failed to delete tag:', error)
          this.$message.error('删除失败: ' + error.message)
        }
      }).catch(() => {
        this.$message.info('已取消删除')
      })
    },

    async toggleTag(tag) {
      const originalState = tag.isEnabled
      try {
        const response = await fetch(`/api/tags/${tag.id}/toggle`, {
          method: 'PATCH',
          credentials: 'include'
        })

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`)
        }

        const result = await response.json()
        if (result.success) {
          this.$message.success(`已${tag.isEnabled ? '启用' : '禁用'}标签`)
        } else {
          throw new Error(result.message || 'Toggle failed')
        }
      } catch (error) {
        console.error('Failed to toggle tag:', error)
        this.$message.error('更新状态失败: ' + error.message)
        // 回退状态
        tag.isEnabled = originalState
      }
    },

    // 标签对话框相关方法
    resetTagForm() {
      this.tagForm = {
        tagName: '',
        description: '',
        tagType: '',
        color: '#409eff',
        isEnabled: true
      }
      this.currentEditingTag = null
      if (this.$refs.tagFormRef) {
        this.$refs.tagFormRef.resetFields()
      }
    },

    handleTagDialogClose() {
      this.tagDialogVisible = false
      this.resetTagForm()
    },

    async submitTagForm() {
      try {
        const valid = await this.$refs.tagFormRef.validate()
        if (!valid) return

        this.tagSubmitting = true

        // 确保颜色值是十六进制格式
        if (this.tagForm.color) {
          this.tagForm.color = this.convertColorToHex(this.tagForm.color)
        }

        if (this.tagDialogMode === 'add') {
          await this.createTag()
        } else {
          await this.updateTag()
        }

        this.$message.success(this.tagDialogMode === 'add' ? '标签创建成功' : '标签更新成功')
        this.handleTagDialogClose()
        this.loadTags()
      } catch (error) {
        console.error('Submit tag form error:', error)
        this.$message.error('操作失败: ' + error.message)
      } finally {
        this.tagSubmitting = false
      }
    },

    async createTag() {
      const response = await fetch('/api/tags', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify(this.tagForm)
      })

      if (!response.ok) {
        const errorData = await response.json()
        throw new Error(errorData.message || `HTTP error! status: ${response.status}`)
      }

      return await response.json()
    },

    async updateTag() {
      if (!this.currentEditingTag) {
        throw new Error('没有选择要编辑的标签')
      }

      const response = await fetch(`/api/tags/${this.currentEditingTag.id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify(this.tagForm)
      })

      if (!response.ok) {
        const errorData = await response.json()
        throw new Error(errorData.message || `HTTP error! status: ${response.status}`)
      }

      return await response.json()
    },

    // 标签规则对话框相关方法
    resetTaggingRuleForm() {
      this.taggingRuleForm = {
        ruleName: '',
        ruleDescription: '',
        alertType: null,
        alertSubtype: '',
        matchField: '',
        matchType: 'exact',
        matchValue: '',
        tags: [],
        priority: 0,
        isEnabled: true
      }
      this.currentEditingTaggingRule = null
      if (this.$refs.taggingRuleFormRef) {
        this.$refs.taggingRuleFormRef.resetFields()
      }
    },

    handleTaggingRuleDialogClose() {
      this.taggingRuleDialogVisible = false
      this.resetTaggingRuleForm()
    },

    async loadAvailableTags() {
      try {
        const response = await fetch('/api/tags?size=100&isEnabled=true', {
          credentials: 'include'
        })

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`)
        }

        const data = await response.json()
        this.availableTags = data.records || []
      } catch (error) {
        console.error('Failed to load available tags:', error)
        // 使用模拟数据作为 fallback
        this.availableTags = [
          { id: '1', tagName: '高危威胁', tagType: 'threat-level' },
          { id: '2', tagName: '中危威胁', tagType: 'threat-level' },
          { id: '3', tagName: 'SQL注入', tagType: 'attack-type' },
          { id: '4', tagName: 'XSS攻击', tagType: 'attack-type' },
          { id: '5', tagName: 'WAF告警', tagType: 'source-system' },
          { id: '6', tagName: '待处理', tagType: 'status' }
        ]
      }
    },

    async loadAlertMetadata() {
      try {
        const response = await fetch('/api/alert-metadata/all', {
          credentials: 'include'
        })
        if (response.ok) {
          const metadata = await response.json()
          this.alertTypes = metadata.types || []

          // 转换子类型数据格式 - 显示 subtype_code
          const subtypes = metadata.subtypes || {}
          this.subtypeOptions = {}
          for (const typeId in subtypes) {
            this.subtypeOptions[typeId] = subtypes[typeId].map(subtype => ({
              value: subtype.subtypeCode,
              label: `${subtype.subtypeLabel} (${subtype.subtypeCode})`
            }))
          }

          // 转换字段数据格式 - 显示英文名字和数据类型
          const fields = metadata.fields || {}
          this.fieldOptions = {}
          for (const typeId in fields) {
            this.fieldOptions[typeId] = fields[typeId].map(field => ({
              value: field.fieldName,
              label: `${field.fieldLabel} (${field.fieldName})`
            }))
          }
        } else {
          this.$message.error('获取告警元数据失败')
        }
      } catch (error) {
        console.error('Failed to load alert metadata:', error)
        // 使用默认数据作为 fallback
        this.alertTypes = [
          { id: 1, typeLabel: '网络攻击' },
          { id: 2, typeLabel: '恶意样本' },
          { id: 3, typeLabel: '主机行为' }
        ]
      }
    },

    onTaggingAlertTypeChange() {
      // 当告警类型改变时，清空子类型和字段选择
      this.taggingRuleForm.alertSubtype = ''
      this.taggingRuleForm.matchField = ''
    },

    async submitTaggingRuleForm() {
      try {
        const valid = await this.$refs.taggingRuleFormRef.validate()
        if (!valid) return

        this.taggingRuleSubmitting = true

        if (this.taggingRuleDialogMode === 'add') {
          await this.createTaggingRule()
        } else {
          await this.updateTaggingRule()
        }

        this.$message.success(this.taggingRuleDialogMode === 'add' ? '标签规则创建成功' : '标签规则更新成功')
        this.handleTaggingRuleDialogClose()
        this.loadTaggingRules()
      } catch (error) {
        console.error('Submit tagging rule form error:', error)
        this.$message.error('操作失败: ' + error.message)
      } finally {
        this.taggingRuleSubmitting = false
      }
    },

    async createTaggingRule() {
      const response = await fetch('/api/alert/tagging-rules', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify(this.taggingRuleForm)
      })

      if (!response.ok) {
        const errorData = await response.json()
        throw new Error(errorData.message || `HTTP error! status: ${response.status}`)
      }

      return await response.json()
    },

    async updateTaggingRule() {
      if (!this.currentEditingTaggingRule) {
        throw new Error('没有选择要编辑的标签规则')
      }

      const response = await fetch(`/api/alert/tagging-rules/${this.currentEditingTaggingRule.id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json'
        },
        credentials: 'include',
        body: JSON.stringify(this.taggingRuleForm)
      })

      if (!response.ok) {
        const errorData = await response.json()
        throw new Error(errorData.message || `HTTP error! status: ${response.status}`)
      }

      return await response.json()
    }
  },

  mounted() {
    // 初始化时加载数据
    this.loadTaggingRules()
    this.loadTags()
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

/* 规则管理样式 */
.rule-section {
  padding: 20px 0;
}

.rule-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.rule-header h4 {
  margin: 0;
  color: #303133;
  font-size: 18px;
  font-weight: 600;
}

.filter-bar {
  margin-bottom: 20px;
  padding: 15px;
  background: #fafafa;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
}

.el-pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* 表格内的标签样式 */
.el-table .el-tag {
  margin-right: 5px;
  margin-bottom: 5px;
}

.el-table .el-tag:last-child {
  margin-right: 0;
}

/* 标签管理样式 */
.color-display {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 1px solid #e4e7ed;
  margin: 0 auto;
}

/* 标签对话框样式 */
.color-picker-container {
  display: flex;
  align-items: center;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 标签规则对话框样式 */
.match-conditions-container {
  width: 100%;
}

.match-condition-item {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.match-condition-item:last-child {
  margin-bottom: 15px;
}

.tags-container {
  width: 100%;
}

.el-dialog .el-form-item:last-child {
  margin-bottom: 0;
}
</style>