<template>
  <div class="anime-page-shell">
    <section class="anime-card">
      <div class="anime-card-header">
        <div>
          <h1 class="anime-card-title">私聊采集</h1>
          <div class="anime-card-desc">
            QQ/企微/微信单聊消息采集记录 · 应用与本机执行节点绑定
          </div>
        </div>
        <div class="anime-card-actions">
          <button class="anime-btn ghost" :disabled="loading" @click="reload">
            <RefreshCw :size="18" :class="{ 'animate-spin': loading }" />
            <span>刷新数据</span>
          </button>
        </div>
      </div>

      <div v-if="err" class="anime-error im-page-error" role="alert">{{ err }}</div>

      <div class="anime-card-body">
        <section v-if="isAdmin" class="node-panel">
          <div class="node-panel-heading">
            <div>
              <h2 class="node-panel-title">本机 OpenCode 节点</h2>
              <div class="anime-card-desc">电脑主动连接服务器；配对后由管理员批准，再绑定到指定私聊。</div>
            </div>
            <div class="node-panel-actions">
              <span class="anime-badge node-stat node-stat--online">在线 {{ onlineNodeCount }} / {{ nodes.length }}</span>
              <span class="anime-badge node-stat node-stat--pending">待批准 {{ pendingNodeCount }}</span>
              <button class="anime-btn ghost" :disabled="nodeLoading" @click="loadNodes">刷新节点</button>
              <button class="anime-btn primary" :disabled="pairingLoading" @click="makePairingCode">
                {{ pairingLoading ? '生成中...' : '生成配对码' }}
              </button>
            </div>
          </div>
          <div class="im-live-region" role="status" aria-live="polite" aria-atomic="true">{{ nodeActionMessage }}</div>
          <div v-if="pairingCode" class="pairing-code-box">
            <div>
              <strong>一次性配对码</strong>
              <div class="pairing-code">{{ pairingCode.code }}</div>
              <small>有效期至 {{ formatTime(pairingCode.expiresAt) }}。在目标电脑设置 OPENCODE_PAIR_CODE 后启动 Agent。</small>
            </div>
            <button class="anime-btn ghost" @click="copyPairingCode">{{ pairingCopied ? '已复制' : '复制' }}</button>
          </div>
          <div v-if="nodes.length" class="node-grid">
            <article v-for="node in nodes" :key="node.id" class="node-card">
              <div class="node-card-top">
                <div>
                  <strong>{{ node.name }}</strong>
                  <div class="node-meta">{{ node.platform || '未知平台' }} · Agent {{ node.agentVersion || '未知版本' }}</div>
                </div>
                <span
                  class="anime-badge node-status"
                  :class="{
                    'node-status--online': node.online && node.status === 'APPROVED' && !node.capabilitiesPending,
                    'node-status--offline': node.status === 'APPROVED' && !node.online && !node.capabilitiesPending,
                    'node-status--pending': node.status === 'PENDING' || node.capabilitiesPending,
                    'node-status--revoked': node.status === 'REVOKED' || node.status === 'REJECTED'
                  }"
                >{{ nodeStatusLabel(node) }}</span>
              </div>
              <div class="node-meta">最近心跳：{{ node.lastSeenAt ? formatTime(node.lastSeenAt) : '尚未连接' }}</div>
              <div class="node-capabilities">能力：{{ node.approvedCapabilities.length ? node.approvedCapabilities.join('、') : '未批准' }}</div>
              <div v-if="node.capabilitiesPending" class="node-warning">节点新增了待审批能力，已批准能力仍可继续使用。</div>
              <div class="node-card-actions">
                <button v-if="node.status === 'PENDING' || node.capabilitiesPending" class="anime-btn primary" :disabled="nodeActionId === node.id" @click="approveNode(node)">批准能力</button>
                <button v-if="node.status === 'PENDING'" class="anime-btn ghost" :disabled="nodeActionId === node.id" @click="rejectNode(node)">拒绝</button>
                <button
                  class="anime-btn ghost"
                  :aria-label="`重命名节点 ${node.name}`"
                  :disabled="nodeActionId === node.id"
                  :ref="element => setNodeRenameButtonRef(node.id, element)"
                  @click="renameNode(node)"
                >重命名</button>
                <button v-if="node.status !== 'REVOKED'" class="anime-btn danger" :disabled="nodeActionId === node.id" @click="revokeNode(node)">撤销</button>
              </div>
            </article>
          </div>
          <div v-else-if="!nodeLoading" class="node-empty">还没有配对节点。生成配对码后，在需要执行任务的电脑上启动 Agent。</div>
        </section>

        <div class="im-toolbar">
          <select v-model="scope" class="anime-scope-select im-scope-select" aria-label="私聊范围">
            <option value="all">全部</option>
            <option value="unassigned">未分配</option>
            <option value="assigned">已分配</option>
            <option value="bound">已绑定</option>
          </select>
          <div class="anime-tabs" role="group" aria-label="按平台筛选私聊">
            <button v-for="t in platformTabs" :key="t.key" class="anime-tab" :class="{ active: platform === t.key }" :aria-pressed="platform === t.key" @click="togglePlatform(t.key)">{{ t.label }}</button>
          </div>
          <input v-model="convSearch" type="text" aria-label="搜索昵称或会话ID" placeholder="搜索昵称或会话ID..." class="anime-search-input im-list-search" />
        </div>

        <div class="im-split-view">
          <div class="im-list-panel">
            <div v-if="loading && conversations.length === 0" class="anime-empty">
              <span class="anime-loader-spinner"></span>
              <span class="anime-empty-text">加载中...</span>
            </div>
            <div v-else-if="conversations.length === 0" class="anime-empty">
              <div class="anime-empty-text">暂无私聊记录</div>
            </div>
            <button v-for="c in filteredConversations" :key="c.platform + ':' + c.conversationId" class="im-group-item" :class="{ 'active': selected?.conversationId === c.conversationId && selected?.platform === c.platform }" :aria-current="selected?.conversationId === c.conversationId && selected?.platform === c.platform ? 'true' : undefined" @click="selectConversation(c)">
              <div class="im-item-heading">
                <span class="anime-badge" :class="getPlatformColor(c.platform)">{{ getPlatformLabel(c.platform) }}</span>
                <span class="im-item-name">{{ getConvName(c) }}</span>
                <span v-if="c.appName" class="anime-badge blue">{{ c.appName }}</span>
              </div>
              <div class="im-last-message">
                {{ c.lastMessage || '暂无消息' }}
              </div>
              <div class="im-item-meta">
                <span class="im-message-count">{{ c.messageCount }} 条消息</span>
                <span v-if="c.lastMessageTime">{{ formatTime(c.lastMessageTime) }}</span>
              </div>
              <div class="im-item-owner">
                <span v-if="c.createdBy">归属: {{ c.createdBy }}</span>
              </div>
            </button>
          </div>

          <div class="im-detail-panel">
            <template v-if="!selected">
              <div class="anime-empty">
                <div class="anime-empty-text">请选择左侧私聊查看消息</div>
              </div>
            </template>
            <template v-else>
              <div class="im-detail-heading">
                <span class="anime-badge" :class="getPlatformColor(selected.platform)">{{ getPlatformLabel(selected.platform) }}</span>
                <span class="im-detail-title">{{ getConvName(selected) }}</span>
                <span class="im-conversation-id">会话ID: {{ selected.conversationId }}</span>
              </div>

              <div v-if="!selected.createdBy || selected.createdBy === currentUserName" class="im-binding-panel">
                <div class="im-binding-title">消息执行端</div>
                <div class="im-binding-controls">
                  <select v-model="bindingTarget" class="anime-app-select im-binding-select" aria-label="选择私聊消息执行端">
                    <option value="none">不绑定应用</option>
                    <option v-if="isAdmin" value="legacy-opencode">🖥️ 旧版 OpenCode 通道</option>
                    <option v-for="node in approvedNodes" :key="node.id" :value="`node:${node.id}`">🖥️ {{ node.name }} · {{ node.online ? '在线' : '离线' }}</option>
                    <option v-if="selected.opencodeNodeId && !findNode(selected.opencodeNodeId)" :value="`node:${selected.opencodeNodeId}`">已撤销节点 #{{ selected.opencodeNodeId }}</option>
                    <option v-for="app in appList" :key="app.id" :value="`app:${app.id}`">{{ app.icon || '🤖' }} {{ app.name }}</option>
                  </select>
                  <button class="anime-btn primary" @click="saveBinding" :disabled="bindSaving">
                    <span v-if="bindSaving">保存中...</span>
                    <span v-else>保存绑定</span>
                  </button>
                </div>
                <div v-if="selected.appName" class="im-current-binding">
                  当前绑定：<span>{{ selected.appName }}</span>
                </div>
                <div v-if="selected.appId === -1 && !selected.opencodeNodeId" class="im-legacy-note">
                  该会话使用原有 OpenCode 直连/桥接配置。
                </div>
                <div v-if="selected.opencodeNodeId" class="node-warning">
                  当前绑定节点：{{ findNode(selected.opencodeNodeId)?.name || `#${selected.opencodeNodeId}` }}（{{ selectedNodeStatus }}）。节点离线或能力未批准时消息会立即失败。
                </div>
              </div>

              <div class="im-message-toolbar">
                <input v-model="searchKeyword" type="text" aria-label="搜索私聊消息内容或用户ID" placeholder="搜索消息内容或用户ID..." class="anime-search-input im-message-search" @keyup.enter="searchMessages" />
                <button class="anime-btn ghost" @click="searchMessages" :disabled="msgLoading">
                  <span>搜索</span>
                </button>
                <button v-if="searchKeyword" class="anime-btn ghost" @click="clearSearch">
                  <span>清除</span>
                </button>
              </div>

              <div v-if="msgLoading" class="anime-empty">
                <span class="anime-loader-spinner"></span>
                <span class="anime-empty-text">消息加载中...</span>
              </div>
              <div v-else-if="messages.length === 0" class="anime-empty">
                <div class="anime-empty-text">{{ searchKeyword ? '未找到匹配消息' : '暂无消息记录' }}</div>
              </div>
              <div v-else class="message-list">
                <div v-for="m in messages" :key="m.id" class="im-message-card">
                  <div class="im-message-meta">
                    <span class="anime-badge blue">用户: {{ m.userId || '未知' }}</span>
                    <span class="anime-badge muted">{{ m.messageType || '文本' }}</span>
                    <span class="anime-badge im-sync-state" :class="m.synced ? 'im-sync-state--synced' : 'im-sync-state--unsynced'">{{ m.synced ? '已同步' : '未同步' }}</span>
                    <span class="im-message-time">{{ formatTime(m.messageTime) }}</span>
                  </div>
                  <div class="im-message-body">{{ m.rawMessage }}</div>
                </div>
              </div>
              <div v-if="msgTotal > messages.length" class="im-pagination">
                <button class="anime-btn ghost" :disabled="msgLoading || page <= 0" @click="page--; loadMessages()">上一页</button>
                <span class="anime-code">第 {{ page + 1 }} 页 · 共 {{ msgTotal }} 条</span>
                <button class="anime-btn ghost" :disabled="msgLoading || (page + 1) * pageSize >= msgTotal" @click="page++; loadMessages()">下一页</button>
              </div>
            </template>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, computed, nextTick } from 'vue'
import { RefreshCw } from 'lucide-vue-next'
import { fetchConversations, fetchPrivateMessages } from '../api/console'
import {
  approveOpencodeNode,
  bindConversationNode,
  createPairingCode,
  fetchOpencodeNodes,
  rejectOpencodeNode,
  renameOpencodeNode,
  revokeOpencodeNode,
} from '../api/opencodeNodes'
import { api } from '../api/client'
import type { ConversationSummary, GroupMessageItem } from '../types/console'
import type { OpencodeNode, PairingCode } from '../types/opencodeNode'

interface KbApp {
  id: number
  name: string
  icon: string
  isDefault: boolean
}

const currentUserName = computed(() => localStorage.getItem('chatbase_original_username') || localStorage.getItem('chatbase_user') || '')
const isAdmin = computed(() => localStorage.getItem('chatbase_role') === 'admin')
const nodes = ref<OpencodeNode[]>([])
const nodeLoading = ref(false)
const pairingLoading = ref(false)
const pairingCode = ref<PairingCode | null>(null)
const pairingCopied = ref(false)
const nodeActionId = ref<number | null>(null)
const nodeActionMessage = ref('')
const nodeRenameButtons = new Map<number, HTMLElement>()
const bindingTarget = ref('none')
const approvedNodes = computed(() => nodes.value.filter(node => node.status === 'APPROVED'))
const onlineNodeCount = computed(() => nodes.value.filter(node => node.online).length)
const pendingNodeCount = computed(() => nodes.value.filter(node => node.status === 'PENDING' || node.capabilitiesPending).length)
const selectedNodeStatus = computed(() => {
  if (!selected.value?.opencodeNodeId) return '未绑定'
  const node = findNode(selected.value.opencodeNodeId)
  return node ? nodeStatusLabel(node) : '节点不存在'
})

const filteredConversations = computed(() => {
  const q = convSearch.value.trim().toLowerCase()
  if (!q) return conversations.value
  return conversations.value.filter(c => {
    const id = String(c.conversationId ?? '').toLowerCase()
    const name = String(c.userNickname ?? c.title ?? '').toLowerCase()
    const userId = String(c.userId ?? '').toLowerCase()
    return id.includes(q) || name.includes(q) || userId.includes(q)
  })
})

const platformTabs = [{ key: 'qq', label: 'QQ' }, { key: 'wecom', label: '企微' }, { key: 'wx', label: '微信' }]
const platform = ref<string | null>(null)
const scope = ref('all')
const convSearch = ref('')
const conversations = ref<ConversationSummary[]>([])
const loading = ref(false)
const err = ref<string | null>(null)

const selected = ref<ConversationSummary | null>(null)
const messages = ref<GroupMessageItem[]>([])
const msgLoading = ref(false)
const msgTotal = ref(0)
const page = ref(0)
const pageSize = 40
const appList = ref<KbApp[]>([])
const bindSaving = ref(false)
const searchKeyword = ref('')

async function loadApps() {
  try {
    const resp = await api.get('/kb/app/list')
    appList.value = resp.data || []
  } catch (e) {
    console.error('加载应用列表失败', e)
  }
}

async function reload() {
  loading.value = true
  err.value = null
  try {
    const all = await fetchConversations()
    conversations.value = applyFilters(all)
    if (selected.value) {
      const still = conversations.value.find((c) => c.conversationId === selected.value!.conversationId && c.platform === selected.value!.platform)
      if (!still) { selected.value = null; messages.value = [] }
      else { selected.value = still; setBindingTarget(still) }
    }
  } catch (e: any) { err.value = e?.message || '加载失败'; conversations.value = [] }
  finally { loading.value = false }
}

function applyFilters(list: ConversationSummary[]): ConversationSummary[] {
  let result = list
  if (platform.value) {
    result = result.filter(c => c.platform === platform.value)
  }
  if (scope.value === 'unassigned') {
    result = result.filter(c => !c.createdBy)
  } else if (scope.value === 'bound') {
    // 「已绑定」= 已绑定应用（appId），与「已分配」（createdBy）区分
    result = result.filter(c => c.appId || c.opencodeNodeId)
  } else if (scope.value === 'assigned') {
    result = result.filter(c => c.createdBy)
  }
  return result
}

function selectConversation(c: ConversationSummary) {
  selected.value = c
  setBindingTarget(c)
  page.value = 0
  loadMessages()
}

async function loadMessages() {
  if (!selected.value) return
  msgLoading.value = true
  try {
    const res = await fetchPrivateMessages({
      conversationId: selected.value.conversationId,
      page: page.value,
      size: pageSize,
      keyword: searchKeyword.value || undefined
    })
    messages.value = res.records
    msgTotal.value = res.total
  } catch { messages.value = []; msgTotal.value = 0 }
  finally { msgLoading.value = false }
}

function searchMessages() {
  page.value = 0
  loadMessages()
}

function clearSearch() {
  searchKeyword.value = ''
  page.value = 0
  loadMessages()
}

function findNode(nodeId: number): OpencodeNode | undefined {
  return nodes.value.find(node => node.id === nodeId)
}

function setBindingTarget(conversation: ConversationSummary) {
  if (conversation.opencodeNodeId) bindingTarget.value = `node:${conversation.opencodeNodeId}`
  else if (conversation.appId === -1) bindingTarget.value = 'legacy-opencode'
  else if (conversation.appId) bindingTarget.value = `app:${conversation.appId}`
  else bindingTarget.value = 'none'
}

function nodeStatusLabel(node: OpencodeNode): string {
  if (node.status === 'REVOKED') return '已撤销'
  if (node.status === 'REJECTED') return '已拒绝'
  if (node.status === 'PENDING') return '待批准'
  if (node.capabilitiesPending) return node.online ? '在线 · 有待审能力' : '离线 · 有待审能力'
  return node.online ? '在线' : '离线'
}

function setNodeRenameButtonRef(nodeId: number, element: unknown) {
  if (typeof HTMLElement !== 'undefined' && element instanceof HTMLElement) {
    nodeRenameButtons.set(nodeId, element)
  } else {
    nodeRenameButtons.delete(nodeId)
  }
}

async function focusNodeRenameButton(nodeId: number) {
  await nextTick()
  nodeRenameButtons.get(nodeId)?.focus()
}

async function announceNodeAction(nodeId: number, message: string) {
  nodeActionMessage.value = message
  await focusNodeRenameButton(nodeId)
}

async function loadNodes() {
  if (!isAdmin.value) return
  nodeLoading.value = true
  try { nodes.value = await fetchOpencodeNodes() }
  catch (e: any) { err.value = e?.message || '节点列表加载失败' }
  finally { nodeLoading.value = false }
}

async function makePairingCode() {
  pairingLoading.value = true
  err.value = null
  nodeActionMessage.value = ''
  try {
    pairingCode.value = await createPairingCode()
    pairingCopied.value = false
    nodeActionMessage.value = '配对码已生成'
  }
  catch (e: any) { err.value = e?.message || '配对码生成失败' }
  finally { pairingLoading.value = false }
}

async function copyPairingCode() {
  if (!pairingCode.value) return
  nodeActionMessage.value = ''
  try {
    await navigator.clipboard.writeText(pairingCode.value.code)
    pairingCopied.value = true
    err.value = null
    nodeActionMessage.value = '配对码已复制'
  } catch { err.value = '复制失败，请手动复制配对码' }
}

async function approveNode(node: OpencodeNode) {
  nodeActionMessage.value = ''
  nodeActionId.value = node.id
  let succeeded = false
  try {
    await approveOpencodeNode(node.id)
    await loadNodes()
    succeeded = true
  }
  catch (e: any) { err.value = e?.message || '节点批准失败' }
  finally { nodeActionId.value = null }
  if (succeeded) await announceNodeAction(node.id, `已批准节点“${node.name}”`)
  else await focusNodeRenameButton(node.id)
}

async function rejectNode(node: OpencodeNode) {
  if (!window.confirm(`确定拒绝节点“${node.name}”吗？`)) return
  nodeActionMessage.value = ''
  nodeActionId.value = node.id
  let succeeded = false
  try {
    await rejectOpencodeNode(node.id)
    await loadNodes()
    succeeded = true
  }
  catch (e: any) { err.value = e?.message || '节点拒绝失败' }
  finally { nodeActionId.value = null }
  if (succeeded) await announceNodeAction(node.id, `已拒绝节点“${node.name}”`)
  else await focusNodeRenameButton(node.id)
}

async function renameNode(node: OpencodeNode) {
  const name = window.prompt('输入新的节点名称', node.name)?.trim()
  if (!name || name === node.name) return
  nodeActionMessage.value = ''
  nodeActionId.value = node.id
  let succeeded = false
  try {
    await renameOpencodeNode(node.id, name)
    await loadNodes()
    succeeded = true
  }
  catch (e: any) { err.value = e?.message || '节点重命名失败' }
  finally { nodeActionId.value = null }
  if (succeeded) await announceNodeAction(node.id, `节点已重命名为“${name}”`)
  else await focusNodeRenameButton(node.id)
}

async function revokeNode(node: OpencodeNode) {
  if (!window.confirm(`撤销“${node.name}”后，该节点将立即断开，相关私聊停止执行。继续吗？`)) return
  nodeActionMessage.value = ''
  nodeActionId.value = node.id
  let succeeded = false
  try {
    await revokeOpencodeNode(node.id)
    await loadNodes()
    succeeded = true
  }
  catch (e: any) { err.value = e?.message || '节点撤销失败' }
  finally { nodeActionId.value = null }
  if (succeeded) await announceNodeAction(node.id, `已撤销节点“${node.name}”`)
  else await focusNodeRenameButton(node.id)
}

async function saveBinding() {
  if (!selected.value) return
  bindSaving.value = true
  err.value = null
  try {
    const target = bindingTarget.value
    if (target.startsWith('node:')) {
      if (!isAdmin.value) throw new Error('仅管理员可绑定 OpenCode 节点')
      const nodeId = Number(target.slice(5))
      await bindConversationNode(selected.value.id, nodeId)
      selected.value.opencodeNodeId = nodeId
      selected.value.appId = -1
      selected.value.appName = '本地opencode'
    } else if (target === 'legacy-opencode') {
      if (!isAdmin.value) throw new Error('仅管理员可绑定本机 OpenCode')
      if (selected.value.opencodeNodeId) {
        await api.delete(`/console/conversations/${selected.value.id}/opencode-node`)
      } else {
        await api.put(`/console/conversations/${selected.value.id}/app`, { appId: -1, appName: '本地opencode' })
      }
      selected.value.opencodeNodeId = null
      selected.value.appId = -1
      selected.value.appName = '本地opencode'
    } else if (target === 'none') {
      await api.delete(`/console/conversations/${selected.value.id}/app`)
      selected.value.opencodeNodeId = null
      selected.value.appId = null
      selected.value.appName = null
    } else {
      const appId = Number(target.slice(4))
      const appName = appList.value.find(app => app.id === appId)?.name || null
      await api.put(`/console/conversations/${selected.value.id}/app`, { appId, appName })
      selected.value.opencodeNodeId = null
      selected.value.appId = appId
      selected.value.appName = appName
    }
    await reload()
  } catch (e: any) {
    err.value = e?.message || e?.response?.data?.message || '绑定失败'
  } finally {
    bindSaving.value = false
  }
}

function togglePlatform(key: string) {
  platform.value = platform.value === key ? null : key
}

function getPlatformColor(p: string): string { if (p === 'qq') return 'green'; if (p === 'wecom') return 'blue'; if (p === 'wx') return 'purple'; return 'purple' }
function getPlatformLabel(p: string): string { if (p === 'qq') return 'QQ'; if (p === 'wecom') return '企微'; if (p === 'wx') return '微信'; return '私聊' }
function getConvName(c: ConversationSummary): string {
  if (c.userNickname && c.userNickname.trim()) return c.userNickname.trim()
  if (c.title && c.title.trim()) return c.title.trim()
  return c.userId || c.conversationId
}
function formatTime(t: any): string {
  if (!t) return ''
  if (Array.isArray(t) && t.length >= 5) {
    const [y, m, d, h = 0, min = 0] = t
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(h).padStart(2, '0')}:${String(min).padStart(2, '0')}`
  }
  if (typeof t === 'string') return t.slice(0, 16).replace('T', ' ')
  return String(t)
}

watch([platform, scope], () => { selected.value = null; messages.value = []; reload() })
onMounted(() => {
  reload()
  loadApps()
  loadNodes()
})
</script>

<style scoped>
@keyframes spin { to { transform: rotate(360deg); } }
.animate-spin { animation: spin 1s linear infinite; }
.node-panel { margin: 0 0 22px; padding: 18px; border: 1px solid var(--ui-border); border-radius: var(--ui-radius-lg); background: var(--ui-surface-muted); }
.node-panel-heading, .node-card-top { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.node-panel-title { margin: 0 0 4px; color: var(--ui-text); font-size: 16px; font-weight: 700; }
.im-live-region { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); clip-path: inset(50%); white-space: nowrap; border: 0; }
.node-panel-actions { display: flex; align-items: center; justify-content: flex-end; flex-wrap: wrap; gap: 8px; margin-left: auto; }
.node-stat, .node-status { border: 1px solid transparent; }
.node-stat--online, .node-status--online { background: color-mix(in srgb, var(--ui-success) 12%, var(--ui-surface)); color: var(--ui-success); }
.node-stat--pending, .node-status--pending { background: color-mix(in srgb, var(--ui-accent) 14%, var(--ui-surface)); color: color-mix(in srgb, var(--ui-accent) 55%, var(--ui-text)); }
.node-status--offline { background: var(--ui-surface-muted); color: var(--ui-text-secondary); border-color: var(--ui-border); }
.node-status--revoked { background: color-mix(in srgb, var(--ui-danger) 8%, var(--ui-surface)); color: var(--ui-danger); }
.pairing-code-box { display: flex; align-items: center; justify-content: space-between; gap: 14px; flex-wrap: wrap; margin-top: 14px; padding: 14px; border: 1px dashed var(--ui-border); border-radius: var(--ui-radius-md); background: var(--ui-surface); color: var(--ui-text); }
.pairing-code-box > div { flex: 1 1 240px; min-width: 0; }
.pairing-code { margin: 8px 0; color: var(--ui-primary); font: 700 18px ui-monospace, SFMono-Regular, Consolas, monospace; letter-spacing: .08em; overflow-wrap: anywhere; }
.pairing-code-box small, .node-meta { color: var(--ui-text-muted); font-size: 12px; line-height: 1.5; }
.node-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 250px), 1fr)); gap: 12px; margin-top: 14px; }
.node-card { display: flex; flex-direction: column; gap: 10px; min-width: 0; padding: 14px; border: 1px solid var(--ui-border); border-radius: var(--ui-radius-md); background: var(--ui-surface); }
.node-card-top > div:first-child { min-width: 0; }
.node-card-top strong { color: var(--ui-text); overflow-wrap: anywhere; }
.node-capabilities { color: var(--ui-text-secondary); font-size: 12px; line-height: 1.5; overflow-wrap: anywhere; }
.node-card-actions { display: flex; align-items: center; justify-content: flex-start; flex-wrap: wrap; gap: 7px; margin-top: auto; }
.node-card-actions .anime-btn { flex: 1 1 82px; min-width: 0; padding: 7px 9px; font-size: 12px; white-space: nowrap; }
.anime-btn.danger { border: 1px solid color-mix(in srgb, var(--ui-danger) 34%, var(--ui-border)); background: color-mix(in srgb, var(--ui-danger) 5%, var(--ui-surface)); color: var(--ui-danger); }
.anime-btn.danger:hover { border-color: var(--ui-danger); background: color-mix(in srgb, var(--ui-danger) 10%, var(--ui-surface)); }
.node-warning { margin-top: 4px; padding: 9px 11px; border: 1px solid color-mix(in srgb, var(--ui-accent) 34%, var(--ui-border)); border-radius: var(--ui-radius-sm); background: color-mix(in srgb, var(--ui-accent) 11%, var(--ui-surface)); color: color-mix(in srgb, var(--ui-accent) 55%, var(--ui-text)); font-size: 12px; line-height: 1.5; overflow-wrap: anywhere; }
.node-empty { margin-top: 14px; padding: 18px; border: 1px dashed var(--ui-border); border-radius: var(--ui-radius-md); background: var(--ui-surface); color: var(--ui-text-muted); text-align: center; }
.im-page-error { margin: 0 24px 16px; }
.im-toolbar, .im-message-toolbar, .im-binding-controls { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; }
.im-toolbar { margin-bottom: 14px; }
.im-toolbar .anime-tabs { margin: 0; }
.im-scope-select { flex: 0 0 auto; min-width: 112px; }
.im-list-search { flex: 1 1 220px; min-width: min(220px, 100%); }
.im-split-view { display: grid; grid-template-columns: minmax(240px, 320px) minmax(0, 1fr); min-height: 480px; overflow: hidden; border: 1px solid var(--ui-border); border-radius: var(--ui-radius-md); background: var(--ui-surface); }
.im-list-panel { min-width: 0; max-height: min(70vh, 600px); overflow-y: auto; padding: 12px; border-right: 1px solid var(--ui-border); }
.im-group-item { display: flex; flex-direction: column; gap: 8px; width: 100%; min-width: 0; padding: 12px; margin: 0 0 8px; border: 1px solid var(--ui-border); border-radius: var(--ui-radius-md); background: var(--ui-surface); color: var(--ui-text); font: inherit; text-align: left; cursor: pointer; transition: border-color .16s ease, background-color .16s ease; }
.im-group-item:hover { border-color: color-mix(in srgb, var(--ui-primary) 45%, var(--ui-border)); background: var(--ui-surface-muted); }
.im-group-item.active { border-color: var(--ui-primary); background: var(--ui-primary-soft); box-shadow: inset 3px 0 var(--ui-primary); }
.im-item-heading, .im-item-meta { display: flex; align-items: center; gap: 8px; min-width: 0; }
.im-item-heading { flex-wrap: wrap; }
.im-item-name { flex: 1 1 90px; min-width: 0; color: var(--ui-text); font-weight: 650; overflow-wrap: anywhere; }
.im-item-meta { justify-content: space-between; color: var(--ui-text-muted); font-size: 12px; }
.im-message-count { color: var(--ui-primary); font-weight: 600; }
.im-item-owner { min-height: 16px; color: var(--ui-text-muted); font-size: 12px; }
.im-last-message { overflow: hidden; color: var(--ui-text-secondary); font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.im-detail-panel { display: flex; min-width: 0; max-height: min(70vh, 600px); flex-direction: column; gap: 14px; overflow-y: auto; padding: 18px; }
.im-detail-heading { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; }
.im-detail-title { min-width: 0; color: var(--ui-text); font-weight: 650; overflow-wrap: anywhere; }
.im-conversation-id { min-width: 0; color: var(--ui-text-muted); font-size: 12px; overflow-wrap: anywhere; }
.im-binding-panel { margin-bottom: 2px; padding: 14px; border: 1px solid var(--ui-border); border-radius: var(--ui-radius-md); background: var(--ui-surface-muted); }
.im-binding-title { margin-bottom: 10px; color: var(--ui-text-secondary); font-size: 13px; font-weight: 700; }
.im-binding-controls { align-items: stretch; }
.im-binding-select { flex: 1 1 220px; min-width: min(200px, 100%); }
.im-current-binding { margin-top: 9px; color: var(--ui-text-muted); font-size: 12px; overflow-wrap: anywhere; }
.im-current-binding span { color: var(--ui-primary); font-weight: 600; }
.im-legacy-note { margin-top: 8px; color: var(--ui-text-muted); font-size: 12px; line-height: 1.5; }
.im-message-search { flex: 1 1 220px; min-width: min(220px, 100%); }
.im-message-toolbar .anime-btn { flex: 0 0 auto; }
.message-list { display: flex; min-width: 0; flex-direction: column; gap: 9px; }
.im-message-card { min-width: 0; padding: 12px; border: 1px solid var(--ui-border); border-radius: var(--ui-radius-md); background: var(--ui-surface); }
.im-message-meta { display: flex; align-items: center; flex-wrap: wrap; gap: 7px; margin-bottom: 8px; }
.im-message-time { color: var(--ui-text-muted); font-size: 12px; }
.im-message-body { color: var(--ui-text); font-size: 14px; line-height: 1.6; white-space: pre-wrap; overflow-wrap: anywhere; }
.im-sync-state--synced { background: color-mix(in srgb, var(--ui-success) 12%, var(--ui-surface)); color: var(--ui-success); }
.im-sync-state--unsynced { background: color-mix(in srgb, var(--ui-accent) 14%, var(--ui-surface)); color: color-mix(in srgb, var(--ui-accent) 55%, var(--ui-text)); }
.im-pagination { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; padding-top: 12px; border-top: 1px solid var(--ui-border); }
.anime-app-select, .anime-search-input, .anime-scope-select { min-height: 38px; padding: 8px 11px; border: 1px solid var(--ui-border); border-radius: var(--ui-radius-sm); background: var(--ui-surface); color: var(--ui-text); font: inherit; box-shadow: none; }
.anime-app-select:focus, .anime-search-input:focus, .anime-scope-select:focus { border-color: var(--ui-focus); outline: 2px solid color-mix(in srgb, var(--ui-focus) 18%, transparent); }
@media (max-width: 800px) {
  .im-split-view { grid-template-columns: minmax(0, 1fr); }
  .im-list-panel { max-height: min(34vh, 280px); border-right: 0; border-bottom: 1px solid var(--ui-border); }
  .im-detail-panel { max-height: min(70vh, 680px); }
}
@media (max-width: 480px) {
  .im-page-error { margin: 0 14px 14px; }
  .node-panel { padding: 14px; }
  .node-panel-actions { justify-content: flex-start; margin-left: 0; }
  .node-panel-actions .anime-btn { flex: 1 1 132px; }
  .node-card-actions .anime-btn { flex-basis: 82px; }
  .pairing-code-box > div, .pairing-code-box > button { flex: 1 1 100%; }
  .im-toolbar { align-items: stretch; }
  .im-toolbar .anime-tabs, .im-scope-select, .im-list-search { flex: 1 1 100%; }
  .im-detail-panel { padding: 14px; }
  .im-binding-controls > * { flex: 1 1 100%; }
  .im-message-toolbar .anime-btn { flex: 1 1 auto; }
}
</style>
