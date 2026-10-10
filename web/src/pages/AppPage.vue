<template>
  <div class="anime-page-shell">
    <section class="anime-card" :aria-busy="loading">
      <div class="anime-card-header">
        <div>
          <h1 class="anime-card-title">应用管理</h1>
          <p class="anime-card-desc">Dify应用配置 · 知识库关联 · 机器人绑定</p>
        </div>
        <div class="anime-card-actions">
          <button class="anime-btn primary" @click="openCreateModal">
            <Plus :size="18" />
            <span>新建应用</span>
          </button>
        </div>
      </div>

      <div v-if="err" class="anime-error page-error" role="alert" aria-live="assertive">{{ err }}</div>

      <div class="anime-card-body">
        <div class="anime-tabs page-tabs" role="group" aria-label="应用范围">
          <button class="anime-tab" :class="{ active: activeTab === 'mine' }" :aria-pressed="activeTab === 'mine'" @click="activeTab = 'mine'">我的应用</button>
          <button class="anime-tab" :class="{ active: activeTab === 'all' }" :aria-pressed="activeTab === 'all'" @click="activeTab = 'all'">应用大厅</button>
        </div>
        <div v-if="loading" class="anime-empty" role="status" aria-live="polite">
          <span class="anime-loader-spinner"></span>
          <span class="anime-empty-text">加载中...</span>
        </div>

        <div v-else-if="filteredAppList.length === 0" class="anime-empty">
          <div class="anime-empty-icon">🤖</div>
          <div class="anime-empty-text">{{ activeTab === 'mine' ? '暂无应用，点击上方按钮创建' : '暂无公开应用' }}</div>
        </div>

        <div v-else class="app-grid">
          <div v-for="app in filteredAppList" :key="app.id" class="anime-card app-card">
            <div class="app-header">
              <div class="app-title-row">
                <span class="app-icon">{{ app.icon || '🤖' }}</span>
                <span class="app-name">{{ app.name }}</span>
              </div>
              <div class="app-badges">
                <span v-if="app.isDefault" class="anime-badge yellow">默认</span>
                <span class="anime-badge" :class="app.isPublic ? 'green' : 'gray'">{{ app.isPublic ? '公开' : '私有' }}</span>
                <span class="anime-badge" :class="app.status ? 'green' : 'pink'">{{ app.status ? '启用' : '禁用' }}</span>
              </div>
            </div>
            
            <div class="app-desc">{{ app.description || '无描述' }}</div>
            
            <div class="app-info-section">
              <div class="info-row">
                <span class="info-label">Dify应用:</span>
                <span class="anime-code">{{ app.difyAppName || '未验证' }}</span>
                <span class="anime-badge blue">{{ app.difyAppMode || '-' }}</span>
                  <a :href="difyConsoleUrl" target="_blank" rel="noopener noreferrer" class="dify-link">
                  <ExternalLink :size="14" />
                  <span>控制台</span>
                </a>
              </div>
              <div class="info-row">
                <span class="info-label">知识库分类:</span>
                <span v-if="app.categoryId" class="info-value">{{ getCategoryName(app.categoryId) }}</span>
                <span v-else class="info-muted">未关联</span>
                <span v-if="app.categoryId" class="anime-badge purple">{{ getCategoryKbCount(app.categoryId) }} 个</span>
                <button v-if="app.categoryId && getCategoryKbList(app.categoryId).length > 0" class="anime-btn ghost sm" @click="showCategoryKb(app)">
                  <BookOpen :size="14" />
                  <span>查看知识库</span>
                </button>
              </div>
              <div class="info-row">
                <span class="info-label">绑定机器人:</span>
                <span class="anime-badge" :class="(app.qqGroups || 0) > 0 ? 'green' : 'gray'">QQ {{ app.qqGroups || 0 }}群</span>
                <span class="anime-badge" :class="(app.wxGroups || 0) > 0 ? 'blue' : 'gray'">企微 {{ app.wxGroups || 0 }}群</span>
                <button v-if="(app.qqGroups || 0) > 0 || (app.wxGroups || 0) > 0" class="anime-btn ghost sm" @click="showBoundGroups(app)">
                  <Users :size="14" />
                  <span>详情</span>
                </button>
              </div>
              <div class="info-row">
                <span class="info-label">创建者:</span>
                <span class="info-value">{{ app.createBy }}</span>
              </div>
            </div>
            
            <div class="app-actions">
              <button class="anime-btn blue" @click="verifyApp(app)">
                <RefreshCw :size="16" />
                <span>验证</span>
              </button>
              <button v-if="!app.isDefault" class="anime-btn primary" @click="setDefaultApp(app)">
                <Star :size="16" />
                <span>设为默认</span>
              </button>
              <button v-if="app.createBy === currentUserName" class="anime-btn ghost" @click="editApp(app)">
                <Edit3 :size="16" />
                <span>编辑</span>
              </button>
              <button v-if="!app.isDefault && app.createBy === currentUserName" class="anime-btn ghost danger" @click="deleteApp(app)">
                <Trash2 :size="16" />
                <span>删除</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 创建/编辑应用弹窗 -->
      <div v-if="showCreateModal" class="anime-modal-overlay" @click.self="closeModal">
      <div ref="appDialogRef" class="anime-modal" role="dialog" aria-modal="true" tabindex="-1" :aria-label="editingApp ? '编辑应用' : '创建应用'" style="max-width: 650px;">
        <div class="anime-modal-header">
          <span>{{ editingApp ? '编辑应用' : '创建应用' }}</span>
          <button class="anime-modal-close" aria-label="关闭应用设置窗口" @click="closeModal">✕</button>
        </div>
        <div class="anime-modal-body">
          <!-- 步骤指引 -->
          <div v-if="!editingApp" class="create-steps">
            <div class="step-item">
              <div class="step-num">1</div>
              <div class="step-content">
                <div class="step-title">前往Dify控制台创建应用</div>
                <div class="step-desc">在Dify中创建Chatbot或Agent应用，配置知识库和参数</div>
                <a :href="difyConsoleUrl" target="_blank" rel="noopener noreferrer" class="anime-btn blue step-btn">
                  <ExternalLink :size="16" />
                  <span>打开Dify控制台</span>
                </a>
              </div>
            </div>
            <div class="step-item">
              <div class="step-num">2</div>
              <div class="step-content">
                <div class="step-title">获取API Key</div>
                <div class="step-desc">在应用详情页左侧导航找到"API访问"，复制API Key</div>
                <div class="step-hint">
                  <img src="" alt="API Key位置示意" class="step-image-placeholder" />
                  <span>API Key格式：app-xxxxxxxxxxxxx</span>
                </div>
              </div>
            </div>
            <div class="step-item">
              <div class="step-num">3</div>
              <div class="step-content">
                <div class="step-title">回到此处填写信息</div>
                <div class="step-desc">将API Key粘贴到下方输入框，验证后保存</div>
              </div>
            </div>
          </div>

          <div class="anime-form-group">
            <label for="app-name">应用名称 *</label>
            <input id="app-name" v-model="form.name" class="anime-input" aria-label="应用名称" placeholder="如：医疗助手、群聊助手" />
          </div>
          <div class="anime-form-group">
            <label for="app-description">应用描述</label>
            <textarea id="app-description" v-model="form.description" class="anime-input" aria-label="应用描述" rows="2" placeholder="应用用途说明"></textarea>
          </div>
          <div class="anime-form-group">
            <label for="app-icon">应用图标</label>
            <input id="app-icon" v-model="form.icon" class="anime-input" aria-label="应用图标" placeholder="如：🏥、💬、🤖、📚" />
          </div>
          <div class="anime-form-group">
            <div class="form-field-label">创建者</div>
            <div class="creator-display">
              <User :size="16" />
              <span>{{ currentUserName }}</span>
            </div>
            <div class="form-hint">应用公开后，其他用户将看到此创建者信息</div>
          </div>
          <div class="anime-form-group">
            <label for="app-dify-api-key">Dify API Key *</label>
            <div class="api-key-input-row">
              <input id="app-dify-api-key" v-model="form.difyApiKey" class="anime-input" :type="showApiKey ? 'text' : 'password'" autocomplete="new-password" aria-label="Dify API Key" placeholder="从Dify控制台获取，格式：app-xxxxx" />
              <button type="button" class="anime-btn ghost" :aria-label="showApiKey ? '隐藏 Dify API Key' : '显示 Dify API Key'" :aria-pressed="showApiKey" @click="showApiKey = !showApiKey">
                <EyeOff v-if="showApiKey" :size="16" aria-hidden="true" />
                <Eye v-else :size="16" aria-hidden="true" />
                <span>{{ showApiKey ? '隐藏' : '显示' }}</span>
              </button>
            </div>
            <div class="api-key-actions">
              <button class="anime-btn ghost" @click="verifyApiKey">
                <RefreshCw :size="16" />
                <span>验证API Key</span>
              </button>
              <a :href="difyConsoleUrl" target="_blank" rel="noopener noreferrer" class="anime-btn blue">
                <ExternalLink :size="16" />
                <span>获取API Key</span>
              </a>
            </div>
            <div v-if="verifiedInfo" class="verified-info" role="status" aria-live="polite">
              <div class="verified-success">
                <CheckCircle :size="16" />
                <span>验证成功</span>
              </div>
              <div>Dify应用名称: {{ verifiedInfo.difyAppName }}</div>
              <div>应用模式: {{ verifiedInfo.difyAppMode }}</div>
              <div v-if="verifiedInfo.difyAppMode === 'workflow' || verifiedInfo.difyAppMode === 'agent'" class="verified-warning">
                <AlertCircle :size="14" />
                <span>请确保应用已在Dify控制台发布，否则API调用会失败</span>
              </div>
            </div>
            <div v-if="verifyError" class="verified-error" role="alert" aria-live="assertive">
              <XCircle :size="16" />
              <span>{{ verifyError }}</span>
            </div>
          </div>
          <div class="anime-form-group">
            <label for="app-category">关联知识库分类（可选）</label>
            <select id="app-category" v-model="form.categoryId" class="anime-input" aria-label="关联知识库分类">
              <option value="">不关联分类（使用Dify应用配置的知识库）</option>
              <option v-for="cat in categoryList" :key="cat.id" :value="cat.id">
                {{ cat.name }} ({{ cat.kbCount || 0 }} 知识库)
              </option>
            </select>
            <div class="form-hint">关联分类后，可查看该分类下的知识库列表</div>
          </div>
          <div class="anime-form-group">
            <label style="display: flex; align-items: center; gap: 8px;">
              <input type="checkbox" v-model="form.isPublic" aria-label="公开应用" />
              公开应用（其他用户可使用）
            </label>
          </div>
        </div>
        <div class="anime-modal-footer">
          <button class="anime-btn ghost" @click="closeModal">取消</button>
          <button class="anime-btn primary" @click="saveApp" :disabled="saving || !form.difyApiKey">
            <span v-if="saving">保存中...</span>
            <span v-else>{{ editingApp ? '更新' : '创建' }}</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 绑定群组详情弹窗 -->
    <div v-if="showGroupsModal" class="anime-modal-overlay" @click.self="closeGroupsModal">
      <div ref="groupsDialogRef" class="anime-modal" role="dialog" aria-modal="true" tabindex="-1" :aria-label="`绑定群组详情：${selectedApp?.name || ''}`" style="max-width: 700px;">
        <div class="anime-modal-header">
          <span>绑定群组详情 - {{ selectedApp?.name }}</span>
          <button class="anime-modal-close" aria-label="关闭绑定群组详情" @click="closeGroupsModal">✕</button>
        </div>
        <div class="anime-modal-body">
<div class="groups-tabs" role="group" aria-label="群组平台">
              <button class="anime-tab" :class="{ active: groupsTab === 'qq' }" :aria-pressed="groupsTab === 'qq'" @click="groupsTab = 'qq'">
                QQ群 ({{ qqGroups.length }})
              </button>
              <button class="anime-tab" :class="{ active: groupsTab === 'wx' }" :aria-pressed="groupsTab === 'wx'" @click="groupsTab = 'wx'">
                企微群 ({{ wxGroups.length }})
              </button>
            </div>
          <div v-if="groupsTab === 'qq'" class="groups-list">
            <div v-if="qqGroups.length === 0" class="anime-empty">
              <span class="anime-empty-text">暂无绑定的QQ群</span>
            </div>
            <div v-else>
              <div v-for="g in qqGroups" :key="g.id" class="group-item">
                <span class="anime-badge green">QQ群</span>
                <span class="group-name">{{ g.groupName || g.groupId }}</span>
                <span class="group-members">{{ g.memberCount || 0 }} 成员</span>
                <RouterLink to="/console/im" class="anime-btn ghost sm">管理</RouterLink>
              </div>
            </div>
          </div>
          <div v-if="groupsTab === 'wx'" class="groups-list">
            <div v-if="wxGroups.length === 0" class="anime-empty">
              <span class="anime-empty-text">暂无绑定的企微群</span>
            </div>
            <div v-else>
              <div v-for="g in wxGroups" :key="g.id" class="group-item">
                <span class="anime-badge blue">企微群</span>
                <span class="group-name">{{ g.groupName || g.groupId }}</span>
                <span class="group-members">{{ g.memberCount || 0 }} 成员</span>
                <RouterLink to="/console/im" class="anime-btn ghost sm">管理</RouterLink>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 分类知识库列表弹窗 -->
    <div v-if="showKbModal" class="anime-modal-overlay" @click.self="closeKbListModal">
      <div ref="kbListDialogRef" class="anime-modal" role="dialog" aria-modal="true" tabindex="-1" :aria-label="`知识库列表：${selectedApp?.name || ''}`" style="max-width: 600px;">
        <div class="anime-modal-header">
          <span>知识库列表 - {{ selectedApp?.name }} ({{ selectedCategory?.name }})</span>
          <button class="anime-modal-close" aria-label="关闭知识库列表" @click="closeKbListModal">✕</button>
        </div>
        <div class="anime-modal-body">
          <div v-if="categoryKbList.length === 0" class="anime-empty">
            <span class="anime-empty-text">该分类下暂无知识库</span>
          </div>
          <div v-else class="kb-list-modal">
            <div v-for="kb in categoryKbList" :key="kb.id" class="kb-item">
              <span class="kb-title">{{ kb.name }}</span>
              <span class="anime-badge blue">{{ kb.docCount || 0 }} 文档</span>
              <span class="anime-badge" :class="kb.status ? 'green' : 'pink'">{{ kb.status ? '启用' : '禁用' }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useModalDialog } from '../composables/useModalDialog'
import { RouterLink } from 'vue-router'
import { Plus, RefreshCw, Edit3, Trash2, Star, Users, BookOpen, ExternalLink, CheckCircle, XCircle, AlertCircle, User, Eye, EyeOff } from 'lucide-vue-next'
import { api } from '../api/client'
import { getCurrentUser } from '../lib/user'

interface KbApp {
  id: number
  name: string
  description: string
  icon: string
  difyApiKey: string
  difyAppName: string
  difyAppMode: string
  categoryId: number | null
  isDefault: boolean
  isPublic: boolean
  createBy: string
  status: boolean
  createTime: string
  qqGroups?: number
  wxGroups?: number
  boundGroupsCount?: number
}

interface Category {
  id: number
  name: string
  kbCount?: number
}

interface ImGroup {
  id: number
  platform: string
  groupId: string
  groupName: string
  memberCount: number
}

interface KbKnowledgeBase {
  id: number
  name: string
  docCount: number
  status: boolean
  categoryId?: number
}

interface Category {
  id: number
  name: string
  kbCount?: number
}

const difyConsoleUrl = 'https://cloud.dify.ai'

const activeTab = ref<'mine' | 'all'>('mine')
const filteredAppList = computed(() => {
  if (activeTab.value === 'all') return appList.value.filter(a => a.isPublic)
  return appList.value.filter(a => a.createBy === currentUserName.value)
})

const appList = ref<KbApp[]>([])
const categoryList = ref<Category[]>([])
const kbList = ref<KbKnowledgeBase[]>([])
const loading = ref(true)
const err = ref('')
const showCreateModal = ref(false)
const showApiKey = ref(false)
const showGroupsModal = ref(false)
const showKbModal = ref(false)
const closeGroupsModal = () => { showGroupsModal.value = false }
const closeKbListModal = () => { showKbModal.value = false }
const editingApp = ref<KbApp | null>(null)
const selectedApp = ref<KbApp | null>(null)
const selectedCategory = ref<Category | null>(null)
const boundGroups = ref<ImGroup[]>([])
const categoryKbList = ref<KbKnowledgeBase[]>([])
const verifiedInfo = ref<{ difyAppName: string; difyAppMode: string } | null>(null)
const verifyError = ref('')
const saving = ref(false)
const groupsTab = ref<'qq' | 'wx'>('qq')

const qqGroups = computed(() => boundGroups.value.filter((g: ImGroup) => g.platform === 'qq'))
const wxGroups = computed(() => boundGroups.value.filter((g: ImGroup) => g.platform === 'wx' || g.platform === 'wecom'))

const currentUserName = computed(() => {
  return localStorage.getItem('chatbase_original_username') || localStorage.getItem('chatbase_user') || '未知用户'
})

const form = ref({
  name: '',
  description: '',
  icon: '',
  difyApiKey: '',
  categoryId: '',
  isPublic: true
})

const loadApps = async () => {
  loading.value = true
  err.value = ''
  try {
    const res = await api.get('/kb/app/list')
    appList.value = res.data || []
    for (const app of appList.value) {
      try {
        const groupsRes = await api.get(`/kb/app/${app.id}/groups`)
        const groups: ImGroup[] = groupsRes.data || []
        app.qqGroups = groups.filter((g: ImGroup) => g.platform === 'qq').length
        app.wxGroups = groups.filter((g: ImGroup) => g.platform === 'wx' || g.platform === 'wecom').length
        app.boundGroupsCount = groups.length
      } catch {
        app.qqGroups = 0
        app.wxGroups = 0
        app.boundGroupsCount = 0
      }
    }
  } catch (e: any) {
    err.value = e.response?.data?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

const loadCategories = async () => {
  try {
    const res = await api.get('/kb/category/tree')
    const flatList: Category[] = []
    const flatten = (items: any[]) => {
      items.forEach(item => {
        flatList.push({ id: item.id, name: item.name, kbCount: item.kbCount || 0 })
        if (item.children?.length) flatten(item.children)
      })
    }
    flatten(res.data || [])
    categoryList.value = flatList
  } catch (e) {
    console.error('加载分类失败', e)
  }
}

const loadKbList = async () => {
  try {
    const res = await api.get('/kb/page', { params: { pageNum: 1, pageSize: 100 } })
    kbList.value = res.data.records || []
  } catch (e) {
    console.error('加载知识库失败', e)
  }
}

const getCategoryName = (id: number | null) => {
  if (!id) return '未关联'
  const cat = categoryList.value.find(c => c.id === id)
  return cat?.name || '未知'
}

const getCategoryKbCount = (id: number | null) => {
  if (!id) return 0
  const cat = categoryList.value.find(c => c.id === id)
  return cat?.kbCount || 0
}

const getCategoryKbList = (id: number) => {
  return kbList.value.filter(kb => kb.categoryId === id)
}

const openCreateModal = () => {
  editingApp.value = null
  showApiKey.value = false
  verifiedInfo.value = null
  verifyError.value = ''
  form.value = { name: '', description: '', icon: '', difyApiKey: '', categoryId: '', isPublic: true }
  showCreateModal.value = true
}

const verifyApiKey = async () => {
  if (!form.value.difyApiKey) {
    verifyError.value = '请输入API Key'
    return
  }
  verifyError.value = ''
  try {
    const res = await api.post('/kb/app/verify', { apiKey: form.value.difyApiKey })
    if (!res.data || !res.data.difyAppName) {
      verifyError.value = 'API Key验证失败，请检查是否正确'
      verifiedInfo.value = null
      return
    }
    verifiedInfo.value = {
      difyAppName: res.data.difyAppName,
      difyAppMode: res.data.difyAppMode || 'unknown'
    }
  } catch (e: any) {
    verifyError.value = e.response?.data?.message || 'API Key验证失败'
    verifiedInfo.value = null
  }
}

const verifyApp = async (app: KbApp) => {
  try {
    await api.get(`/kb/app/${app.id}/info`)
    loadApps()
  } catch (e: any) {
    err.value = e.response?.data?.message || '验证失败'
  }
}

const showBoundGroups = async (app: KbApp) => {
  selectedApp.value = app
  try {
    const res = await api.get(`/kb/app/${app.id}/groups`)
    boundGroups.value = res.data || []
    groupsTab.value = 'qq'
    showGroupsModal.value = true
  } catch (e: any) {
    err.value = e.response?.data?.message || '加载群组失败'
  }
}

const showCategoryKb = (app: KbApp) => {
  if (!app.categoryId) return
  selectedApp.value = app
  const cat = categoryList.value.find(c => c.id === app.categoryId)
  selectedCategory.value = cat || null
  categoryKbList.value = getCategoryKbList(app.categoryId)
  showKbModal.value = true
}

const setDefaultApp = async (app: KbApp) => {
  try {
    await api.put(`/kb/app/${app.id}/default`)
    loadApps()
  } catch (e: any) {
    err.value = e.response?.data?.message || '设置失败'
  }
}

const editApp = (app: KbApp) => {
  editingApp.value = app
  showApiKey.value = false
  form.value = {
    name: app.name,
    description: app.description || '',
    icon: app.icon || '',
    difyApiKey: app.difyApiKey,
    categoryId: app.categoryId ? String(app.categoryId) : '',
    isPublic: app.isPublic
  }
  verifiedInfo.value = app.difyAppName ? {
    difyAppName: app.difyAppName,
    difyAppMode: app.difyAppMode || ''
  } : null
  verifyError.value = ''
  showCreateModal.value = true
}

const deleteApp = async (app: KbApp) => {
  if (!confirm(`确定删除应用 "${app.name}"？\n已绑定的群组将解除绑定。`)) return
  try {
    await api.delete(`/kb/app/${app.id}`)
    loadApps()
  } catch (e: any) {
    err.value = e.response?.data?.message || '删除失败'
  }
}

const saveApp = async () => {
  if (!form.value.name || !form.value.difyApiKey) {
    err.value = '请填写必填项'
    return
  }
  saving.value = true
  try {
    const payload = {
      name: form.value.name,
      description: form.value.description,
      icon: form.value.icon,
      difyApiKey: form.value.difyApiKey,
      categoryId: form.value.categoryId ? Number(form.value.categoryId) : null,
      isPublic: form.value.isPublic
    }
    if (editingApp.value) {
      await api.put('/kb/app', { ...payload, id: editingApp.value.id })
    } else {
      await api.post('/kb/app', payload)
    }
    closeModal()
    loadApps()
  } catch (e: any) {
    err.value = e.response?.data?.message || '保存失败'
  } finally {
    saving.value = false
  }
}

const closeModal = () => {
  showCreateModal.value = false
  showApiKey.value = false
  editingApp.value = null
  verifiedInfo.value = null
  verifyError.value = ''
  form.value = { name: '', description: '', icon: '', difyApiKey: '', categoryId: '', isPublic: true }
  err.value = ''
}

const { dialogRef: appDialogRef } = useModalDialog(showCreateModal, closeModal)
const { dialogRef: groupsDialogRef } = useModalDialog(showGroupsModal, closeGroupsModal)
const { dialogRef: kbListDialogRef } = useModalDialog(showKbModal, closeKbListModal)

onMounted(() => {
  loadApps()
  loadCategories()
  loadKbList()
})
</script>

<style scoped>
.app-card {
  padding: 20px;
  background: var(--ui-surface);
  border: 1px solid var(--ui-border);
  box-shadow: var(--ui-shadow-card);
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.app-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.app-icon {
  font-size: 24px;
}

.app-name {
  font-size: 18px;
  font-weight: 700;
  color: var(--ui-text);
}

.app-badges {
  display: flex;
  gap: 6px;
}

.app-desc {
  font-size: 14px;
  color: var(--ui-text-muted);
  margin-bottom: 16px;
}

.app-info-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
  padding: 14px;
  background: var(--ui-surface-muted);
  border-radius: var(--ui-radius-md);
}

.info-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  flex-wrap: wrap;
}

.info-label {
  color: var(--ui-text-muted);
  min-width: 80px;
}

.info-value {
  color: var(--ui-text);
}

.info-muted {
  color: var(--ui-text-muted);
}

.dify-link {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--ui-primary-hover);
  font-size: 13px;
  text-decoration: none;
}

.dify-link:hover {
  text-decoration: underline;
}

.app-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.danger {
  color: var(--ui-danger) !important;
}

.danger:hover {
  background: var(--ui-danger-soft) !important;
  border-color: var(--ui-danger-border) !important;
  color: var(--ui-danger) !important;
}

.create-steps {
  margin-bottom: 20px;
  padding: 16px;
  background: var(--ui-surface-muted);
  border-radius: var(--ui-radius-md);
}

.step-item {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.step-item:last-child {
  margin-bottom: 0;
}

.step-num {
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--ui-primary);
  color: white;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.step-content {
  flex: 1;
}

.step-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--ui-text);
  margin-bottom: 4px;
}

.step-desc {
  font-size: 13px;
  color: var(--ui-text-muted);
  margin-bottom: 8px;
}

.api-key-actions {
  display: flex;
  gap: 10px;
  margin-top: 8px;
}

.api-key-input-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.api-key-input-row .anime-input {
  flex: 1;
  min-width: 0;
}

.verified-info {
  margin-top: 12px;
  padding: 12px;
  background: var(--ui-success-soft);
  border: 1px solid var(--ui-success-border);
  border-radius: var(--ui-radius-md);
  font-size: 13px;
}

.verified-success {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--ui-success);
  font-weight: 700;
  margin-bottom: 8px;
}

.verified-error {
  margin-top: 12px;
  padding: 10px;
  background: var(--ui-danger-soft);
  border: 1px solid var(--ui-danger-border);
  border-radius: var(--ui-radius-md);
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--ui-danger);
  font-size: 13px;
}

.verified-warning {
  margin-top: 8px;
  padding: 8px;
  background: var(--ui-warning-soft);
  border-radius: var(--ui-radius-md);
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--ui-warning);
  font-size: 13px;
}

.form-hint {
  font-size: 12px;
  color: var(--ui-text-muted);
  margin-top: 4px;
}

.creator-display {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: var(--ui-surface-muted);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  font-weight: 600;
  color: var(--ui-text);
  font-size: 14px;
}

.groups-tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.groups-list {
  max-height: 400px;
  overflow-y: auto;
}

.group-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--ui-surface-muted);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  margin-bottom: 8px;
}

.group-name {
  font-weight: 600;
  color: var(--ui-text);
  flex: 1;
}

.group-members {
  font-size: 13px;
  color: var(--ui-text-muted);
}

.kb-list-modal {
  max-height: 400px;
  overflow-y: auto;
}

.kb-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--ui-surface-muted);
  border: 1px solid var(--ui-border);
  border-radius: var(--ui-radius-md);
  margin-bottom: 8px;
}

.kb-title {
  font-weight: 600;
  color: var(--ui-text);
  flex: 1;
}
</style>

<style scoped>
.page-error { margin: 0 28px 16px; }
.anime-card-title { margin: 0; }
.anime-card-desc { margin: 4px 0 0; }
.page-tabs { margin-bottom: 18px; }
.app-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(min(100%, 390px), 1fr)); gap: 16px; }
.app-card { min-width: 0; padding: 18px; background: var(--ui-surface); border: 1px solid var(--ui-border); border-radius: var(--ui-radius-lg); box-shadow: var(--ui-shadow-card); }
.app-header { display: flex; align-items: flex-start; justify-content: space-between; flex-wrap: wrap; gap: 10px; margin-bottom: 12px; }
.app-title-row { min-width: 0; }
.app-icon { display: grid; place-items: center; width: 36px; height: 36px; background: var(--ui-primary-soft); border-radius: var(--ui-radius-md); font-size: 20px; }
.app-name { color: var(--ui-text); font-size: 17px; font-weight: 700; overflow-wrap: anywhere; }
.app-badges { display: flex; flex-wrap: wrap; gap: 6px; }
.app-desc { color: var(--ui-text-secondary); font-size: 14px; line-height: 1.55; margin-bottom: 14px; overflow-wrap: anywhere; }
.app-info-section { display: flex; flex-direction: column; gap: 10px; margin-bottom: 14px; padding: 12px; background: var(--ui-surface-muted); border: 1px solid var(--ui-border); border-radius: var(--ui-radius-md); }
.info-row { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; font-size: 13px; }
.info-label { color: var(--ui-text-secondary); min-width: 90px; font-weight: 600; }
.info-value { color: var(--ui-text); overflow-wrap: anywhere; }
.info-muted { color: var(--ui-text-muted); }
.dify-link { color: var(--ui-primary-hover); font-size: 13px; text-decoration: none; }
.dify-link:hover { color: var(--ui-primary); text-decoration: underline; }
.app-actions { display: flex; flex-wrap: wrap; gap: 8px; }
.app-actions .anime-btn { min-height: 36px; }
.danger { color: var(--ui-danger) !important; }
.danger:hover { background: var(--ui-danger-soft) !important; border-color: var(--ui-danger-border) !important; color: var(--ui-danger) !important; }
.create-steps { margin-bottom: 20px; padding: 16px; background: var(--ui-surface-muted); border: 1px solid var(--ui-border); border-radius: var(--ui-radius-md); }
.step-item { display: flex; gap: 12px; margin-bottom: 16px; }
.step-item:last-child { margin-bottom: 0; }
.step-num { display: grid; place-items: center; width: 28px; height: 28px; flex: 0 0 28px; background: var(--ui-primary); color: #fff; border-radius: 50%; font-size: 12px; font-weight: 700; }
.step-content { flex: 1; min-width: 0; }
.step-title { color: var(--ui-text); font-size: 14px; font-weight: 700; margin-bottom: 4px; }
.step-desc, .step-hint { color: var(--ui-text-secondary); font-size: 13px; margin-bottom: 8px; }
.api-key-actions { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 8px; }
.verified-info, .verified-error, .verified-warning { margin-top: 10px; padding: 12px; border-radius: var(--ui-radius-md); font-size: 13px; }
.verified-info { background: var(--ui-success-soft); border: 1px solid var(--ui-success-border); color: var(--ui-text); }
.verified-success { display: flex; align-items: center; gap: 6px; color: var(--ui-success); font-weight: 700; margin-bottom: 8px; }
.verified-error { display: flex; align-items: center; gap: 6px; color: var(--ui-danger); background: var(--ui-danger-soft); border: 1px solid var(--ui-danger-border); }
.verified-warning { display: flex; align-items: center; gap: 6px; color: var(--ui-warning); background: var(--ui-warning-soft); border: 1px solid var(--ui-warning-soft); }
.form-hint { color: var(--ui-text-muted); font-size: 12px; margin-top: 4px; }
.form-field-label { color: var(--ui-text-secondary); font-size: 13px; margin-bottom: 6px; font-weight: 600; }
.creator-display { display: flex; align-items: center; gap: 8px; padding: 10px 12px; background: var(--ui-surface-muted); border: 1px solid var(--ui-border); border-radius: var(--ui-radius-md); color: var(--ui-text); font-size: 14px; }
.groups-tabs { display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 16px; }
.groups-list, .kb-list-modal { max-height: 400px; overflow-y: auto; }
.group-item, .kb-item { display: flex; align-items: center; gap: 12px; min-width: 0; padding: 12px; margin-bottom: 8px; background: var(--ui-surface-muted); border: 1px solid var(--ui-border); border-radius: var(--ui-radius-md); }
.group-name, .kb-title { color: var(--ui-text); font-weight: 600; flex: 1; min-width: 0; overflow-wrap: anywhere; }
.group-members { color: var(--ui-text-secondary); font-size: 13px; white-space: nowrap; }
.anime-page-shell .anime-badge.blue, .anime-page-shell .anime-badge.purple { background: var(--ui-surface-muted); color: var(--ui-text-secondary); border: 1px solid var(--ui-border); }
.anime-page-shell .anime-badge.green { background: var(--ui-success-soft); color: var(--ui-success); border: 1px solid var(--ui-success-border); }
.anime-page-shell .anime-badge.pink { background: var(--ui-danger-soft); color: var(--ui-danger); border: 1px solid var(--ui-danger-border); }
.anime-page-shell .anime-badge.yellow { background: var(--ui-warning-soft); color: var(--ui-warning); border: 1px solid var(--ui-warning-soft); }
.anime-page-shell .anime-badge.gray, .anime-page-shell .anime-badge.muted { background: var(--ui-surface-muted); color: var(--ui-text-secondary); border: 1px solid var(--ui-border); }
@media (max-width: 760px) {
  .page-error { margin: 0 16px 14px; }
  .app-grid { grid-template-columns: minmax(0, 1fr); }
  .app-header { flex-direction: column; }
  .info-label { min-width: 100%; }
  .app-actions .anime-btn { flex: 1 1 calc(50% - 8px); }
  .api-key-actions > * { flex: 1 1 100%; }
  .group-item, .kb-item { flex-wrap: wrap; }
  .group-name, .kb-title { flex-basis: 100%; }
}
</style>
