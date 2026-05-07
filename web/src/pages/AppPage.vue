<template>
  <div class="anime-page-shell">
    <section class="anime-card">
      <div class="anime-card-header">
        <div>
          <div class="anime-card-title">应用管理</div>
          <div class="anime-card-desc">Dify应用配置 · 知识库关联 · 机器人绑定</div>
        </div>
        <div class="anime-card-actions">
          <button class="anime-btn primary" @click="openCreateModal">
            <Plus :size="18" />
            <span>新建应用</span>
          </button>
        </div>
      </div>

      <div v-if="err" class="anime-error" style="margin: 16px 28px;">{{ err }}</div>

      <div class="anime-card-body">
        <div v-if="loading" class="anime-empty">
          <span class="anime-loader-spinner"></span>
          <span class="anime-empty-text">加载中...</span>
        </div>

        <div v-else-if="appList.length === 0" class="anime-empty">
          <div class="anime-empty-icon">🤖</div>
          <div class="anime-empty-text">暂无应用，点击上方按钮创建</div>
        </div>

        <div v-else class="kb-grid">
          <div v-for="app in appList" :key="app.id" class="anime-card app-card">
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
                <a :href="difyConsoleUrl" target="_blank" class="dify-link">
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
              <button class="anime-btn ghost" @click="editApp(app)">
                <Edit3 :size="16" />
                <span>编辑</span>
              </button>
              <button v-if="!app.isDefault" class="anime-btn ghost danger" @click="deleteApp(app)">
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
      <div class="anime-modal" style="max-width: 650px;">
        <div class="anime-modal-header">
          <span>{{ editingApp ? '编辑应用' : '创建应用' }}</span>
          <button class="anime-modal-close" @click="closeModal">✕</button>
        </div>
        <div class="anime-modal-body">
          <!-- 步骤指引 -->
          <div v-if="!editingApp" class="create-steps">
            <div class="step-item">
              <div class="step-num">1</div>
              <div class="step-content">
                <div class="step-title">前往Dify控制台创建应用</div>
                <div class="step-desc">在Dify中创建Chatbot或Agent应用，配置知识库和参数</div>
                <a :href="difyConsoleUrl" target="_blank" class="anime-btn blue step-btn">
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
            <label>应用名称 *</label>
            <input v-model="form.name" class="anime-input" placeholder="如：医疗助手、群聊助手" />
          </div>
          <div class="anime-form-group">
            <label>应用描述</label>
            <textarea v-model="form.description" class="anime-input" rows="2" placeholder="应用用途说明"></textarea>
          </div>
          <div class="anime-form-group">
            <label>应用图标</label>
            <input v-model="form.icon" class="anime-input" placeholder="如：🏥、💬、🤖、📚" />
          </div>
          <div class="anime-form-group">
            <label>创建者</label>
            <div class="creator-display">
              <User :size="16" />
              <span>{{ currentUserName }}</span>
            </div>
            <div class="form-hint">应用公开后，其他用户将看到此创建者信息</div>
          </div>
          <div class="anime-form-group">
            <label>Dify API Key *</label>
            <input v-model="form.difyApiKey" class="anime-input" placeholder="从Dify控制台获取，格式：app-xxxxx" />
            <div class="api-key-actions">
              <button class="anime-btn ghost" @click="verifyApiKey">
                <RefreshCw :size="16" />
                <span>验证API Key</span>
              </button>
              <a :href="difyConsoleUrl" target="_blank" class="anime-btn blue">
                <ExternalLink :size="16" />
                <span>获取API Key</span>
              </a>
            </div>
            <div v-if="verifiedInfo" class="verified-info">
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
            <div v-if="verifyError" class="verified-error">
              <XCircle :size="16" />
              <span>{{ verifyError }}</span>
            </div>
          </div>
          <div class="anime-form-group">
            <label>关联知识库分类（可选）</label>
            <select v-model="form.categoryId" class="anime-input">
              <option value="">不关联分类（使用Dify应用配置的知识库）</option>
              <option v-for="cat in categoryList" :key="cat.id" :value="cat.id">
                {{ cat.name }} ({{ cat.kbCount || 0 }} 知识库)
              </option>
            </select>
            <div class="form-hint">关联分类后，可查看该分类下的知识库列表</div>
          </div>
          <div class="anime-form-group">
            <label style="display: flex; align-items: center; gap: 8px;">
              <input type="checkbox" v-model="form.isPublic" />
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
    <div v-if="showGroupsModal" class="anime-modal-overlay" @click.self="showGroupsModal = false">
      <div class="anime-modal" style="max-width: 700px;">
        <div class="anime-modal-header">
          <span>绑定群组详情 - {{ selectedApp?.name }}</span>
          <button class="anime-modal-close" @click="showGroupsModal = false">✕</button>
        </div>
        <div class="anime-modal-body">
<div class="groups-tabs">
              <button class="anime-tab" :class="{ active: groupsTab === 'qq' }" @click="groupsTab = 'qq'">
                QQ群 ({{ qqGroups.length }})
              </button>
              <button class="anime-tab" :class="{ active: groupsTab === 'wx' }" @click="groupsTab = 'wx'">
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
    <div v-if="showKbModal" class="anime-modal-overlay" @click.self="showKbModal = false">
      <div class="anime-modal" style="max-width: 600px;">
        <div class="anime-modal-header">
          <span>知识库列表 - {{ selectedApp?.name }} ({{ selectedCategory?.name }})</span>
          <button class="anime-modal-close" @click="showKbModal = false">✕</button>
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
import { RouterLink } from 'vue-router'
import { Plus, RefreshCw, Edit3, Trash2, Star, Users, BookOpen, ExternalLink, CheckCircle, XCircle, AlertCircle, User } from 'lucide-vue-next'
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

const appList = ref<KbApp[]>([])
const categoryList = ref<Category[]>([])
const kbList = ref<KbKnowledgeBase[]>([])
const loading = ref(true)
const err = ref('')
const showCreateModal = ref(false)
const showGroupsModal = ref(false)
const showKbModal = ref(false)
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
  const user = getCurrentUser()
  return user || '未知用户'
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
  editingApp.value = null
  verifiedInfo.value = null
  verifyError.value = ''
  form.value = { name: '', description: '', icon: '', difyApiKey: '', categoryId: '', isPublic: true }
  err.value = ''
}

onMounted(() => {
  loadApps()
  loadCategories()
  loadKbList()
})
</script>

<style scoped>
.app-card {
  padding: 20px;
  border: 2px solid var(--anime-border);
  background: var(--anime-bg-card);
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
  color: var(--anime-pink);
}

.app-badges {
  display: flex;
  gap: 6px;
}

.app-desc {
  font-size: 14px;
  color: var(--anime-text-muted);
  margin-bottom: 16px;
}

.app-info-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 16px;
  padding: 12px;
  background: rgba(168, 216, 234, 0.05);
  border-radius: var(--anime-radius-lg);
}

.info-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.info-label {
  color: var(--anime-text-muted);
  min-width: 80px;
}

.info-value {
  color: var(--anime-text-primary);
}

.info-muted {
  color: var(--anime-text-muted);
}

.dify-link {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--anime-blue);
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
  border-color: var(--anime-pink);
  color: var(--anime-pink);
}

.create-steps {
  margin-bottom: 20px;
  padding: 16px;
  background: rgba(168, 216, 234, 0.1);
  border-radius: var(--anime-radius-lg);
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
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--anime-pink);
  color: white;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 600;
}

.step-content {
  flex: 1;
}

.step-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--anime-text-primary);
  margin-bottom: 4px;
}

.step-desc {
  font-size: 13px;
  color: var(--anime-text-muted);
  margin-bottom: 8px;
}

.step-btn {
  margin-top: 4px;
}

.step-hint {
  font-size: 13px;
  color: var(--anime-text-muted);
}

.api-key-actions {
  display: flex;
  gap: 10px;
  margin-top: 8px;
}

.verified-info {
  margin-top: 12px;
  padding: 12px;
  background: rgba(184, 233, 148, 0.1);
  border: 2px solid var(--anime-green);
  border-radius: var(--anime-radius-lg);
}

.verified-success {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--anime-green);
  font-weight: 600;
  margin-bottom: 8px;
}

.verified-error {
  margin-top: 12px;
  padding: 10px;
  background: rgba(255, 183, 197, 0.1);
  border: 2px solid var(--anime-pink);
  border-radius: var(--anime-radius-lg);
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--anime-pink);
}

.verified-warning {
  margin-top: 8px;
  padding: 8px;
  background: rgba(255, 200, 50, 0.1);
  border-radius: var(--anime-radius-lg);
  display: flex;
  align-items: center;
  gap: 6px;
  color: #e6a700;
  font-size: 13px;
}

.form-hint {
  font-size: 12px;
  color: var(--anime-text-muted);
  margin-top: 4px;
}

.creator-display {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: rgba(168, 216, 234, 0.1);
  border: 2px solid var(--anime-border);
  border-radius: var(--anime-radius-lg);
  font-weight: 600;
  color: var(--anime-text-primary);
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
  padding: 12px;
  background: rgba(255, 183, 197, 0.05);
  border: 2px solid var(--anime-border);
  border-radius: var(--anime-radius-lg);
  margin-bottom: 8px;
}

.group-name {
  font-weight: 600;
  color: var(--anime-text-primary);
  flex: 1;
}

.group-members {
  font-size: 13px;
  color: var(--anime-text-muted);
}

.kb-list-modal {
  max-height: 400px;
  overflow-y: auto;
}

.kb-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: rgba(168, 216, 234, 0.05);
  border: 2px solid var(--anime-border);
  border-radius: var(--anime-radius-lg);
  margin-bottom: 8px;
}

.kb-title {
  font-weight: 600;
  color: var(--anime-text-primary);
  flex: 1;
}

.anime-btn.sm {
  padding: 4px 10px;
  font-size: 13px;
}
</style>