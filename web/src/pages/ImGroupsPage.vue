<template>
  <div class="pageShell">
    <section class="card imCard">
      <div class="cardHeader">
        <div>
          <div class="h1">群聊采集</div>
          <div class="muted">来自 <code>group_message</code> 表；点击群查看消息</div>
        </div>
        <div class="right">
          <button type="button" class="btn btnGhost" :disabled="loading" @click="reload">刷新</button>
        </div>
      </div>

      <div class="imToolbar">
        <div class="tabs">
          <button
            v-for="t in platformTabs"
            :key="t.key"
            type="button"
            class="tab"
            :class="{ active: platform === t.key }"
            @click="platform = t.key"
          >
            {{ t.label }}
          </button>
        </div>
      </div>

      <div v-if="err" class="error" style="margin: 0 18px 12px">{{ err }}</div>

      <div class="imSplit">
        <div class="imListPane">
          <div v-if="loading && groups.length === 0" class="muted" style="padding: 16px">加载中…</div>
          <div v-else-if="groups.length === 0" class="empty" style="margin: 12px">暂无群记录</div>
          <button
            v-for="g in groups"
            :key="g.platform + ':' + g.groupId"
            type="button"
            class="imRow"
            :class="{ active: selected?.groupId === g.groupId && selected?.platform === g.platform }"
            @click="selectGroup(g)"
          >
            <span class="imRowPlat">{{ platformLabel(g.platform) }}</span>
            <span class="imRowId">{{ g.groupId }}</span>
            <span class="imRowMeta">{{ g.messageCount }} 条</span>
          </button>
        </div>

        <div class="imDetailPane">
          <template v-if="!selected">
            <div class="muted" style="padding: 20px">请选择左侧群聊</div>
          </template>
          <template v-else>
            <div class="imDetailHead">
              <div class="h2" style="margin: 0">{{ platformLabel(selected.platform) }}</div>
              <code class="imDetailGid">{{ selected.groupId }}</code>
            </div>
            <div v-if="msgLoading" class="muted" style="padding: 12px">消息加载中…</div>
            <div v-else class="msgScroll">
              <div v-for="m in messages" :key="m.id" class="msgItem">
                <div class="msgItemTop">
                  <span class="badge">{{ m.messageType || 'text' }}</span>
                  <span v-if="m.synced != null" class="badge">{{ m.synced ? '已同步' : '未同步' }}</span>
                  <span class="muted">{{ m.messageTime || '' }}</span>
                  <span class="muted">用户 {{ m.userId || '—' }}</span>
                </div>
                <div class="msgItemBody">{{ m.rawMessage }}</div>
              </div>
            </div>
            <div v-if="msgTotal > messages.length" class="imPager">
              <button type="button" class="btn btnGhost" :disabled="msgLoading || page <= 0" @click="page--; loadMessages()">
                上一页
              </button>
              <span class="muted">第 {{ page + 1 }} 页 · 共 {{ msgTotal }} 条</span>
              <button
                type="button"
                class="btn btnGhost"
                :disabled="msgLoading || (page + 1) * pageSize >= msgTotal"
                @click="page++; loadMessages()"
              >
                下一页
              </button>
            </div>
          </template>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { fetchGroups, fetchGroupMessages } from '../api/console'
import type { GroupMessageItem, GroupSummary } from '../types/console'
import { platformLabel } from '../lib/platformLabel'

const platformTabs = [
  { key: 'all' as const, label: '全部' },
  { key: 'qq' as const, label: 'QQ 群' },
  { key: 'wecom' as const, label: '企微群聊' },
]

const platform = ref<'all' | 'qq' | 'wecom'>('all')
const groups = ref<GroupSummary[]>([])
const loading = ref(false)
const err = ref<string | null>(null)

const selected = ref<GroupSummary | null>(null)
const messages = ref<GroupMessageItem[]>([])
const msgLoading = ref(false)
const msgTotal = ref(0)
const page = ref(0)
const pageSize = 40

async function reload() {
  loading.value = true
  err.value = null
  try {
    groups.value = await fetchGroups(platform.value)
    if (selected.value) {
      const still = groups.value.find((g) => g.groupId === selected.value!.groupId && g.platform === selected.value!.platform)
      if (!still) {
        selected.value = null
        messages.value = []
      }
    }
  } catch (e: any) {
    err.value = e?.message || '加载失败'
    groups.value = []
  } finally {
    loading.value = false
  }
}

function selectGroup(g: GroupSummary) {
  selected.value = g
  page.value = 0
  loadMessages()
}

async function loadMessages() {
  if (!selected.value) return
  const gid = selected.value.groupId
  if (gid == null || String(gid).trim() === '') {
    messages.value = []
    msgTotal.value = 0
    return
  }
  msgLoading.value = true
  try {
    const apiPlat =
      selected.value.platform === 'qq' ? 'qq' : selected.value.platform === 'wx' ? 'wx' : 'all'
    const res = await fetchGroupMessages({
      groupId: gid,
      platform: apiPlat,
      page: page.value,
      size: pageSize,
    })
    messages.value = res.records
    msgTotal.value = res.total
  } catch {
    messages.value = []
    msgTotal.value = 0
  } finally {
    msgLoading.value = false
  }
}

watch(platform, () => {
  selected.value = null
  messages.value = []
  reload()
})

onMounted(() => reload())
</script>
