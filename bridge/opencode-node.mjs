#!/usr/bin/env node

import { randomUUID } from 'node:crypto'
import { chmod, mkdir, readFile, rename, writeFile } from 'node:fs/promises'
import os from 'node:os'
import path from 'node:path'
import process from 'node:process'

const VERSION = '1.0.0'
const CAPABILITIES = Object.freeze([
  'opencode.health',
  'opencode.session.list',
  'opencode.session.get',
  'opencode.session.create',
  'opencode.prompt.submit',
  'opencode.message.list',
  'opencode.question.list',
  'opencode.question.reply',
])

const WS_URL = process.env.CHATBASE_NODE_WS_URL
const PAIR_CODE = process.env.OPENCODE_PAIR_CODE
const NODE_NAME = (process.env.BRIDGE_NAME || os.hostname()).slice(0, 100)
const OPENCODE_URL = validateOpencodeUrl(process.env.OPENCODE_URL || 'http://127.0.0.1:4096')
const OPENCODE_USERNAME = process.env.OPENCODE_SERVER_USERNAME || 'opencode'
const OPENCODE_PASSWORD = process.env.OPENCODE_SERVER_PASSWORD || ''
const DEFAULT_DIRECTORY = process.env.OPENCODE_DEFAULT_DIRECTORY || ''
const credentialDir = path.join(os.homedir(), '.chatbase')
const credentialPath = path.join(credentialDir, 'opencode-node.json')

class HttpStatusError extends Error {
  constructor(status) {
    super(`HTTP ${status}`)
    this.status = status
  }
}

const handlers = new Map([
  ['opencode.health', async () => request('GET', '/global/health')],
  ['opencode.session.list', async () => request('GET', '/api/session')],
  ['opencode.session.get', async (params, signal) => request('GET', `/api/session/${encodeId(params.sessionId)}`, undefined, signal)],
  ['opencode.session.create', async (params, signal) => {
    const body = { ...params }
    if (DEFAULT_DIRECTORY) body.directory = DEFAULT_DIRECTORY
    else delete body.directory // Server paths belong to the server; use the local OpenCode default on this PC.
    return request('POST', '/api/session', body, signal)
  }],
  ['opencode.prompt.submit', async (params, signal) => request('POST', `/api/session/${encodeId(params.sessionId)}/prompt`, strip(params, ['sessionId']), signal)],
  ['opencode.message.list', async (params, signal) => request('GET', `/api/session/${encodeId(params.sessionId)}/message`, undefined, signal)],
  ['opencode.question.list', async (params, signal) => request('GET', `/api/session/${encodeId(params.sessionId)}/question`, undefined, signal)],
  ['opencode.question.reply', async (params, signal) => request('POST', `/api/session/${encodeId(params.sessionId)}/question/${encodeId(params.requestId)}/reply`, strip(params, ['sessionId', 'requestId']), signal)],
])

if (!WS_URL) {
  console.error('配置缺失：请设置 CHATBASE_NODE_WS_URL。')
  process.exit(2)
}
if (typeof WebSocket === 'undefined') {
  console.error('当前 Node.js 不提供原生 WebSocket，请使用 Node.js 22 或更新版本。')
  process.exit(2)
}

let credentials = await loadCredentials()
if ((!credentials.nodeId || !credentials.nodeToken) && !PAIR_CODE) {
  console.error('本机尚未配对：请先在 ChatBase 管理页生成配对码并设置 OPENCODE_PAIR_CODE。')
  process.exit(2)
}
if ((!credentials.nodeId || !credentials.nodeToken) && PAIR_CODE) await saveCredentials(credentials)
let pairingCode = PAIR_CODE || null
let pairingAttempted = false
let running = true
let retryDelay = 1000
let currentSocket = null
const inFlight = new Map()

process.on('SIGINT', stop)
process.on('SIGTERM', stop)

while (running) {
  try {
    await connectOnce()
    retryDelay = 1000
  } catch (error) {
    if (running) console.error(`节点连接中断：${safeMessage(error)}；${Math.ceil(retryDelay / 1000)} 秒后重连。`)
  }
  if (!running) break
  await delay(retryDelay + Math.floor(Math.random() * 500))
  retryDelay = Math.min(retryDelay * 2, 30000)
}

async function connectOnce() {
  const target = new URL(WS_URL)
  if (!['ws:', 'wss:'].includes(target.protocol)) throw new Error('CHATBASE_NODE_WS_URL 必须使用 ws:// 或 wss://')

  await new Promise((resolve, reject) => {
    const socket = new WebSocket(target)
    currentSocket = socket
    let settled = false

    const finish = (error) => {
      if (settled) return
      settled = true
      currentSocket = null
      for (const controller of inFlight.values()) controller.abort()
      inFlight.clear()
      error ? reject(error) : resolve()
    }

    socket.addEventListener('open', () => {
      const base = {
        protocolVersion: '1',
        nodeKey: credentials?.nodeKey || randomUUID(),
        name: NODE_NAME,
        platform: `${process.platform}-${process.arch}`,
        agentVersion: VERSION,
        capabilities: CAPABILITIES,
      }
      credentials = { ...(credentials || {}), nodeKey: base.nodeKey }
      if (credentials.nodeId && credentials.nodeToken) {
        send(socket, { type: 'auth', ...base, nodeId: credentials.nodeId, nodeToken: credentials.nodeToken })
      } else if (pairingCode && !pairingAttempted) {
        pairingAttempted = true
        send(socket, { type: 'hello', ...base, pairingCode })
      } else {
        running = false
        console.error('本机节点凭据无效或已撤销。请在 ChatBase 管理页生成新的配对码后重启 Agent。')
        finish(new Error('需要新的节点配对码'))
        socket.close()
      }
    })

    socket.addEventListener('message', event => {
      void handleFrame(socket, event.data).catch(error => {
        console.error(`节点协议处理失败：${safeMessage(error)}`)
      })
    })
    socket.addEventListener('error', () => finish(new Error('WebSocket 网络连接失败')))
    socket.addEventListener('close', event => {
      finish(new Error(`WebSocket 已关闭（${event.code}）`))
    })
  })
}

async function handleFrame(socket, raw) {
  let frame
  try { frame = JSON.parse(typeof raw === 'string' ? raw : Buffer.from(raw).toString('utf8')) }
  catch { throw new Error('服务器发送了无效 JSON') }

  if (frame.type === 'paired') {
    if (!Number.isInteger(frame.nodeId) || typeof frame.nodeToken !== 'string') throw new Error('服务器配对响应无效')
    credentials = { ...(credentials || {}), nodeId: frame.nodeId, nodeToken: frame.nodeToken }
    await saveCredentials(credentials)
    pairingCode = null
    pairingAttempted = false
    console.log(`电脑已配对，节点编号 ${frame.nodeId}，等待管理员批准。`)
    return
  }
  if (frame.type === 'authenticated') {
    console.log(frame.approval === 'APPROVED' ? `本机节点 ${frame.nodeId} 已连接。` : `本机节点 ${frame.nodeId} 已连接，等待管理员批准。`)
    return
  }
  if (frame.type === 'error') {
    if (frame.errorCode === 'NODE_AUTH_REJECTED' && credentials?.nodeId && pairingCode && !pairingAttempted) {
      credentials = { nodeKey: credentials.nodeKey }
      await saveCredentials(credentials)
      socket.close(4002, 'retry with the supplied pairing code')
      return
    }
    if (pairingAttempted && !credentials?.nodeId) {
      running = false
      console.error('配对失败：配对码无效、已使用或已过期。请重新生成配对码并重启 Agent。')
      socket.close(4002, 'pairing failed')
      return
    }
    if (frame.errorCode === 'NODE_AUTH_REJECTED') {
      running = false
      console.error('节点凭据已失效或节点已撤销。请在 ChatBase 管理页生成新的配对码后重启 Agent。')
      socket.close(4002, 'node credentials rejected')
      return
    }
    throw new Error(`服务器拒绝节点请求（${frame.errorCode || 'ERROR'}）`)
  }
  if (frame.type === 'ping') {
    send(socket, { type: 'pong', sentAt: frame.sentAt || Date.now() })
    return
  }
  if (frame.type === 'cancel') {
    inFlight.get(frame.id)?.abort()
    return
  }
  if (frame.type === 'invoke') {
    await invoke(socket, frame)
    return
  }
  throw new Error('服务器发送了不支持的协议帧')
}

async function invoke(socket, frame) {
  if (typeof frame.id !== 'string' || typeof frame.command !== 'string' || !handlers.has(frame.command)) {
    send(socket, { type: 'error', id: frame.id, errorCode: 'UNSUPPORTED_COMMAND', message: '节点不支持该命令' })
    return
  }
  const controller = new AbortController()
  inFlight.set(frame.id, controller)
  const startedAt = Date.now()
  const timeoutMs = Math.max(1, Math.min(320000, Number(frame.deadlineMs) - Date.now()))
  const timer = setTimeout(() => controller.abort(), timeoutMs)
  try {
    if (frame.command === 'opencode.prompt.submit') {
      send(socket, { type: 'progress', id: frame.id, message: '本机 OpenCode 正在处理请求…' })
    }
    const result = await handlers.get(frame.command)(frame.params || {}, controller.signal)
    send(socket, { type: 'result', id: frame.id, httpStatus: result.status, data: result.data })
    console.log(`${frame.command} 完成，HTTP ${result.status}，${Date.now() - startedAt}ms`)
  } catch (error) {
    const cancelled = controller.signal.aborted
    send(socket, {
      type: 'error',
      id: frame.id,
      errorCode: cancelled ? 'CANCELLED_OR_TIMEOUT' : 'OPENCODE_REQUEST_FAILED',
      message: cancelled ? '本机 OpenCode 请求已取消或超时' : safeMessage(error),
    })
    console.log(`${frame.command} 失败，${Date.now() - startedAt}ms`)
  } finally {
    clearTimeout(timer)
    inFlight.delete(frame.id)
  }
}

async function request(method, endpoint, body, signal) {
  const headers = new Headers({ Accept: 'application/json' })
  if (body !== undefined) headers.set('Content-Type', 'application/json')
  if (OPENCODE_PASSWORD) {
    headers.set('Authorization', `Basic ${Buffer.from(`${OPENCODE_USERNAME}:${OPENCODE_PASSWORD}`, 'utf8').toString('base64')}`)
  }
  const response = await fetch(new URL(endpoint, OPENCODE_URL), {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    signal,
    redirect: 'error',
  })
  const text = await response.text()
  let data = null
  if (text) {
    try { data = JSON.parse(text) }
    catch { data = text.slice(0, 4000) }
  }
  if (!response.ok) throw new HttpStatusError(response.status)
  return { status: response.status, data }
}

function validateOpencodeUrl(value) {
  const url = new URL(value)
  const loopback = new Set(['127.0.0.1', 'localhost', '[::1]', '::1'])
  if (!['http:', 'https:'].includes(url.protocol) || !loopback.has(url.hostname.toLowerCase())
      || url.username || url.password || url.search || url.hash) {
    throw new Error('OPENCODE_URL 只允许指向本机回环地址，例如 http://127.0.0.1:4096')
  }
  return url.href.endsWith('/') ? url.href : `${url.href}/`
}

function encodeId(value) {
  if (typeof value !== 'string' || !value || value === '.' || value === '..' || value.includes('/') || value.includes('\\')) {
    throw new Error('OpenCode 会话标识无效')
  }
  return encodeURIComponent(value)
}

function strip(value, excluded) {
  const result = { ...value }
  for (const key of excluded) delete result[key]
  return result
}

function send(socket, frame) {
  if (socket.readyState !== WebSocket.OPEN) throw new Error('节点连接已关闭')
  socket.send(JSON.stringify(frame))
}

async function loadCredentials() {
  let savedNodeKey = null
  try {
    const value = JSON.parse(await readFile(credentialPath, 'utf8'))
    if (Number.isInteger(value.nodeId) && typeof value.nodeToken === 'string' && typeof value.nodeKey === 'string') return value
    if (typeof value.nodeKey === 'string') savedNodeKey = value.nodeKey
  } catch { /* First run or unreadable old credentials: a fresh pairing is required. */ }
  return { nodeKey: savedNodeKey || randomUUID() }
}

async function saveCredentials(value) {
  await mkdir(credentialDir, { recursive: true, mode: 0o700 })
  const temporary = `${credentialPath}.${process.pid}.tmp`
  await writeFile(temporary, `${JSON.stringify(value, null, 2)}\n`, { encoding: 'utf8', mode: 0o600 })
  try { await chmod(temporary, 0o600) } catch { /* Windows uses its user profile ACL. */ }
  await rename(temporary, credentialPath)
  try { await chmod(credentialPath, 0o600) } catch { /* Windows uses its user profile ACL. */ }
}

function safeMessage(error) {
  if (error instanceof HttpStatusError) return `OpenCode API 返回 HTTP ${error.status}`
  if (error?.name === 'AbortError') return '请求已取消或超时'
  return String(error?.message || '本机 OpenCode 请求失败').slice(0, 180)
}

function delay(ms) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

function stop() {
  running = false
  currentSocket?.close(1000, 'agent stopping')
  for (const controller of inFlight.values()) controller.abort()
}
