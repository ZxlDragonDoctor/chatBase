# ChatBase 详细设计文档

## 1. 系统概述

ChatBase 是一个基于 Spring Boot + Vue 3 的智能对话系统，集成 Dify AI 平台，支持 QQ 群聊和企业微信消息收集与智能回复。系统提供知识库管理、FAQ 管理、会话管理、数据分析等完整功能。

### 1.1 核心特性

- **智能对话**：集成 Dify AI，支持多轮对话、上下文记忆、FAQ 优先匹配
- **知识库管理**：批量上传文档、自动同步到 Dify 知识库、搜索功能
- **FAQ 管理**：自动提取高频问答、手动维护、优先级匹配
- **会话管理**：多会话切换、历史记录查询、会话标题自定义
- **IM 集成**：QQ 群聊（NapCat）、企业微信（回调模式）消息收集与智能回复
- **数据分析**：Token 统计、费用统计、关键词热度、用户反馈、群活跃度
- **机器人管理**：多机器人状态监控、在线状态、消息统计
- **应用管理**：多 Dify 应用配置、群组绑定、权限控制

### 1.2 技术栈

| 层级 | 技术选型 |
|------|----------|
| **后端** | Java 17, Spring Boot 2.7.6, MyBatis-Plus 3.5.15, WebSocket, Redis, MySQL 8.0 |
| **前端** | Vue 3.5, TypeScript 5.7, Vite 6.1, ECharts 6.0, Lucide Icons, CropperJS 1.6.2 |
| **AI 平台** | Dify API |
| **部署** | Docker Compose, Nginx |
| **依赖库** | Apache HttpClient 4.5.14, Fastjson 2.0.40, Lombok, Spring Security Core 5.7.5 |

---

## 2. 系统架构

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                          前端层 (Vue 3)                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────┐ │
│  │ 登录注册 │ │ AI 问答  │ │ 知识库   │ │ 统计面板 │ │ 控制台 │ │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └────────┘ │
└───────────────────────────────┬─────────────────────────────────┘
                                │ HTTP / WebSocket
┌───────────────────────────────▼─────────────────────────────────┐
│                        后端层 (Spring Boot)                       │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │ 控制器层 │ │ 服务层   │ │ 数据层   │ │ 配置层   │           │
│  │Controller│ │ Service  │ │ Mapper   │ │ Config   │           │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘           │
│                                                                   │
│  ┌───────────────────────────────────────────────────────────┐   │
│  │                    核心模块                                │   │
│  │  Chat  │  Dify  │  KB   │  IM   │  QQ   │  WeChat │ Stats │   │
│  └───────────────────────────────────────────────────────────┘   │
└───────────────┬───────────────────────┬─────────────────────────┘
                │                       │
┌───────────────▼──────────┐  ┌────────▼────────────────────────┐
│    数据层                 │  │    外部服务                      │
│  ┌────────────────────┐  │  │  ┌──────────┐ ┌──────────────┐  │
│  │ MySQL 8.0          │  │  │  │ Dify API │ │ NapCat QQ    │  │
│  │ Redis 7            │  │  │  └──────────┘ └──────────────┘  │
│  └────────────────────┘  │  │  ┌──────────────────────────┐   │
│                           │  │  │ 企业微信回调             │   │
└───────────────────────────┘  │  └──────────────────────────┘   │
                               └─────────────────────────────────┘
```

### 2.2 模块划分

| 模块 | 包路径 | 职责 |
|------|--------|------|
| **chat** | `com.zxl.chatbase.chat` | 聊天会话管理、消息处理、数据清理 |
| **dify** | `com.zxl.chatbase.dify` | Dify API 集成、对话、文件上传、知识库操作 |
| **kb** | `com.zxl.chatbase.kb` | 知识库管理、分类、文档、FAQ、反馈、应用、关键词 |
| **im** | `com.zxl.chatbase.im` | IM 消息采集、群聊管理、用户管理、机器人管理 |
| **qq** | `com.zxl.chatbase.qq` | QQ 机器人 WebSocket 处理、NapCat 集成 |
| **wxroboot** | `com.zxl.chatbase.wxroboot` | 企业微信机器人回调处理、消息加解密 |
| **statistics** | `com.zxl.chatbase.statistics` | 统计分析、Token、费用、关键词、聚合 |
| **feedback** | `com.zxl.chatbase.feedback` | 用户反馈收集与统计 |
| **user** | `com.zxl.chatbase.user` | 用户注册、登录、信息管理 |
| **controller** | `com.zxl.chatbase.controller` | REST API 控制器 |
| **config** | `com.zxl.chatbase.config` | 配置类、拦截器、线程池、跨域 |
| **common** | `com.zxl.chatbase.common` | 通用组件、限流服务、异常定义 |
| **upload** | `com.zxl.chatbase.upload` | 文件上传进度管理 |

---

## 3. 核心模块设计

### 3.1 聊天模块 (chat)

#### 3.1.1 类结构

```
chat/
├── entity/ChatSession.java          # 会话实体
├── mapper/ChatSessionMapper.java    # 会话数据访问
└── service/
    ├── ChatService.java             # 聊天服务接口
    ├── ChatSessionService.java      # 会话服务接口
    ├── CleanupService.java          # 清理服务接口
    └── impl/
        ├── ChatServiceImpl.java     # 聊天服务实现
        ├── ChatSessionServiceImpl.java  # 会话服务实现
        └── CleanupServiceImpl.java  # 清理服务实现
```

#### 3.1.2 ChatServiceImpl 核心流程

```java
// 聊天核心处理流程
1. 接收请求（Web/IM）
2. 检查频率限制（IM 渠道：按 groupId+userId，5 秒窗口最多 1 次）
3. 获取或创建会话 ID（Redis 缓存：chatbase:conversation:{channel}:{userId}）
4. 优先匹配 FAQ（按优先级排序，匹配关键词）
   ├── 匹配成功 → 返回 FAQ 答案，更新 hit_count
   └── 匹配失败 → 调用 Dify API
5. 调用 DifyService.chat() 获取 AI 回答
6. 保存会话记录到 kb_conversation 表
7. 更新 Redis 会话 ID
8. 更新 chat_session 表（消息数、最后消息时间）
9. 返回响应
```

#### 3.1.3 ChatSessionServiceImpl 会话管理

- **创建会话**：生成唯一 sessionId，初始化 dify_conversation_id
- **会话列表**：按用户、渠道分页查询
- **会话消息**：查询 kb_conversation 表中该会话的所有对话
- **删除会话**：逻辑删除（status=0），同时清理 Redis 缓存
- **更新标题**：支持自定义会话标题（默认取首条消息摘要）

#### 3.1.4 CleanupServiceImpl 定时清理

| 任务 | 执行时间 | 功能 |
|------|----------|------|
| cleanupExpiredConversations() | 每天 03:00 | 清理 Redis 和数据库中过期会话（7 天 TTL） |
| cleanupOldMessages() | 每天 04:30 | 清理超过 90 天的消息数据 |

### 3.2 Dify 集成模块 (dify)

#### 3.2.1 类结构

```
dify/
├── config/DifyConfig.java           # Dify 配置类
├── model/
│   ├── request/
│   │   ├── DifyChatRequest.java     # 对话请求
│   │   ├── DifyFileUploadRequest.java  # 文件上传请求
│   │   └── FileInfo.java            # 文件信息
│   └── response/
│       ├── DifyChatResponse.java    # 对话响应
│       ├── DifyStreamChatResponse.java  # 流式对话响应
│       ├── DifyDatasetResponse.java # 数据集响应
│       ├── DifyDocumentResponse.java  # 文档响应
│       ├── DifyFileUploadResponse.java  # 文件上传响应
│       ├── BatchUploadResponse.java # 批量上传响应
│       ├── RetrieverResource.java   # 引用来源
│       ├── Usage.java               # Token 使用量
│       └── Metadata.java            # 元数据
└── server/
    ├── DifyService.java             # Dify 服务接口
    └── impl/DifyServiceImpl.java    # Dify 服务实现
```

#### 3.2.2 DifyServiceImpl 核心方法

| 方法 | 功能 | HTTP 方法 | 端点 |
|------|------|-----------|------|
| `chat()` | 发送对话请求 | POST | `/v1/chat-messages` |
| `uploadFile()` | 上传文件 | POST | `/v1/files/upload` |
| `createDataset()` | 创建数据集 | POST | `/v1/datasets` |
| `createDocumentByText()` | 创建文本文档 | POST | `/v1/datasets/{id}/document/create-by-text` |
| `createDocumentByFile()` | 创建文件文档 | POST | `/v1/datasets/{id}/document/create-by-file` |
| `deleteDocument()` | 删除文档 | DELETE | `/v1/datasets/{id}/documents/{docId}` |
| `listDatasets()` | 列出数据集 | GET | `/v1/datasets` |
| `getAppInfo()` | 获取应用信息 | GET | `/v1/info` |

#### 3.2.3 对话请求参数

```java
DifyChatRequest {
    String inputs;              // 输入变量（JSON）
    String query;               // 用户问题
    String responseMode;        // 响应模式：blocking/streaming
    String conversationId;      // 会话 ID（多轮对话）
    String user;                // 用户标识
    List<FileInfo> files;       // 文件附件
}
```

#### 3.2.4 对话响应结构

```java
DifyChatResponse {
    String messageId;           // 消息 ID
    String conversationId;      // 会话 ID
    String answer;              // AI 回答
    Usage usage;                // Token 使用量
    List<RetrieverResource> retrieverResources;  // 引用来源
    String createdAt;           // 创建时间
}
```

### 3.3 知识库模块 (kb)

#### 3.3.1 类结构

```
kb/
├── dto/FeedbackRequest.java     # 反馈请求 DTO
├── entity/                      # 实体类
│   ├── KbApp.java               # 应用配置
│   ├── KbCategory.java          # 分类
│   ├── KbKnowledgeBase.java     # 知识库
│   ├── KbDocument.java          # 文档
│   ├── KbConversation.java      # 会话记录
│   ├── KbFeedback.java          # 反馈
│   ├── KbFaq.java               # FAQ
│   ├── KbFile.java              # 文件
│   ├── KbKeyword.java           # 关键词
│   ├── KbStatistics.java        # 统计
│   ├── SysConfig.java           # 系统配置
│   └── SysUser.java             # 系统用户
├── mapper/                      # 数据访问层（12 个 Mapper）
└── service/
    ├── FileService.java         # 文件服务
    ├── IKbAppService.java       # 应用服务
    ├── IKbCategoryService.java  # 分类服务
    ├── IKbKnowledgeBaseService.java  # 知识库服务
    ├── IKbDocumentService.java  # 文档服务
    ├── IKbConversationService.java  # 会话服务
    ├── IKbFaqService.java       # FAQ 服务
    ├── IKbKeywordService.java   # 关键词服务
    ├── SysConfigService.java    # 系统配置服务
    └── impl/                    # 实现类（11 个）
```

#### 3.3.2 知识库管理流程

```
创建知识库
    ↓
1. 选择分类（kb_category 树形结构）
2. 填写名称、描述
3. 调用 Dify API 创建 Dataset（获取 dify_dataset_id）
4. 保存到 kb_knowledge_base 表
    ↓
上传文档
    ↓
1. 选择知识库
2. 批量上传文件（支持拖拽）
3. 异步上传到 Dify（UploadProgressService 跟踪进度）
4. 保存到 kb_document 表
    ↓
同步文档
    ↓
1. 单个同步：调用 Dify API 更新文档
2. 知识库同步：同步所有未同步文档
3. 从 Dify 同步：拉取 Dify 中的文档信息
```

#### 3.3.3 FAQ 管理

- **手动维护**：CRUD 操作，支持关键词、相似问题、优先级
- **自动提取**：从历史对话中批量提取高频问答（`extractFaq()`）
- **匹配逻辑**：按优先级排序，匹配关键词或问题
- **命中统计**：记录 hit_count，计算满意度

#### 3.3.4 应用管理 (KbApp)

- **多应用支持**：每个应用独立配置 Dify API Key
- **默认应用**：is_default 标记，系统启动时加载
- **群组绑定**：im_group.app_id 关联，优先使用绑定应用
- **权限控制**：is_public 标记，控制用户访问权限
- **API Key 验证**：调用 Dify `/v1/info` 验证 Key 有效性

#### 3.3.5 关键词管理

- **来源**：conversation（对话 query）、im（IM 消息）、document（文档）
- **提取**：定时任务每天 05:00 从最近 7 天数据中提取
- **清理**：每天 06:00 清理超过 90 天的关键词
- **展示**：词云（ECharts Word Cloud）、Top N 排行榜

### 3.4 IM 消息采集模块 (im)

#### 3.4.1 类结构

```
im/
├── consumer/
│   └── GroupMessageConsumer.java    # Redis Stream 消费者
├── dto/
│   ├── BotInfoVO.java               # 机器人信息
│   ├── BotStatusVO.java             # 机器人状态
│   ├── ConsoleOverviewVO.java       # 控制台概览
│   ├── GroupMessageItemVO.java      # 消息项
│   ├── GroupMessagePageVO.java      # 消息分页
│   └── GroupSummaryVO.java          # 群组摘要
├── entity/
│   ├── GroupMessage.java            # 群消息
│   ├── ImGroup.java                 # 群组
│   └── ImUser.java                  # 用户
├── mapper/
│   ├── BotManageMapper.java         # 机器人管理 Mapper
│   ├── GroupMessageMapper.java      # 消息 Mapper
│   ├── ImGroupMapper.java           # 群组 Mapper
│   └── ImUserMapper.java            # 用户 Mapper
└── service/
    ├── BotManageService.java        # 机器人管理服务
    ├── GroupMessageSyncService.java # 消息同步服务
    ├── ImConsoleService.java        # 控制台服务
    ├── ImGroupService.java          # 群组服务
    ├── ImUserService.java           # 用户服务
    └── impl/                        # 实现类
```

#### 3.4.2 消息同步方案

**方案一：Redis Stream（推荐）**

```
消息到达 → WebSocket/回调处理器
    ↓
保存到 group_message 表
    ↓
发布到 Redis Stream（chatbase:group:message:stream）
    ↓
GroupMessageConsumer 消费（每 5 秒阻塞读取，每次 10 条）
    ↓
处理消息（syncSingleMessage）
    ↓
同步到 Dify 知识库
    ↓
更新 synced 状态
```

**方案二：定时轮询（已废弃，保留降级）**

```
定时任务（每 60 秒）
    ↓
查询 synced=false 的消息（最多 200 条）
    ↓
按群 ID 分组，每群一个文档
    ↓
创建/更新 Dify 文档
```

**配置切换**：
```yaml
im:
  sync:
    stream:
      enabled: true   # 启用 Stream 方案
    polling:
      enabled: false  # 禁用定时轮询
```

#### 3.4.3 消息同步核心逻辑（syncSingleMessage）

```java
1. 检查消息类型（text/image/file）
2. 文本消息：
   ├── 按群 ID 查找已有文档
   ├── 合并历史消息内容
   ├── 调用 Dify API 更新文档
   └── 更新 synced=true
3. 文件消息：
   ├── 下载文件（从 URL）
   ├── 上传到 Dify
   ├── 创建文档
   └── 更新 synced=true, dify_file_id
4. 更新 im_group、im_user 表信息
```

#### 3.4.4 机器人管理 (BotManageService)

- **QQ 机器人**：
  - 在线状态：Redis 键 `bot:qq:online`（心跳标记，30s TTL）
  - 昵称获取：优先调用 OneBot API（get_stranger_info），其次配置文件 `qq.bot.nickname`，最终降级 `QQ: {self_id}`
  - 统计：从 group_message 表按 platform='qq' 聚合

- **企业微信机器人**：
  - 默认在线
  - 名称：配置文件 `wechat.corp.botName`
  - 统计：从 group_message 表按 platform='wecom' 聚合

### 3.5 QQ 机器人模块 (qq)

#### 3.5.1 类结构

```
qq/
├── QqBotProperties.java           # QQ 机器人配置
├── QqBotWebSocketHandler.java     # WebSocket 处理器
└── QqWebSocketConfig.java         # WebSocket 配置
```

#### 3.5.2 QqBotWebSocketHandler 核心流程

```java
1. WebSocket 连接建立（NapCat 反向连接）
2. 接收消息（JSON 格式，OneBot 协议）
3. 解析消息类型：
   ├── group_message：群消息
   ├── notice：通知事件
   └── meta_event：元事件（心跳）
4. 群消息处理：
   ├── 过滤机器人自身消息（self_id）
   ├── 解析 @提及（检测是否 @机器人）
   ├── 保存消息到 group_message 表
   ├── 发布到 Redis Stream
   ├── 如果 @机器人：
   │   ├── 检查频率限制（Redis：qq:ratelimit:{groupId}:{userId}）
   │   ├── 调用 ChatService.imChat()
   │   └── 发送回复（OneBot API：send_group_msg）
   └── 更新 im_group、im_user 表
5. 心跳处理：
   └── 更新 Redis 在线状态（bot:qq:online，30s TTL）
```

#### 3.5.3 消息结构（OneBot 协议）

```json
{
  "post_type": "message",
  "message_type": "group",
  "self_id": 123456,
  "group_id": 789012,
  "user_id": 345678,
  "message": "[CQ:at,qq=123456] 你好",
  "raw_message": "@机器人 你好",
  "sender": {
    "nickname": "用户昵称",
    "card": "群名片"
  },
  "time": 1234567890
}
```

### 3.6 企业微信机器人模块 (wxroboot)

#### 3.6.1 类结构

```
wxroboot/
└── webhook/
    ├── config/WXBizJsonMsgCryptConfig.java  # 加解密配置
    ├── entity/                              # 消息实体
    │   ├── DutyChatGroup.java               # 值班群
    │   └── intelligentBot/                  # 智能机器人消息
    │       ├── IntelligentBotMsg.java       # 消息基类
    │       ├── MsgTypeText.java             # 文本消息
    │       ├── MsgTypeImage.java            # 图片消息
    │       ├── MsgTypeMixed.java            # 混合消息
    │       ├── MsgTypeStream.java           # 流式消息
    │       └── MixedMsgItem.java            # 混合消息项
    ├── mapper/IntelligentRobotMapper.java   # 数据访问
    ├── service/
    │   ├── IntelligentRobotService.java     # 服务接口
    │   └── impl/IntelligentRobotServiceImpl.java  # 服务实现
    └── util/                                # 工具类
        ├── HttpUtils.java                   # HTTP 工具
        ├── WeChatUtil.java                  # 微信工具
        └── aes/                             # AES 加解密
            ├── WXBizJsonMsgCrypt.java       # 加解密主类
            ├── PKCS7Encoder.java            # PKCS7 填充
            ├── SHA1.java                    # SHA1 签名
            └── ...
```

#### 3.6.2 IntelligentRobotServiceImpl 核心流程

```java
1. URL 验证（GET 请求）：
   ├── 解密 echostr
   ├── 验证签名（msg_signature, timestamp, nonce）
   └── 返回明文 echostr

2. 消息处理（POST 请求）：
   ├── 解密消息体
   ├── 解析消息类型（文本/图片/混合）
   ├── 保存消息到 group_message 表（platform='wecom'）
   ├── 发布到 Redis Stream
   ├── 获取群组绑定应用（im_group.app_id）
   ├── Redis 分布式锁（防止重复处理，5 分钟 TTL）
   ├── 快速返回空响应（企微要求 5 秒内响应）
   ├── 异步处理：
   │   ├── 调用 ChatService.imChat()
   │   ├── 过滤思考过程（filterThinkingContent）
   │   └── 通过 response_url 回复群聊
   └── Unicode 解码响应内容
```

#### 3.6.3 消息加解密

- **算法**：AES-256-CBC，PKCS7 填充
- **签名**：SHA1（msg_signature）
- **参数**：Token（stoken）、EncodingAESKey（sEncodingAESKey）
- **流程**：
  1. 验证签名：`SHA1(Token + timestamp + nonce + encrypt_msg)`
  2. 解密：Base64 解码 → AES 解密 → 去除 PKCS7 填充
  3. 加密：AES 加密 → Base64 编码 → 构建 XML/JSON 响应

### 3.7 统计分析模块 (statistics)

#### 3.7.1 类结构

```
statistics/
├── dto/
│   ├── ConversationStatisticsVO.java   # 会话统计
│   ├── GroupActiveVO.java              # 群活跃度
│   ├── KeywordHotVO.java               # 热门关键词
│   ├── SystemOverviewVO.java           # 系统概览
│   └── TokenStatisticsVO.java          # Token 统计
└── service/
    ├── StatisticsService.java          # 统计服务接口
    ├── IStatisticsAggregateService.java  # 聚合服务接口
    └── impl/
        ├── StatisticsServiceImpl.java  # 统计服务实现
        └── StatisticsAggregateServiceImpl.java  # 聚合服务实现
```

#### 3.7.2 统计维度

| 维度 | 指标 | 数据源 |
|------|------|--------|
| **Token** | 每日消耗、总计、趋势图、月度统计 | kb_statistics, kb_conversation |
| **费用** | Prompt/Completion 分离计费、趋势图、月度统计 | kb_conversation（prompt_price, completion_price） |
| **会话** | 概览、趋势、按渠道分布 | kb_conversation |
| **群活跃度** | 按平台、按群消息数排名 | group_message, im_group |
| **关键词** | 词云、Top N、按来源、按时间 | kb_keyword |
| **反馈** | 每日统计、总体概览、满意度 | kb_feedback |
| **系统概览** | 消息总数、对话次数、Token、活跃群/用户、成功率 | 聚合多表 |

#### 3.7.3 定时聚合任务

```java
// StatisticsAggregateServiceImpl.aggregateYesterdayStatistics()
// 执行时间：每天 00:05（cron = "0 5 0 * * ?"）

1. 查询昨日 kb_conversation 记录
2. 按渠道（web/im/wx）分组
3. 计算：
   ├── 会话数（COUNT DISTINCT conversation_id）
   ├── 消息数（COUNT）
   ├── 独立用户数（COUNT DISTINCT user_id）
   ├── Token 消耗（SUM tokens, prompt_tokens, completion_tokens）
   ├── 费用（SUM total_price, prompt_price, completion_price）
   ├── 平均延迟（AVG latency_ms）
   └── 反馈统计（JOIN kb_feedback）
4. 写入 kb_statistics 表（uk_stat_date_channel_kb 唯一索引）
```

### 3.8 用户模块 (user)

#### 3.8.1 类结构

```
user/
├── dto/
│   ├── LoginRequest.java          # 登录请求
│   ├── LoginResponse.java         # 登录响应
│   ├── RegisterRequest.java       # 注册请求
│   └── UserVO.java                # 用户视图对象
└── service/
    ├── UserService.java           # 用户服务接口
    └── impl/UserServiceImpl.java  # 用户服务实现
```

#### 3.8.2 核心功能

| 功能 | 说明 |
|------|------|
| **注册** | 用户名唯一校验，BCrypt 密码加密，默认角色 user |
| **登录** | 验证用户名密码，生成 UUID Token，返回用户信息 |
| **Token 管理** | Token 存储 Redis（chatbase:token:{token}），TTL 7 天 |
| **用户信息** | 查询、更新（昵称、头像、邮箱、电话） |
| **头像上传** | MultipartFile 上传，保存到 /uploads/avatars/，支持裁切 |
| **密码管理** | 修改密码（验证旧密码）、检查密码 |
| **角色权限** | admin/user，拦截器校验 |

### 3.9 配置与拦截器 (config)

#### 3.9.1 配置类

| 类名 | 功能 |
|------|------|
| `WebMvcConfig` | MVC 配置、拦截器注册、静态资源映射 |
| `AuthInterceptor` | 用户认证拦截器（校验 Token） |
| `AdminInterceptor` | 管理员权限拦截器（校验 role=admin） |
| `CorsConfig` | 跨域配置（允许所有来源，开发环境） |
| `TokenService` | Token 生成、验证、Redis 管理 |
| `MybatisPlusConfig` | MyBatis-Plus 配置、分页插件 |
| `ThreadPoolConfig` | 线程池配置（异步任务） |
| `RestTemplateConfig` | RestTemplate 配置（HTTP 客户端） |
| `HttpClientConfig` | Apache HttpClient 配置 |
| `JacksonConfig` | Jackson 序列化配置 |
| `ChatProperties` | 聊天配置（max-turns, session-ttl, rate-limit） |

#### 3.9.2 拦截器流程

```
请求到达
    ↓
WebMvcConfig.addInterceptors()
    ↓
AuthInterceptor.preHandle()
    ├── 排除路径：/api/user/login, /api/user/register, /uploads/**, /intellrobot/**
    ├── 获取 Token（Header: Authorization 或 Query: token）
    ├── 校验 Token（Redis：chatbase:token:{token}）
    ├── 设置用户信息到 RequestAttribute（currentUser, originalUsername, role, adminId）
    └── 放行
    ↓
AdminInterceptor.preHandle()（仅 /api/console/**, /api/kb/**, /api/bot/**）
    ├── 检查 role=admin
    └── 拒绝访问 → 返回 403
    ↓
Controller 处理
    ↓
AuthInterceptor.afterCompletion()
    ├── 清除 ThreadLocal（防止内存泄漏）
    └── 清除 RequestAttribute
```

#### 3.9.3 401 处理机制

```
客户端请求
    ↓
后端拦截器校验 Token 失败
    ↓
返回 401 状态码
    ↓
客户端 axios 响应拦截器（client.ts）
    ├── 清除 localStorage：chatbase_token, chatbase_user_info, 
    │   chatbase_role, chatbase_admin_id, chatbase_original_username
    ├── 跳转 /login
    └── 拒绝重复请求（防止 401 循环）
```

### 3.10 文件上传模块 (upload)

#### 3.10.1 类结构

```
upload/
├── entity/UploadProgress.java     # 上传进度实体
└── service/UploadProgressService.java  # 上传进度服务
```

#### 3.10.2 上传进度跟踪

```java
1. 创建任务（taskId = UUID）
2. 初始化进度（total, uploaded, status, progress）
3. 异步上传文件到 Dify
4. 每上传一个文件更新进度
5. 上传完成更新状态（completed）
6. SSE 实时推送（/api/upload/progress/{taskId}/sse）
   ├── SseEmitter，超时 5 分钟
   ├── 每 500ms 推送一次进度
   ├── 任务完成自动清理
   └── 客户端断开时移除 Emitter
```

---

## 4. 数据库设计

### 4.1 表关系图

```
sys_user ──────────────────────────┐
                                   │
kb_category ──┐                    │
              │                    │
kb_knowledge_base ──┐              │
                    │              │
kb_document ────────┤              │
                    │              │
kb_conversation ────┼── kb_feedback│
                    │              │
kb_faq ─────────────┤              │
                    │              │
kb_app ─────────────┼──────────────┘
                    │
im_group ───────────┼── app_id ─── kb_app
                    │
im_user ────────────┤
                    │
group_message ──────┤
                    │
chat_session ───────┤
                    │
kb_statistics ──────┤
                    │
kb_keyword ─────────┤
                    │
kb_file ────────────┤
                    │
sys_config ─────────┘
```

### 4.2 核心表结构

#### 4.2.1 系统用户表（sys_user）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 用户 ID（主键） |
| username | VARCHAR(50) | 用户名（唯一） |
| password | VARCHAR(128) | 密码（BCrypt 加密） |
| nickname | VARCHAR(50) | 昵称 |
| avatar | VARCHAR(255) | 头像 URL |
| email | VARCHAR(100) | 邮箱 |
| phone | VARCHAR(20) | 手机号 |
| status | TINYINT(1) | 状态：0-禁用，1-启用 |
| role | VARCHAR(20) | 角色：admin/user |
| last_login_time | DATETIME | 最后登录时间 |
| last_login_ip | VARCHAR(50) | 最后登录 IP |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| is_deleted | TINYINT(1) | 逻辑删除标记 |

#### 4.2.2 知识库分类表（kb_category）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 分类 ID（主键） |
| parent_id | BIGINT | 父分类 ID，0 为顶级 |
| name | VARCHAR(50) | 分类名称 |
| icon | VARCHAR(255) | 分类图标 |
| sort_order | INT | 排序 |
| description | VARCHAR(255) | 描述 |
| status | TINYINT(1) | 状态 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| create_by | BIGINT | 创建人 ID |

#### 4.2.3 知识库管理表（kb_knowledge_base）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 知识库 ID（主键） |
| name | VARCHAR(100) | 知识库名称 |
| description | TEXT | 描述 |
| category_id | BIGINT | 所属分类 ID |
| dify_dataset_id | VARCHAR(128) | Dify 数据集 ID |
| dify_api_key | VARCHAR(255) | Dify API Key |
| source_type | VARCHAR(32) | 来源：manual/im_sync |
| sync_platform | VARCHAR(32) | 同步平台：qq/wecom |
| sync_group_ids | TEXT | 同步群 ID 列表（JSON） |
| auto_sync | TINYINT(1) | 是否自动同步 |
| sync_interval | INT | 同步间隔（分钟） |
| doc_count | INT | 文档数量 |
| chunk_count | INT | 切片数量 |
| status | TINYINT(1) | 状态 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| create_by | BIGINT | 创建人 ID |

#### 4.2.4 会话记录表（kb_conversation）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 会话 ID（主键） |
| session_id | VARCHAR(64) | 关联会话 ID |
| conversation_id | VARCHAR(64) | Dify 会话 ID（唯一） |
| user_id | VARCHAR(64) | 用户 ID |
| user_nickname | VARCHAR(50) | 用户昵称 |
| channel | VARCHAR(20) | 渠道：web/im/wx |
| group_id | VARCHAR(64) | 群 ID（IM 渠道） |
| knowledge_base_id | BIGINT | 关联知识库 ID |
| app_id | BIGINT | 使用的应用 ID |
| app_name | VARCHAR(100) | 应用名称 |
| query | TEXT | 用户问题 |
| answer | LONGTEXT | AI 回答 |
| dify_response_id | VARCHAR(64) | Dify 响应 ID |
| tokens | INT | 消耗 tokens |
| prompt_tokens | INT | 提示词 tokens |
| completion_tokens | INT | 完成 tokens |
| prompt_price | DECIMAL(10,6) | 提示词费用 |
| completion_price | DECIMAL(10,6) | 完成费用 |
| total_price | DECIMAL(10,6) | 总费用 |
| latency_ms | INT | 响应延迟（毫秒） |
| source_documents | TEXT | 引用文档（JSON） |
| status | TINYINT(1) | 状态：0-失败，1-成功 |
| error_message | TEXT | 错误信息 |
| create_time | DATETIME | 创建时间 |

#### 4.2.5 群聊消息表（group_message）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| platform | VARCHAR(32) | 平台：qq/wecom |
| group_id | VARCHAR(64) | 群 ID |
| user_id | VARCHAR(64) | 用户 ID |
| message_id | VARCHAR(100) | 平台消息 ID（唯一索引） |
| message_type | VARCHAR(32) | 消息类型：text/image/file |
| raw_message | TEXT | 原始消息内容 |
| message_time | DATETIME | 消息发送时间 |
| create_time | DATETIME | 记录创建时间 |
| update_time | DATETIME | 更新时间 |
| synced | TINYINT(1) | 是否已同步 |
| kb_document_id | VARCHAR(128) | 知识库文档 ID |
| file_url | VARCHAR(500) | 文件 URL |
| dify_file_id | VARCHAR(128) | Dify 文件 ID |
| file_name | VARCHAR(255) | 文件名 |

**索引**：
- PRIMARY KEY (id)
- UNIQUE INDEX uk_platform_msgid (platform, message_id)
- INDEX idx_group_time (group_id, message_time)
- INDEX idx_synced (synced)

#### 4.2.6 IM 群组表（im_group）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 群组 ID（主键） |
| platform | VARCHAR(32) | 平台：qq/wecom |
| group_id | VARCHAR(64) | 平台群 ID |
| group_name | VARCHAR(100) | 群名称 |
| member_count | INT | 成员数 |
| owner_id | VARCHAR(64) | 群主 ID |
| robot_id | VARCHAR(64) | 机器人 ID |
| auto_reply | TINYINT(1) | 是否自动回复 |
| kb_id | BIGINT | 关联知识库 ID |
| app_id | BIGINT | 绑定的应用 ID |
| app_name | VARCHAR(100) | 应用名称（冗余） |
| status | TINYINT(1) | 状态 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

**唯一索引**：uk_platform_group (platform, group_id)

#### 4.2.7 应用配置表（kb_app）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 应用 ID（主键） |
| name | VARCHAR(100) | 应用名称 |
| description | VARCHAR(500) | 应用描述 |
| icon | VARCHAR(50) | 应用图标 |
| dify_api_key | VARCHAR(100) | Dify API Key |
| dify_app_name | VARCHAR(100) | Dify 应用名称 |
| dify_app_mode | VARCHAR(50) | Dify 应用模式：chatbot/agent/workflow/completion |
| category_id | BIGINT | 关联分类 ID |
| is_default | TINYINT(1) | 是否默认应用 |
| is_public | TINYINT(1) | 是否公开 |
| create_by | VARCHAR(50) | 创建者 |
| status | TINYINT(1) | 状态 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |

### 4.3 默认数据

#### 4.3.1 默认应用

```sql
INSERT INTO `kb_app` 
  (`name`, `description`, `icon`, `dify_api_key`, `is_default`, `is_public`, `create_by`, `status`)
VALUES 
  ('默认助手', '系统默认应用，使用配置文件中的 API Key', 'robot', 'PLACEHOLDER_API_KEY', 1, 1, 'admin', 1)
ON DUPLICATE KEY UPDATE `update_time` = NOW();
```

#### 4.3.2 系统配置

| config_key | config_value | 说明 |
|------------|--------------|------|
| dify.chat.timeout | 90 | Dify 对话超时时间（秒） |
| dify.chat.maxTurns | 20 | 单会话最大轮数 |
| qq.rateLimit.enabled | true | 是否启用 QQ 限流 |
| qq.rateLimit.perUser | 10 | 每用户每分钟最大请求数 |
| qq.rateLimit.perGroup | 30 | 每群每分钟最大请求数 |
| web.rateLimit.enabled | true | 是否启用 Web 限流 |
| web.rateLimit.perUser | 30 | 每用户每分钟最大请求数 |
| sync.batchSize | 200 | 知识库同步批次大小 |
| sync.interval | 60 | 知识库同步间隔（秒） |
| redis.conversation.ttl | 7 | Redis 会话过期天数 |

---

## 5. 数据流设计

### 5.1 Web 对话流程

```
用户在前端输入问题
    ↓
ChatPage.vue 调用 webChatWithSession()
    ↓
POST /api/chat/web/session
    ↓
ChatController.webChatWithSession()
    ↓
ChatServiceImpl.webChat()
    ├── 1. 检查频率限制（Web 渠道：按 userId）
    ├── 2. 获取或创建会话 ID（Redis）
    ├── 3. 获取应用配置（默认应用或用户选择）
    ├── 4. FAQ 匹配（优先）
    │   ├── 匹配成功 → 返回答案，更新 hit_count
    │   └── 匹配失败 → 继续
    ├── 5. 调用 DifyService.chat()
    │   ├── 构建 DifyChatRequest
    │   ├── 发送 HTTP POST 到 Dify API
    │   ├── 解析响应（answer, usage, retrieverResources）
    │   └── 返回 DifyChatResponse
    ├── 6. 保存会话记录到 kb_conversation 表
    ├── 7. 更新 chat_session 表（message_count, last_message_time）
    └── 8. 返回响应
    ↓
前端渲染 Markdown、引用来源、思考过程
```

### 5.2 QQ 群消息处理流程

```
QQ 群用户发送消息
    ↓
NapCat 捕获消息
    ↓
反向 WebSocket 推送 → ws://localhost:8080/qq/ws
    ↓
QqBotWebSocketHandler.handleTextMessage()
    ├── 1. 解析 OneBot JSON 消息
    ├── 2. 过滤机器人自身消息（self_id）
    ├── 3. 保存消息到 group_message 表（platform='qq'）
    ├── 4. 发布到 Redis Stream（chatbase:group:message:stream）
    ├── 5. 更新 im_group、im_user 表
    ├── 6. 检测是否 @机器人
    │   ├── 未 @ → 结束
    │   └── 已 @ → 继续
    ├── 7. 检查频率限制（Redis：qq:ratelimit:{groupId}:{userId}，5 秒窗口 1 次）
    │   ├── 超限 → 返回提示，结束
    │   └── 未超限 → 继续
    ├── 8. 获取群组绑定应用（im_group.app_id）或默认应用
    ├── 9. 调用 ChatService.imChat()
    │   ├── FAQ 匹配（优先）
    │   └── Dify API 调用
    └── 10. 发送回复到群聊（OneBot API：send_group_msg）
    
Redis Stream 消费（异步）
    ↓
GroupMessageConsumer.consumeMessages()
    ├── 1. 阻塞读取 Stream（最多 5 秒，每次 10 条）
    ├── 2. 处理消息（syncSingleMessage）
    │   ├── 按群 ID 查找已有文档
    │   ├── 合并历史消息
    │   └── 同步到 Dify 知识库
    ├── 3. 更新 synced=true
    └── 4. ACK 确认
```

### 5.3 企业微信群消息处理流程

```
企业微信群用户发送消息
    ↓
企业微信服务器推送回调
    ↓
POST /intellrobot/callback/handle
    ↓
IntelligentRobotController.handleMessage()
    ├── 1. 验证签名（msg_signature, timestamp, nonce）
    ├── 2. 解密消息体（AES-256-CBC）
    ├── 3. 解析消息类型（文本/图片/混合）
    ├── 4. 保存消息到 group_message 表（platform='wecom'）
    ├── 5. 发布到 Redis Stream
    ├── 6. 更新 im_group、im_user 表
    ├── 7. 获取群组绑定应用（im_group.app_id）或默认应用
    ├── 8. Redis 分布式锁（wechat:lock:{messageId}，5 分钟 TTL）
    │   ├── 已存在 → 跳过（防止重复处理）
    │   └── 不存在 → 继续
    ├── 9. 快速返回空响应（企微要求 5 秒内响应）
    └── 10. 异步处理：
        ├── 调用 ChatService.imChat()
        ├── 过滤思考过程（filterThinkingContent）
        ├── Unicode 解码响应
        └── 通过 response_url 回复群聊（HTTP POST）
    
Redis Stream 消费（异步，同 QQ）
    ↓
GroupMessageConsumer.consumeMessages()
    └── ...（同上）
```

### 5.4 知识库文档同步流程

```
用户在前端上传文档
    ↓
KnowledgePage.vue 调用 batchUploadToKb()
    ↓
POST /api/kb/{kbId}/batch-upload
    ↓
KnowledgeBaseController.batchUploadFiles()
    ├── 1. 创建上传任务（UploadProgressService）
    ├── 2. 异步处理：
    │   ├── 遍历文件列表
    │   ├── 每个文件：
    │   │   ├── 上传到 Dify（DifyService.uploadFile()）
    │   │   ├── 创建文档（DifyService.createDocumentByFile()）
    │   │   └── 保存到 kb_document 表
    │   └── 更新进度（UploadProgressService）
    └── 3. 返回 taskId
    
前端 SSE 订阅
    ↓
GET /api/upload/progress/{taskId}/sse
    ↓
UploadProgressController.subscribeProgress()
    ├── 1. 创建 SseEmitter（超时 5 分钟）
    ├── 2. 每 500ms 推送进度
    ├── 3. 任务完成 → 推送 completed → 关闭 Emitter
    └── 4. 客户端断开 → 移除 Emitter
    
前端实时显示进度条
```

### 5.5 统计聚合流程

```
定时任务触发（每天 00:05）
    ↓
StatisticsAggregateServiceImpl.aggregateYesterdayStatistics()
    ├── 1. 查询昨日 kb_conversation 记录
    ├── 2. 按渠道（web/im/wx）分组
    ├── 3. 计算统计指标：
    │   ├── COUNT(DISTINCT conversation_id) → conversation_count
    │   ├── COUNT(*) → message_count
    │   ├── COUNT(DISTINCT user_id) → user_count
    │   ├── SUM(tokens) → total_tokens
    │   ├── SUM(prompt_tokens) → total_prompt_tokens
    │   ├── SUM(completion_tokens) → total_completion_tokens
    │   ├── SUM(total_price) → total_cost
    │   ├── AVG(latency_ms) → avg_latency_ms
    │   └── JOIN kb_feedback → feedback_count, positive_feedback, negative_feedback
    └── 4. 写入 kb_statistics 表（INSERT ... ON DUPLICATE KEY UPDATE）
    
前端查询统计数据
    ↓
StatisticsPage.vue 调用 fetchTokenChartData()
    ↓
GET /api/statistics/token/chart?days=7
    ↓
StatisticsController.getTokenChart()
    └── 查询 kb_statistics 表，返回 ECharts 数据格式
```

---

## 6. 定时任务汇总

| 序号 | 类名 | 方法名 | 执行频率 | 功能说明 | 状态 |
|:----:|------|--------|----------|----------|:----:|
| 1 | GroupMessageConsumer | consumeMessages() | 每 5 秒 | Redis Stream 消息消费者，实时处理新消息 | ✅ 推荐 |
| 2 | GroupMessageSyncServiceImpl | syncToKnowledgeBase() | 每 60 秒 | 同步群消息到 Dify 知识库（批量轮询） | ⚠️ 已过时 |
| 3 | StatisticsAggregateServiceImpl | aggregateYesterdayStatistics() | 每天 00:05 | 聚合昨日统计数据 | ✅ |
| 4 | KeywordSyncServiceImpl | syncKeywordsFromMessages() | 每天 05:00 | 从群聊消息和对话中提取关键词 | ✅ |
| 5 | KeywordSyncServiceImpl | cleanOldKeywords() | 每天 06:00 | 清理超过 90 天的关键词 | ✅ |
| 6 | CleanupServiceImpl | cleanupExpiredConversations() | 每天 03:00 | 清理过期会话数据 | ✅ |
| 7 | CleanupServiceImpl | cleanupOldMessages() | 每天 04:30 | 清理超过 90 天的消息 | ✅ |

---

## 7. 缓存设计

### 7.1 Redis Key 设计

| Key 模式 | 说明 | TTL | 示例 |
|----------|------|-----|------|
| `chatbase:token:{token}` | 用户会话 Token | 7 天 | `chatbase:token:abc123` |
| `chatbase:conversation:{channel}:{userId}` | 用户当前会话 ID | 7 天 | `chatbase:conversation:web:user1` |
| `chatbase:group:message:stream` | IM 消息 Redis Stream | 永久 | - |
| `qq:ratelimit:{groupId}:{userId}` | QQ 群 @机器人限流 | 5 秒 | `qq:ratelimit:123456:789012` |
| `bot:qq:online` | QQ 机器人在线状态 | 30 秒 | - |
| `wechat:lock:{messageId}` | 企微消息分布式锁 | 5 分钟 | `wechat:lock:msg123` |
| `chatbase:upload:progress:{taskId}` | 上传进度 | 1 小时 | `chatbase:upload:progress:task1` |

### 7.2 缓存策略

- **会话 Token**：UUID 生成，Redis 存储，TTL 7 天，每次请求刷新 TTL
- **对话会话 ID**：用户首次对话创建，后续复用，TTL 7 天
- **在线状态**：QQ 心跳每 10 秒更新，30 秒 TTL 自动过期
- **限流**：滑动窗口计数器，5 秒窗口，过期自动清除

---

## 8. 安全设计

### 8.1 认证与授权

- **Token 机制**：UUID Token，Redis 存储，拦截器校验
- **密码加密**：BCrypt 算法，不可逆
- **角色权限**：admin/user，AdminInterceptor 校验管理接口
- **路径排除**：登录、注册、静态资源、企微回调无需认证

### 8.2 数据安全

- **逻辑删除**：is_deleted 标记，不物理删除
- **唯一索引**：防止重复数据（username, platform+group_id, platform+message_id）
- **分布式锁**：企微消息处理防重复（Redis 锁，5 分钟 TTL）
- **频率限制**：QQ 群 @机器人限流（5 秒窗口 1 次）

### 8.3 消息加解密（企业微信）

- **算法**：AES-256-CBC，PKCS7 填充
- **签名**：SHA1（msg_signature）
- **参数**：Token（stoken）、EncodingAESKey（sEncodingAESKey）
- **验证流程**：SHA1(Token + timestamp + nonce + encrypt_msg) 比对签名

---

## 9. 异常处理

### 9.1 异常类型

| 异常类 | 说明 | 处理方式 |
|--------|------|----------|
| `MonitorException` | 监控异常 | 记录日志，返回 500 |
| `RateLimitException` | 频率限制异常 | 返回 429 提示 |
| `AesException` | AES 加解密异常 | 记录日志，返回错误 |

### 9.2 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

| code | 说明 |
|------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未认证 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 429 | 频率限制 |
| 500 | 服务器错误 |

---

## 10. 前端架构

### 10.1 目录结构

```
web/
├── src/
│   ├── main.ts                    # 入口文件
│   ├── pages/                     # 页面组件（11 个）
│   ├── components/                # 公共组件（7 个）
│   ├── api/                       # API 接口（16 个）
│   ├── types/                     # 类型定义
│   ├── lib/                       # 工具库
│   ├── composables/               # Composables（useAuth）
│   └── styles/                    # 样式文件
├── nginx.conf                     # Nginx 配置
├── vite.config.ts                 # Vite 配置
├── tsconfig.json                  # TypeScript 配置
└── package.json                   # 依赖配置
```

### 10.2 路由设计

| 路径 | 页面 | 说明 | 权限 |
|------|------|------|------|
| `/login` | LoginPage | 登录注册 | 公开 |
| `/console/dashboard` | DashboardPage | 系统概览 | admin |
| `/console/groups` | ImGroupsPage | 群聊采集管理 | admin |
| `/console/bots` | BotManagePage | 机器人管理 | admin |
| `/console/statistics` | StatisticsPage | 数据统计 | admin |
| `/console/feedback/manage` | FeedbackManagePage | 反馈管理 | admin |
| `/app` | AppPage | 应用管理 | admin |
| `/chat` | ChatPage | AI 问答 | 登录 |
| `/knowledge` | KnowledgePage | 知识库管理 | admin |
| `/faq` | FaqPage | FAQ 管理 | admin |
| `/feedback` | FeedbackPage | 用户反馈 | 登录 |

### 10.3 状态管理

- **认证状态**：localStorage 存储 token、用户信息、角色
- **API 客户端**：axios 封装，统一拦截器（401 处理、Token 注入）
- **响应式数据**：Vue 3 Composition API（ref, reactive, computed）

### 10.4 关键组件

| 组件 | 功能 |
|------|------|
| `AvatarCropper.vue` | 头像裁切（CropperJS v1.6.2） |
| `UserProfile.vue` | 用户信息弹窗 |
| `GlowCard.vue` | 赛博朋克风格卡片 |
| `KeywordCloud.vue` | 关键词词云（ECharts） |
| `LoadingSpinner.vue` | 加载动画 |
| `ProgressBar.vue` | 进度条 |
| `StatCard.vue` | 统计卡片 |

---

## 11. 部署架构

### 11.1 Docker Compose 服务

| 服务 | 镜像 | 端口 | 说明 |
|------|------|------|------|
| mysql | mysql:8.0 | 3306 | MySQL 数据库 |
| redis | redis:7 | 6379 | Redis 缓存 |
| chatbase-backend | chatbase-backend:latest | 8080 | 后端服务 |
| chatbase-frontend | chatbase-frontend:latest | 80 | 前端服务（Nginx） |
| napcat | mlikiowa/napcat-docker:v4.17.46 | 3000, 6099 | QQ 机器人（可选） |

### 11.2 网络拓扑

```
┌─────────────────────────────────────────┐
│              chatbase-network            │
│                                         │
│  ┌──────────┐    ┌──────────────────┐   │
│  │ Frontend │───▶│ Backend (:8080)  │   │
│  │  (:80)   │    └────────┬─────────┘   │
│  └──────────┘             │              │
│                           ▼              │
│              ┌────────────────────┐      │
│              │ MySQL (:3306)      │      │
│              │ Redis (:6379)      │      │
│              └────────────────────┘      │
│                           │              │
│              ┌────────────▼──────────┐   │
│              │ NapCat (:3000, :6099) │   │
│              └───────────────────────┘   │
└─────────────────────────────────────────┘
         │
         ▼
  外部访问（80 端口）
```

### 11.3 Nginx 配置（web/nginx.conf）

```nginx
server {
    listen 80;
    server_name localhost;

    # 前端静态文件
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # API 代理到后端
    location /api/ {
        proxy_pass http://chatbase-backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # WebSocket 代理（QQ 机器人）
    location /qq/ws {
        proxy_pass http://chatbase-backend:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    # 企业微信回调
    location /intellrobot/ {
        proxy_pass http://chatbase-backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## 12. 配置说明

### 12.1 必填配置

| 配置项 | 环境变量 | 说明 |
|--------|----------|------|
| `difyApp.apiKey` | `DIFYAPP_API_KEY` | Dify Chat API Key |
| `difyApp.datasetApiKey` | `DIFYAPP_DATASET_API_KEY` | Dify Dataset API Key |
| `spring.datasource.password` | `MYSQL_PASSWORD` | MySQL 数据库密码 |

### 12.2 可选配置

| 配置项 | 环境变量 | 默认值 | 说明 |
|--------|----------|--------|------|
| `qq.bot.enable` | `QQ_BOT_ENABLE` | false | 启用 QQ Bot |
| `qq.bot.access-token` | `QQ_BOT_ACCESS_TOKEN` | - | NapCat 访问 Token |
| `qq.bot.self-id` | `QQ_BOT_SELF_ID` | - | 机器人 QQ 号 |
| `qq.bot.nickname` | - | - | 机器人显示名称 |
| `qq.bot.http-base-url` | `QQ_BOT_HTTP_BASE_URL` | http://napcat:3000 | NapCat HTTP 地址 |
| `wechat.corp.stoken` | `WECHAT_CORP_STOKEN` | - | 企业微信 Token |
| `wechat.corp.sEncodingAESKey` | `WECHAT_CORP_S_ENCODING_AES_KEY` | - | 企业微信 EncodingAESKey |
| `wechat.corp.botName` | - | 企业内部机器人 | 企业微信机器人名称 |

### 12.3 聊天配置

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `chat.max-turns-per-session` | 20 | 单会话最大轮数 |
| `chat.session-ttl-days` | 7 | 会话 TTL（天） |
| `chat.rate-limit.window-seconds` | 5 | 限流窗口（秒） |
| `chat.rate-limit.max-requests` | 1 | 窗口内最大请求数 |

---

## 13. API 接口汇总

### 13.1 用户接口（/api/user）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/user/register` | 用户注册 | 公开 |
| POST | `/api/user/login` | 用户登录 | 公开 |
| GET | `/api/user/info?username=` | 获取用户信息 | 登录 |
| PUT | `/api/user/info` | 更新用户信息 | 登录 |
| POST | `/api/user/avatar/upload` | 上传头像 | 登录 |
| POST | `/api/user/logout` | 退出登录 | 登录 |
| POST | `/api/user/change-password` | 修改密码 | 登录 |
| POST | `/api/user/check-password` | 验证密码 | 登录 |

### 13.2 聊天接口（/api/chat）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/chat/ask` | 简单问答 | 登录 |
| POST | `/api/chat/message` | 发送消息 | 登录 |
| POST | `/api/chat/web` | Web 聊天 | 登录 |
| POST | `/api/chat/web/session` | Web 聊天（带会话） | 登录 |
| POST | `/api/chat/im` | IM 聊天 | 系统 |
| POST | `/api/chat/v1/files/upload` | 文件上传 | 登录 |
| POST | `/api/chat/v1/files/batch-upload` | 批量文件上传 | 登录 |

### 13.3 会话接口（/api/chat/session）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/chat/session/create` | 创建会话 | 登录 |
| GET | `/api/chat/session/list` | 会话列表 | 登录 |
| GET | `/api/chat/session/{sessionId}` | 获取会话 | 登录 |
| GET | `/api/chat/session/{sessionId}/messages` | 会话消息 | 登录 |
| DELETE | `/api/chat/session/{sessionId}` | 删除会话 | 登录 |
| PUT | `/api/chat/session/{sessionId}/title` | 更新标题 | 登录 |

### 13.4 知识库接口（/api/kb）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/kb/category/tree` | 分类树 | admin |
| GET | `/api/kb/category/page` | 分类分页 | admin |
| POST | `/api/kb/category` | 创建分类 | admin |
| PUT | `/api/kb/category` | 更新分类 | admin |
| DELETE | `/api/kb/category/{id}` | 删除分类 | admin |
| GET | `/api/kb/page` | 知识库分页 | admin |
| GET | `/api/kb/{id}` | 获取知识库 | admin |
| POST | `/api/kb` | 创建知识库 | admin |
| PUT | `/api/kb` | 更新知识库 | admin |
| DELETE | `/api/kb/{id}` | 删除知识库 | admin |
| POST | `/api/kb/{id}/sync` | 同步知识库 | admin |
| POST | `/api/kb/{id}/batch-upload` | 批量上传文件 | admin |
| POST | `/api/kb/sync-from-dify` | 从 Dify 同步 | admin |
| GET | `/api/kb/dify/list` | 列出 Dify 数据集 | admin |
| GET | `/api/kb/{kbId}/document/page` | 文档分页 | admin |
| POST | `/api/kb/document` | 创建文档 | admin |
| PUT | `/api/kb/document` | 更新文档 | admin |
| DELETE | `/api/kb/document/{id}` | 删除文档 | admin |
| POST | `/api/kb/document/{id}/sync` | 同步文档 | admin |

### 13.5 应用接口（/api/kb/app）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/kb/app/list` | 应用列表 | 登录 |
| GET | `/api/kb/app/page` | 应用分页 | admin |
| GET | `/api/kb/app/{id}` | 获取应用 | admin |
| GET | `/api/kb/app/default` | 获取默认应用 | 登录 |
| POST | `/api/kb/app` | 创建应用 | admin |
| PUT | `/api/kb/app` | 更新应用 | admin |
| DELETE | `/api/kb/app/{id}` | 删除应用 | admin |
| POST | `/api/kb/app/verify` | 验证 API Key | admin |
| GET | `/api/kb/app/{id}/info` | 获取应用信息 | admin |
| PUT | `/api/kb/app/{id}/default` | 设为默认应用 | admin |
| GET | `/api/kb/app/{id}/access` | 检查访问权限 | 登录 |
| GET | `/api/kb/app/{id}/groups` | 获取绑定群组 | admin |

### 13.6 会话与 FAQ 接口（/api/kb/conversation）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/kb/conversation/page` | 会话分页 | admin |
| GET | `/api/kb/conversation/{id}` | 获取会话 | admin |
| POST | `/api/kb/conversation/feedback` | 添加反馈 | 登录 |
| GET | `/api/kb/conversation/feedback/status` | 反馈状态 | 登录 |
| GET | `/api/kb/conversation/faq/page` | FAQ 分页 | admin |
| POST | `/api/kb/conversation/faq` | 创建 FAQ | admin |
| PUT | `/api/kb/conversation/faq` | 更新 FAQ | admin |
| DELETE | `/api/kb/conversation/faq/{id}` | 删除 FAQ | admin |
| GET | `/api/kb/conversation/faq/similar` | 查找相似问题 | 登录 |
| POST | `/api/kb/conversation/faq/extract` | 提取 FAQ | admin |
| GET | `/api/kb/conversation/faq/hot-questions` | 热门问题 | admin |
| GET | `/api/kb/conversation/faq/stats` | FAQ 统计 | admin |

### 13.7 反馈接口（/api/feedback）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| POST | `/api/feedback/submit` | 提交反馈 | 登录 |
| GET | `/api/feedback/page` | 反馈分页 | admin |
| GET | `/api/feedback/user/{userId}` | 用户反馈 | 登录 |
| GET | `/api/feedback/{id}` | 获取反馈 | admin |
| POST | `/api/feedback/{id}/reply` | 回复反馈 | admin |
| PUT | `/api/feedback/{id}/status` | 更新状态 | admin |
| GET | `/api/feedback/stats` | 反馈统计 | admin |

### 13.8 统计接口（/api/statistics）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/statistics/token/daily` | 每日 Token | admin |
| GET | `/api/statistics/token/total` | 总计 Token | admin |
| GET | `/api/statistics/token/chart` | Token 趋势 | admin |
| GET | `/api/statistics/token/monthly` | 月度 Token | admin |
| GET | `/api/statistics/cost/chart` | 费用趋势 | admin |
| GET | `/api/statistics/cost/monthly` | 月度费用 | admin |
| GET | `/api/statistics/group/active` | 群活跃度 | admin |
| GET | `/api/statistics/group/hot-keywords` | 热门关键词 | admin |
| GET | `/api/statistics/keyword/cloud` | 关键词词云 | admin |
| GET | `/api/statistics/keyword/top` | Top 关键词 | admin |
| POST | `/api/statistics/keyword/batch-extract` | 批量提取关键词 | admin |
| POST | `/api/statistics/keyword/sync-latest` | 同步最新关键词 | admin |
| GET | `/api/statistics/conversation/overview` | 会话概览 | admin |
| GET | `/api/statistics/conversation/trend` | 会话趋势 | admin |
| GET | `/api/statistics/system/overview` | 系统概览 | admin |
| GET | `/api/statistics/feedback/daily` | 每日反馈 | admin |
| GET | `/api/statistics/feedback/overview` | 反馈概览 | admin |
| POST | `/api/statistics/aggregate` | 聚合统计 | admin |

### 13.9 控制台接口（/api/console）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/console/overview` | 采集概览 | admin |
| GET | `/api/console/groups` | 群组列表 | admin |
| GET | `/api/console/messages` | 消息分页 | admin |
| PUT | `/api/console/groups/{id}/app` | 绑定应用 | admin |
| DELETE | `/api/console/groups/{id}/app` | 解绑应用 | admin |

### 13.10 机器人接口（/api/bot）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/bot/list` | 机器人列表 | admin |

### 13.11 上传进度接口（/api/upload/progress）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/api/upload/progress/{taskId}` | 获取进度 | admin |
| GET | `/api/upload/progress/{taskId}/sse` | SSE 订阅 | admin |

### 13.12 企业微信回调（/intellrobot）

| 方法 | 路径 | 说明 | 权限 |
|------|------|------|------|
| GET | `/intellrobot/callback/handle` | URL 验证 | 公开 |
| POST | `/intellrobot/callback/handle` | 消息处理 | 公开 |

---

## 14. 性能优化

### 14.1 数据库优化

- **索引策略**：高频查询字段建立索引（user_id, channel, create_time, platform, group_id）
- **唯一索引**：防止重复数据（username, platform+group_id, platform+message_id, conversation_id）
- **全文索引**：kb_document（title, content）、kb_faq（question, keywords）
- **分页查询**：MyBatis-Plus 分页插件，避免全表扫描

### 14.2 缓存优化

- **Redis 缓存**：会话 Token、会话 ID、在线状态、限流计数器
- **TTL 策略**：合理设置过期时间，避免内存泄漏
- **缓存更新**：会话 ID 每次请求刷新 TTL

### 14.3 异步处理

- **线程池**：ThreadPoolConfig 配置核心线程数、最大线程数、队列容量
- **异步任务**：企微消息处理、文档上传、统计聚合
- **SSE 推送**：上传进度实时推送，避免轮询

### 14.4 消息队列

- **Redis Stream**：IM 消息异步处理，替代定时轮询
- **消费者组**：chatbase-sync-group，支持多消费者并行
- **消息确认**：ACK 机制，确保消息不丢失

---

## 15. 扩展性设计

### 15.1 多平台扩展

当前支持 QQ 和企业微信，扩展新平台需：

1. 在 `group_message.platform` 添加新平台标识
2. 实现平台消息处理器（类似 QqBotWebSocketHandler）
3. 在 `im_group.platform`、`im_user.platform` 支持新平台
4. 添加平台配置项

### 15.2 多应用支持

- 每个知识库可绑定独立 Dify 应用
- 群组可绑定特定应用（im_group.app_id）
- 应用优先级：群组绑定 > 默认应用 > 任意启用应用

### 15.3 多租户支持（未来）

- 当前通过 `create_by` 字段区分创建者
- 可扩展 tenant_id 字段实现租户隔离
- 数据隔离：按 tenant_id 过滤查询

---

## 16. 监控与日志

### 16.1 健康检查

| 服务 | 检查方式 | 间隔 | 超时 |
|------|----------|------|------|
| MySQL | mysqladmin ping | 10s | 5s |
| Redis | redis-cli ping | 10s | 5s |
| Backend | curl /api/statistics/system/overview | 30s | 10s |

### 16.2 日志策略

- **Spring Boot 默认日志**：控制台输出，INFO 级别
- **异常日志**：捕获异常，记录堆栈信息
- **业务日志**：关键操作记录（登录、消息处理、同步）

---

*文档版本：v1.0*
*最后更新：2026-05-07*
