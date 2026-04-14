# AGENTS.md

## Project Overview

Spring Boot + Vue 3 chat application with Dify integration, IM message collection (QQ/WeChat Work).

## Tech Stack

- **Backend**: Java 17, Spring Boot 2.7.6, MyBatis-Plus, WebSocket, Redis, MySQL
- **Frontend**: Vue 3, TypeScript, Vite
- **Deployment**: Docker Compose

## Directory Structure

- `src/main/java/com/zxl/chatbase/kb/` - Knowledge Base module (new)
- `src/main/java/com/zxl/chatbase/im/` - IM message collection module
- `src/main/java/com/zxl/chatbase/dify/` - Dify integration module
- `src/main/java/com/zxl/chatbase/qq/` - QQ Bot WebSocket handler
- `src/main/java/com/zxl/chatbase/chat/` - Chat service layer
- `web/` - Vue 3 frontend source
- `sql/` - Database initialization scripts (`init-schema.sql` preferred)

## Developer Commands

### Backend
```bash
mvn spring-boot:run  # Run from project root
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

### Key Settings
- MySQL: `localhost:3306/chat_base` (database name is `chat_base`)
- Redis: `localhost:6379` (optional password via `SPRING_REDIS_PASSWORD`)
- File upload limit: 100MB
- Dify API configured via `difyApp.*` properties

## Data Flow

### QQ Group Message Collection
1. NapCat/go-cqhttp → WebSocket `/qq/ws` → `QqBotWebSocketHandler`
2. Messages saved to `group_message` table via `GroupMessageSyncService.saveGroupMessage()`
3. Scheduled task syncs messages to Dify knowledge base

### Chat Service
1. `ChatController` receives requests (`/api/chat/web`, `/api/chat/im`)
2. `ChatServiceImpl` handles business logic:
   - Gets conversation ID from Redis
   - Calls Dify API via `DifyService`
   - Saves conversation to `kb_conversation` table
3. Response returned to client

## Database Tables

### Original Tables
- `group_message` - QQ/WeChat group messages with sync status
- `group_kb_mapping` - Group to Dify document mapping
- `t_duty_chat_group` - Duty chat group config

### New Knowledge Base Tables (sql/init-schema.sql)
| Table | Description |
|-------|-------------|
| `sys_user` | System users |
| `kb_category` | Knowledge base categories (tree structure) |
| `kb_knowledge_base` | Knowledge bases (linked to Dify datasets) |
| `kb_document` | Documents within knowledge bases |
| `kb_conversation` | Chat conversation history |
| `kb_feedback` | User feedback on AI answers |
| `kb_faq` | Frequently asked questions |
| `kb_statistics` | Daily usage statistics |

## API Endpoints

### Chat APIs
- `GET /api/chat/ask` - Simple Q&A
- `POST /api/chat/web` - Web chat
- `POST /api/chat/im` - IM bot chat
- `POST /api/chat/v1/files/upload` - File upload

### Console APIs
- `GET /api/console/overview` - Collection overview
- `GET /api/console/groups` - Group statistics
- `GET /api/console/messages` - Message pagination

### Knowledge Base APIs
- `GET/POST/PUT/DELETE /api/kb` - Knowledge base CRUD
- `GET/POST/PUT/DELETE /api/kb/category` - Category CRUD
- `GET/POST/PUT/DELETE /api/kb/document` - Document CRUD
- `POST /api/kb/{id}/sync` - Sync documents to Dify
- `GET /api/kb/conversation/page` - Conversation history
- `POST /api/kb/conversation/feedback` - Add feedback
- `GET/POST/PUT/DELETE /api/kb/conversation/faq` - FAQ CRUD

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
```

## Testing

No dedicated test suite found. Use manual API testing or IDE test runners.
