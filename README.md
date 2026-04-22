# ChatBase

基于 Spring Boot + Vue 3 的智能对话系统，集成 Dify AI 平台，支持 QQ 群聊和企业微信消息收集与智能回复。

## 功能特性

### 核心功能
- **智能对话**：集成 Dify AI，支持多轮对话、上下文记忆
- **知识库管理**：批量上传文档、自动同步到 Dify 知识库、搜索功能
- **FAQ 管理**：自动提取高频问答、手动维护、优先级匹配、搜索功能
- **会话管理**：多会话切换、历史记录查询

### IM 集成
- **QQ 群聊**：通过 NapCat 接入，自动回复群消息
- **企业微信**：接收企业微信群消息，智能回复

### 数据分析
- **Token 统计**：消耗趋势图、月度统计、预测分析
- **费用统计**：Prompt/Completion Tokens 分离计费、费用趋势
- **关键词热度**：词云展示、多渠道关键词提取
- **用户反馈**：点赞/踩统计、反馈表单、满意度分析

## 技术栈

### 后端
- Java 17
- Spring Boot 2.7.6
- MyBatis-Plus
- WebSocket
- Redis
- MySQL 8.0

### 前端
- Vue 3 + TypeScript
- Vite
- ECharts
- Lucide Icons

### AI 平台
- Dify API

## 快速开始

### Docker 部署（推荐）

```bash
# 1. 克隆项目
git clone <repository-url>
cd chatBase

# 2. 配置环境变量
cp .env.example .env
vim .env  # 填写 Dify API Key 等必填配置

# 3. 构建并启动
docker-compose up --build -d

# 4. 查看日志
docker-compose logs -f chatbase-backend
```

访问 `http://localhost` 打开前端页面。

详细部署说明请参考 [DEPLOY.md](./DEPLOY.md)。

### 本地开发

```bash
# 后端
mvn spring-boot:run

# 前端
cd web
npm install
npm run dev
```

## 目录结构

```
chatBase/
├── src/main/java/com/zxl/chatbase/
│   ├── chat/           # 聊天服务
│   ├── dify/           # Dify API 集成
│   ├── kb/             # 知识库管理
│   ├── im/             # IM 消息收集
│   ├── qq/             # QQ Bot WebSocket
│   ├── statistics/     # 统计分析
│   ├── feedback/       # 用户反馈
│   └── controller/     # API 控制器
│
├── web/                # Vue 3 前端
│   ├── src/
│   │   ├── pages/      # 页面组件
│   │   ├── api/        # API 接口
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
├── DEPLOY.md           # 部署说明
└── README.md           # 项目说明
```

## 配置说明

### 必填配置

| 配置项 | 说明 |
|--------|------|
| `DIFYAPP_API_KEY` | Dify Chat API Key |
| `DIFYAPP_DATASET_API_KEY` | Dify Dataset API Key |
| `MYSQL_PASSWORD` | MySQL 数据库密码 |

### 可选配置

| 配置项 | 说明 |
|--------|------|
| `QQ_BOT_ENABLE` | 启用 QQ Bot |
| `QQ_BOT_SELF_ID` | 机器人 QQ 号 |
| `WECHAT_CORP_STOKEN` | 企业微信 Token |

## API 接口

### 对话接口
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/chat/ask` | GET | 简单问答 |
| `/api/chat/web` | POST | Web 端对话 |
| `/api/chat/im` | POST | IM 端对话 |
| `/api/chat/v1/files/upload` | POST | 文件上传 |

### 知识库接口
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/kb` | GET/POST/PUT/DELETE | 知识库 CRUD |
| `/api/kb/{id}/sync` | POST | 同步到 Dify |
| `/api/kb/{id}/batch-upload` | POST | 批量上传文件 |
| `/api/kb/{id}/document/page` | GET | 文档列表（支持搜索） |

### 统计接口
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/statistics/token/chart` | GET | Token 趋势 |
| `/api/statistics/token/monthly` | GET | 本月统计 |
| `/api/statistics/cost/chart` | GET | 费用趋势 |
| `/api/statistics/keyword/cloud` | GET | 关键词词云 |
| `/api/statistics/aggregate` | POST | 聚合统计 |

### FAQ 接口
| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/kb/conversation/faq/page` | GET | FAQ 列表（支持搜索） |
| `/api/kb/conversation/faq/extract` | POST | 自动提取 FAQ |

## 数据库表

### 知识库模块
| 表名 | 说明 |
|------|------|
| `kb_knowledge_base` | 知识库管理 |
| `kb_document` | 文档管理 |
| `kb_conversation` | 会话记录 |
| `kb_faq` | 常见问答 |
| `kb_feedback` | 用户反馈 |
| `kb_statistics` | 每日统计 |
| `kb_keyword` | 关键词统计 |

### IM 模块
| 表名 | 说明 |
|------|------|
| `group_message` | 群聊消息采集 |
| `im_group` | 群组信息 |
| `im_user` | 用户信息 |
| `chat_session` | 聊天会话 |

### 系统模块
| 表名 | 说明 |
|------|------|
| `sys_user` | 系统用户 |
| `sys_config` | 系统配置 |

## QQ Bot 配置

使用 NapCat 作为 QQ 协议实现：

1. 启动 NapCat 容器
```bash
docker compose --profile qq up -d
```

2. 访问 `http://<server>:6099` 扫码登录，并配置网络配置

3. 配置反向 WebSocket客户端：
![img.png](img.png)

4.配置Http服务器
![img_1.png](img_1.png)

项目地址：https://github.com/NapNeko/NapCatQQ

⚠️ **注意**：务必使用小号登录，防止被封禁。

## 企业微信配置

在企业微信管理后台配置机器人：

1. **回调URL**：`http://<server>/intellrobot/callback/handle`
2. **配置参数**：
   - `WECHAT_CORP_STOKEN` - Token
   - `WECHAT_CORP_S_ENCODING_AES_KEY` - EncodingAESKey

3. 消息处理流程：
   - GET 请求：URL验证
   - POST 请求：接收消息并回复

## 数据流程

### QQ 群消息收集
```
QQ群消息 → NapCat → WebSocket(/qq/ws) 
→ QqBotWebSocketHandler → group_message 表
→ 定时同步到 Dify 知识库
```

### 企业微信群消息收集
```
企业微信群消息 → 回调(/intellrobot/callback/handle)
→ IntelligentRobotService → 消息解密验证
→ group_message 表 + im_group/im_user 表同步
→ ChatService(Dify API) → 返回答案 → Webhook回复群聊
```

### Web 对话流程
```
用户提问 → ChatController → ChatService
→ FAQ匹配(优先) / Dify API调用
→ kb_conversation 表 → 返回答案
```

## 常见问题

1. **QQ消息收到但不回答**：确认 @机器人 而不是只发消息
2. **知识库删除失败**：检查 Dify API Key 配置是否正确
3. **统计数据为空**：调用 `/api/statistics/aggregate` 聚合统计
4. **Token费用显示为0**：历史数据无费用信息，新对话正常记录

