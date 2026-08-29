# AGENTS.md

## Project Overview

Spring Boot + Vue 3 chat application with Dify integration, IM message collection (QQ/WeChat Work), user management, feedback system, and knowledge base management.

## Tech Stack

- **Backend**: Java 17, Spring Boot 2.7.6, MyBatis-Plus, WebSocket, Redis, MySQL, Lombok
- **Frontend**: Vue 3, TypeScript, Vite, vue-router, Lucide icons
- **Infrastructure**: Docker Compose, SSE (Server-Sent Events)

## Directory Structure

```
chatBase/
├── pom.xml                          # Maven build (requires <parameters>true</parameters>)
├── sql/init-schema.sql              # Database initialization
├── uploads/                         # File uploads (avatars, etc.)
│
├── src/main/java/com/zxl/chatbase/
│   ├── ChatBaseApplication.java    # Main class
│   │
│   ├── config/                      # Config & interceptors
│   │   ├── AuthInterceptor.java     # JWT auth, injects @RequestAttribute("currentUser")
│   │   ├── AdminInterceptor.java    # Admin role check for admin-only paths
│   │   ├── WebMvcConfig.java        # Interceptor registration & exclude paths
│   │   ├── TokenService.java        # JWT token management
│   │   ├── CorsConfig.java
│   │   ├── JacksonConfig.java
│   │   ├── MybatisPlusConfig.java
│   │   ├── RestTemplateConfig.java
│   │   ├── HttpClientConfig.java
│   │   ├── ThreadPoolConfig.java
│   │   ├── ChatProperties.java
│   │   └── ...
│   │
│   ├── controller/                  # REST controllers
│   │   ├── UserController.java          # /api/user/**
│   │   ├── ChatController.java          # /api/**
│   │   ├── ChatSessionController.java   # /api/chat/session/**
│   │   ├── StatisticsController.java    # /api/statistics/**
│   │   ├── KnowledgeBaseController.java # /api/kb/**
│   │   ├── KbAppController.java         # /api/kb/app/**
│   │   ├── ConversationController.java  # /api/kb/conversation/**
│   │   ├── ImConsoleController.java     # /api/console/**
│   │   ├── FeedbackController.java      # /api/feedback/**
│   │   ├── BotManageController.java     # /api/bot/**
│   │   ├── DatasetController.java       # /api/dify/datasets/**
│   │   ├── UploadProgressController.java# /api/upload/** (SSE progress)
│   │   ├── IntelligentRobotController.java # /intellrobot/**
│   │   └── QqBotController.java         # /api/qq-bot/** (QQ扫码)
│   │
│   ├── chat/                        # Chat service (web + IM)
│   │   ├── service/
│   │   │   ├── ChatService.java / ChatServiceImpl.java
│   │   │   ├── ChatSessionService.java / ChatSessionServiceImpl.java
│   │   │   ├── CleanupService.java / CleanupServiceImpl.java
│   │   │   ├── ...
│   │   ├── entity/ChatSession.java
│   │   └── mapper/ChatSessionMapper.java
│   │
│   ├── im/                          # IM message collection module
│   │   ├── service/
│   │   │   ├── ImConsoleService.java / ImConsoleServiceImpl.java
│   │   │   ├── ImGroupService.java / ImGroupServiceImpl.java
│   │   │   ├── ImSyncService.java / ImSyncServiceImpl.java
│   │   │   ├── ImUserService.java / ImUserServiceImpl.java
│   │   │   ├── ImConversationService.java / ImConversationServiceImpl.java
│   │   │   ├── BotManageService.java / BotManageServiceImpl.java
│   │   │   └── GroupMessageSyncService.java / GroupMessageSyncServiceImpl.java
│   │   ├── consumer/GroupMessageConsumer.java
│   │   └── dto/
│   │
│   ├── command/                       # Bot interactive command framework
│   │   ├── CommandHandler.java        # Command handler interface
│   │   ├── BotCommandDispatcher.java  # Command registry + dispatcher
│   │   └── handler/
│   │       ├── HelpCommand.java       # /help /帮助
│   │       ├── NewCommand.java        # /new /重置
│   │       ├── StatusCommand.java     # /status /状态
│   │       ├── HistoryCommand.java    # /history /历史
│   │       ├── FeedbackCommand.java   # /feedback /反馈
│   │       ├── ClearCommand.java      # /clear /清空
│   │       ├── AppCommand.java        # /app /应用
│   │       └── StatsCommand.java      # /stats /统计
│   │
│   ├── opencode/                    # 本地 opencode serve 集成
│   │   └── service/OpencodeService.java   # 远程驱动本机 opencode（会话映射/轮询）
│   │
│   ├── kb/                          # Knowledge Base module
│   │   ├── service/
│   │   │   ├── IKbCategoryService.java / KbCategoryServiceImpl.java
│   │   │   ├── IKbKnowledgeBaseService.java / KbKnowledgeBaseServiceImpl.java
│   │   │   ├── IKbDocumentService.java / KbDocumentServiceImpl.java
│   │   │   ├── IKbConversationService.java / KbConversationServiceImpl.java
│   │   │   ├── IKbAppService.java / KbAppServiceImpl.java
│   │   │   ├── IKbFaqService.java / KbFaqServiceImpl.java
│   │   │   ├── IKbKeywordService.java / KbKeywordServiceImpl.java
│   │   │   ├── IKbUserCategoryMappingService.java
│   │   │   ├── FileService.java / FileServiceImpl.java
│   │   │   ├── IKeywordSyncService.java / KeywordSyncServiceImpl.java
│   │   │   ├── SysConfigService.java / SysConfigServiceImpl.java
│   │   ├── entity/
│   │   │   ├── KbCategory.java, KbKnowledgeBase.java, KbDocument.java
│   │   │   ├── KbConversation.java, KbFeedback.java, KbFaq.java
│   │   │   ├── KbStatistics.java, KbApp.java, KbKeyword.java
│   │   │   ├── KbUserCategoryMapping.java, KbFile.java
│   │   │   └── SysUser.java, SysConfig.java
│   │   ├── mapper/
│   │   │   └── ...Mapper.java (17 mappers)
│   │   └── dto/FeedbackRequest.java
│   │
│   ├── statistics/                  # Statistics module
│   │   ├── service/
│   │   │   ├── StatisticsService.java / StatisticsServiceImpl.java
│   │   │   └── IStatisticsAggregateService.java
│   │   └── dto/
│   │       ├── TokenStatisticsVO.java, GroupActiveVO.java, KeywordHotVO.java
│   │       ├── ConversationStatisticsVO.java, SystemOverviewVO.java
│   │
│   ├── dify/                        # Dify API integration
│   │   ├── server/DifyService.java
│   │   ├── config/DifyConfig.java
│   │   └── model/ (request/response DTOs)
│   │
│   ├── feedback/                    # User feedback module
│   │   └── service/
│   │       ├── IFeedbackFormService.java
│   │       └── IFeedbackStatsService.java
│   │
│   ├── user/                        # User auth module
│   │   └── service/UserService.java
│   │   └── dto/ (LoginRequest, LoginResponse, RegisterRequest, UserVO)
│   │
│   ├── upload/                      # File upload progress
│   │   ├── service/UploadProgressService.java
│   │   └── entity/UploadProgress.java
│   │
│   ├── common/                      # Common utilities
│   │   ├── service/RateLimitService.java (Redis rate limiting)
│   │   ├── RateLimitException.java, MonitorException.java
│   │   └── ResultCode.java
│   │
│   ├── wxroboot/                    # WeChat Work integration
│   │   └── webhook/
│   │       ├── service/IntelligentRobotService.java
│   │       └── util/WeChatUtil.java, HttpUtils.java
│   │
│   └── qq/                          # QQ Bot 模块
│       ├── QqBotProperties.java         # QQ机器人配置（NapCat地址、WebUI等）
│       ├── QqBotWebSocketHandler.java   # QQ消息WebSocket
│       ├── QqWebSocketConfig.java       # WebSocket配置
│       └── NapCatService.java          # NapCat WebUI API代理服务（扫码登录）
│
└── web/                             # Vue 3 frontend
    ├── src/
    │   ├── main.ts
    │   ├── App.vue                  # Root component with nav layout
    │   ├── router/index.ts          # Route config
    │   ├── api/                     # API client modules
    │   │   ├── client.ts            # Axios instance + interceptors
    │   │   ├── user.ts              # /api/user/**
    │   │   ├── chat.ts              # /api/**
    │   │   ├── console.ts           # /api/console/**
    │   │   ├── statistics.ts        # /api/statistics/**
    │   │   ├── kb.ts                # /api/kb/**
    │   │   ├── bot.ts               # /api/bot/**
    │   │   ├── faq.ts               # FAQ endpoints
    │   │   ├── feedback.ts          # Feedback endpoints
    │   │   ├── feedbackForm.ts      # Feedback form submit
    │   │   ├── feedbackManage.ts    # Admin feedback management
    │   │   ├── feedbackStats.ts     # Feedback stats
    │   │   ├── session.ts           # Chat sessions
    │   │   ├── upload.ts            # File upload
    │   │   └── progress.ts          # Upload progress (SSE)
    │   │
    │   ├── pages/                   # Route pages
    │   │   ├── LoginPage.vue        # /login
    │   │   ├── DashboardPage.vue    # /console/dashboard
    │   │   ├── StatisticsPage.vue   # /console/statistics
    │   │   ├── ImGroupsPage.vue     # /console/im
    │   │   ├── ImSingleChatPage.vue # /console/im/single (私聊采集+opencode绑定)
    │   │   ├── KnowledgePage.vue    # /console/knowledge
    │   │   ├── AppPage.vue          # /console/app
    │   │   ├── BotManagePage.vue    # /console/bots
    │   │   ├── FaqPage.vue          # /console/faq
    │   │   ├── ChatPage.vue         # /chat
    │   │   ├── FeedbackPage.vue     # /feedback
    │   │   ├── AdminAppsPage.vue    # /console/admin/apps (admin only)
    │   │   ├── AdminKbsPage.vue     # /console/admin/kbs (admin only)
    │   │   ├── UserManagePage.vue   # /console/admin/users (admin only)
    │   │   ├── FeedbackManagePage.vue # /console/feedback-manage (admin only)
    │   │   └── ...
    │   │
    │   ├── components/              # Shared components
    │   │   └── UserProfile.vue      # User profile/details modal
    │   │
    │   └── types/                   # TypeScript type definitions
    │       └── console.ts
    │
    └── index.html
```

## Developer Commands

### Backend
```bash
mvn spring-boot:run  # Run from project root (requires MySQL + Redis)
```

### Frontend
```bash
cd web
npm install
npm run dev
npm run build  # TypeScript check via vue-tsc before build
```

## Startup Order

1. MySQL + Redis must be running first
2. Start backend on port 8080
3. Frontend dev server proxies `/api/*` to `http://127.0.0.1:8080`

## Configuration

- **Dev config**: `src/main/resources/application.yaml` (profile: `local`)
- **Prod config**: `src/main/resources/application-prod.yaml`
- **Main class**: `com.zxl.chatbase.ChatBaseApplication`
- **pom.xml** requires `<parameters>true</parameters>` in `maven-compiler-plugin` for Spring to resolve parameter names

### Key Settings
| Setting | Default |
|---------|---------|
| MySQL | `localhost:3306/chat_base` |
| Redis | `localhost:6379` (optional password via `SPRING_REDIS_PASSWORD`) |
| File upload | 100MB max |
| Dify API | configured via `difyApp.*` properties |
| opencode | `opencode.enabled=false`; local overrides in `application-local.yaml` (git-ignored); prod via `OPENCODE_*` env vars |

## Auth & Permission Model

### Interceptors (WebMvcConfig.java)
| Interceptor | Paths | Excludes |
|-------------|-------|----------|
| `AuthInterceptor` | `/api/**` | `/api/user/login`, `/api/user/register`, `/api/chat/**`, `/api/upload/**`, `/api/uploads/**`, `/api/feedback/submit`, `/api/feedback/user/**`, `/qq/**`, `/intellrobot/**`, `/error`, `/uploads/**` |
| `AdminInterceptor` | `/api/feedback/page`, `/api/feedback/*/reply`, `/api/feedback/*/status`, `/api/feedback/stats`, `/api/user/list`, `/api/user/*/detail`, `/api/user/*/role`, `/api/user/*/status`, `/api/user/*/remove`, `/api/kb/app/admin/**`, `/api/kb/admin/**` |

### Flow
1. AuthInterceptor validates JWT token → injects `@RequestAttribute("currentUser")` with username
2. AdminInterceptor checks SysUser.role == "admin" for admin-only paths
3. Public paths (chat, feedback submit, QQ WebSocket) bypass auth entirely

### Data Isolation (User Scope)
Implemented via query-time filtering in all service layers:
- **Rule**: `created_by = currentUser OR created_by IS NULL` (system-level records)
- **Statistics**: Admin sees all by default (`scope=all`), or own data (`scope=mine`); regular users always see own data
- **Groups**: Visible if `created_by IS NULL` (unclaimed) OR `created_by = currentUser` (claimed/assigned)
- **Categories**: Visible if `create_by = currentUser` OR `create_by IS NULL` (system defaults like "群聊消息")
- **Apps/Knowledge Bases**: Filtered by `created_by = currentUser`

## Data Flow

### QQ Bot QR Login (NapCat WebUI 代理)
1. 前端点击「扫码登录」→ `QqBotController` → `NapCatService` 代理到 NapCat WebUI API
2. NapCat 返回 QQ 登录二维码图片（从 ptlogin2.qq.com 拉取），后端以 base64 返回前端
3. 前端 `<img>` 渲染二维码 + 2s 轮询 NapCat `CheckLoginStatus`
4. 用户手机 QQ 扫码 → NapCat 检测到 `isLogin=true` → 前端关闭弹窗、刷新列表

### QQ Group Message Collection
1. NapCat/go-cqhttp → WebSocket `/qq/ws` → `QqBotWebSocketHandler`
2. Messages saved to `group_message` table via `GroupMessageSyncService.saveGroupMessage()`
3. Scheduled task syncs messages to Dify knowledge base

### Chat Service
1. `ChatController` receives requests
2. Rate limiting via `RateLimitService` (Redis-based)
3. `ChatServiceImpl` handles business logic:
   - Gets/creates conversation ID from Redis
   - Calls Dify API via `DifyService`
   - Saves conversation to `kb_conversation` table
4. Supports web chat (with sessions) and IM bot chat

### Private Chat → Local Opencode (远程驱动本机 opencode)
1. 私聊消息到达 QQ/企微/微信处理器后，先经 `ImConversationService.isOpencodeBound(conversationId)` 判断是否绑定 opencode（`appId == -1L`）
2. 若绑定 → `OpencodeService.chat()` 走 opencode 通道；否则走原 Dify 链路
3. `OpencodeService`: Redis 中 `opencode:session:<conversationId>` 映射 opencode sessionId（TTL 7 天）
   - `POST /api/session` 创建会话（directory/agent）→ `POST /api/session/{id}/prompt` 发消息 → 每 2s 轮询 `GET /api/session/{id}/message` 取最新 assistant 文本
   - Basic Auth（username=opencode / OPENCODE_SERVER_PASSWORD）
4. 回复写入 `kb_conversation`（appId=-1, appName=本地opencode），再回发私聊
5. 绑定「本地opencode」仅 admin 可用（`ImConversationServiceImpl.isAdmin()` 校验 SysUser.role=="admin"）

### File Upload
1. Files uploaded to Dify `/files/upload` API, then synced to dataset
2. Progress tracked via `UploadProgressService` + SSE endpoint
3. Avatar uploads stored locally in `uploads/` directory

## API Endpoints

### User & Auth (`/api/user`)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/register` | None | Register new user |
| POST | `/login` | None | Login, returns JWT token |
| POST | `/logout` | Auth | Logout |
| GET | `/info` | Auth | Get user profile (`?username=`) |
| PUT | `/info` | Auth | Update user profile |
| POST | `/avatar/upload` | Auth | Upload avatar |
| POST | `/change-password` | Auth | Change password |
| POST | `/check-password` | Auth | Verify current password |
| GET | `/list` | Admin | List all users |
| GET | `/{id}/detail` | Admin | Get user detail |
| PUT | `/{id}/role` | Admin | Update user role |
| PUT | `/{id}/status` | Admin | Toggle user status |
| DELETE | `/{id}/remove` | Admin | Delete user |

### Chat (`/api/chat`)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/ask` | None | Simple Q&A |
| POST | `/message` | None | Send message |
| POST | `/web` | None | Web chat |
| POST | `/web/session` | None | Web chat with session |
| POST | `/im` | None | IM bot chat |
| POST | `/v1/files/upload` | None | Upload file |
| POST | `/v1/files/batch-upload` | None | Batch upload files |

### Chat Session (`/api/chat/session`)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/create` | None | Create session |
| GET | `/list` | None | List user sessions |
| GET | `/{sessionId}` | None | Get session |
| GET | `/{sessionId}/messages` | None | Get session messages |
| DELETE | `/{sessionId}` | None | Delete session |
| PUT | `/{sessionId}/title` | None | Update session title |

### Statistics (`/api/statistics`)

Most endpoints support `scope=all` (admin view all) / `scope=mine` (view own data).

| Method | Path | Scope | Description |
|--------|------|-------|-------------|
| GET | `/token/daily` | ✅ | Daily token usage (default 7d) |
| GET | `/token/total` | ✅ | Total token usage (30d) |
| GET | `/token/chart` | ✅ | Token chart data |
| GET | `/token/monthly` | ✅ | Monthly token data |
| GET | `/cost/chart` | ✅ | Cost chart data |
| GET | `/cost/monthly` | ✅ | Monthly cost data |
| GET | `/group/active` | ✅ | Group active rank |
| GET | `/group/hot-keywords` | ✅ | Hot keywords |
| GET | `/conversation/overview` | ✅ | Conversation overview |
| GET | `/conversation/trend` | ✅ | Conversation trend |
| GET | `/system/overview` | ✅ | System overview |
| GET | `/keyword/cloud` | - | Keyword cloud |
| GET | `/keyword/top` | - | Top keywords |
| POST | `/keyword/batch-extract` | - | Batch extract keywords |
| POST | `/keyword/sync-latest` | - | Sync latest keywords |
| GET | `/feedback/daily` | - | Daily feedback stats |
| GET | `/feedback/overview` | - | Feedback overview |
| POST | `/aggregate` | - | Aggregate statistics |

### Console / IM (`/api/console`)
| Method | Path | Scope | Description |
|--------|------|-------|-------------|
| GET | `/overview` | ✅ | Collection overview |
| GET | `/groups` | ✅ | Group list (platform/scope) |
| GET | `/messages` | ✅ | Message pagination |
| PUT | `/groups/{id}/app` | Auth | Bind app to group |
| DELETE | `/groups/{id}/app` | Auth | Unbind app from group |
| POST | `/groups/{id}/assign` | Auth | Assign group to user |

### Knowledge Base (`/api/kb`)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/category/tree` | Auth | Category tree |
| GET | `/category/page` | Auth | Category page |
| POST | `/category` | Auth | Create category |
| PUT | `/category` | Auth | Update category |
| DELETE | `/category/{id}` | Auth | Delete category |
| GET | `/page` | Auth | KB page list |
| GET | `/admin/page` | Admin | KB page (all) |
| GET | `/{id}` | Auth | Get KB by ID |
| POST | `` | Auth | Create KB |
| PUT | `` | Auth | Update KB |
| DELETE | `/{id}` | Auth | Delete KB |
| POST | `/{id}/sync` | Auth | Sync documents to Dify |
| POST | `/{id}/batch-upload` | Auth | Batch upload files |
| POST | `/sync-from-dify` | Auth | Sync KBs from Dify |
| GET | `/dify/list` | Auth | List Dify datasets |
| GET | `/{kbId}/document/page` | Auth | Document page |
| POST | `/document` | Auth | Create document |
| PUT | `/document` | Auth | Update document |
| DELETE | `/document/{id}` | Auth | Delete document |
| POST | `/document/{id}/sync` | Auth | Sync document to Dify |
| POST | `/{kbId}/link-category` | Auth | Link KB to category |
| DELETE | `/{kbId}/link-category/{mappingId}` | Auth | Unlink category |
| GET | `/{kbId}/link-category/list` | Auth | List linked categories |

### App (`/api/kb/app`)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/list` | Auth | List apps (user scope) |
| GET | `/admin/list` | Admin | List all apps |
| GET | `/page` | Auth | App page |
| GET | `/{id}` | Auth | Get app |
| GET | `/default` | Auth | Get default app |
| POST | `` | Auth | Create app |
| PUT | `` | Auth | Update app |
| DELETE | `/{id}` | Auth | Delete app |
| POST | `/verify` | Auth | Verify API key |
| GET | `/{id}/info` | Auth | Get app info |
| PUT | `/{id}/default` | Auth | Set default app |
| GET | `/{id}/access` | Auth | Check app access |
| GET | `/{id}/groups` | Auth | Get bound groups |

### Conversation / FAQ (`/api/kb/conversation`)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/page` | None | Conversation history |
| GET | `/{id}` | None | Get conversation |
| POST | `/feedback` | None | Add feedback (thumbs up/down) |
| GET | `/feedback/status` | None | Get feedback status |
| GET | `/faq/page` | None | FAQ page |
| POST | `/faq` | None | Create FAQ |
| PUT | `/faq` | None | Update FAQ |
| DELETE | `/faq/{id}` | None | Delete FAQ |
| GET | `/faq/similar` | None | Find similar FAQ |
| POST | `/faq/extract` | None | Extract FAQ from conversations |
| GET | `/faq/hot-questions` | None | Hot questions |
| GET | `/faq/stats` | None | FAQ stats |

### Feedback (`/api/feedback`)
| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/submit` | None | Submit feedback |
| GET | `/page` | Admin | Feedback page |
| GET | `/user/{userId}` | None | User feedback page |
| GET | `/{id}` | Auth | Get feedback |
| POST | `/{id}/reply` | Admin | Reply to feedback |
| PUT | `/{id}/status` | Admin | Update feedback status |
| GET | `/stats` | Admin | Feedback stats |

### Other
| Method | Path | Controller | Description |
|--------|------|------------|-------------|
| GET | `/api/bot/list` | BotManageController | List bots (user scope) |
| POST | `/api/dify/datasets` | DatasetController | Create Dify dataset |
| GET | `/api/upload/progress/{taskId}` | UploadProgressController | Get upload progress |
| GET | `/api/upload/progress/{taskId}/sse` | UploadProgressController | SSE progress stream |
| GET/POST | `/intellrobot/callback/handle` | IntelligentRobotController | WeChat Work callback |
| GET | `/api/qq-bot/qrcode` | QqBotController | Get QQ QR code (base64 image) |
| GET | `/api/qq-bot/qrcode/status` | QqBotController | Poll QQ login status |
| POST | `/api/qq-bot/qrcode/refresh` | QqBotController | Refresh QR code |
| GET | `/api/qq-bot/status` | QqBotController | Get QQ login info |
| GET | `/api/qq-bot/available` | QqBotController | Check NapCat WebUI availability |

## Database Tables

### IM Tables
| Table | Description |
|-------|-------------|
| `group_message` | QQ/WeChat group messages with sync status |
| `group_kb_mapping` | Group to Dify document/knowledge base mapping |
| `im_group` | IM groups (with `created_by` for ownership) |
| `im_user` | IM users |
| `t_duty_chat_group` | Duty chat group config |

### Knowledge Base Tables (`sql/init-schema.sql`)
| Table | Description | Key Fields |
|-------|-------------|------------|
| `sys_user` | System users | username, password, role (admin/user), status |
| `kb_category` | Categories (tree) | name, parent_id, create_by |
| `kb_knowledge_base` | Knowledge bases | name, dify_dataset_id, created_by |
| `kb_document` | Documents | knowledge_base_id, title, content, dify_document_id |
| `kb_conversation` | Chat history | session_id, app_id, query, answer, channel |
| `kb_feedback` | User feedback | conversation_id, rating, feedback_type, content |
| `kb_faq` | FAQs | knowledge_base_id, question, answer |
| `kb_statistics` | Daily stats | date, token_count, conversation_count |
| `kb_app` | App configs | name, api_key, dify_app_id, created_by |
| `kb_keyword` | Keywords | word, source, count |
| `kb_user_category_mapping` | KB-category links | kb_id, category_id |

## QQ Bot Configuration

NapCat/go-cqhttp should connect via reverse WebSocket:
```yaml
ws-reverse:
  - url: ws://localhost:8080/qq/ws
```

Robot QQ number configured in `application.yaml`:
```yaml
qq:
  bot:
    self-id: YOUR_ROBOT_QQ_NUMBER
    webui-base-url: "http://localhost:6099"   # NapCat WebUI 地址
    webui-token: ""                            # NapCat WebUI token
```

## Frontend Pages & Routes

| Route | Page | Description | Admin Only |
|-------|------|-------------|------------|
| `/login` | LoginPage | Login / Register | No |
| `/console/dashboard` | DashboardPage | System dashboard overview | No |
| `/console/statistics` | StatisticsPage | Usage statistics with scope toggle | No |
| `/console/im` | ImGroupsPage | Group chat management | No |
| `/console/im/single` | ImSingleChatPage | Private chat collection + Dify/opencode binding (opencode admin only) | No |
| `/console/knowledge` | KnowledgePage | Knowledge base management | No |
| `/console/app` | AppPage | App management | No |
| `/console/bots` | BotManagePage | Robot/bot management | No |
| `/console/faq` | FaqPage | FAQ management | No |
| `/chat` | ChatPage | Web chat interface | No |
| `/feedback` | FeedbackPage | Submit feedback | No |
| `/console/feedback-manage` | FeedbackManagePage | Admin feedback review | Yes |
| `/console/admin/apps` | AdminAppsPage | All apps management | Yes |
| `/console/admin/kbs` | AdminKbsPage | All KBs management | Yes |
| `/console/admin/users` | UserManagePage | User management | Yes |

### Auth State in Frontend

`App.vue` manages auth state via `syncAuthState()`:
- Reads `chatbase_role`, `chatbase_user` from localStorage
- `isAdmin` is a `ref` (not `computed`) synced on route change and mount
- Login → stores token, user, role in localStorage → navigates to dashboard
- Logout → clears localStorage → navigates to login

## Known Issues & Conventions

- **Parameter names**: `pom.xml` must have `<parameters>true</parameters>` in `maven-compiler-plugin` for `@RequestParam`, `@RequestAttribute` to work
- **Group visibility**: `im_group.created_by` default is NULL (public). `bindApp()` sets `created_by`, `unbindApp()` sets it back to NULL
- **System categories**: "群聊消息" category has `create_by = NULL` (visible to all, editable by none)
- **No test suite**: Manual API testing only
