#!/usr/bin/env node
/**
 * ChatBase opencode 反向桥接 Agent（零依赖，Node.js 22+ 原生 WebSocket）
 *
 * 思路（对齐 OpenClaw Node / GitHub Actions Runner）：
 *   本机 agent 主动外连 ChatBase 服务器 WebSocket，
 *   服务器通过该连接反向下发 HTTP 代理请求到本机 opencode serve。
 *   —— 无需 frp / 内网穿透 / 公网入站端口。
 *
 * 用法：
 *   node opencode-bridge.mjs
 *
 * 环境变量：
 *   BRIDGE_URL      服务器桥接地址（必填）
 *                   例：ws://127.0.0.1:8080/opencode/bridge
 *                       wss://your-domain.com/opencode/bridge
 *   BRIDGE_TOKEN    桥接 token（必填，与服务器 OPENCODE_BRIDGE_TOKEN 一致）
 *   OPENCODE_URL    本机 opencode serve 地址（默认 http://127.0.0.1:4096）
 *   BRIDGE_NAME     本机名称（可选，默认取 hostname）
 */

import os from 'node:os';

const BRIDGE_URL = process.env.BRIDGE_URL || '';
const BRIDGE_TOKEN = process.env.BRIDGE_TOKEN || '';
const OPENCODE_URL = (process.env.OPENCODE_URL || 'http://127.0.0.1:4096').replace(/\/$/, '');
const BRIDGE_NAME = process.env.BRIDGE_NAME || os.hostname();

const RECONNECT_BASE_MS = 1000;
const RECONNECT_MAX_MS = 30000;

if (!BRIDGE_URL || !BRIDGE_TOKEN) {
  console.error('请设置 BRIDGE_URL 与 BRIDGE_TOKEN 环境变量');
  console.error('示例：');
  console.error('  BRIDGE_URL=wss://your-domain.com/opencode/bridge \\');
  console.error('  BRIDGE_TOKEN=your-token \\');
  console.error('  node opencode-bridge.mjs');
  process.exit(1);
}

let ws = null;
let reconnectDelay = RECONNECT_BASE_MS;
let reconnectTimer = null;

function log(...args) {
  console.log(new Date().toISOString(), ...args);
}

function connect() {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer);
    reconnectTimer = null;
  }

  const url = BRIDGE_URL.includes('?')
    ? `${BRIDGE_URL}&token=${encodeURIComponent(BRIDGE_TOKEN)}`
    : `${BRIDGE_URL}?token=${encodeURIComponent(BRIDGE_TOKEN)}`;

  log(`正在连接 ${BRIDGE_URL} ...`);
  ws = new WebSocket(url);

  ws.addEventListener('open', () => {
    reconnectDelay = RECONNECT_BASE_MS;
    log('已连接服务器');
    ws.send(JSON.stringify({
      type: 'hello',
      name: BRIDGE_NAME,
      agent: 'opencode-bridge/1.0',
    }));
  });

  ws.addEventListener('message', async (event) => {
    let msg;
    try {
      msg = JSON.parse(typeof event.data === 'string' ? event.data : event.data.toString());
    } catch {
      return;
    }

    if (msg.type === 'auth-ok') {
      log('鉴权通过');
      return;
    }
    if (msg.type === 'hello-ok') {
      log(`桥接就绪: bridgeId=${msg.bridgeId}`);
      return;
    }
    if (msg.type === 'ping') {
      safeSend(JSON.stringify({ type: 'pong' }));
      return;
    }
    if (msg.type === 'req') {
      await handleRequest(msg);
    }
  });

  ws.addEventListener('close', (event) => {
    log(`连接关闭: code=${event.code} reason=${event.reason || '-'}`);
    scheduleReconnect();
  });

  ws.addEventListener('error', (err) => {
    log('连接错误:', err.message || err);
  });
}

async function handleRequest(msg) {
  const { id, method, path, body } = msg;
  const target = `${OPENCODE_URL}${path || '/'}`;
  log(`→ ${method} ${path}`);

  try {
    const headers = { 'Content-Type': 'application/json' };
    // opencode serve 的 Basic Auth（若设置了 OPENCODE_SERVER_PASSWORD）
    if (process.env.OPENCODE_SERVER_PASSWORD) {
      const user = process.env.OPENCODE_SERVER_USERNAME || 'opencode';
      const cred = Buffer.from(`${user}:${process.env.OPENCODE_SERVER_PASSWORD}`).toString('base64');
      headers['Authorization'] = `Basic ${cred}`;
    }

    const init = {
      method: method || 'GET',
      headers,
    };
    if (body && method !== 'GET' && method !== 'HEAD') {
      init.body = typeof body === 'string' ? body : JSON.stringify(body);
    }

    const resp = await fetch(target, init);
    const text = await resp.text();
    log(`← ${resp.status} ${path} (${text.length} bytes)`);

    safeSend(JSON.stringify({
      type: 'res',
      id,
      status: resp.status,
      body: text,
    }));
  } catch (err) {
    log(`请求失败 ${path}:`, err.message);
    safeSend(JSON.stringify({
      type: 'res',
      id,
      status: 502,
      body: JSON.stringify({ error: err.message }),
    }));
  }
}

function safeSend(data) {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(data);
  }
}

function scheduleReconnect() {
  if (reconnectTimer) return;
  log(`${Math.round(reconnectDelay / 1000)}s 后重连...`);
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null;
    connect();
  }, reconnectDelay);
  reconnectDelay = Math.min(reconnectDelay * 2, RECONNECT_MAX_MS);
}

// 优雅退出
process.on('SIGINT', () => {
  log('正在退出...');
  if (ws) ws.close();
  process.exit(0);
});
process.on('SIGTERM', () => {
  if (ws) ws.close();
  process.exit(0);
});

connect();
