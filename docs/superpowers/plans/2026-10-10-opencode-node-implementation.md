# OpenClaw 式 opencode 节点实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 增加受配对和能力批准保护的本机 opencode 节点通道，并让管理员在现有私聊管理页逐会话启用，同时保留 Dify、直连和旧桥接行为。

**Architecture:** ChatBase 继续作为 IM Gateway。新增独立 `/opencode/node` WSS 协议、节点注册表和 typed RPC；Node Agent 只把固定 opencode 命令映射成本机 OpenCode API 调用。`im_conversation.opencode_node_id` 决定是否走新节点，空值继续走旧 transport。

**Tech Stack:** Java 17、Spring Boot 2.7、MyBatis-Plus、Redis、Spring WebSocket、Node.js 22 原生 WebSocket/fetch、Vue 3/TypeScript、MySQL、Nginx。

---

## 实施边界

- 当前工作区已有大量用户修改和暂存内容；只编辑本计划列出的目标文件，不重置、stash 或格式化全仓文件。
- 按用户已批准的设计实施：多节点、私聊显式绑定、管理员配对与批准、私聊页嵌入节点管理、旧链路并行兼容、离线即时失败且不排队/不故障转移。
- 新节点协议与 `/opencode/bridge` V1 分开；V1 和 `opencode.bridge.transport=direct|bridge|auto` 的旧语义保留。
- 本次不添加或运行测试；用户请求实施但没有要求测试/验证。下面的验收项记录实现应满足的行为，不作为本次测试命令。

## 文件分工

### 数据和节点服务

- Create `src/main/java/com/zxl/chatbase/opencode/node/entity/OpencodeNode.java`：节点持久实体。
- Create `src/main/java/com/zxl/chatbase/opencode/node/mapper/OpencodeNodeMapper.java`：节点数据库访问。
- Create `src/main/java/com/zxl/chatbase/opencode/node/dto/OpencodeNodeVO.java`、`PairingCodeVO.java`、`NodePairRequest.java`：对前端和 Agent 的字段投影，不返回 token hash。
- Create `src/main/java/com/zxl/chatbase/opencode/node/OpencodeNodeService.java`、`OpencodeNodeServiceImpl.java`：配对码、节点批准/拒绝/撤销/重命名、会话绑定和节点选择。
- Modify `src/main/java/com/zxl/chatbase/im/entity/ImConversation.java`、`src/main/java/com/zxl/chatbase/im/dto/ConversationSummaryVO.java`、`src/main/java/com/zxl/chatbase/im/mapper/ImConversationMapper.java`、`src/main/java/com/zxl/chatbase/im/service/ImConversationService.java`、`src/main/java/com/zxl/chatbase/im/service/impl/ImConversationServiceImpl.java`：保存、查询并授权节点绑定。
- Modify `sql/init-schema.sql`、`sql/upgrade-existing-db.sql`：新表与可重复执行的新增列迁移。

### WebSocket RPC 与本机 Agent

- Create `src/main/java/com/zxl/chatbase/opencode/node/OpencodeNodeProperties.java`：新节点通道开关、等待/调用超时和心跳时限。
- Create `src/main/java/com/zxl/chatbase/opencode/node/OpencodeNodeRegistry.java`、`OpencodeNodeSession.java`：活动连接、节点状态和请求 Future。
- Create `src/main/java/com/zxl/chatbase/opencode/node/OpencodeNodeCommand.java`、`OpencodeNodeCommandMapper.java`、`OpencodeNodeWebSocketConfig.java`、`OpencodeNodeWebSocketHandler.java`、`OpencodeNodeRpcClient.java`：协议定义、`/opencode/node` 注册和 RPC 路由。
- Create `bridge/opencode-node.mjs`：新协议 Node Agent。保持 `bridge/opencode-bridge.mjs` 为旧 V1 客户端。
- Modify `src/main/java/com/zxl/chatbase/opencode/OpencodeService.java`：按绑定选择 direct/legacy/new-node transport，并保留现有聊天和 OpenCode API 高层行为。
- Modify `src/main/java/com/zxl/chatbase/command/handler/NewCommand.java`、`ClearCommand.java`、`StatusCommand.java`：通过 `OpencodeService` 读写会话映射，避免命令绕过节点作用域。

### 管理 API、界面与部署文档

- Create `src/main/java/com/zxl/chatbase/controller/OpencodeNodeController.java`：管理员 REST 管理入口。
- Modify `src/main/java/com/zxl/chatbase/config/WebMvcConfig.java`：新管理路由注册管理员拦截器。
- Create `web/src/api/opencodeNodes.ts`、`web/src/types/opencodeNode.ts`：节点 API 和类型。
- Modify `web/src/pages/ImSingleChatPage.vue`、`web/src/types/console.ts`：内嵌节点管理卡片和逐私聊绑定控件。
- Modify `src/main/resources/application.yaml`、`src/main/resources/application-prod.yaml`、`.env.example`、`web/nginx.conf`、`bridge/README.md`、`DEPLOY.md`：配置、WSS 代理和新旧 Agent 使用说明。

## 任务 1：节点模型与数据库迁移

**Files:** 上述数据和节点服务文件中的 entity、mapper、VO、会话 entity/DTO/mapper 以及两个 SQL 文件。

- [x] 定义节点字段：`id`、稳定 `nodeKey`、`name`、`platform`、`agentVersion`、`tokenHash`、`capabilities`、`approvedCapabilities`、`status`、`createdBy`、`lastSeenAt`、`createTime`、`updateTime`。状态限定为 `PENDING`、`APPROVED`、`REJECTED`、`REVOKED`。
- [x] 在 `init-schema.sql` 创建 `opencode_node`：`node_key` 唯一，`token_hash` 为 64 位 SHA-256 十六进制摘要，能力集合存 TEXT JSON；按项目现有表的时间戳、字符集和索引惯例编写。
- [x] 给 `im_conversation` 加 `opencode_node_id BIGINT DEFAULT NULL` 和普通索引，不加跨表级联删除；删除节点后会话保留绑定值并显示节点已撤销。
- [x] 在 `upgrade-existing-db.sql` 用项目现有 `information_schema` 检查与动态 `ALTER TABLE` 模式实现幂等迁移。
- [x] 在 `ImConversation` 和 `ConversationSummaryVO` 增加 `Long opencodeNodeId`；更新 `ImConversationMapper.selectAccessibleConversations` 的 SELECT 列并映射 `opencode_node_id AS opencodeNodeId`。
- [x] 为节点实体创建 BaseMapper；节点 DTO 只包含页面需要的字段，严禁序列化 `tokenHash`。

期望的 SQL 结构核心如下：

```sql
CREATE TABLE IF NOT EXISTS `opencode_node` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `node_key` VARCHAR(64) NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `platform` VARCHAR(32) DEFAULT NULL,
  `agent_version` VARCHAR(40) DEFAULT NULL,
  `token_hash` CHAR(64) NOT NULL,
  `capabilities` TEXT NOT NULL,
  `approved_capabilities` TEXT DEFAULT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  `created_by` VARCHAR(64) NOT NULL,
  `last_seen_at` DATETIME DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_opencode_node_key` (`node_key`),
  KEY `idx_opencode_node_status` (`status`)
);
```

## 任务 2：配对生命周期与管理员 REST API

**Files:** 节点 service/DTO 文件，`OpencodeNodeController.java`，`WebMvcConfig.java`，`ImConversationService.java` 和 `ImConversationServiceImpl.java`。

- [x] `createPairingCode(adminId)` 用 `SecureRandom` 生成不可猜测的 Base64 URL 安全码，Redis 仅存 SHA-256 摘要和管理员 ID，TTL 为 10 分钟；消费时使用 Redis 原子取删，成功一次后立即失效。
- [x] `enrollNode(request, pairingCode)` 原子消费配对码，验证节点能力均属于固定命令集合，生成稳定节点 key 与 256 位随机节点凭据，数据库只存摘要，原始凭据只在本次 WSS 配对响应中返回。
- [x] `approveNode(id, adminId)` 将 `approvedCapabilities` 固定为本次声明集合并置为 `APPROVED`；`rejectNode` 置为 `REJECTED`；`revokeNode` 置为 `REVOKED`、撤销凭据并断开活动连接；`renameNode` 仅更新名称。
- [x] 添加管理端点：`POST /api/opencode/nodes/pairing-codes`、`GET /api/opencode/nodes`、`POST /api/opencode/nodes/{id}/approve`、`POST /api/opencode/nodes/{id}/reject`、`PUT /api/opencode/nodes/{id}/name`、`DELETE /api/opencode/nodes/{id}`。
- [x] 添加会话端点：`PUT /api/console/conversations/{id}/opencode-node` 接受 `{ "nodeId": 123 }`，设置 `app_id=-1`、`app_name=本地opencode` 和节点 ID。`DELETE` 只清空节点 ID，保留 `app_id=-1`，让该会话回到旧 opencode transport；沿用现有 `/app` 删除路径可完全解绑 opencode。只有管理员可绑定/解绑此特殊执行端。
- [x] 在 `WebMvcConfig` 给 `/api/opencode/**` 和会话节点绑定端点增加 `AdminInterceptor` 保护；Controller 仍使用现有 `@RequestAttribute("currentUser")` 身份来源。
- [x] 列表中离线的已批准节点仍可绑定，但返回连接状态供界面明确警告；撤销节点保留会话的 `opencode_node_id`，运行时返回“节点已撤销”，管理员重新绑定后恢复。

## 任务 3：新节点 WebSocket 协议和 RPC 注册表

**Files:** 节点 properties、registry、session、command、WebSocket config/handler/RPC client。

- [x] `OpencodeNodeCommand` 固定列出：`opencode.health`、`opencode.session.list`、`opencode.session.get`、`opencode.session.create`、`opencode.prompt.submit`、`opencode.message.list`、`opencode.question.list`、`opencode.question.reply`。
- [x] 配置新端点 `/opencode/node`，与 `/opencode/bridge` 完全分离。新端点首个文本帧必须在 10 秒内是 `hello` 或 `auth`；在身份通过前不注册节点、不接受调用。不要从 query string 取配对码或节点 token。
- [x] 首次配对 `hello` 包含 `protocolVersion`、`pairingCode`、`nodeKey`、`name`、`platform`、`agentVersion` 和 `capabilities`；配对响应返回 `nodeId`、一次性节点凭据和 `approval=PENDING`。待审批凭据只允许重连和心跳。
- [x] 后续重连 `auth` 包含 `nodeId` 和节点凭据；服务器以摘要比对并检查节点状态。`APPROVED` 节点只能执行 `approvedCapabilities`；其他状态不能发起或接收 invoke。
- [x] Agent 重连时若声明能力和记录不一致，服务器把新增能力置为待审批，旧批准能力与当前声明的交集可继续工作；审批前拒绝新增能力。
- [x] RPC `invoke` 使用 `{type,id,command,params,deadlineMs}`；Agent 用 `result`、`progress`、`error` 回包；服务器的 `cancel` 帧只包含对应 invoke ID。协议版本不兼容时以策略关闭码和不含凭据的错误原因断开。
- [x] `OpencodeNodeSession` 以 `ConcurrentHashMap<String, CompletableFuture<NodeResult>>` 跟踪请求；发送/完成/超时/断线/撤销都清理 pending map。JSON 统一由 `ObjectMapper` 编解码，禁止字符串拼接 JSON。
- [x] `OpencodeNodeRegistry` 以数据库 node ID 找在线 session，不使用“单个全局 active node”；一个 node key 同时只保留一条活动连接，新连接替换旧连接并使旧请求失败。
- [x] 心跳刷新 `lastSeenAt`；超过配置的离线阈值后列表显示离线。超时、撤销或 WebSocket 关闭时完成该节点的 pending Future，不重放 invoke。

RPC 调用 DTO 固定成下列形状，不携带 HTTP method/path：

```java
public record NodeInvokeRequest(String id, String command, JsonNode params, long deadlineMs) {}
public record NodeInvokeResult(String id, String status, JsonNode data, String errorCode, String message) {}
```

## 任务 4：Node.js 22 本机 Agent

**Files:** Create `bridge/opencode-node.mjs`；不修改旧 `bridge/opencode-bridge.mjs` 的 V1 握手和消息格式。

- [x] 读取 `CHATBASE_NODE_WS_URL`、`OPENCODE_PAIR_CODE`、`OPENCODE_URL`、`OPENCODE_SERVER_USERNAME`、`OPENCODE_SERVER_PASSWORD` 和 `BRIDGE_NAME`。新 Agent 第一次用 pair code，之后从用户配置目录读取 `nodeId` 和 node token；终端不打印 code/token。
- [x] 建立原生 `WebSocket` 客户端，发送上述 `hello`/`auth` 帧，处理 `paired`、`invoke`、`cancel`、`ping`，断线按 1 到 30 秒指数退避重连。
- [x] 实现精确命令分派表：每个 command 映射到一条固定本机 URL 和 method；`sessionId`、`requestId` 均通过 `encodeURIComponent` 编码。默认只允许 `http://127.0.0.1:4096` 或用户显式配置的 loopback 地址。
- [x] 请求最多运行到 `deadlineMs`；处理 cancel 时中止本地 fetch。OpenCode Basic Auth 只加到本机 fetch header，不转发到 ChatBase。
- [x] 不实现 `fetch(method,path,body)` 通用远程接口；未知 command 返回 `UNSUPPORTED_COMMAND`。记录日志时只写命令名、状态码和耗时，不写凭据、完整 prompt 或消息正文。
- [x] 更新 `bridge/README.md`：新 Agent 配对、重连、存储凭据和配置本机 OpenCode 的说明；将旧 V1 用法标为兼容模式。

Node 端固定映射核心示意：

```js
const handlers = {
  'opencode.health': health,
  'opencode.session.list': listSessions,
  'opencode.session.get': getSession,
  'opencode.session.create': createSession,
  'opencode.prompt.submit': submitPrompt,
  'opencode.message.list': listMessages,
  'opencode.question.list': listQuestions,
  'opencode.question.reply': replyQuestion,
};
```

## 任务 5：OpencodeService 路由与会话隔离

**Files:** `OpencodeService.java`、节点 RPC client/mapper、`NewCommand.java`、`ClearCommand.java`、`StatusCommand.java`。

- [x] 注入会话绑定读取能力；每个现有 OpenCode 调用先取 `opencodeNodeId`。若为空，完整沿用现在的 `callOpencode` 逻辑；非空时调用严格的 Java `OpencodeNodeCommandMapper`，把当前七种 HTTP method/path 组合翻译为上述固定命令。
- [x] 命令映射限定为：`GET /api/session` → `opencode.session.list`；`GET /api/session/{sessionId}` → `opencode.session.get`；`POST /api/session` → `opencode.session.create`；`POST /api/session/{sessionId}/prompt` → `opencode.prompt.submit`；`GET /api/session/{sessionId}/message` → `opencode.message.list`；`GET /api/session/{sessionId}/question` → `opencode.question.list`；`POST /api/session/{sessionId}/question/{requestId}/reply` → `opencode.question.reply`。
- [x] 命令映射按 method 和完整路径模板匹配；拒绝额外 query、路径段穿越、未知方法和未知端点。`sessionId`/`requestId` 只进入 typed `params`。
- [x] 保持 `chat`、`chatStreaming`、模型选择、交互问题回复和回复截断的现有高层接口。新节点产生的 progress 用现有 `StreamingCallback` 转发，结果仍由原 QQ/企微/微信通道回发。
- [x] 对新节点 session Redis key 加 node ID 作用域：`opencode:session:node:<nodeId>:<conversationId>`。旧 key 仅供 `nodeId=null` 旧 transport 使用；首次绑定新节点时创建新会话，不把旧节点 session ID 发送给新节点。
- [x] `NewCommand`、`ClearCommand`、`StatusCommand` 不再直接访问旧无节点前缀的 Redis key；通过 `OpencodeService` 的 reset/status 方法解析当前会话执行端。
- [x] 节点离线、撤销、能力未批准、timeout 或 disconnect 都返回不排队、不重放的可读错误；新路径不得 fallback 到 direct 或 V1 bridge。

Java 路由分支应保持这一规则：

```java
Long nodeId = imConversationService.getOpencodeNodeId(conversationId);
if (nodeId == null) {
    return callLegacyOpencode(method, path, body);
}
return nodeRpcClient.invoke(nodeId, commandMapper.map(method, path, body));
```

## 任务 6：私聊页节点管理和绑定 UI

**Files:** Create `web/src/types/opencodeNode.ts`、`web/src/api/opencodeNodes.ts`；modify `web/src/types/console.ts`、`web/src/pages/ImSingleChatPage.vue`。

- [x] 新类型 `OpencodeNode` 包含 `id/name/platform/agentVersion/status/lastSeenAt/capabilities/approvedCapabilities`；`PairingCode` 只包含明文配对码和过期时间，仅用于本次创建响应。
- [x] API 函数覆盖 `createPairingCode/list/approve/reject/rename/revoke/bindConversation/unbindConversation`，统一使用现有 Axios `api` client。
- [x] 页面顶部新增节点摘要：在线/总数、生成配对码、待审批节点。节点卡显示名称、平台、在线状态、最近心跳与能力，提供批准、拒绝、重命名、撤销操作。
- [x] 选中私聊时展示执行端下拉框，内部值区分 `app:<id>`、`legacy-opencode`、`node:<id>` 和 `none`。普通 Dify app 保留现有选项；`legacy-opencode` 调用现有 app 绑定接口设置 `appId=-1` 并清空 node ID；`node:<id>` 调用新 node binding endpoint；`none` 沿用现有 unbind API。
- [x] 对离线或撤销节点显示明显状态和不执行提示；只允许管理员看到和使用配对/审批/绑定控件。服务端权限仍是最终防线，前端 localStorage 仅用于显示控制。
- [x] 加载、空态、API 错误、配对码复制和保存中状态都使用当前页面既有 `anime-*` 样式变量，移动窄屏下区域可换行，不改变其他页面全局视觉。

## 任务 7：配置与部署文档

**Files:** `OpencodeNodeProperties.java`、`application.yaml`、`application-prod.yaml`、`web/nginx.conf`、`DEPLOY.md`、`bridge/README.md`。

- [x] 添加配置 `opencode.node.enabled`、`opencode.node.request-timeout-seconds`、`opencode.node.heartbeat-seconds`、`opencode.node.offline-after-seconds` 和 `opencode.node.pairing-ttl-seconds`，默认值分别为 `true`、320、30、90、600；生产环境通过 `OPENCODE_NODE_*` 覆盖。
- [x] `web/nginx.conf` 为 `/opencode/node` 配置独立 WebSocket Upgrade 转发，后端地址、超时和 buffer 行为参考当前 `/opencode/bridge` 路由；不删除旧路由。
- [x] `DEPLOY.md` 写明管理员配对、电脑安装 Node.js 22、启动新 Agent、管理员批准、私聊绑定、WSS/出站网络要求和断线显示方式。
- [x] `.env.example` 已有用户修改，先阅读当前工作树版本，只追加缺少的 `OPENCODE_NODE_*` 示例变量，不覆盖现有内容。

## 任务 8：完成前人工检查清单

- [x] 逐项对照设计文档的七条验收场景，检查新旧路由分支、权限、节点生命周期、跨节点 Redis key 和离线错误是否在实现中都有对应代码。
- [x] 检查 `git diff --` 仅包含本计划列出的文件，并确认用户原有暂存/未提交内容未被覆盖或一并提交。
- [x] 不运行测试、构建或其他实现验证命令；用户之后明确要求验证时再执行。
