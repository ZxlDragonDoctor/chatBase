# ChatBase OpenCode 本机 Agent

## 新版 OpenClaw 式 Node 通道（推荐）

新版 Agent 与旧版 V1 桥接并行工作。电脑主动连到 ChatBase 的 `/opencode/node`，使用一次性配对码登记；管理员批准节点后，再在「私聊采集」页面把某个私聊绑定到该电脑。无需开放本机入站端口，也不需要 frp。

### 前置条件

1. Node.js 22 或更新版本。
2. 在一个终端启动 `opencode serve`，建议只监听回环地址：
   ```powershell
   $env:OPENCODE_SERVER_PASSWORD = "请设置强密码"
   opencode serve --hostname 127.0.0.1 --port 4096
   ```
   macOS/Linux 可使用：
   ```bash
   OPENCODE_SERVER_PASSWORD="请设置强密码" opencode serve --hostname 127.0.0.1 --port 4096
   ```
3. 后端设置 `OPENCODE_ENABLED=true` 和 `OPENCODE_NODE_ENABLED=true`；公网部署为 `/opencode/node` 启用 HTTPS/WSS 反向代理。

### 首次配对

1. 管理员登录 ChatBase，打开「私聊采集」并生成一次性配对码（10 分钟内有效）。
2. 在目标电脑另开一个 PowerShell 终端，填写服务器地址和刚生成的配对码：
   ```powershell
   $env:CHATBASE_NODE_WS_URL = "wss://你的域名/opencode/node"
   $env:OPENCODE_PAIR_CODE = "粘贴一次性配对码"
   $env:OPENCODE_URL = "http://127.0.0.1:4096"
   $env:OPENCODE_DEFAULT_DIRECTORY = "D:\Projects\my-repo"
   $env:OPENCODE_SERVER_USERNAME = "opencode"
   $env:OPENCODE_SERVER_PASSWORD = "与 opencode serve 相同的强密码"
   node bridge\opencode-node.mjs
   ```
3. 回到管理页批准新节点，再为私聊选择该节点并保存。

Agent 不会在终端输出配对码或节点凭据。配对完成后，节点 ID 和凭据保存在当前 Windows 用户目录 `%USERPROFILE%\.chatbase\opencode-node.json`；macOS/Linux 保存到 `~/.chatbase/opencode-node.json`。再次运行时删除 `OPENCODE_PAIR_CODE` 即可自动认证重连。撤销节点后需生成新配对码重新配对。

节点只接受固定 OpenCode 命令。每次调用都由 ChatBase 根据节点状态和已批准能力校验；离线或能力未批准时，消息会直接返回错误，不会排队或改走另一台电脑。私聊会话 ID 按节点隔离。

### Agent 环境变量

| 变量 | 必填 | 说明 | 默认 |
|------|------|------|------|
| `CHATBASE_NODE_WS_URL` | 是 | ChatBase 节点地址，生产环境使用 `wss://.../opencode/node` | - |
| `OPENCODE_PAIR_CODE` | 首次配对需要 | 管理页面生成的一次性配对码 | - |
| `OPENCODE_URL` | 否 | 本机 OpenCode 服务，仅允许 loopback 地址 | `http://127.0.0.1:4096` |
| `OPENCODE_DEFAULT_DIRECTORY` | 否 | 新会话使用的本机项目目录；不填时使用本机 OpenCode 默认目录 | - |
| `OPENCODE_SERVER_USERNAME` | 否 | 本机 OpenCode Basic Auth 用户名 | `opencode` |
| `OPENCODE_SERVER_PASSWORD` | 否 | 本机 OpenCode Basic Auth 密码，仅发给本机服务 | - |
| `BRIDGE_NAME` | 否 | 管理页展示的电脑名称 | hostname |

## 旧版 V1 桥接（兼容）

以下 `opencode-bridge.mjs` 使用共享静态 token 和通用 HTTP method/path 代理，仅保留给已有部署迁移使用。新电脑建议使用上面的受配对和能力审批的 Node 通道。

本机 agent **主动外连** ChatBase 服务器 WebSocket，服务器通过该连接反向调用本机
`opencode serve`。**无需 frp / 内网穿透 / 公网入站端口**（对齐 OpenClaw Node /
GitHub Actions Runner 的 outbound-agent 模式）。

```
开发者本机                              云服务器
┌─────────────────────┐                ┌──────────────────────┐
│  opencode serve     │◄──HTTP──┐      │                      │
│  127.0.0.1:4096     │         │      │   ChatBase Backend   │
│                     │         │      │   /opencode/bridge   │
│  opencode-bridge.mjs│──WSS──────────►│   (WebSocket 反向)   │
│  (outbound dial)    │         │      │                      │
└─────────────────────┘                └──────────────────────┘
```

## 前置条件

1. Node.js 22+（使用原生 WebSocket 与 fetch）
2. 本机 `opencode serve` 已启动：
   ```bash
   export OPENCODE_SERVER_PASSWORD="your-strong-password"
   opencode serve --port 4096
   ```
3. 服务器 `.env` 已配置：
   ```env
   OPENCODE_ENABLED=true
   OPENCODE_BRIDGE_ENABLED=true
   OPENCODE_BRIDGE_TOKEN=please-change-me-to-a-long-random-string
   OPENCODE_BRIDGE_TRANSPORT=auto    # auto | bridge | direct
   ```

## 启动

```bash
# macOS / Linux
BRIDGE_URL=wss://your-domain.com/opencode/bridge \
BRIDGE_TOKEN=please-change-me-to-a-long-random-string \
OPENCODE_URL=http://127.0.0.1:4096 \
node bridge/opencode-bridge.mjs
```

```powershell
# Windows PowerShell
$env:BRIDGE_URL = "wss://your-domain.com/opencode/bridge"
$env:BRIDGE_TOKEN = "please-change-me-to-a-long-random-string"
$env:OPENCODE_URL = "http://127.0.0.1:4096"
node bridge\opencode-bridge.mjs
```

本机开发（后端跑在本机 8080）可用 `ws://127.0.0.1:8080/opencode/bridge`。

## 环境变量

| 变量 | 必填 | 说明 | 默认 |
|------|------|------|------|
| `BRIDGE_URL` | 是 | 服务器桥接地址 `ws://` 或 `wss://` | - |
| `BRIDGE_TOKEN` | 是 | 与服务器 `OPENCODE_BRIDGE_TOKEN` 一致 | - |
| `OPENCODE_URL` | 否 | 本机 opencode serve 地址 | `http://127.0.0.1:4096` |
| `BRIDGE_NAME` | 否 | 本机显示名 | hostname |
| `OPENCODE_SERVER_PASSWORD` | 否 | opencode Basic Auth 密码 | - |
| `OPENCODE_SERVER_USERNAME` | 否 | Basic Auth 用户名 | `opencode` |

## 工作原理

1. agent 通过 `wss://…/opencode/bridge?token=…` 主动外连服务器
2. 服务器校验 token，回 `auth-ok` / `hello-ok`
3. 服务器把 opencode HTTP 调用封装为 `{type:"req",id,method,path,body}` 下发
4. agent 用 `fetch` 打本机 `OPENCODE_URL`，把结果以 `{type:"res",id,status,body}` 回传
5. 断线自动指数退避重连（1s → 30s）

## 常用运维

```bash
# 查看桥接是否在线（admin）
curl -H "Authorization: Bearer <token>" http://127.0.0.1:8080/api/health

# 后端日志关键字
docker compose logs -f chatbase-backend | grep -i bridge
```

## 与 frp / Tailscale 对比

| 方案 | 入站端口 | 依赖 | 运维成本 | 安全 |
|------|----------|------|----------|------|
| **反向桥接（本方案）** | 无 | Node 22+ | 低（一条命令） | token + WSS |
| frp 反向隧道 | 需开放 remotePort | frps/frpc | 高（易断链） | 隧道暴露端口 |
| Tailscale | 无 | Tailscale 账号 | 中 | 网络层隔离 |

> 若服务器与 opencode 在同一台机器，直接设 `OPENCODE_BASE_URL=http://127.0.0.1:4096`，
> 并把 `OPENCODE_BRIDGE_TRANSPORT=direct` 即可，无需 agent。
