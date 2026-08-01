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

### 3. 构建并启动服务

首次部署需要先构建镜像：

```bash
# 构建镜像并启动（首次部署）
docker compose up --build -d

# 仅启动（镜像已存在时）
docker compose up -d

# 启动包含QQ Bot的服务（首次）
docker compose --profile qq up --build -d
```

### 4. 查看日志

```bash
docker-compose logs -f chatbase-backend
```

## 服务说明

| 服务 | 端口 | 说明 |
|------|------|------|
| chatbase-frontend | 80 | Vue 前端，Nginx |
| chatbase-backend | 8080 | Spring Boot 后端 |
| mysql | 3306 | MySQL 8.0 |
| redis | 6379 | Redis 7 |
| napcat | 3000/6099 | QQ Bot（可选） |

## 可选服务

### QQ Bot (NapCat)

如需启用 QQ 群聊功能：

1. 设置 `QQ_BOT_ENABLE=true`
2. 配置 `QQ_BOT_SELF_ID`（机器人 QQ 号）
3. 首次启动需要扫码登录 NapCat

登录方式：
- 访问 `http://<server-ip>:6099` 打开 NapCat WebUI
- 扫码登录 QQ 账号

### 企业微信

如需启用企业微信功能，配置相关参数：
- `WECHAT_CORP_STOKEN`
- `WECHAT_CORP_S_ENCODING_AES_KEY`

### 本地 opencode serve（可选）

> 通过私聊会话远程驱动开发者**本机**的 opencode，实现"人在服务器、代理在本机"的远程编码代理。

#### 1. 架构

```
服务器 ChatBase ──frp 反向隧道──▶ 开发者本机 opencode serve (127.0.0.1:4096)
```

- opencode serve 只绑定本机 127.0.0.1，不暴露公网
- 服务器通过 frp 将本机 4096 端口映射为内网可访问的隧道地址
- 请求路径：私聊消息 → ChatBase 判断会话绑定（appId=-1）→ OpencodeService → frp → 本机 opencode

#### 2. 本机启动 opencode serve

```bash
# Windows PowerShell / macOS / Linux 均可
export OPENCODE_SERVER_PASSWORD="your-strong-password"   # 必设，Basic Auth
opencode serve --port 4096
```

- 用户名固定为 `opencode`（除非设置 `OPENCODE_SERVER_USERNAME`）
- 密码 `OPENCODE_PASSWORD` 必须与此处的 `OPENCODE_SERVER_PASSWORD` 一致

#### 3. frp 反向隧道（可选，生产环境必配）

在**开发者本机**运行 frp 客户端，将本机 opencode serve 暴露给服务器：

```ini
# frpc.ini（本机）
[opencode-serve]
type = tcp
local_ip = 127.0.0.1
local_port = 4096
remote_port = 4096
```

服务器需在 frps 中开放对应 `remote_port` 并设置 token。

> 若服务器与 opencode 在同一台机器，可省略 frp，直接将 `OPENCODE_BASE_URL` 指向本机地址。

#### 4. 服务器配置环境变量

在 `.env` 中添加：

```env
OPENCODE_ENABLED=true
OPENCODE_BASE_URL=http://<frp隧道地址>:4096   # 或本机 http://127.0.0.1:4096
OPENCODE_PASSWORD=your-strong-password
OPENCODE_DEFAULT_DIRECTORY=/path/to/project    # 本机项目根目录（可选）
OPENCODE_TIMEOUT_SECONDS=300
```

#### 5. 使用

1. 以 **admin** 登录 Web 控制台 → 「私聊采集」`/console/im/single`
2. 在会话详情「应用绑定」下拉中选择 **🖥️ 本地opencode** 并保存
3. 通过该平台私聊消息，即可远程驱动本机 opencode（回复写入 `kb_conversation` 审计）

⚠️ 只有 admin 用户能看到并绑定「本地opencode」选项。

## 常用命令

```bash
# 启动所有服务
docker-compose up -d

# 停止所有服务
docker-compose down

# 重启后端
docker-compose restart chatbase-backend

# 查看后端日志
docker-compose logs -f chatbase-backend

# 重新构建镜像
docker-compose build --no-cache

# 进入后端容器
docker-compose exec chatbase-backend sh

# 进入 MySQL 容器
docker-compose exec mysql mysql -u root -p
```

## 数据库初始化

首次启动时，MySQL 会自动执行 `sql/init-schema.sql` 初始化数据库表结构。

如需手动执行：

```bash
docker-compose exec mysql mysql -u chatbase -p chat_base < /docker-entrypoint-initdb.d/01-init-schema.sql
```

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

备份数据：

```bash
docker-compose exec mysql mysqldump -u root -p chat_base > backup.sql
```

## 更新部署

```bash
# 拉取最新代码
git pull

# 重新构建并启动
docker-compose up --build -d

# 仅重启后端（修改配置后）
docker-compose restart chatbase-backend
```

## 故障排查

### 后端无法连接 MySQL

```bash
# 检查 MySQL 是否运行
docker-compose ps mysql

# 检查 MySQL 日志
docker-compose logs mysql
```

### 后端无法连接 Redis

```bash
# 检查 Redis 是否运行
docker-compose ps redis

# 测试 Redis 连接
docker-compose exec redis redis-cli ping
```

### 前端无法访问后端 API

检查 Nginx 配置中的 `proxy_pass` 地址是否正确：
- 容器内使用服务名：`http://chatbase-backend:8080`

### QQ Bot 无法连接

```bash
# 检查 NapCat 状态
docker-compose logs napcat

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
│   └ init-schema.sql      # 数据库初始化脚本
│   └── add-cost-fields.sql # 增量更新脚本（可选）
├── src/                    # 后端源码
│   └── main/java/com/zxl/chatbase/
│       ├── opencode/       # 本地 opencode serve 集成
│       └── ...             # 其余业务模块
└── web/
    ├── Dockerfile          # 前端镜像
    ├── nginx.conf          # Nginx 配置
    └── src/                # 前端源码
```