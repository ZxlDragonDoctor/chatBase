# ChatBase

基于 Spring Boot + Vue 3 的智能对话系统，集成 Dify AI 平台，支持 QQ 群聊和企业微信消息收集与智能回复。

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.6-brightgreen)
![Vue](https://img.shields.io/badge/Vue-3.5-green)
![License](https://img.shields.io/badge/License-MIT-blue)

## 功能特性

### 核心功能

| 功能 | 说明 |
|------|------|
| **智能对话** | 集成 Dify AI，支持多轮对话、上下文记忆、FAQ 优先匹配 |
| **知识库管理** | 批量上传文档、自动同步到 Dify、搜索、分类管理 |
| **FAQ 管理** | 自动提取高频问答、手动维护、优先级匹配 |
| **会话管理** | 多会话切换、历史记录、自定义标题 |
| **应用管理** | 多 Dify 应用配置、群组绑定、权限控制 |

### IM 集成

| 平台 | 接入方式 | 功能 |
|------|----------|------|
| **QQ 群聊** | NapCat 反向 WebSocket | 扫码登录、消息收集、智能回复、在线监控 |
| **企业微信** | 回调模式 | 消息收集、智能回复、加解密 |
| **微信个人号** | iLink 协议 | 扫码登录、消息收集、智能回复 |

### 数据分析

- **Token 统计**：消耗趋势图、月度统计、预测分析
- **费用统计**：Prompt/Completion 分离计费、费用趋势
- **关键词热度**：词云展示、多渠道提取
- **用户反馈**：评分、反馈表单、满意度分析
- **群活跃度**：按平台统计、排名

### 机器人管理

- 多机器人状态监控（QQ / 企业微信 / 微信个人号）
- 扫码登录（微信 ilink / QQ NapCat WebUI 代理）
- 在线状态实时检测（Redis 心跳）
- 消息统计（今日/总计）
- QQ 昵称自动获取（OneBot API）

## 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 17 | 开发语言 |
| Spring Boot | 2.7.6 | 应用框架 |
| MyBatis-Plus | 3.5.15 | ORM 框架 |
| WebSocket | - | QQ 机器人通信 |
| Redis | 7 | 缓存、会话、消息队列 |
| MySQL | 8.0 | 关系数据库 |

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5 | UI 框架 |
| TypeScript | 5.7 | 类型安全 |
| Vite | 6.1 | 构建工具 |
| ECharts | 6.0 | 数据可视化 |
| Lucide Icons | - | 图标库 |
| CropperJS | 1.6.2 | 头像裁切 |

## 快速开始

### Docker 部署（推荐）

```bash
# 1. 克隆项目
git clone <repository-url>
cd chatBase

# 2. 配置环境变量
cp .env.example .env
vim .env  # 填写必填配置

# 3. 构建并启动
docker-compose up --build -d

# 4. 查看日志
docker-compose logs -f chatbase-backend
```

访问 http://localhost 打开前端页面。

### 本地开发

```bash
# 1. 启动 MySQL 和 Redis
docker run -d --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=zxl123 -e MYSQL_DATABASE=chat_base mysql:8.0
docker run -d --name redis -p 6379:6379 redis:7

# 2. 初始化数据库
mysql -u root -pzxl123 chat_base < sql/init-schema.sql

# 3. 启动后端
mvn spring-boot:run

# 4. 启动前端
cd web && npm install && npm run dev
```

访问 http://localhost:5173 打开前端页面。

## 配置说明

### 必填配置

| 环境变量 | 说明 | 示例 |
|----------|------|------|
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 | `root123` |
| `MYSQL_PASSWORD` | 数据库密码 | `chatbase123` |
| `DIFYAPP_API_KEY` | Dify Chat API Key | `app-xxxxxxxx` |
| `DIFYAPP_DATASET_API_KEY` | Dify Dataset API Key | `dataset-xxxxxxxx` |

### 可选配置

| 环境变量 | 说明 | 默认值 |
|----------|------|--------|
| `REDIS_PASSWORD` | Redis 密码 | 无 |
| `QQ_BOT_ENABLE` | 启用 QQ 机器人 | `false` |
| `QQ_BOT_ACCESS_TOKEN` | NapCat Token | - |
| `QQ_BOT_SELF_ID` | 机器人 QQ 号 | - |
| `QQ_BOT_HTTP_BASE_URL` | NapCat HTTP 地址 | `http://napcat:3000` |
| `QQ_BOT_WEBUI_BASE_URL` | NapCat WebUI 地址 | `http://napcat:6099` |
| `QQ_BOT_WEBUI_TOKEN` | NapCat WebUI token | - |
| `WECHAT_CORP_STOKEN` | 企业微信 Token | - |
| `WECHAT_CORP_S_ENCODING_AES_KEY` | 企业微信 EncodingAESKey | - |
| `JAVA_OPTS` | JVM 参数 | `-Xms512m -Xmx2048m` |

完整配置说明请参考 [USER_GUIDE.md](./USER_GUIDE.md#4-配置说明)。

## 功能模块

### 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                        前端层 (Vue 3)                        │
│  登录注册 │ AI 问答 │ 知识库 │ 统计面板 │ 控制台 │ 机器人管理     |
└───────────────────────────┬─────────────────────────────────┘
                            │ HTTP / WebSocket
┌───────────────────────────▼─────────────────────────────────┐
│                      后端层 (Spring Boot)                    |
│  Chat │ Dify │ KB │ IM │ QQ │ WeChat │ Statistics │ User    │
└───────────────┬───────────────────────┬─────────────────────┘
                │                       │
┌───────────────▼──────────┐  ┌────────▼────────────────────┐
│    数据层                 │  │    外部服务                  │
│  MySQL 8.0 │ Redis 7     │  │  Dify API │ NapCat │ 企业微信|
└──────────────────────────┘  └─────────────────────────────┘
```

### 模块说明

| 模块 | 包路径 | 职责 |
|------|--------|------|
| **chat** | `com.zxl.chatbase.chat` | 聊天会话管理、消息处理、数据清理 |
| **dify** | `com.zxl.chatbase.dify` | Dify API 集成、对话、文件上传 |
| **kb** | `com.zxl.chatbase.kb` | 知识库、分类、文档、FAQ、应用、关键词 |
| **im** | `com.zxl.chatbase.im` | IM 消息采集、群组管理、机器人管理 |
| **qq** | `com.zxl.chatbase.qq` | QQ 机器人 WebSocket + WebUI 扫码登录代理 |
| **wxroboot** | `com.zxl.chatbase.wxroboot` | 企业微信回调处理、消息加解密 |
| **statistics** | `com.zxl.chatbase.statistics` | 统计分析、Token、费用、关键词 |
| **user** | `com.zxl.chatbase.user` | 用户注册、登录、信息管理 |

### 页面功能

| 页面 | 路径 | 权限 | 功能 |
|------|------|------|------|
| 登录注册 | `/login` | 公开 | 用户认证 |
| 系统概览 | `/console/dashboard` | 登录 | 统计卡片、快捷导航 |
| 数据统计 | `/console/statistics` | 登录 | Token/费用趋势、词云、活跃度（admin 可切换全部/个人） |
| 群聊采集 | `/console/im` | 登录 | 群列表、消息查询、应用绑定 |
| 知识库管理 | `/console/knowledge` | 登录 | 分类、知识库、文档、FAQ |
| 应用管理 | `/console/app` | 登录 | Dify 应用配置、API Key 验证 |
| 机器人管理 | `/console/bots` | 登录 | 机器人状态、消息统计 |
| FAQ 管理 | `/console/faq` | 登录 | 手动维护、自动提取 |
| AI 问答 | `/chat` | 公开 | 多会话对话、文件附件、引用来源 |
| 用户反馈 | `/feedback` | 公开 | 提交反馈、查看历史 |
| 反馈管理 | `/console/feedback-manage` | admin | 反馈处理、回复 |
| 应用管理(全部) | `/console/admin/apps` | admin | 所有应用管理 |
| 知识库管理(全部) | `/console/admin/kbs` | admin | 所有知识库管理 |
| 用户管理 | `/console/admin/users` | admin | 用户增删改查 |

### 数据隔离与权限

- **用户角色**：`admin` / `user`，通过 AuthInterceptor + AdminInterceptor 控制
- **数据隔离规则**：`created_by = 当前用户 OR created_by IS NULL`（系统级记录所有人可见）
- **统计页**：admin 可通过 `scope=all`（默认）看全部数据，`scope=mine` 看自己的；普通用户始终只看自己的
- **群聊可见性**：`created_by IS NULL`（未分配/公共）或 `created_by = 当前用户`（已认领）
- **分类可见性**：`create_by = 当前用户` 或 `create_by IS NULL`（系统默认分类）
- **应用/知识库**：按 `created_by = 当前用户` 过滤
- **pom.xml** 必须配置 `<parameters>true</parameters>` 在 `maven-compiler-plugin` 中，否则 `@RequestParam`/`@RequestAttribute` 运行时报 `Name not specified` 异常

## QQ 机器人配置

### 1. 启动 NapCat

```bash
docker-compose --profile qq up -d
```

### 2. 扫码登录（WebUI 代理模式）

在「机器人管理」页面点击 QQ 卡片「扫码登录」，后端代理 NapCat WebUI API，一键扫码。
也可直接访问 http://\<server\>:6099 手动登录。

⚠️ **务必使用小号，防止封禁。**

### 3. 配置网络

**反向 WebSocket**：
- URL：`ws://chatbase-backend:8080/qq/ws`
- 启用：是

**HTTP 服务器**：
- 地址：`0.0.0.0`
- 端口：`3000`
- 启用：是

### 4. 配置 ChatBase

```yaml
qq:
  bot:
    enable: true
    access-token: "your-napcat-token"
    self-id: 123456789
    http-base-url: "http://napcat:3000"
    nickname: "ChatBase"  # 可选
```

### 5. 测试

在 QQ 群中 @机器人 发送消息，等待回复。

⚠️ **必须 @机器人 才会触发回复。**

## 企业微信配置

### 1. 创建机器人

企业微信管理后台 → 应用管理 → 机器人 → 创建新机器人

### 2. 配置回调 URL

- **URL**：`http://\<server\>/intellrobot/callback/handle`
- **Token**：自定义（与配置一致）
- **EncodingAESKey**：随机生成（与配置一致）

### 3. 配置 ChatBase

```bash
WECHAT_CORP_STOKEN=your-token
WECHAT_CORP_S_ENCODING_AES_KEY=your-aes-key
WECHAT_CORP_BOT_ID=your-bot-id
WECHAT_CORP_SECRET=your-secret
```

### 4. 测试

在企业微信群中添加机器人，发送消息等待异步回复。

## 数据流程

### Web 对话

```
用户提问 → ChatController → ChatService
  → FAQ 匹配(优先) / Dify API 调用
  → kb_conversation 表 → 返回答案
```

### QQ 群消息

```
QQ 群消息 → NapCat → WebSocket(/qq/ws)
  → QqBotWebSocketHandler → group_message 表
  → Redis Stream → GroupMessageConsumer
  → 同步到 Dify 知识库
```

### 企业微信群消息

```
企业微信消息 → 回调(/intellrobot/callback/handle)
  → 消息解密验证 → group_message 表
  → Redis Stream → 异步处理
  → ChatService → Webhook 回复
```

## 定时任务

| 任务 | 频率 | 功能 | 状态 |
|------|------|------|:----:|
| Redis Stream 消费 | 每 5 秒 | 实时处理 IM 消息 | ✅ 推荐 |
| 定时同步（废弃） | 每 60 秒 | 批量同步群消息 | ⚠️ 已过时 |
| 统计聚合 | 每天 00:05 | 聚合昨日统计数据 | ✅ |
| 关键词提取 | 每天 05:00 | 提取关键词 | ✅ |
| 关键词清理 | 每天 06:00 | 清理 90 天前关键词 | ✅ |
| 会话清理 | 每天 03:00 | 清理过期会话 | ✅ |
| 消息清理 | 每天 04:30 | 清理 90 天前消息 | ✅ |

详细定时任务说明请参考 [USER_GUIDE.md](./USER_GUIDE.md#8-定时任务说明)。

## 数据库表

### 核心表

| 表名 | 说明 |
|------|------|
| `sys_user` | 系统用户 |
| `kb_category` | 知识库分类（树形结构） |
| `kb_knowledge_base` | 知识库管理 |
| `kb_document` | 文档管理 |
| `kb_conversation` | 会话记录 |
| `kb_faq` | 常见问答 |
| `kb_feedback` | 用户反馈 |
| `kb_statistics` | 每日统计 |
| `kb_keyword` | 关键词统计 |
| `kb_app` | 应用配置 |
| `group_message` | 群聊消息采集 |
| `im_group` | 群组信息 |
| `im_user` | 用户信息 |
| `chat_session` | 聊天会话 |
| `sys_config` | 系统配置 |

完整表结构请参考 [DESIGN.md](./DESIGN.md#4-数据库设计)。

## API 接口

### 主要接口

| 分类 | 路径前缀 | 说明 |
|------|----------|------|
| 用户 | `/api/user` | 注册、登录、信息管理 |
| 聊天 | `/api/chat` | 对话、文件上传 |
| 会话 | `/api/chat/session` | 会话 CRUD |
| 知识库 | `/api/kb` | 知识库、分类、文档 |
| 应用 | `/api/kb/app` | 应用管理、API Key 验证 |
| FAQ | `/api/kb/conversation/faq` | FAQ CRUD、提取 |
| 反馈 | `/api/feedback` | 提交、管理、统计 |
| 统计 | `/api/statistics` | Token、费用、关键词 |
| 控制台 | `/api/console` | 群聊采集管理 |
| 机器人 | `/api/bot` | 机器人列表 |
| 上传进度 | `/api/upload/progress` | SSE 实时推送 |

完整 API 列表请参考 [DESIGN.md](./DESIGN.md#13-api-接口汇总)。

## 文档导航

| 文档 | 说明 |
|------|------|
| [DESIGN.md](./DESIGN.md) | 详细设计文档（架构、模块、数据库、数据流、API） |
| [USER_GUIDE.md](./USER_GUIDE.md) | 使用文档（部署、配置、功能操作、故障排查、FAQ） |
| [DEPLOY.md](./DEPLOY.md) | 部署指南（Docker 部署、环境变量、服务管理） |

## 常见问题

| 问题 | 解决方法 |
|------|----------|
| QQ 消息收到但不回复 | 必须 @机器人，检查 NapCat 连接状态 |
| 知识库删除失败 | 检查 Dify API Key 配置 |
| 统计数据为空 | 调用 `/api/statistics/aggregate` 聚合统计 |
| Token 费用显示为 0 | 历史数据无费用，新对话正常记录 |
| Docker 启动后无法访问 | 检查容器状态和日志 `docker-compose logs` |

更多问题请参考 [USER_GUIDE.md](./USER_GUIDE.md#13-常见问题faq)。

## 项目结构

```
chatBase/
├── src/main/java/com/zxl/chatbase/
│   ├── chat/           # 聊天服务
│   ├── dify/           # Dify API 集成
│   ├── kb/             # 知识库管理
│   ├── im/             # IM 消息收集
│   ├── qq/             # QQ Bot（WebSocket + WebUI 扫码登录代理）
│   ├── wxroboot/       # 企业微信机器人
│   ├── statistics/     # 统计分析
│   ├── feedback/       # 用户反馈
│   ├── user/           # 用户管理
│   ├── config/         # 配置类
│   └── controller/     # API 控制器
│
├── web/                # Vue 3 前端
│   ├── src/
│   │   ├── pages/      # 页面组件
│   │   ├── api/        # API 接口
│   │   ├── components/ # 公共组件
│   │   └── lib/        # 工具库
│   ├── nginx.conf      # Nginx 配置
│   └── Dockerfile      # 前端镜像
│
├── sql/                # 数据库脚本
│   └── init-schema.sql # 初始化脚本
│
├── Dockerfile          # 后端镜像
├── docker-compose.yml  # 部署编排
├── .env.example        # 环境变量示例
├── DESIGN.md           # 详细设计文档
├── USER_GUIDE.md       # 使用文档
└── README.md           # 项目说明
```

## 许可证

MIT License

---

## 重要注意事项

- **pom.xml 编译参数**：`maven-compiler-plugin` 必须配置 `<parameters>true</parameters>`，否则 `@RequestParam` / `@RequestAttribute` 运行时报 `Name not specified` 异常。
- **数据隔离**：所有业务数据通过 `created_by` 字段按用户维度过滤。普通用户仅看自己的数据，admin 可在统计页切换 scope=all/mine。
- **前端路由**：admin 菜单项（反馈管理、应用管理、知识库管理、用户管理）在登录后自动显示，基于 `localStorage.getItem('chatbase_role')`。

*最后更新：2026-07-30*
