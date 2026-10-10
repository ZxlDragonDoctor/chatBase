<template>
  <div class="anime-page-shell">
    <section class="anime-card" :aria-busy="loading">
      <div class="anime-card-header">
        <div>
          <h1 ref="botPageHeadingRef" class="anime-card-title" tabindex="-1">机器人管理</h1>
          <p class="anime-card-desc">QQ 机器人 · 企业微信机器人 · 微信个人号 · 运行状态监控</p>
        </div>
        <div class="anime-card-actions">
          <button class="anime-btn ghost" @click="loadBots">
            <RefreshCw :size="16" :class="{ 'animate-spin': loading }" />
            <span>刷新</span>
          </button>
        </div>
      </div>

      <div v-if="error" class="anime-error page-error" role="alert" aria-live="assertive">{{ error }}</div>

      <div class="anime-card-body">
        <div v-if="loading" class="bot-loading" role="status" aria-live="polite">
          <span class="anime-loader-spinner" style="width: 28px; height: 28px;"></span>
          <div style="color: var(--anime-text-muted); font-size: 14px;">加载中...</div>
        </div>

        <template v-else>
          <div class="bot-stat-grid">
            <div class="bot-stat-card">
              <div class="bot-stat-icon total">
                <Cpu :size="20" />
              </div>
              <div class="bot-stat-body">
                <div class="bot-stat-value">{{ bots.length }}</div>
                <div class="bot-stat-label">机器人总数</div>
              </div>
            </div>
            <div class="bot-stat-card">
              <div class="bot-stat-icon online">
                <Activity :size="20" />
              </div>
              <div class="bot-stat-body">
                <div class="bot-stat-value green">{{ onlineCount }}</div>
                <div class="bot-stat-label">在线数量</div>
              </div>
            </div>
            <div class="bot-stat-card">
              <div class="bot-stat-icon today">
                <MessageCircle :size="20" />
              </div>
              <div class="bot-stat-body">
                <div class="bot-stat-value pink">{{ todayMessages }}</div>
                <div class="bot-stat-label">今日消息</div>
              </div>
            </div>
            <div class="bot-stat-card">
              <div class="bot-stat-icon messages">
                <Database :size="20" />
              </div>
              <div class="bot-stat-body">
                <div class="bot-stat-value purple">{{ formatNumber(totalMessages) }}</div>
                <div class="bot-stat-label">总消息数</div>
              </div>
            </div>
          </div>

          <div class="anime-divider"></div>

          <div class="bot-list">
            <div v-for="bot in bots" :key="bot.platform" class="bot-card">
              <div class="bot-card-header">
                <div class="bot-card-identity">
                  <div class="bot-card-icon" :class="bot.platform">
                    <span v-if="bot.platform === 'qq'">Q</span>
                    <span v-else-if="bot.platform === 'wecom'">W</span>
                    <span v-else-if="bot.platform === 'wx'">WX</span>
                    <span v-else>B</span>
                  </div>
                  <div class="bot-card-info">
                    <div class="bot-card-name-row">
                      <span class="bot-card-name">{{ bot.name }}</span>
                      <span class="bot-status-indicator" :class="bot.online ? 'online' : bot.statusLabel === '回调已配置' ? 'configured' : 'offline'">
                        <span class="bot-status-dot" :class="bot.online ? 'online' : bot.statusLabel === '回调已配置' ? 'configured' : 'offline'"></span>
                        {{ bot.statusLabel || (bot.online ? '在线' : '离线') }}
                      </span>
                    </div>
                    <div class="bot-card-detail">
                      <span v-if="bot.platform === 'qq' && bot.botId">QQ: {{ bot.botId }} · NapCat</span>
                      <span v-else-if="bot.platform === 'qq'">QQ · NapCat</span>
                      <span v-else-if="bot.platform === 'wecom'">企业微信 · 回调模式</span>
                      <span v-else-if="bot.platform === 'wx'">微信个人号 · ilink 协议</span>
                      <span v-else>-</span>
                    </div>
                  </div>
                </div>
                <div class="bot-card-actions" v-if="bot.platform === 'wx' || bot.platform === 'qq'">
                  <button v-if="bot.online" class="anime-btn xs danger" :aria-label="`断开${bot.name}`" @click="handleDisconnect(bot.platform)">
                    <LogOut :size="14" />
                    <span>断开</span>
                  </button>
                  <button v-else class="anime-btn xs primary" :aria-label="`扫码登录${bot.name}`" @click="handleQrLogin(bot.platform)">
                    <QrCode :size="14" />
                    <span>扫码登录</span>
                  </button>
                </div>
              </div>
              <div class="bot-card-stats">
                <div class="bot-s-item">
                  <span class="bot-s-label">群聊数</span>
                  <span class="bot-s-value">{{ bot.groupCount }}</span>
                </div>
                <div class="bot-s-item">
                  <span class="bot-s-label">今日消息</span>
                  <span class="bot-s-value">{{ bot.todayMessages }}</span>
                </div>
                <div class="bot-s-item">
                  <span class="bot-s-label">总消息</span>
                  <span class="bot-s-value">{{ formatNumber(bot.totalMessages) }}</span>
                </div>
                <div class="bot-s-item">
                  <span class="bot-s-label">最后活跃</span>
                  <span class="bot-s-value">{{ bot.lastActiveTime ? formatDate(bot.lastActiveTime) : '无' }}</span>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>
    </section>

    <!-- QR Code Login Modal -->
    <Teleport to="body">
      <div v-if="showQrModal" class="anime-modal-overlay" @click.self="closeQrModal">
        <div ref="qrDialogRef" class="anime-modal qr-modal" role="dialog" aria-modal="true" tabindex="-1" :aria-label="`${qrPlatform === 'wx' ? '微信' : 'QQ'}扫码登录`">
          <div class="anime-modal-header">
            <div class="anime-modal-title">{{ qrPlatform === 'wx' ? '微信' : 'QQ' }}扫码登录</div>
            <button class="anime-modal-close" aria-label="关闭扫码登录窗口" @click="closeQrModal">✕</button>
          </div>
          <div class="qr-modal-body">
              <div v-if="qrFetching" class="qr-loading" role="status" aria-live="polite">
              <span class="anime-loader-spinner" style="width:36px;height:36px"></span>
              <div class="qr-hint">获取二维码中...</div>
            </div>
              <div v-else-if="qrError" class="qr-error" role="alert" aria-live="assertive">
              <div class="qr-error-text">{{ qrError }}</div>
              <button class="anime-btn sm" @click="fetchQrCode">重试</button>
            </div>
            <template v-else>
              <div class="qr-image-wrapper">
                <canvas v-if="qrPlatform === 'wx'" ref="qrCanvasRef" class="qr-canvas" role="img" aria-label="微信登录二维码，请使用微信扫码"></canvas>
                <canvas v-else ref="qqQrCanvasRef" class="qr-canvas" role="img" aria-label="QQ登录二维码，请使用QQ扫码"></canvas>
              </div>
              <div class="qr-status" role="status" aria-live="polite">
                <span v-if="qrStatus === 'pending'" class="qr-status-text">
                  请使用{{ qrPlatform === 'wx' ? '微信' : 'QQ' }}扫描二维码登录
                </span>
                <span v-else-if="qrStatus === 'confirmed'" class="qr-status-text success">
                  登录成功！机器人已上线
                </span>
                <span v-else-if="qrStatus === 'expired'" class="qr-status-text error">
                  二维码已过期
                </span>
              </div>
              <div class="qr-hint" v-if="qrStatus === 'pending'">
                <span class="qr-dot-pulse"></span>
                等待扫码中... ({{ pollCount }}s)
              </div>
            </template>
          </div>
          <div class="anime-modal-footer">
            <button class="anime-btn" @click="closeQrModal">
              关闭
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { RefreshCw, Cpu, Activity, MessageCircle, Database, LogOut, QrCode } from 'lucide-vue-next'
import { listBots, getWxQrCode, pollWxQrCodeStatus, getWxBotStatus, disconnectWxBot, getQqQrCode, getQqQrCodeStatus, type BotInfo } from '../api/bot'
import QRCode from 'qrcode'
import { useModalDialog } from '../composables/useModalDialog'

const bots = ref<BotInfo[]>([])
const loading = ref(false)
const error = ref<string | null>(null)

const onlineCount = computed(() => bots.value.filter(b => b.online).length)
const todayMessages = computed(() => bots.value.reduce((sum, b) => sum + b.todayMessages, 0))
const totalMessages = computed(() => bots.value.reduce((sum, b) => sum + b.totalMessages, 0))

async function loadBots() {
  loading.value = true
  error.value = null
  try {
    bots.value = await listBots()
  } catch (e: any) {
    error.value = e?.response?.data?.message || '加载机器人列表失败'
  } finally {
    loading.value = false
  }
}

function formatNumber(n: number): string {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return n.toString()
}

function formatDate(dateStr: string): string {
  const d = new Date(dateStr)
  return d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

onMounted(() => { loadBots() })

const showQrModal = ref(false)
const qrPlatform = ref<'wx' | 'qq'>('wx')
const qrFetching = ref(false)
const qrError = ref('')
const qrCanvasRef = ref<HTMLCanvasElement | null>(null)
const qqQrCanvasRef = ref<HTMLCanvasElement | null>(null)
const qrCodeKey = ref('')
const qrQrImg = ref('')
const qqQrUrl = ref('')
const qrStatus = ref<'pending' | 'confirmed' | 'expired' | ''>('')
const pollCount = ref(0)
let pollTimer: ReturnType<typeof setInterval> | null = null

function closeQrModal() {
  showQrModal.value = false
  stopPolling()
}

const botPageHeadingRef = ref<HTMLElement | null>(null)
const { dialogRef: qrDialogRef } = useModalDialog(showQrModal, closeQrModal, () => botPageHeadingRef.value)

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

async function renderQrToCanvas(text: string) {
  const canvas = qrCanvasRef.value
  if (!canvas) throw new Error('Canvas 未就绪')
  await QRCode.toCanvas(canvas, text, { width: 220, margin: 2 })
}

async function renderQqQrToCanvas(text: string) {
  const canvas = qqQrCanvasRef.value
  if (!canvas) throw new Error('Canvas 未就绪')
  await QRCode.toCanvas(canvas, text, { width: 220, margin: 2 })
}

async function fetchQrCode() {
  qrFetching.value = true
  qrError.value = ''
  qrCodeKey.value = ''
  qrQrImg.value = ''
  qrStatus.value = ''
  pollCount.value = 0
  try {
    if (qrPlatform.value === 'wx') {
      const data = await getWxQrCode()
      if (!data.qrcode_img_content) {
        throw new Error('二维码数据为空')
      }
      qrCodeKey.value = data.qrcode
      qrStatus.value = 'pending'
      qrFetching.value = false
      await nextTick()
      await renderQrToCanvas(data.qrcode_img_content)
    } else {
      const data = await getQqQrCode()
      if (data.error) {
        throw new Error(data.error)
      }
      if (!data.qrcode_url) {
        throw new Error('二维码数据为空')
      }
      qqQrUrl.value = data.qrcode_url
      qrStatus.value = 'pending'
      qrFetching.value = false
      await nextTick()
      await renderQqQrToCanvas(data.qrcode_url)
    }
    startPolling()
  } catch (e: any) {
    qrError.value = e?.message || '获取二维码失败'
    qrFetching.value = false
  }
}

function startPolling() {
  stopPolling()
  pollTimer = setInterval(async () => {
    pollCount.value++
    try {
      if (qrPlatform.value === 'wx') {
        if (!qrCodeKey.value) return
        const data = await pollWxQrCodeStatus(qrCodeKey.value)
        if (data.error) {
          qrError.value = data.error
          stopPolling()
          return
        }
        if (data.status === 'confirmed') {
          qrStatus.value = 'confirmed'
          stopPolling()
          // 后端 login 已写凭证并 markOnline；稍候刷新列表，避免首帧仍显示离线
          setTimeout(async () => {
            try {
              await getWxBotStatus()
            } catch { /* ignore */ }
            await loadBots()
            // 再补一次，覆盖轮询线程冷启动延迟
            setTimeout(() => { loadBots() }, 1200)
            setTimeout(() => { closeQrModal() }, 800)
          }, 400)
        } else if (data.status === 'expired') {
          qrStatus.value = 'expired'
          stopPolling()
        }
      } else {
        const data = await getQqQrCodeStatus()
        if (data.isLogin) {
          qrStatus.value = 'confirmed'
          stopPolling()
          setTimeout(() => {
            closeQrModal()
            loadBots()
          }, 1500)
        } else if (data.isOffline || (data.qrcode_url && qqQrUrl.value && data.qrcode_url !== qqQrUrl.value)) {
          qrStatus.value = 'expired'
          stopPolling()
        }
      }
    } catch {
      pollCount.value++
    }
  }, 2000)
}

async function handleQrLogin(platform: 'wx' | 'qq' = 'wx') {
  qrPlatform.value = platform
  showQrModal.value = true
  await fetchQrCode()
}

async function handleDisconnect(platform: 'wx' | 'qq') {
  const name = platform === 'wx' ? '微信' : 'QQ'
  if (!confirm(`确定要断开${name}机器人连接吗？`)) return
  try {
    if (platform === 'wx') {
      await disconnectWxBot()
    }
    // QQ bot 断开：清除 NapCat 连接
    loadBots()
  } catch (e: any) {
    error.value = e?.response?.data?.message || '断开连接失败'
  }
}

onUnmounted(() => {
  stopPolling()
})
</script>

<style scoped>
.bot-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 48px;
}

.bot-stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 8px;
}

.bot-stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: #ffffff;
  border-radius: var(--anime-radius-lg);
  box-shadow: var(--anime-shadow-sm);
  border: 1px solid var(--anime-border);
}

.bot-stat-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--anime-radius);
  display: grid;
  place-items: center;
  flex-shrink: 0;
}

.bot-stat-body {
  flex: 1;
}

.bot-stat-value {
  font-size: 22px;
  font-weight: 800;
  color: var(--anime-text-primary);
  line-height: 1.2;
}

.bot-stat-value.green { color: var(--ui-success); }
.bot-stat-value.pink { color: var(--ui-primary-hover); }
.bot-stat-value.purple { color: var(--ui-text-secondary); }

.bot-stat-label {
  font-size: 12px;
  color: var(--anime-text-muted);
  margin-top: 2px;
}

.bot-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bot-card {
  background: #ffffff;
  border-radius: var(--anime-radius-lg);
  box-shadow: var(--anime-shadow-sm);
  border: 1px solid var(--anime-border);
  overflow: hidden;
  transition: all 0.2s ease;
}

.bot-card:hover {
  box-shadow: var(--anime-shadow-md);
  border-color: var(--anime-border-accent);
}

.bot-card-header {
  padding: 18px 22px;
  border-bottom: 1px solid var(--anime-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.bot-card-identity {
  display: flex;
  align-items: center;
  gap: 16px;
}

.bot-card-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--anime-radius);
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 800;
  color: #fff;
  flex-shrink: 0;
}

.bot-card-icon.qq { background: linear-gradient(135deg, #4fc3f7, #29b6f6); }
.bot-card-icon.wecom { background: linear-gradient(135deg, #81c784, #66bb6a); }
.bot-card-icon.wx { background: linear-gradient(135deg, #81c784, #4fc3f7); }

.bot-card-info {
  flex: 1;
}

.bot-card-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}

.bot-card-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--anime-text-primary);
}

.bot-status-indicator {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  font-weight: 700;
}

.bot-status-indicator.online { color: #388e3c; }
.bot-status-indicator.offline { color: var(--anime-text-muted); }
.bot-status-indicator.configured { color: #0288d1; }

.bot-status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
}

.bot-status-dot.online {
  background: #81c784;
  box-shadow: 0 0 6px rgba(129, 199, 132, 0.5);
}
.bot-status-dot.configured { background: #4fc3f7; }

.bot-status-dot.offline { background: var(--anime-text-muted); }

.bot-card-detail {
  font-size: 13px;
  color: var(--anime-text-muted);
}

.bot-card-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
  margin-left: 12px;
}

.bot-card-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
}

.bot-s-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 14px 12px;
  gap: 4px;
  border-right: 1px solid var(--anime-border-light);
}

.bot-s-item:last-child { border-right: none; }

.bot-s-label {
  font-size: 11px;
  color: var(--anime-text-muted);
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.3px;
}

.bot-s-value {
  font-size: 16px;
  font-weight: 800;
  color: var(--anime-text-primary);
}

.animate-spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.qr-modal {
  max-width: 380px;
  width: 90vw;
}

.qr-modal-body {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 32px;
  gap: 16px;
}

.qr-loading, .qr-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px 20px;
}

.qr-error-icon {
  font-size: 36px;
}

.qr-error-text {
  color: var(--anime-text-muted);
  font-size: 14px;
  text-align: center;
}

.qr-image-wrapper {
  background: #fff;
  border-radius: var(--anime-radius);
  padding: 12px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
  border: 1px solid var(--anime-border);
}

.qr-canvas {
  display: block;
  width: 220px;
  height: 220px;
  border-radius: 8px;
}

.qr-status {
  text-align: center;
}

.qr-status-text {
  font-size: 14px;
  font-weight: 600;
  color: var(--anime-text-muted);
}

.qr-status-text.success { color: #388e3c; }
.qr-status-text.error { color: #e53935; }

.qr-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--anime-text-muted);
}

.qr-dot-pulse {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #4fc3f7;
  animation: qr-pulse 1.4s ease-in-out infinite;
}

@keyframes qr-pulse {
  0%, 100% { opacity: 0.4; transform: scale(0.8); }
  50% { opacity: 1; transform: scale(1.2); }
}

@media (max-width: 768px) {
  .bot-stat-grid { grid-template-columns: repeat(2, 1fr); }
  .bot-card-stats { grid-template-columns: repeat(2, 1fr); }
}
</style>

<style scoped>
.page-error { margin: 0 28px 16px; }
.anime-card-title { margin: 0; }
.anime-card-desc { margin: 4px 0 0; }
.bot-loading { display: flex; flex-direction: column; align-items: center; gap: 12px; padding: 48px; color: var(--ui-text-secondary); }
.bot-stat-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 8px; }
.bot-stat-card, .bot-card { background: var(--ui-surface); border: 1px solid var(--ui-border); border-radius: var(--ui-radius-lg); box-shadow: var(--ui-shadow-card); }
.bot-stat-card { display: flex; align-items: center; gap: 14px; min-width: 0; padding: 16px; }
.bot-stat-icon { width: 42px; height: 42px; display: grid; place-items: center; flex: 0 0 42px; background: var(--ui-surface-muted); color: var(--ui-text-secondary); border: 1px solid var(--ui-border); border-radius: var(--ui-radius-md); }
.bot-stat-icon.total { color: var(--ui-primary); background: var(--ui-primary-soft); border-color: var(--ui-primary-border); }
.bot-stat-icon.online { color: var(--ui-success); background: var(--ui-success-soft); border-color: var(--ui-success-border); }
.bot-stat-icon.today { color: var(--ui-accent); background: var(--ui-warning-soft); border-color: var(--ui-warning-soft); }
.bot-stat-icon.messages { color: var(--ui-text-secondary); }
.bot-stat-value { color: var(--ui-text); font-size: 22px; font-weight: 750; line-height: 1.2; }
.bot-stat-value.green { color: var(--ui-success); }
.bot-stat-value.pink { color: var(--ui-primary-hover); }
.bot-stat-value.purple { color: var(--ui-text-secondary); }
.bot-stat-label, .bot-card-detail, .bot-s-label { color: var(--ui-text-secondary); }
.bot-stat-label { margin-top: 3px; font-size: 12px; }
.bot-list { display: flex; flex-direction: column; gap: 12px; }
.bot-card { overflow: hidden; transition: border-color .15s ease; }
.bot-card:hover { border-color: var(--ui-primary-border); }
.bot-card-header { display: flex; align-items: center; justify-content: space-between; gap: 14px; padding: 16px 18px; border-bottom: 1px solid var(--ui-border); }
.bot-card-identity { display: flex; align-items: center; gap: 14px; min-width: 0; }
.bot-card-icon { width: 42px; height: 42px; border-radius: var(--ui-radius-md); color: #fff; background: var(--ui-text-secondary); }
.bot-card-icon.qq { background: var(--ui-primary); }
.bot-card-icon.wecom { background: var(--ui-success); }
.bot-card-icon.wx { background: var(--ui-text-secondary); }
.bot-card-name { color: var(--ui-text); font-size: 16px; font-weight: 700; }
.bot-status-indicator { display: inline-flex; align-items: center; gap: 6px; padding: 3px 8px; border: 1px solid transparent; border-radius: 999px; font-size: 11px; font-weight: 700; }
.bot-status-indicator.online { color: var(--ui-success); background: var(--ui-success-soft); border-color: var(--ui-success-border); }
.bot-status-indicator.configured { color: var(--ui-primary-hover); background: var(--ui-primary-soft); border-color: var(--ui-primary-border); }
.bot-status-indicator.offline { color: var(--ui-text-secondary); background: var(--ui-surface-muted); border-color: var(--ui-border); }
.bot-status-dot { width: 7px; height: 7px; border-radius: 50%; }
.bot-status-dot.online { background: var(--ui-success); }
.bot-status-dot.configured { background: var(--ui-primary); }
.bot-status-dot.offline { background: var(--ui-text-muted); }
.bot-card-detail { font-size: 13px; }
.bot-card-actions { display: flex; flex-wrap: wrap; gap: 8px; flex-shrink: 0; }
.bot-card-stats { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); }
.bot-s-item { display: flex; flex-direction: column; align-items: center; gap: 4px; padding: 13px 10px; border-right: 1px solid var(--ui-border); }
.bot-s-item:last-child { border-right: 0; }
.bot-s-label { font-size: 11px; font-weight: 600; }
.bot-s-value { color: var(--ui-text); font-size: 15px; font-weight: 700; text-align: center; overflow-wrap: anywhere; }
.qr-loading, .qr-error { color: var(--ui-text-secondary); }
.qr-error-text { color: var(--ui-danger); }
.qr-image-wrapper { background: var(--ui-surface); border: 1px solid var(--ui-border); border-radius: var(--ui-radius-md); box-shadow: var(--ui-shadow-card); }
.qr-status-text { color: var(--ui-text-secondary); }
.qr-status-text.success { color: var(--ui-success); }
.qr-status-text.error { color: var(--ui-danger); }
.qr-dot-pulse { background: var(--ui-primary); }
.page-error { border-color: var(--ui-danger-border); background: var(--ui-danger-soft); color: var(--ui-danger); }
@media (max-width: 980px) {
  .bot-stat-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 640px) {
  .page-error { margin: 0 16px 14px; }
  .bot-stat-grid { gap: 8px; }
  .bot-stat-card { padding: 12px; gap: 10px; }
  .bot-card-header { align-items: flex-start; flex-direction: column; }
  .bot-card-actions { margin: 0; }
  .bot-card-stats { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .bot-s-item:nth-child(2) { border-right: 0; }
  .bot-s-item:nth-child(-n + 2) { border-bottom: 1px solid var(--ui-border); }
  .qr-modal-body { padding: 20px 16px; }
  .qr-canvas { width: min(220px, 64vw); height: auto; aspect-ratio: 1; }
}
@media (prefers-reduced-motion: reduce) {
  .animate-spin, .qr-dot-pulse { animation: none; }
  .bot-card { transition: none; }
}
</style>
