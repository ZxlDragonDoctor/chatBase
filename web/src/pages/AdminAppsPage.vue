<template>
  <div class="anime-page-shell">
    <section class="anime-card">
      <div class="anime-card-header">
        <div>
          <div class="anime-card-title">应用管理</div>
          <div class="anime-card-desc">全部应用 · 含私有应用 · 仅管理员可见</div>
        </div>
      </div>

      <div v-if="err" class="anime-error" style="margin: 16px 28px;">{{ err }}</div>

      <div class="anime-card-body">
        <div class="filter-bar">
          <Search :size="16" class="search-icon" />
          <input v-model="keyword" class="anime-input" placeholder="搜索应用名称、创建者..." style="max-width: 300px;" />
          <span class="filter-count">共 {{ filteredList.length }} 个应用</span>
          <button class="anime-btn ghost" :disabled="loading" @click="loadApps">
            <RefreshCw :size="16" />
            <span>刷新</span>
          </button>
        </div>

        <div v-if="loading" class="anime-empty">
          <span class="anime-loader-spinner"></span>
          <span class="anime-empty-text">加载中...</span>
        </div>

        <div v-else-if="filteredList.length === 0" class="anime-empty">
          <div class="anime-empty-icon">🤖</div>
          <div class="anime-empty-text">{{ keyword ? '未找到匹配的应用' : '暂无应用' }}</div>
        </div>

        <div v-else class="kb-grid">
          <div v-for="app in filteredList" :key="app.id" class="anime-card app-card" @click="openDetail(app.id)">
            <div class="app-header">
              <div class="app-title-row">
                <span class="app-icon">{{ app.icon || '🤖' }}</span>
                <span class="app-name">{{ app.name }}</span>
              </div>
              <div class="app-badges">
                <span class="anime-badge" :class="app.isPublic ? 'green' : 'gray'">{{ app.isPublic ? '公开' : '私有' }}</span>
                <span class="anime-badge" :class="app.status ? 'green' : 'pink'">{{ app.status ? '启用' : '禁用' }}</span>
              </div>
            </div>

            <div class="app-desc">{{ app.description || '无描述' }}</div>

            <div class="app-info-section">
              <div class="info-row">
                <span class="info-label">Dify应用:</span>
                <span class="anime-code">{{ app.difyAppName || '未验证' }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">创建者:</span>
                <span class="info-value">{{ app.createBy }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">创建时间:</span>
                <span class="info-value">{{ app.createTime }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <div v-if="showDetail && detail" class="anime-modal-overlay" @click.self="showDetail = false">
      <div class="anime-modal detail-modal">
        <div class="anime-modal-header">
          <span class="modal-title">应用详情 #{{ detail.id }}</span>
          <button class="anime-modal-close" @click="showDetail = false">✕</button>
        </div>
        <div class="anime-modal-body">
          <table class="detail-table">
            <tr>
              <td class="dt-label">名称</td>
              <td class="dt-value">{{ detail.name }}</td>
            </tr>
            <tr>
              <td class="dt-label">描述</td>
              <td class="dt-value">{{ detail.description || '无' }}</td>
            </tr>
            <tr>
              <td class="dt-label">图标</td>
              <td class="dt-value">{{ detail.icon || '🤖' }}</td>
            </tr>
            <tr>
              <td class="dt-label">Dify API Key</td>
              <td class="dt-value anime-code">{{ maskKey(detail.difyApiKey) }}</td>
            </tr>
            <tr>
              <td class="dt-label">Dify 应用名</td>
              <td class="dt-value">{{ detail.difyAppName || '未验证' }}</td>
            </tr>
            <tr>
              <td class="dt-label">Dify 模式</td>
              <td class="dt-value">{{ detail.difyAppMode || '-' }}</td>
            </tr>
            <tr>
              <td class="dt-label">公开</td>
              <td class="dt-value"><span class="anime-badge" :class="detail.isPublic ? 'green' : 'gray'">{{ detail.isPublic ? '是' : '否' }}</span></td>
            </tr>
            <tr>
              <td class="dt-label">默认</td>
              <td class="dt-value"><span class="anime-badge" :class="detail.isDefault ? 'pink' : 'muted'">{{ detail.isDefault ? '是' : '否' }}</span></td>
            </tr>
            <tr>
              <td class="dt-label">状态</td>
              <td class="dt-value"><span class="anime-badge" :class="detail.status ? 'green' : 'pink'">{{ detail.status ? '启用' : '禁用' }}</span></td>
            </tr>
            <tr>
              <td class="dt-label">创建者</td>
              <td class="dt-value">{{ detail.createBy || '-' }}</td>
            </tr>
            <tr>
              <td class="dt-label">创建时间</td>
              <td class="dt-value">{{ detail.createTime || '-' }}</td>
            </tr>
            <tr>
              <td class="dt-label">更新时间</td>
              <td class="dt-value">{{ detail.updateTime || '-' }}</td>
            </tr>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Search, RefreshCw } from 'lucide-vue-next'
import { api } from '../api/client'

interface KbAppItem {
  id: number
  name: string
  description: string
  icon: string
  difyAppName: string
  difyAppMode: string
  isPublic: boolean
  createBy: string
  status: boolean
  createTime: string
}

interface KbAppDetail extends KbAppItem {
  difyApiKey: string
  isDefault: boolean
  categoryId: number | null
  updateTime: string
}

const appList = ref<KbAppItem[]>([])
const loading = ref(true)
const err = ref('')
const keyword = ref('')
const showDetail = ref(false)
const detail = ref<KbAppDetail | null>(null)

const filteredList = computed(() => {
  const kw = keyword.value.toLowerCase().trim()
  if (!kw) return appList.value
  return appList.value.filter(a =>
    a.name.toLowerCase().includes(kw) ||
    a.description?.toLowerCase().includes(kw) ||
    a.createBy?.toLowerCase().includes(kw) ||
    a.difyAppName?.toLowerCase().includes(kw)
  )
})

function maskKey(key: string | null): string {
  if (!key) return '-'
  if (key.length <= 8) return '****'
  return key.slice(0, 4) + '****' + key.slice(-4)
}

async function openDetail(id: number) {
  try {
    const res = await api.get(`/kb/app/${id}`)
    detail.value = res.data
    showDetail.value = true
  } catch {
    err.value = '加载详情失败'
  }
}

async function loadApps() {
  loading.value = true
  err.value = ''
  try {
    const res = await api.get('/kb/app/admin/list')
    appList.value = res.data || []
  } catch (e: any) {
    err.value = e.response?.data?.message || '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(loadApps)
</script>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: rgba(168, 216, 234, 0.05);
  border-radius: var(--anime-radius-lg);
}
.search-icon { color: var(--anime-text-muted); flex-shrink: 0; }
.filter-count { font-size: 13px; color: var(--anime-text-muted); margin-left: auto; }
.app-card { padding: 20px; border: 2px solid var(--anime-border); background: var(--anime-bg-card); cursor: pointer; transition: all 0.2s; }
.app-card:hover { border-color: var(--anime-pink); box-shadow: var(--anime-shadow-soft); }
.app-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.app-title-row { display: flex; align-items: center; gap: 10px; }
.app-icon { font-size: 24px; }
.app-name { font-size: 18px; font-weight: 700; color: var(--anime-pink); }
.app-badges { display: flex; gap: 6px; }
.app-desc { font-size: 14px; color: var(--anime-text-muted); margin-bottom: 16px; }
.app-info-section { display: flex; flex-direction: column; gap: 10px; padding: 12px; background: rgba(168, 216, 234, 0.05); border-radius: var(--anime-radius-lg); }
.info-row { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.info-label { color: var(--anime-text-muted); min-width: 80px; }
.info-value { color: var(--anime-text-primary); }
.kb-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
.detail-modal { max-width: 560px; }
.detail-table { width: 100%; border-collapse: collapse; }
.detail-table tr { border-bottom: 1px solid var(--anime-border); }
.detail-table tr:last-child { border-bottom: none; }
.detail-table td { padding: 10px 8px; font-size: 14px; vertical-align: top; }
.dt-label { color: var(--anime-text-muted); width: 120px; font-weight: 600; white-space: nowrap; }
.dt-value { color: var(--anime-text-primary); word-break: break-all; }
</style>
