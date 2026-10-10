# ChatBase 部署指南

## 环境要求

- Docker 20.10+
- Docker Compose 2.0+
- 至少 4GB 可用内存

## 快速部署

### 1. 克隆项目

```bash
git clone <repository-url>
cd chatBase
```

### 2. 配置环境变量

```bash
cp .env.example .env
```

编辑 `.env` 文件，填写以下必填配置：

```env
# MySQL
MYSQL_ROOT_PASSWORD=your_root_password
MYSQL_USER=chatbase
MYSQL_PASSWORD=your_password

# Dify API（必填）
DIFYAPP_API_KEY=your_dify_chat_api_key
DIFYAPP_DATASET_API_KEY=your_dify_dataset_api_key

# Redis（可选密码）
REDIS_PASSWORD=
```

### 3. 一键启动

```bash
# 推荐：一键脚本（自动检查 .env、构建、启动、健康检查）
bash scripts/deploy.sh

# 同时启动 QQ Bot
bash scripts/deploy.sh --with-qq

# 强制重新构建
bash scripts/deploy.sh --rebuild

# 停止
bash scripts/deploy.sh --down
```

或手动 docker compose：

```bash
# 首次部署构建并启动
docker compose up --build -d

# 仅启动（镜像已存在时）
docker compose up -d

# 启动包含 QQ Bot 的服务（首次）
docker compose --profile qq up --build -d
```

### 4. 查看日志

```bash
docker compose logs -f chatbase-backend
```

## 服务说明

| 服务 | 端口 | 说明 |
|------|------|------|
| 宿主机 Nginx | 80 / 443 | SSL 终止 + 反代（Let's Encrypt） |
| chatbase-frontend | 127.0.0.1:8081 | Vue 前端容器（不对公网直出） |
| chatbase-backend | 8080 | Spring Boot 后端 |
| mysql | 3306 | MySQL 8.0 |
| redis | 6379 | Redis 7 |
| napcat | 3000/6099 | QQ Bot（可选） |

### HTTPS / SSL（your-domain.example.com）

1. 域名 A 记录指向服务器公网 IP
2. 前端容器只绑 `127.0.0.1:8081`（见 `docker-compose.yml`）
3. 在服务器执行：

```bash
cd /opt/chatBase
bash scripts/setup-ssl.sh
# 或指定域名/邮箱
DOMAIN=your-domain.example.com EMAIL=you@example.com bash scripts/setup-ssl.sh
```

4. 证书路径：`/etc/letsencrypt/live/<domain>/`
5. 自动续期：certbot timer + deploy hook reload nginx
6. **云厂商安全组需放行 443/TCP**（80 用于 HTTP→HTTPS 跳转与续期）

## 可选服务

### QQ Bot (NapCat)

如需启用 QQ 群聊功能：

1. 设置 `QQ_BOT_ENABLE=true`
2. 配置 `QQ_BOT_SELF_ID`（机器人 QQ 号）
3. 配置 `QQ_BOT_HTTP_BASE_URL`（默认 `http://chatbase-napcat:3000`，容器网络内服务名）
4. 配置 `QQ_BOT_WEBUI_BASE_URL`（默认 `http://chatbase-napcat:6099`）与 `QQ_BOT_WEBUI_TOKEN`（NapCat WebUI 的 `webui.json` 中 `token` 值），用于 Web 控制台「机器人管理」页一键扫码登录
5. 首次启动需要扫码登录 NapCat

登录方式：
- 访问 `http://<server-ip>:6099` 打开 NapCat WebUI
- 扫码登录 QQ 账号

### 企业微信

如需启用企业微信功能，配置相关参数：
- `WECHAT_CORP_STOKEN`
- `WECHAT_CORP_S_ENCODING_AES_KEY`

### 本地 opencode serve（可选）

> 通过私聊会话远程驱动开发者**本机**的 opencode，实现"人在服务器、代理在本机"的远程编码代理。

#### 新版 OpenClaw 式 Node 通道（推荐）

本机 Agent 以固定能力 RPC 主动连到 `/opencode/node`。配对码由管理员在 `/console/im/single` 生成，电脑登记后还需管理员批准；随后将指定私聊绑定到该节点。没有入站端口或 frp，也不会改变未绑定 OpenCode 会话的 Dify 行为。

后端环境变量：

```env
OPENCODE_ENABLED=true
OPENCODE_NODE_ENABLED=true
OPENCODE_NODE_TIMEOUT_SECONDS=320
OPENCODE_NODE_HEARTBEAT_SECONDS=30
OPENCODE_NODE_OFFLINE_AFTER_SECONDS=90
OPENCODE_NODE_PAIRING_TTL_SECONDS=600
```

在目标电脑的第一个终端启动 `opencode serve`：

```powershell
$env:OPENCODE_SERVER_PASSWORD = "与 OpenCode 本机服务使用的强密码"
opencode serve --hostname 127.0.0.1 --port 4096
```

在第二个 PowerShell 终端设置配对码并启动 Agent：

```powershell
$env:CHATBASE_NODE_WS_URL = "wss://your-domain.com/opencode/node"
$env:OPENCODE_PAIR_CODE = "从 ChatBase 管理页复制的一次性配对码"
$env:OPENCODE_URL = "http://127.0.0.1:4096"
$env:OPENCODE_DEFAULT_DIRECTORY = "D:\Projects\my-repo"
$env:OPENCODE_SERVER_USERNAME = "opencode"
$env:OPENCODE_SERVER_PASSWORD = "与第一个终端相同的密码"
node bridge\opencode-node.mjs
```

配对码 10 分钟后过期且只能使用一次。批准节点后，私聊详情可选择该节点；离线节点可以绑定，但执行时会立即提示离线。Agent 凭据保存在当前 Windows 用户目录 `%USERPROFILE%\.chatbase\opencode-node.json`，重连无需再次输入配对码。公网部署需要为 `/opencode/node` 配置 HTTPS/WSS，详见 [bridge/README.md](./bridge/README.md)。

#### 旧版 V1 反向桥接（兼容）

```
开发者本机                              云服务器
┌─────────────────────┐                ┌──────────────────────┐
│  opencode serve     │◄──HTTP──┐      │                      │
│  127.0.0.1:4096     │         │      │   ChatBase Backend   │
│  opencode-bridge ───┼──WSS──────────►│   /opencode/bridge   │
└─────────────────────┘                └──────────────────────┘
```

本机 agent **主动外连**服务器 WebSocket（对齐 OpenClaw Node / GitHub Runner 模式）：
- 无需 frp、无需公网入站端口、无需内网穿透
- 断线自动重连，token 鉴权
- 详见 [bridge/README.md](./bridge/README.md)

#### 2. 本机启动 opencode serve

```bash
# Windows PowerShell / macOS / Linux 均可
export OPENCODE_SERVER_PASSWORD="your-strong-password"   # 必设，Basic Auth
opencode serve --port 4096
```

- 用户名固定为 `opencode`（除非设置 `OPENCODE_SERVER_USERNAME`）
- 密码 `OPENCODE_PASSWORD` 必须与此处的 `OPENCODE_SERVER_PASSWORD` 一致

#### 3. 本机启动反向桥接 agent（Node.js 22+）

```bash
BRIDGE_URL=wss://your-domain.com/opencode/bridge \
BRIDGE_TOKEN=please-change-me-to-a-long-random-string \
OPENCODE_URL=http://127.0.0.1:4096 \
node bridge/opencode-bridge.mjs
```

生产走 Nginx/WSS（`web/nginx.conf` 已代理 `/opencode/bridge`）；
本机开发可用 `ws://127.0.0.1:8080/opencode/bridge`。

#### 4. 服务器配置环境变量

在 `.env` 中添加：

```env
OPENCODE_ENABLED=true
OPENCODE_BRIDGE_ENABLED=true
OPENCODE_BRIDGE_TOKEN=please-change-me-to-a-long-random-string
OPENCODE_BRIDGE_TRANSPORT=auto    # auto=优先桥接，无桥接回退直连
# 可选：直连回退地址（同机 / Tailscale / frp）
OPENCODE_BASE_URL=http://127.0.0.1:4096
OPENCODE_PASSWORD=your-strong-password
OPENCODE_DEFAULT_DIRECTORY=/path/to/project
OPENCODE_TIMEOUT_SECONDS=300
```

#### V1 使用

1. 以 **admin** 登录 Web 控制台 → 「私聊采集」`/console/im/single`
2. 在会话详情「消息执行端」下拉中选择 **旧版 OpenCode 通道** 并保存
3. 通过该平台私聊消息，即可远程驱动本机 opencode（回复写入 `kb_conversation` 审计）

⚠️ 新版节点配对、批准和节点绑定仅对 admin 开放。

#### 备选：直连 HTTP（同机 / frp / Tailscale）

若服务器与 opencode 同机，直接 `OPENCODE_BASE_URL=http://127.0.0.1:4096`、
`OPENCODE_BRIDGE_TRANSPORT=direct`，无需 agent。

若坚持 frp 反向隧道（不推荐，运维成本高），见
[OPS_TROUBLESHOOTING.md](./OPS_TROUBLESHOOTING.md) 第 1 节历史方案；
更轻量的替代是 Tailscale：两台机器加入同一 tailnet，`OPENCODE_BASE_URL=http://<tailscale-ip>:4096`。

## 常用命令

```bash
# 启动所有服务
docker compose up -d

# 停止所有服务
docker compose down

# 重启后端
docker compose restart chatbase-backend

# 查看后端日志
docker compose logs -f chatbase-backend

# 重新构建镜像
docker compose build --no-cache

# 进入后端容器
docker compose exec chatbase-backend sh

# 进入 MySQL 容器
docker compose exec mysql mysql -u root -p
```

## 数据库初始化

首次启动时，MySQL 会自动执行 `sql/init-schema.sql` 初始化数据库表结构。

如需手动执行：

```bash
docker compose exec mysql mysql -u chatbase -p chat_base < /docker-entrypoint-initdb.d/01-init-schema.sql
```

> ⚠️ 若服务器上的 MySQL 数据卷已存在（非全新初始化），`init-schema.sql` 不会自动执行。此时需手动执行增量升级脚本 `sql/upgrade-existing-db.sql`（幂等，可重复运行），补齐 `im_conversation`、`kb_keyword` 等表及新字段/索引。示例：
>
> ```bash
> docker compose exec -T chatbase-mysql mysql -u root -p"${MYSQL_ROOT_PASSWORD}" chat_base < sql/upgrade-existing-db.sql
> ```

## 健康检查

```bash
# 检查后端服务
curl http://localhost:8080/api/statistics/system/overview

# 检查前端服务
curl http://localhost:80/
```

## 数据持久化

数据存储在 Docker volumes 中：

- `mysql_data` - MySQL 数据
- `redis_data` - Redis 数据
- `qq_files` - QQ Bot 文件
- `napcat_data` - NapCat 配置
- `uploads_data` - 用户上传文件（头像、附件等，挂载到 `/app/uploads`）

备份数据：

```bash
docker compose exec mysql mysqldump -u root -p chat_base > backup.sql
```

## 更新部署

```bash
# 拉取最新代码
git pull

# 重新构建并启动
docker compose up --build -d

# 仅重启后端（修改配置后）
docker compose restart chatbase-backend
```

## 故障排查

### 后端无法连接 MySQL

```bash
# 检查 MySQL 是否运行
docker compose ps mysql

# 检查 MySQL 日志
docker compose logs mysql
```

### 后端无法连接 Redis

```bash
# 检查 Redis 是否运行
docker compose ps redis

# 测试 Redis 连接
docker compose exec redis redis-cli ping
```

### 前端无法访问后端 API

检查 Nginx 配置中的 `proxy_pass` 地址是否正确：
- 容器内使用服务名：`http://chatbase-backend:8080`

### QQ Bot 无法连接

```bash
# 检查 NapCat 状态
docker compose logs napcat

# 确认 WebSocket 连接配置
# NapCat 需配置反向 WebSocket: ws://chatbase-backend:8080/qq/ws
```

## 生产环境建议

1. **修改默认端口**：编辑 `docker-compose.yml` 修改对外端口
2. **配置 HTTPS**：使用 Nginx 或 Traefik 反向代理
3. **数据库备份**：设置定时备份任务
4. **监控告警**：配置 Prometheus + Grafana
5. **日志收集**：配置 ELK 或 Loki
6. **opencode 安全**：本机 serve 必须设置 `OPENCODE_SERVER_PASSWORD`；frp 隧道仅限可信主机；服务器侧 `OPENCODE_PASSWORD` 与之一致

## 目录结构

```
chatBase/
├── Dockerfile              # 后端镜像
├── docker-compose.yml      # 部署编排
├── .env.example            # 环境变量示例
├── sql/
│   ├── init-schema.sql          # 全新初始化脚本（首次挂载 volume 时自动执行）
│   └── upgrade-existing-db.sql  # 已有库升级脚本（幂等，可重复执行）
├── src/                    # 后端源码
│   └── main/java/com/zxl/chatbase/
│       ├── opencode/       # 本地 opencode serve 集成
│       └── ...             # 其余业务模块
└── web/
    ├── Dockerfile          # 前端镜像
    ├── nginx.conf          # Nginx 配置
    └── src/                # 前端源码
```
