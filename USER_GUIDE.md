# ChatBase 使用文档

## 1. 快速开始

### 1.1 环境要求

| 组件 | 版本要求 | 说明 |
|------|----------|------|
| Docker | 20.10+ | 容器运行时 |
| Docker Compose | 2.0+ | 服务编排 |
| Java（本地开发） | 17 | JDK 17 或更高 |
| Node.js（本地开发） | 18+ | 前端开发环境 |
| MySQL | 8.0 | 数据库 |
| Redis | 7 | 缓存 |

### 1.2 系统要求

- **CPU**：2 核及以上
- **内存**：4GB 及以上（推荐 8GB）
- **磁盘**：10GB 可用空间（含数据库数据、上传文件）
- **操作系统**：Linux / macOS / Windows（WSL2）

---

## 2. Docker 部署（推荐）

### 2.1 克隆项目

```bash
git clone <repository-url>
cd chatBase
```

### 2.2 配置环境变量

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑配置文件
vim .env
```

### 2.3 环境变量说明

| 变量名 | 必填 | 说明 | 示例 |
|--------|------|------|------|
| `MYSQL_ROOT_PASSWORD` | 是 | MySQL root 密码 | `root123` |
| `MYSQL_USER` | 是 | 数据库用户名 | `chatbase` |
| `MYSQL_PASSWORD` | 是 | 数据库密码 | `chatbase123` |
| `REDIS_PASSWORD` | 否 | Redis 密码（留空表示无密码） | `redis123` |
| `DIFYAPP_API_KEY` | 是 | Dify Chat API Key | `app-xxxxxxxx` |
| `DIFYAPP_DATASET_API_KEY` | 是 | Dify Dataset API Key | `dataset-xxxxxxxx` |
| `QQ_BOT_ENABLE` | 否 | 启用 QQ 机器人（true/false） | `false` |
| `QQ_BOT_ACCESS_TOKEN` | 否 | NapCat 访问 Token | `your-token` |
| `QQ_BOT_SELF_ID` | 否 | 机器人 QQ 号 | `123456789` |
| `QQ_BOT_HTTP_BASE_URL` | 否 | NapCat HTTP 地址 | `http://napcat:3000` |
| `WECHAT_CORP_STOKEN` | 否 | 企业微信 Token | `your-token` |
| `WECHAT_CORP_S_ENCODING_AES_KEY` | 否 | 企业微信 EncodingAESKey | `your-aes-key` |
| `WECHAT_CORP_BOT_ID` | 否 | 企业微信机器人 ID | `your-bot-id` |
| `WECHAT_CORP_SECRET` | 否 | 企业微信机器人 Secret | `your-secret` |
| `JAVA_OPTS` | 否 | JVM 参数 | `-Xms512m -Xmx2048m` |
| `NAPCAT_IMAGE` | 否 | NapCat 镜像名称 | `mlikiowa/napcat-docker:v4.17.46` |

### 2.4 启动服务

```bash
# 构建并启动所有服务
docker-compose up --build -d

# 查看服务状态
docker-compose ps

# 查看后端日志
docker-compose logs -f chatbase-backend

# 查看前端日志
docker-compose logs -f chatbase-frontend
```

### 2.5 启动 QQ 机器人（可选）

```bash
# 启动 NapCat 服务
docker-compose --profile qq up -d

# 访问 NapCat 管理界面
# http://<服务器IP>:6099
# 扫码登录 QQ，配置反向 WebSocket
```

### 2.6 访问系统

- **前端页面**：http://localhost
- **后端 API**：http://localhost:8080

### 2.7 停止服务

```bash
# 停止所有服务
docker-compose down

# 停止并删除数据卷（慎用！）
docker-compose down -v
```

---

## 3. 本地开发

### 3.1 启动数据库

```bash
# 使用 Docker 启动 MySQL 和 Redis
docker run -d --name mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=zxl123 \
  -e MYSQL_DATABASE=chat_base \
  mysql:8.0 \
  --character-set-server=utf8mb4 \
  --collation-server=utf8mb4_unicode_ci

docker run -d --name redis \
  -p 6379:6379 \
  redis:7

# 初始化数据库
mysql -u root -pzxl123 chat_base < sql/init-schema.sql
```

### 3.2 启动后端

```bash
# 配置 application-local.yaml
# 确保数据库连接信息正确

# 启动 Spring Boot
mvn spring-boot:run

# 或使用 IDE 运行 ChatBaseApplication.java
```

### 3.3 启动前端

```bash
cd web

# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 访问 http://localhost:5173
```

### 3.4 前端构建

```bash
# TypeScript 类型检查 + 构建
npm run build

# 构建产物在 web/dist 目录
```

---

## 4. 配置说明

### 4.1 配置文件结构

```
src/main/resources/
├── application.yaml              # 主配置文件
├── application-local.yaml        # 本地开发配置
└── application-prod.yaml         # 生产环境配置
```

### 4.2 主配置（application.yaml）

```yaml
server:
  port: 8080                      # 服务端口

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/chat_base  # 数据库连接
    username: root
    password: zxl123
  redis:
    host: localhost
    port: 6379

chat:
  max-turns-per-session: 20       # 单会话最大轮数
  session-ttl-days: 7             # 会话 TTL（天）
  rate-limit:
    window-seconds: 5             # 限流窗口（秒）
    max-requests: 1               # 窗口内最大请求数
```

### 4.3 本地配置（application-local.yaml）

```yaml
# Dify 配置
difyApp:
  url: "https://api.dify.ai/v1"
  apiKey: "app-xxxxxxxx"          # Dify Chat API Key
  datasetApiKey: "dataset-xxxxxxxx"  # Dify Dataset API Key
  timeOut: 180                    # 超时时间（秒）

# QQ 机器人配置
qq:
  bot:
    enable: true                  # 启用 QQ 机器人
    ws-port: 8081                 # WebSocket 端口
    access-token: "your-token"    # NapCat Token
    self-id: 123456789            # 机器人 QQ 号
    nickname: ""                  # 机器人昵称（可选）
    http-base-url: "http://127.0.0.1:3000"  # NapCat HTTP 地址
    file-save-path: /data/qq_files/  # 文件保存路径

# 企业微信配置
wechat:
  corp:
    stoken: "your-token"          # 企业微信 Token
    sEncodingAESKey: "your-aes-key"  # EncodingAESKey
    botName: "企业内部机器人"      # 机器人显示名称
```

### 4.4 生产配置（application-prod.yaml）

```yaml
# 使用环境变量注入
difyApp:
  apiKey: ${DIFYAPP_API_KEY}
  datasetApiKey: ${DIFYAPP_DATASET_API_KEY}

qq:
  bot:
    access-token: ${QQ_BOT_ACCESS_TOKEN}
    self-id: ${QQ_BOT_SELF_ID}
    http-base-url: ${QQ_BOT_HTTP_BASE_URL}

wechat:
  corp:
    stoken: ${WECHAT_CORP_STOKEN}
    sEncodingAESKey: ${WECHAT_CORP_S_ENCODING_AES_KEY}
```

---

## 5. QQ 机器人配置

### 5.1 NapCat 安装

NapCat 是基于 OneBot 协议的 QQ 机器人框架。

**Docker 部署**：
```bash
docker-compose --profile qq up -d
```

**访问管理界面**：
- URL：http://<服务器IP>:6099
- 使用小号扫码登录（⚠️ 务必使用小号，防止封号）

### 5.2 NapCat 网络配置

**配置反向 WebSocket**：
1. 登录 NapCat 管理界面
2. 进入"网络配置"
3. 添加反向 WebSocket 客户端：
   - URL：`ws://chatbase-backend:8080/qq/ws`
   - 启用：是

**配置 HTTP 服务器**：
1. 添加 HTTP 服务器：
   - 地址：`0.0.0.0`
   - 端口：`3000`
   - 启用：是

### 5.3 ChatBase 配置

在 `application-local.yaml` 或 `.env` 中配置：

```yaml
qq:
  bot:
    enable: true
    access-token: "your-napcat-token"  # 与 NapCat 配置一致
    self-id: 123456789                # 你的机器人 QQ 号
    http-base-url: "http://napcat:3000"  # NapCat HTTP 地址
    nickname: "ChatBase"              # 可选，机器人显示名称
```

### 5.4 测试 QQ 机器人

1. 确保 NapCat 已登录并连接
2. 在 QQ 群中 @机器人 发送消息
3. 查看后端日志确认消息接收
4. 机器人应回复 AI 回答

### 5.5 常见问题

| 问题 | 原因 | 解决方法 |
|------|------|----------|
| QQ 消息收到但不回复 | 未 @机器人 | 确保消息中包含 @机器人 |
| 机器人回复慢 | Dify API 响应慢 | 检查网络连接，增加超时时间 |
| NapCat 连接失败 | WebSocket 地址错误 | 检查 `ws://chatbase-backend:8080/qq/ws` 是否正确 |
| 限流提示 | 5 秒内多次 @机器人 | 等待 5 秒后再次发送 |

---

## 6. 企业微信配置

### 6.1 创建企业微信机器人

1. 登录企业微信管理后台
2. 进入"应用管理" → "机器人"
3. 创建新机器人
4. 记录以下信息：
   - CorpID
   - BotID
   - Secret

### 6.2 配置回调 URL

在企业微信管理后台配置：

- **回调 URL**：`http://<服务器域名>/intellrobot/callback/handle`
- **Token**：自定义（与配置文件一致）
- **EncodingAESKey**：随机生成（与配置文件一致）

### 6.3 ChatBase 配置

在 `.env` 中配置：

```bash
WECHAT_CORP_STOKEN=your-token
WECHAT_CORP_S_ENCODING_AES_KEY=your-aes-key
WECHAT_CORP_BOT_ID=your-bot-id
WECHAT_CORP_SECRET=your-secret
```

### 6.4 测试企业微信机器人

1. 在企业微信群中添加机器人
2. 发送消息到群聊
3. 查看后端日志确认消息接收
4. 机器人应异步回复 AI 回答

### 6.5 注意事项

- 企业微信要求 5 秒内响应，系统采用异步处理
- 确保回调 URL 可公网访问
- 消息加解密配置必须正确，否则无法接收消息

---

## 7. 功能操作指南

### 7.1 用户注册与登录

#### 7.1.1 注册账号

1. 访问登录页面（http://localhost/login）
2. 点击"注册"按钮
3. 填写：
   - 用户名（唯一，登录用）
   - 密码（至少 6 位）
   - 昵称（可选）
4. 点击"注册"完成

#### 7.1.2 登录系统

1. 输入用户名和密码
2. 点击"登录"
3. 登录成功后跳转到控制台

#### 7.1.3 修改个人信息

1. 点击右上角用户头像
2. 选择"个人信息"
3. 可修改：
   - 昵称
   - 邮箱
   - 手机号
   - 头像（支持裁切）
4. 点击"保存"

#### 7.1.4 修改密码

1. 点击用户头像 → "修改密码"
2. 输入旧密码
3. 输入新密码（两次确认）
4. 点击"保存"

### 7.2 AI 问答（Web 端）

#### 7.2.1 开始对话

1. 点击左侧菜单"AI 问答"
2. 点击"新建会话"
3. 在输入框输入问题
4. 按 Enter 或点击发送按钮

#### 7.2.2 会话管理

- **切换会话**：点击左侧会话列表
- **删除会话**：点击会话右侧删除图标
- **重命名会话**：点击会话标题编辑
- **查看历史**：自动保存所有对话记录

#### 7.2.3 文件附件

1. 点击输入框左侧"附件"按钮
2. 选择文件（支持拖拽）
3. 支持本地上传或输入 URL
4. 发送时自动上传到 Dify

#### 7.2.4 引用来源

- AI 回答底部显示引用的知识库文档
- 点击可查看文档详情
- 显示匹配度百分比

#### 7.2.5 反馈

- 点击回答底部的 👍 或 👎
- 可填写详细反馈内容
- 管理员可查看并回复

### 7.3 知识库管理

#### 7.3.1 分类管理

1. 点击左侧菜单"知识库"
2. 切换到"分类"标签页
3. 点击"添加分类"
4. 填写：
   - 分类名称
   - 父分类（可选，支持树形结构）
   - 图标、排序、描述
5. 点击"保存"

**操作**：
- 编辑分类：点击编辑图标
- 删除分类：点击删除图标（需无子分类和知识库）
- 拖拽排序：调整 sort_order

#### 7.3.2 创建知识库

1. 切换到"知识库"标签页
2. 点击"创建知识库"
3. 填写：
   - 知识库名称
   - 所属分类
   - 描述
4. 点击"保存"（自动调用 Dify API 创建 Dataset）

#### 7.3.3 上传文档

1. 在知识库列表中点击目标知识库
2. 切换到"文档"标签页
3. 点击"批量上传"
4. 选择文件（支持多选、拖拽）
5. 支持格式：TXT、PDF、DOCX、MD 等
6. 点击"上传"

**上传进度**：
- 实时显示进度条
- SSE 推送更新
- 上传完成自动刷新列表

#### 7.3.4 文档管理

- **查看文档**：点击文档名称查看内容
- **编辑文档**：修改标题、内容
- **删除文档**：从本地和 Dify 同步删除
- **同步文档**：手动同步到 Dify
- **搜索文档**：输入关键词搜索

#### 7.3.5 从 Dify 同步

1. 点击"从 Dify 同步"按钮
2. 系统拉取 Dify 中所有数据集和文档
3. 自动同步到本地数据库

### 7.4 FAQ 管理

#### 7.4.1 手动添加 FAQ

1. 点击左侧菜单"FAQ 管理"
2. 点击"添加 FAQ"
3. 填写：
   - 问题
   - 答案（支持 Markdown）
   - 关键词（逗号分隔）
   - 相似问题（JSON 数组）
   - 优先级（数字越大越优先）
4. 点击"保存"

#### 7.4.2 自动提取 FAQ

1. 点击"自动提取"按钮
2. 设置参数：
   - 时间范围（最近 N 天）
   - 最小命中次数
   - 目标知识库
3. 点击"提取"
4. 系统从历史对话中批量提取高频问答

#### 7.4.3 管理 FAQ

- **搜索**：输入问题或关键词搜索
- **编辑**：点击编辑图标
- **删除**：点击删除图标
- **启用/禁用**：切换状态开关
- **查看统计**：命中次数、满意度

### 7.5 应用管理

#### 7.5.1 创建应用

1. 点击左侧菜单"应用管理"
2. 点击"创建应用"
3. 按步骤指引填写：
   - **步骤 1**：应用名称、描述、图标
   - **步骤 2**：Dify API Key（点击"验证"检查有效性）
   - **步骤 3**：关联分类（可选）
4. 点击"保存"

#### 7.5.2 验证 API Key

1. 在创建/编辑弹窗中输入 API Key
2. 点击"验证"按钮
3. 系统调用 Dify `/v1/info` 验证
4. 显示应用名称和模式

#### 7.5.3 设置默认应用

1. 在应用卡片中点击"设为默认"
2. 系统对话将优先使用该应用
3. 只能有一个默认应用

#### 7.5.4 查看绑定群组

1. 点击应用卡片"查看群组"
2. 显示所有绑定该应用的群组
3. 可跳转到群聊管理页面

### 7.6 群聊采集管理

#### 7.6.1 查看群列表

1. 点击左侧菜单"群聊采集"
2. 选择平台标签（全部/QQ/企微）
3. 左侧显示群列表
4. 显示信息：
   - 群名称
   - 成员数
   - 消息数
   - 最后活跃时间

#### 7.6.2 绑定应用

1. 点击群名称查看详情
2. 在"应用绑定"区域选择应用
3. 点击"保存"
4. 该群的消息将使用绑定应用回答

#### 7.6.3 解绑应用

1. 在详情面板点击"解绑"
2. 确认操作
3. 群恢复使用默认应用

#### 7.6.4 查看消息

1. 在详情面板切换到"消息"标签
2. 支持搜索关键词
3. 分页显示消息列表
4. 显示：
   - 发送人
   - 消息内容
   - 发送时间
   - 同步状态

### 7.7 机器人管理

#### 7.7.1 查看机器人列表

1. 点击左侧菜单"机器人"
2. 显示统计卡片：
   - 机器人总数
   - 在线数量
   - 今日消息数
   - 总消息数

#### 7.7.2 机器人状态

- **在线**：绿色圆点（QQ 机器人有心跳）
- **离线**：灰色圆点
- **QQ 机器人**：
  - 显示昵称（API 自动获取或配置）
  - 显示 QQ 号
  - 显示群聊数、消息统计
- **企业微信机器人**：
  - 默认在线
  - 显示配置名称

#### 7.7.3 刷新状态

点击"刷新"按钮更新在线状态和统计数据

### 7.8 数据统计

#### 7.8.1 Token 统计

1. 点击左侧菜单"数据统计"
2. 查看 Token 消耗趋势图
3. 切换时间周期（7 天/30 天）
4. 查看本月统计：
   - 总 Token
   - Prompt Token
   - Completion Token
   - 预测用量

#### 7.8.2 费用统计

1. 查看费用消耗趋势图
2. 支持按日/本月切换
3. 显示 Prompt/Completion 费用分离

#### 7.8.3 群活跃度

1. 查看群聊活跃度排行榜
2. 按消息数排序
3. 显示平台图标（QQ/企微）

#### 7.8.4 关键词热度

1. 查看关键词词云
2. 词大小表示出现频率
3. 支持按来源过滤（对话/IM/文档）

#### 7.8.5 问答成功率

1. 查看成功率进度条
2. 用户满意度统计
3. 反馈数量统计

#### 7.8.6 聚合统计

1. 点击"聚合统计"按钮
2. 手动触发昨日数据聚合
3. 用于补录历史数据

### 7.9 反馈管理

#### 7.9.1 用户提交反馈

1. 点击左侧菜单"用户反馈"
2. 切换到"提交反馈"标签
3. 填写：
   - 星级评分（1-5 星）
   - 反馈类型（6 种类型）
   - 详细描述
   - 联系方式（邮箱或手机号）
4. 点击"提交"

#### 7.9.2 查看我的反馈

1. 切换到"我的反馈"标签
2. 查看历史反馈列表
3. 显示管理员回复

#### 7.9.3 管理员处理反馈

1. 点击左侧菜单"反馈管理"
2. 查看统计概览：
   - 总反馈数
   - 待处理
   - 已处理
   - 平均评分
3. 按状态筛选（全部/待处理/已处理）
4. 点击反馈查看详情
5. 填写管理员回复
6. 点击"标记已处理"

---

## 8. 定时任务说明

### 8.1 任务列表

| 任务 | 执行时间 | 功能 | 配置项 |
|------|----------|------|--------|
| Redis Stream 消费 | 每 5 秒 | 实时处理 IM 消息 | `im.sync.stream.enabled` |
| 定时同步（废弃） | 每 60 秒 | 批量同步群消息 | `im.sync.polling.enabled` |
| 统计聚合 | 每天 00:05 | 聚合昨日统计数据 | - |
| 关键词提取 | 每天 05:00 | 从对话中提取关键词 | - |
| 关键词清理 | 每天 06:00 | 清理 90 天前关键词 | - |
| 会话清理 | 每天 03:00 | 清理过期会话 | - |
| 消息清理 | 每天 04:30 | 清理 90 天前消息 | - |

### 8.2 切换消息同步方案

**使用 Redis Stream（推荐）**：
```yaml
im:
  sync:
    stream:
      enabled: true
    polling:
      enabled: false
```

**使用定时轮询（降级）**：
```yaml
im:
  sync:
    stream:
      enabled: false
    polling:
      enabled: true
```

---

## 9. 数据备份与恢复

### 9.1 数据库备份

```bash
# 备份 MySQL 数据库
docker exec chatbase-mysql mysqldump -u root -p${MYSQL_ROOT_PASSWORD} chat_base > backup_$(date +%Y%m%d).sql

# 备份到本地
scp user@server:/path/to/backup.sql ./
```

### 9.2 数据库恢复

```bash
# 恢复数据库
docker exec -i chatbase-mysql mysql -u root -p${MYSQL_ROOT_PASSWORD} chat_base < backup_20260507.sql
```

### 9.3 文件备份

```bash
# 备份上传文件
tar -czf uploads_backup_$(date +%Y%m%d).tar.gz uploads/
```

---

## 10. 故障排查

### 10.1 后端启动失败

**检查日志**：
```bash
docker-compose logs chatbase-backend
```

**常见问题**：

| 问题 | 原因 | 解决方法 |
|------|------|----------|
| 数据库连接失败 | MySQL 未启动或密码错误 | 检查 `MYSQL_PASSWORD` 配置 |
| Redis 连接失败 | Redis 未启动或密码错误 | 检查 `REDIS_PASSWORD` 配置 |
| 端口被占用 | 8080 端口已被使用 | 修改 `server.port` 或停止占用进程 |
| Dify API Key 无效 | Key 配置错误 | 检查 `DIFYAPP_API_KEY` |

### 10.2 前端无法访问

**检查日志**：
```bash
docker-compose logs chatbase-frontend
```

**常见问题**：

| 问题 | 原因 | 解决方法 |
|------|------|----------|
| 502 Bad Gateway | 后端未启动 | 检查后端服务状态 |
| 页面空白 | 构建失败 | 检查前端日志，重新构建 |
| API 请求失败 | Nginx 配置错误 | 检查 `web/nginx.conf` |

### 10.3 QQ 机器人不回复

**排查步骤**：

1. 检查 NapCat 是否在线
   ```bash
   docker-compose ps chatbase-napcat
   ```

2. 检查 WebSocket 连接
   ```bash
   docker-compose logs chatbase-backend | grep "WebSocket"
   ```

3. 检查消息是否接收
   ```bash
   docker-compose logs chatbase-backend | grep "group_message"
   ```

4. 检查是否 @机器人
   - 必须 @机器人 才会回复
   - 仅发消息不会触发

5. 检查限流
   - 5 秒窗口内只能 @1 次
   - 等待 5 秒后重试

### 10.4 企业微信机器人不回复

**排查步骤**：

1. 检查回调 URL 是否可访问
   ```bash
   curl http://<域名>/intellrobot/callback/handle
   ```

2. 检查加解密配置
   - Token 必须一致
   - EncodingAESKey 必须一致

3. 检查后端日志
   ```bash
   docker-compose logs chatbase-backend | grep "wechat"
   ```

4. 检查分布式锁
   - 同一消息不会重复处理
   - 锁 TTL 5 分钟

### 10.5 知识库同步失败

**排查步骤**：

1. 检查 Dify API Key
   ```bash
   curl -H "Authorization: Bearer $DIFYAPP_DATASET_API_KEY" \
     https://api.dify.ai/v1/datasets
   ```

2. 检查网络连接
   ```bash
   curl -I https://api.dify.ai
   ```

3. 检查文档格式
   - 支持 TXT、PDF、DOCX、MD
   - 文件大小限制 100MB

4. 查看上传进度
   - 前端显示进度条
   - SSE 实时推送

---

## 11. 性能调优

### 11.1 JVM 参数

```bash
# 在 .env 中配置
JAVA_OPTS=-Xms512m -Xmx2048m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError
```

**推荐配置**：

| 内存 | Xms | Xmx | 适用场景 |
|------|-----|-----|----------|
| 4GB | 512m | 1024m | 小型部署 |
| 8GB | 1024m | 2048m | 中型部署 |
| 16GB | 2048m | 4096m | 大型部署 |

### 11.2 数据库优化

**连接池配置**：
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10      # 最大连接数
      minimum-idle: 5            # 最小空闲连接
      connection-timeout: 30000  # 连接超时（毫秒）
      idle-timeout: 600000       # 空闲超时（毫秒）
      max-lifetime: 1800000      # 连接最大生命周期（毫秒）
```

**索引优化**：
- 高频查询字段已建立索引
- 避免全表扫描
- 使用 EXPLAIN 分析慢查询

### 11.3 Redis 优化

**内存管理**：
```bash
# 设置最大内存
redis-cli CONFIG SET maxmemory 512mb

# 设置淘汰策略
redis-cli CONFIG SET maxmemory-policy allkeys-lru
```

**持久化**：
- AOF 已启用（appendonly yes）
- 定期 RDB 快照

### 11.4 Nginx 优化

```nginx
# 启用 Gzip 压缩
gzip on;
gzip_types text/plain application/json text/css application/javascript;

# 缓存静态文件
location /assets/ {
  expires 30d;
  add_header Cache-Control "public, immutable";
}

# 限制上传大小
client_max_body_size 100m;
```

---

## 12. 安全建议

### 12.1 密码安全

- 使用强密码（至少 8 位，包含大小写字母、数字、特殊字符）
- 定期更换密码
- 不要使用默认密码

### 12.2 网络安全

- 生产环境使用 HTTPS
- 配置防火墙，仅开放必要端口
- 使用反向代理（Nginx）

### 12.3 数据安全

- 定期备份数据库
- 敏感信息加密存储（密码 BCrypt 加密）
- 不要将 `.env` 文件提交到 Git

### 12.4 访问控制

- 限制管理员账号数量
- 普通用户默认 role=user
- 定期审查用户权限

---

## 13. 常见问题（FAQ）

### 13.1 部署相关

**Q：Docker 启动后无法访问前端？**
A：检查前端容器状态 `docker-compose ps`，查看日志 `docker-compose logs chatbase-frontend`。

**Q：数据库初始化失败？**
A：确保 `sql/init-schema.sql` 文件存在，MySQL 容器已完全启动（healthcheck 通过）。

**Q：如何重置数据库？**
A：`docker-compose down -v` 删除数据卷，然后 `docker-compose up -d` 重新初始化。

### 13.2 功能相关

**Q：为什么 QQ 消息收到但不回复？**
A：必须 @机器人 才会触发回复，仅发消息不会回复。

**Q：如何修改会话最大轮数？**
A：修改 `chat.max-turns-per-session` 配置，默认 20 轮。

**Q：如何查看 Dify 数据集？**
A：在知识库管理页面点击"从 Dify 同步"，或调用 `/api/kb/dify/list`。

**Q：FAQ 自动提取不准确？**
A：调整提取参数（时间范围、最小命中次数），或手动编辑 FAQ。

**Q：如何更换默认应用？**
A：在应用管理页面，点击目标应用的"设为默认"按钮。

### 13.3 性能相关

**Q：系统响应慢怎么办？**
A：
1. 检查 Dify API 响应时间
2. 增加 JVM 内存（JAVA_OPTS）
3. 优化数据库查询（添加索引）
4. 检查网络延迟

**Q：Redis 内存占用高？**
A：
1. 检查 Key 数量 `redis-cli DBSIZE`
2. 设置 maxmemory 和淘汰策略
3. 清理过期 Key

**Q：数据库连接数过多？**
A：
1. 调整 HikariCP 连接池大小
2. 检查是否有连接泄漏
3. 增加 MySQL max_connections

### 13.4 其他

**Q：支持哪些文件格式上传？**
A：TXT、PDF、DOCX、MD、HTML 等文本格式，不支持图片、视频。

**Q：如何自定义前端主题？**
A：修改 `web/src/styles/cyberpunk.css`，或添加新样式文件。

**Q：能否对接其他 AI 平台？**
A：当前仅支持 Dify，可扩展 `dify` 模块适配其他平台。

**Q：是否支持多语言？**
A：当前仅支持中文，可扩展 i18n 支持多语言。

---

## 14. 版本历史

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| v1.0 | 2026-05-07 | 初始版本，完整功能 |

---

## 15. 技术支持

- **项目地址**：GitHub Repository
- **问题反馈**：Issues
- **文档**：README.md, DESIGN.md, USER_GUIDE.md

---

*文档版本：v1.0*
*最后更新：2026-05-07*
