<template>
  <div class="pageShell">
    <section class="card">
      <div class="cardHeader">
        <div>
          <div class="h1">概览</div>
          <div class="muted">采集消息、机器人状态与 Web 问答入口</div>
        </div>
        <div class="right">
          <RouterLink class="btn" to="/chat">Web 问答</RouterLink>
          <RouterLink class="btn btnGhost" to="/console/im">群聊管理</RouterLink>
        </div>
      </div>

      <div v-if="loadErr" class="error" style="margin: 18px">{{ loadErr }}</div>

      <div v-else-if="!overview" class="empty" style="margin: 24px">加载中…</div>

      <div v-else class="dashBody">
        <div class="statGrid">
          <div class="statCard">
            <div class="statNum">{{ overview.totalMessages }}</div>
            <div class="statLabel">已采集消息条数</div>
          </div>
          <div class="statCard">
            <div class="statNum">{{ overview.distinctGroups }}</div>
            <div class="statLabel">去重群聊数</div>
          </div>
        </div>

        <div class="h2" style="margin-top: 18px">按平台</div>
        <div class="platformChips">
          <span v-for="(n, p) in msgByPlatform" :key="p" class="pill">
            {{ platformLabel(p) }} · 消息 {{ n }}
            <template v-if="groupByPlatform[p] != null">
              · 群 {{ groupByPlatform[p] }}
            </template>
          </span>
          <span v-if="platformKeys.length === 0" class="muted">暂无数据</span>
        </div>

        <div class="h2" style="margin-top: 22px">机器人</div>
        <div class="botGrid">
          <div class="botCard">
            <div class="botTitle">QQ（NapCat / OneBot）</div>
            <ul class="botList muted">
              <li>启用：{{ botQq.enabled ? '是' : '否' }}</li>
              <li>机器人 QQ：{{ botQq.selfId || '—' }}</li>
              <li>WS 端口：{{ botQq.wsPort }}</li>
              <li>HTTP 已配置：{{ botQq.httpConfigured ? '是' : '否' }}</li>
              <li v-if="botQq.httpBaseUrlPreview">
                地址预览：<code>{{ botQq.httpBaseUrlPreview }}</code>
              </li>
            </ul>
          </div>
          <div class="botCard">
            <div class="botTitle">企业微信智能机器人</div>
            <ul class="botList muted">
              <li>回调路径：<code>{{ botWecom.callbackPath }}</code></li>
              <li>{{ botWecom.note }}</li>
            </ul>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { fetchOverview } from '../api/console'
import type { BotStatus, ConsoleOverview } from '../types/console'
import { platformLabel } from '../lib/platformLabel'

const overview = ref<ConsoleOverview | null>(null)
const loadErr = ref<string | null>(null)

const msgByPlatform = computed(() => overview.value?.messageCountByPlatform ?? {})
const groupByPlatform = computed(() => overview.value?.groupCountByPlatform ?? {})
const platformKeys = computed(() => Object.keys(msgByPlatform.value))

const defaultBots = (): BotStatus => ({
  qq: {
    enabled: false,
    selfId: 0,
    wsPort: 0,
    httpConfigured: false,
    httpBaseUrlPreview: null,
  },
  wecom: { callbackPath: '—', note: '—' },
})

const botQq = computed(() => ({ ...defaultBots().qq, ...overview.value?.bots?.qq }))
const botWecom = computed(() => ({ ...defaultBots().wecom, ...overview.value?.bots?.wecom }))

onMounted(async () => {
  try {
    overview.value = await fetchOverview()
    console.log(overview.value)
  } catch (e: any) {
    loadErr.value = e?.message || '无法加载概览（请确认后端已启动）'
  }
})
</script>
