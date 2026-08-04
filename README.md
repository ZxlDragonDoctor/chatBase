# 🤖 ChatBase · 智能对话客服系统 / Multi-channel AI Customer-Service System

> 把 QQ / 企业微信 / 微信个人号的消息统一接入 Dify 大模型 + 知识库，自动智能回复；
> 还能将一段私聊会话绑定到**自己电脑上的 opencode**，实现"人在服务器、AI 在本机"的远程编码代理。
>
> **Turn QQ / WeCom / personal WeChat into an AI assistant** backed by Dify LLM + RAG knowledge — and remote-drive the opencode agent running on your own machine from a private chat, over an frp tunnel.

<div align="center">

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.6-brightgreen)
![Vue](https://img.shields.io/badge/Vue-3.5-green)
![TypeScript](https://img.shields.io/badge/TypeScript-5.7-blue)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![Redis](https://img.shields.io/badge/Redis-7-red)
![Dify](https://img.shields.io/badge/Dify-LLM-orange)
![MIT](https://img.shields.io/badge/License-MIT-blue)

**🚀 一键 Docker 部署 / One-command deploy**  ·
**📊 Token·费用看板 / Analytics dashboard**  ·
**🧠 私聊遥控本机 AI / Remote local-agent control**

</div>

---

## 📖 目录 / Table of Contents

- [项目简介 · What is it](#-项目简介--what-is-it)
- [核心特性 · Features](#-核心特性--features)
- [技术栈 · Tech Stack](#-技术栈--tech-stack)
- [系统架构 · Architecture](#-系统架构--architecture)
- [快速开始 · Quick Start](#-快速开始--quick-start)
- [配置说明 · Configuration](#-配置说明--configuration)
- [功能模块 · Modules](#-功能模块--modules)
- [数据隔离与权限 · Security & Isolation](#-数据隔离与权限--security--isolation)
- [多渠道 IM 接入 · IM Integration](#-多渠道-im-接入--im-integration)
- [私聊遥控本机 opencode · Remote opencode](#-私聊遥控本机-opencode--remote-opencode)
- [定时任务 · Scheduled Tasks](#-定时任务--scheduled-tasks)
- [数据库表 · Database](#-数据库表--database)
- [API 接口 · API](#-api-接口--api)
- [文档 & 常见问题 · Docs & FAQ](#-文档--常见问题--docs--faq)
- [项目结构 · Project Structure](#-项目结构--project-structure)
- [许可证 · License](#-许可证--license)
- [重要注意事项 · Notes](#-重要注意事项--notes)

---

## 📌 项目简介 / What is it

ChatBase 是一套**开箱即用的多渠道智能客服 + AI 知识库**解决方案。后端对接 Dify 大模型，前端提供管理看板，把分散在 QQ、企业微信、微信个人号里的用户消息统一汇聚、自动回复、沉淀为知识库，并支持数据统计与反馈闭环。

- 难点在于「多渠道」：QQ 走 NapCat（WebUI 扫码登录）、企业微信走回调模式、微信个人号走 iLink 协议，各通道差异巨大，本项目统一抽象为一套 IM 消息模型。
- 亮点在于「远程遥控本机 AI」：私聊会话可绑定到开发者自己电脑上运行的 **opencode**，通过 frp 反向隧道，实现服务器端收到聊天 → 本机 AI 编程代理 → 回发的完整闭环。

**Keywords:** AI chatbot · customer service · RAG knowledge base · Dify · QQ Bot (NapCat) · WeCom · WeChat personal · remote coding agent · opencode · Vue 3 · Spring Boot

---

## ✨ 核心特性 / Features

| 模块 Tier | 能力 What you get | 说明 |
|-----------|------------------|------|
| 🧠 **AI 对话 / Dialogue** | Dify 多轮对话、FAQ 优先命中、引用溯源 | 基于会话上下文，命中 FAQ 优先返回，未命中走大模型；带 Retriever 引用来源 |
| 📚 **知识库 / Knowledge** | 批量传文档、自动同步 Dify、分类、搜索 | 支持 TXT/PDF/DOCX/MD，Dify Dataset 同步，树形分类，进度条（SSE） |
| 💬 **多渠道 IM / Channels** | QQ 群/私聊、企微回调、微信 iLink | 统一消息抽象，扫码/回调接入，群与私聊全覆盖 |
| 🖥️ **远程 opencode / Remote** | 私聊绑定本机 opencode，frp 隧道遥控 | 会话级绑定特殊应用（appId=-1），仅 admin 可用，全程审计落库 |
| 📊 **数据洞察 / Analytics** | Token/费用趋势、关键词云、群活跃、命中率 | 按日/月统计，支持 admin 切换到全部/个人维度 |
| 🧾 **FAQ & 反馈 / Feedback** | 高频问答自动抽取、评分、后台处理 | 星级+类型+描述反馈，管理员回复，满意度分析 |
| 🛡️ **权限隔离 / Security** | admin/user 角色 + `created_by` 数据隔离 | 拦截器鉴权 + 查询级数据过滤，多租户友好 |

---

## 🛠 技术栈 / Tech Stack

### 后端 / Backend

| 技术 Technology | 版本 | 用途 Purpose |
|-----------------|------|--------------|
| Java | 17 | 开发语言 |
| Spring Boot | 2.7.6 | 应用框架 |
| MyBatis-Plus | 3.5.15 | ORM |
| WebSocket | - | QQ 机器人通信 |
| Redis | 7 | 缓存 / 会话 / 消息队列(Stream) |
| MySQL | 8.0 | 关系数据库 |
| Apache HttpClient | 4.5.14 | HTTP 调用 Dify 等 |
| Fastjson | 2.0.40 | JSON 处理 |
| Spring Security Crypto | 5.7 | BCrypt 密码加密 / AES |

### 前端 / Frontend

| 技术 Technology | 版本 | 用途 Purpose |
|-----------------|------|--------------|
| Vue | 3.5 | UI 框架 (Composition API) |
| TypeScript | 5.7 | 类型安全 |
| Vite | 6.1 | 构建工具 |
| ECharts | 6.0 | 数据可视化（趋势图 / 词云） |
| Lucide Icons | - | 图标库 |
| CropperJS | 1.6.2 | 头像裁切 |

---

## 🏗 系统架构 / Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                          前端层 Frontend (Vue 3)                  │
│  登录注册 │ AI问答 │ 知识库 │ 统计看板 │ 控制台 │ 机器人管理        │
└────────────────────────────────┬─────────────────────────────────┘
                                 │ HTTP / WebSocket
┌────────────────────────────────▼─────────────────────────────────┐
│                       后端层 Backend (Spring Boot)                 │
│   Controller │ Service │ Mapper │ Config │ Interceptor            │
│  ┌───────────────────────────────┐                                │
│  │ Chat│Dify│KB│IM│QQ│WeCom│Wx│   │ Stats│Opencode│Feedback│User  │
│  └───────────────────────────────┘                                │
└───────────────┬──────────────────────────┬────────────────────────┘
                │                          │
┌───────────────▼──────────┐   ┌───────────▼────────────────────────┐
│    数据层 Data            │   │    外部服务 External               │
│  MySQL 8 · Redis 7        │   │  Dify API · NapCat(QQ)            │
└──────────────────────────┘   │  WeCom 回调 · 本机 opencode(frp)   │
                               └────────────────────────────────────┘
```

### 私聊遥控本机 opencode（特色数据流）

```
管理员私聊消息 → IM通道(QQ WebSocket / 企微回调 / 微信iLink)
  → 会话绑定判定 isOpencodeBound()（appId = -1）
  → OpencodeService.chat()
  → frp 反向隧道 → 本机 opencode serve (127.0.0.1:4096)
  → 创建会话 → 发送消息 → 轮询回复 → 回复落 kb_conversation 审计
  → 回发私聊
```

> ⚠️ 后端运行在容器内，访问宿主机 frps 必须用网桥网关 `http://172.17.0.1:14096`，**不能用 `127.0.0.1`**（容器内回环是容器自身）。详见 [DEPLOY.md](./DEPLOY.md)。

---

## 🚀 快速开始 / Quick Start

### Docker 部署（推荐）/ Docker Deploy (recommended)

```bash
# 1. 克隆项目
git clone <repository-url>
cd chatBase

# 2. 配置环境变量
cp .env.example .env
vim .env    # 填写必填配置

# 3. 构建并启动
docker compose up --build -d

# （可选）启动含 QQ Bot 的服务
docker compose --profile qq up --build -d

# 4. 查看日志
docker compose logs -f chatbase-backend
```

访问 **http://localhost** 打开前端页面。

### 本地开发 / Local Dev

```bash
# 1. 启动 MySQL 和 Redis
docker run -d --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=zxl123 -e MYSQL_DATABASE=chat_base mysql:8.0
docker run -d --name redis -p 6379:6379 redis:7

# 2. 初始化数据库
mysql -u root -pzxl123 chat_base < sql/init-schema.sql

# 3. 启动后端（local profile，参考 application-local.yaml，该文件被 git 忽略需自建）
mvn spring-boot:run -Dspring-boot.run.profiles=local

# 4. 启动前端
cd web && npm install && npm run dev
```

访问 **http://localhost:5173** 打开前端页面。

### 本机对接 opencode（可选）

```bash
# 本机启动 opencode serve（带密码鉴权）
$env:OPENCODE_SERVER_PASSWORD="your-password"
opencode serve --port 4096

# 配置 application-local.yaml 启用并指向本机
# opencode.enabled=true / base-url=http://127.0.0.1:4096 / password=<同上> / username=opencode

# 生产环境：用 frp 将本机 4096 隧道映射到服务器，再配置：
# OPENCODE_ENABLED=true / OPENCODE_BASE_URL=http://172.17.0.1:14096 / OPENCODE_PASSWORD=<同密码>
```

---

## ⚙️ 配置说明 / Configuration

### 必填配置 / Required

| 环境变量 Env | 说明 | 示例 |
|--------------|------|------|
| `MYSQL_ROOT_PASSWORD` | MySQL root 密码 | `root123` |
| `MYSQL_USER` | 数据库用户名 | `chatbase` |
| `MYSQL_PASSWORD` | 数据库密码 | `chatbase123` |
| `DIFYAPP_API_KEY` | Dify Chat API Key | `app-xxxxxxxx` |
| `DIFYAPP_DATASET_API_KEY` | Dify Dataset API Key | `dataset-xxxxxxxx` |

### 可选配置 / Optional

| 环境变量 Env | 说明 | 默认值 Default |
|--------------|------|----------------|
| `REDIS_PASSWORD` | Redis 密码 | 无 |
| `QQ_BOT_ENABLE` | 启用 QQ 机器人 | `false` |
| `QQ_BOT_ACCESS_TOKEN` | NapCat Token | - |
| `QQ_BOT_SELF_ID` | 机器人 QQ 号 | - |
| `QQ_BOT_HTTP_BASE_URL` | NapCat HTTP 地址 | `http://chatbase-napcat:3000` |
| `QQ_BOT_WEBUI_BASE_URL` | NapCat WebUI 地址（扫码登录代理） | `http://chatbase-napcat:6099` |
| `QQ_BOT_WEBUI_TOKEN` | NapCat WebUI 鉴权 token | - |
| `WECHAT_CORP_STOKEN` | 企业微信 Token | - |
| `WECHAT_CORP_S_ENCODING_AES_KEY` | 企业微信 EncodingAESKey | - |
| `WECHAT_CORP_BOT_ID` | 企微机器人 ID | - |
| `WECHAT_CORP_SECRET` | 企微机器人 Secret | - |
| `WX_BOT_ENABLE` | 启用微信个人号 iLink | `false` |
| `WX_BOT_TOKEN` | 微信 iLink token | - |
| `WX_BOT_BASE_URL` | 微信 iLink 服务地址 | - |
| `WX_BOT_BOT_ID` | 微信机器人 ID | - |
| `WX_BOT_NICKNAME` | 微信机器人昵称 | `微信机器人` |
| `OPENCODE_ENABLED` | 启用本地 opencode 集成 | `false` |
| `OPENCODE_BASE_URL` | opencode serve 地址（容器内访问宿主机 frps） | `http://172.17.0.1:14096` |
| `OPENCODE_PASSWORD` | opencode serve 密码（对应 `OPENCODE_SERVER_PASSWORD`） | - |
| `OPENCODE_USERNAME` | opencode Basic Auth 用户名 | `opencode` |
| `OPENCODE_DEFAULT_DIRECTORY` | 本机项目根目录 | - |
| `OPENCODE_DEFAULT_AGENT` | opencode agent | `build` |
| `OPENCODE_TIMEOUT_SECONDS` | 等待回复超时（秒） | `300` |
| `JAVA_OPTS` | JVM 参数 | `-Xms512m -Xmx2048m` |
| `NAPCAT_IMAGE` | NapCat 镜像名称 | `mlikiowa/napcat-docker:v4.17.46` |

> 完整配置说明请参考 [USER_GUIDE.md](./USER_GUIDE.md#4-配置说明)。

---

## 🧩 功能模块 / Modules

| 模块 Module | 包路径 Package | 职责 Responsibility |
|-------------|----------------|---------------------|
| **chat** | `com.zxl.chatbase.chat` | 聊天会话、消息处理、数据清理 |
| **dify** | `com.zxl.chatbase.dify` | Dify API 集成、对话、文件上传 |
| **kb** | `com.zxl.chatbase.kb` | 知识库、分类、文档、FAQ、应用、关键词 |
| **im** | `com.zxl.chatbase.im` | IM 消息采集、会话绑定、机器人管理 |
| **opencode** | `com.zxl.chatbase.opencode` | 本地 opencode serve 集成（远程遥控本机 AI） |
| **qq** | `com.zxl.chatbase.qq` | QQ 机器人 WebSocket + WebUI 扫码登录代理 |
| **wxroboot** | `com.zxl.chatbase.wxroboot` | 企业微信回调处理、消息加解密 |
| **statistics** | `com.zxl.chatbase.statistics` | 统计分析、Token、费用、关键词聚合 |
| **feedback** | `com.zxl.chatbase.feedback` | 用户反馈收集与统计 |
| **user** | `com.zxl.chatbase.user` | 用户注册、登录、信息管理 |
| **upload** | `com.zxl.chatbase.upload` | 文件上传进度（SSE） |
| **config** | `com.zxl.chatbase.config` | 配置类、拦截器、跨域、限流 |

### 页面功能 / Pages

| 页面 Page | 路径 Route | 权限 | 功能 |
|-----------|-----------|------|------|
| 登录注册 | `/login` | 公开 | 用户认证 |
| 系统概览 | `/console/dashboard` | 登录 | 统计卡片、快捷导航 |
| 数据统计 | `/console/statistics` | 登录 | Token/费用趋势、词云、活跃度（admin 可切换全部/个人） |
| 群聊采集 | `/console/im` | 登录 | 群列表、消息查询、应用绑定 |
| 私聊采集 | `/console/im/single` | 登录 | 私聊会话列表、Dify/opencode 应用绑定（opencode 仅 admin） |
| 知识库管理 | `/console/knowledge` | 登录 | 分类、知识库、文档、FAQ |
| 应用管理 | `/console/app` | 登录 | Dify 应用配置、API Key 验证 |
| 机器人管理 | `/console/bots` | 登录 | 机器人状态、消息统计、扫码登录 |
| FAQ 管理 | `/console/faq` | 登录 | 手动维护、自动提取 |
| AI 问答 | `/chat` | 公开 | 多会话对话、文件附件、引用来源 |
| 用户反馈 | `/feedback` | 公开 | 提交反馈、查看历史 |
| 反馈管理 | `/console/feedback-manage` | admin | 反馈处理、回复 |
| 应用管理(全部) | `/console/admin/apps` | admin | 所有应用管理 |
| 知识库管理(全部) | `/console/admin/kbs` | admin | 所有知识库管理 |
| 用户管理 | `/console/admin/users` | admin | 用户增删改查 |

---

## 🔐 数据隔离与权限 / Security & Isolation

- **角色模型**：`admin` / `user`，`AuthInterceptor` + `AdminInterceptor` 双重校验
- **数据隔离规则**：`created_by = 当前用户 OR created_by IS NULL`（系统级记录所有人可见）
- **统计页**：admin 默认看全部（`scope=all`），可切 `scope=mine`；普通用户始终只看自己
- **群聊可见性**：`created_by IS NULL`（公共/未认领）或 `created_by = 当前用户`（已认领）
- **分类可见性**：`create_by = 当前用户` 或 `create_by IS NULL`（系统默认分类如"群聊消息"）
- **应用/知识库**：按 `created_by = 当前用户` 过滤；admin 管理端 `/api/kb/app/admin/**`、`/api/kb/admin/**` 看全部
- **认证排除**：登录、注册、Web 聊天、反馈提交、QQ WebSocket、企微回调等为公开路径

> ⚠️ `pom.xml` 的 `maven-compiler-plugin` 必须配置 `<parameters>true</parameters>`，否则 `@RequestParam` / `@RequestAttribute` 运行时报 `Name not specified` 异常。

---

## 💬 多渠道 IM 接入 / IM Integration

| 平台 Platform | 接入方式 | 功能 |
|---------------|----------|------|
| **QQ 群聊** | NapCat 反向 WebSocket | 扫码登录、消息收集、智能回复、在线监控 |
| **QQ 私聊** | NapCat 反向 WebSocket | 会话级应用绑定、opencode 远程控制 |
| **企业微信** | 回调模式 | 消息收集、智能回复、AES 加解密 |
| **微信个人号** | iLink 协议 | 扫码登录、消息收集、智能回复 |

### QQ 机器人配置（NapCat）

```bash
docker compose --profile qq up -d
```

**WebUI 扫码登录**：在「机器人管理」页点击 QQ 卡片「扫码登录」，后端代理 NapCat WebUI API 一键扫码；也可直接访问 `http://<server>:6099`。

**反向 WebSocket**：`ws://chatbase-backend:8080/qq/ws`
**HTTP 服务器**：端口 `3000`
**ChatBase 配置**：`QQ_BOT_ENABLE` / `QQ_BOT_SELF_ID` / `QQ_BOT_ACCESS_TOKEN` / `QQ_BOT_HTTP_BASE_URL` / `QQ_BOT_WEBUI_BASE_URL` / `QQ_BOT_WEBUI_TOKEN`

> ⚠️ 务必使用 QQ **小号**，防止封号。回复仅在消息 **@机器人** 时触发。

### 企业微信配置

- 回调 URL：`http://<server>/intellrobot/callback/handle`
- 配置 `WECHAT_CORP_STOKEN` / `WECHAT_CORP_S_ENCODING_AES_KEY` / `WECHAT_CORP_BOT_ID` / `WECHAT_CORP_SECRET`
- 企微要求 5 秒内响应，系统采用异步处理 + Redis 分布式锁防重复

---

## 🖥️ 私聊遥控本机 opencode / Remote opencode

> 通过私聊会话远程驱动开发者**本机**的 opencode，实现"人在服务器、代理在本机"的远程编码代理。

**前提条件**：
1. 本机 `opencode serve --port 4096` 并设置 `OPENCODE_SERVER_PASSWORD`
2. 服务器经 frp 隧道可达本机，配置 `OPENCODE_ENABLED=true` / `OPENCODE_BASE_URL` / `OPENCODE_PASSWORD`
3. 以 **admin** 在「私聊采集」会话详情选择 **🖥️ 本地opencode** 绑定（仅 admin 可见该选项）

**数据流**：见上文架构图。会话映射存 Redis（`opencode:session:<conversationId>`，TTL 7 天），回复写入 `kb_conversation` 审计后回发私聊。

> 部署细节、frp 配置（frpc.toml）、隧道排错见 [DEPLOY.md](./DEPLOY.md)。

---

## ⏰ 定时任务 / Scheduled Tasks

| 任务 Task | 频率 | 功能 | 状态 |
|-----------|------|------|:----:|
| Redis Stream 消费 | 每 5 秒 | 实时处理 IM 消息 | ✅ 推荐 |
| 定时同步（废弃） | 每 60 秒 | 批量同步群消息到 Dify | ⚠️ 已过时 |
| 统计聚合 | 每天 00:05 | 聚合昨日统计数据 | ✅ |
| 关键词提取 | 每天 05:00 | 从对话中提取关键词 | ✅ |
| 关键词清理 | 每天 06:00 | 清理 90 天前关键词 | ✅ |
| 会话清理 | 每天 03:00 | 清理过期会话 | ✅ |
| 消息清理 | 每天 04:30 | 清理 90 天前消息 | ✅ |

---

## 🗃 数据库表 / Database

### 核心表 / Core Tables

| 表名 Table | 说明 |
|-----------|------|
| `sys_user` | 系统用户 |
| `kb_category` | 知识库分类（树形结构） |
| `kb_knowledge_base` | 知识库管理 |
| `kb_document` | 文档管理 |
| `kb_conversation` | 会话记录（含 Token/费用） |
| `kb_faq` | 常见问答 |
| `kb_feedback` | 用户反馈 |
| `kb_statistics` | 每日统计 |
| `kb_keyword` | 关键词统计 |
| `kb_app` | 应用配置 |
| `im_conversation` | IM 单聊会话（opencode 绑定） |
| `group_message` | IM 群/私聊消息采集 |
| `im_group` | 群组信息 |
| `im_user` | 用户信息 |
| `chat_session` | 聊天会话 |
| `sys_config` | 系统配置 |

> 完整表结构请参考 [DESIGN.md](./DESIGN.md#4-数据库设计)。
> 已有部署升级：执行 `sql/upgrade-existing-db.sql`（幂等，可重复）。

---

## 🔌 API 接口 / API

### 主要分组 / Groups

| 分类 | 路径前缀 | 说明 |
|------|----------|------|
| 用户 | `/api/user` | 注册、登录、信息管理 |
| 聊天 | `/api/chat` | 对话、文件上传 |
| 会话 | `/api/chat/session` | 会话 CRUD |
| 知识库 | `/api/kb` | 知识库、分类、文档 |
| 应用 | `/api/kb/app` | 应用管理、API Key 验证 |
| FAQ | `/api/kb/conversation/faq` | FAQ CRUD、提取 |
| 反馈 | `/api/feedback` | 提交、管理、统计 |
| 统计 | `/api/statistics` | Token、费用、关键词、聚合 |
| 控制台 | `/api/console` | 群聊采集、私聊会话、消息 |
| 机器人 | `/api/bot` | 机器人列表 |
| 上传进度 | `/api/upload/progress` | SSE 实时推送 |
| QQ Bot | `/api/qq-bot` | QQ 扫码登录代理 |

> 完整 API 列表请参考 [DESIGN.md](./DESIGN.md#13-api-接口汇总)。

---

## 📚 文档 & 常见问题 / Docs & FAQ

| 文档 | 说明 |
|------|------|
| [USER_GUIDE.md](./USER_GUIDE.md) | 使用文档：部署、配置、功能、排查、FAQ |
| [DEPLOY.md](./DEPLOY.md) | 部署指南：Docker、环境变量、服务管理、opencode & frp |
| [DESIGN.md](./DESIGN.md) | 详细设计：架构、模块、数据库、数据流、API |

| 常见问题 Issue | 解决 Fix |
|----------------|----------|
| QQ 消息收到但不回复 | 必须 @机器人，检查 NapCat 连接 |
| 私聊绑定 opencode 回复「未启用」 | 服务器 `OPENCODE_ENABLED=true` 且本机 opencode 经 frp 可达 |
| opencode 回复「未返回结果」 | 检查本机 serve、`OPENCODE_BASE_URL` 隧道地址、`OPENCODE_PASSWORD` 匹配 |
| 统计数据为空 | 调用 `/api/statistics/aggregate` 聚合 |
| Docker 启动后无法访问 | `docker compose logs -f chatbase-backend` 排查 |

---

## 📁 项目结构 / Project Structure

```
chatBase/
├── src/main/java/com/zxl/chatbase/
│   ├── chat/           # 聊天服务
│   ├── dify/           # Dify API 集成
│   ├── kb/             # 知识库管理
│   ├── im/             # IM 消息采集
│   ├── opencode/       # 本地 opencode serve 集成
│   ├── qq/             # QQ Bot（WebSocket + WebUI 扫码登录代理）
│   ├── wxroboot/       # 企业微信机器人
│   ├── statistics/     # 统计分析
│   ├── feedback/       # 用户反馈
│   ├── user/           # 用户管理
│   ├── config/         # 配置类（含 OpencodeProperties）
│   ├── common/         # 通用工具、限流、异常
│   └── controller/     # API 控制器
│
├── web/                # Vue 3 前端
│   ├── src/pages/      # 页面组件
│   ├── src/api/        # API 接口
│   ├── src/components/ # 公共组件
│   ├── nginx.conf      # Nginx 配置
│   └── Dockerfile      # 前端镜像
│
├── sql/                # 数据库脚本
│   ├── init-schema.sql          # 全新初始化（首次挂载 volume 时执行）
│   └── upgrade-existing-db.sql  # 已有库升级（幂等）
│
├── Dockerfile          # 后端镜像
├── docker-compose.yml  # 部署编排
├── .env.example        # 环境变量示例
├── DESIGN.md           # 详细设计文档
├── USER_GUIDE.md       # 使用文档
└── DEPLOY.md           # 部署指南
```

---

## 📄 许可证 / License

MIT License

---

## ⚠️ 重要注意事项 / Notes

- **pom.xml 编译参数**：`maven-compiler-plugin` 需配置 `<parameters>true</parameters>`，否则 `@RequestParam` / `@RequestAttribute` 报 `Name not specified`。
- **数据隔离**：所有业务数据通过 `created_by` 按用户维度过滤；普通用户仅看自己，admin 可在统计页切 `scope=all/mine`。
- **前端路由**：admin 菜单基于 `localStorage.getItem('chatbase_role')` 动态显示。
- **容器网络**：`chatbase-napcat` 等跨容器访问用服务名；访问宿主机 frps 用网桥网关 `172.17.0.1`。

---

*最后更新 / Last updated：2026-08-04 · 中文为主 / Chinese-primary, English-mirror*