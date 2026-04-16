# ChatBase 智能聊天与知识库系统

基于 Spring Boot + Vue 3 的群聊数据采集 + Dify 知识库 + NapCat + 智能问答系统。

## QqBot、WeChatBot智能机器人展示
- QqBot
<img width="1792" height="925" alt="image" src="https://github.com/user-attachments/assets/1474aa11-a4f7-4522-97d8-f203357ee032" />

- WeChatBot
<img width="1745" height="861" alt="image" src="https://github.com/user-attachments/assets/8602ec5c-803d-49bc-bf53-7cceb460ea60" />


## 功能特性

- **多平台接入**：支持 QQ群、企业微信群消息采集
- **智能问答**：@机器人 自动调用 Dify 大模型回答
- **知识库同步**：群消息自动同步到 Dify 知识库
- **文件管理**：支持文件上传与知识库关联
- **Web聊天**：提供 Web 端聊天接口
- **限流保护**：内置限流防刷机制
- **控制台**：群消息统计与查询

## 技术栈

- 后端：Java 17、Spring Boot 2.7.6、MyBatis-Plus、WebSocket、Redis、MySQL
- 前端：Vue 3、TypeScript、Vite
- 外部服务：Dify API、NapCat

## 快速开始

### 1. 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 7+

### 2. 初始化数据库

```sql
CREATE DATABASE chat_base;
-- 执行 sql/init-schema.sql
```

### 3. 配置 Dify

在 `application-local.yaml` 中配置：

```yaml
difyApp:
  url: "https://api.dify.ai/v1"
  apiKey: "你的sk-xxx"
  datasetId: "你的dataset-xxx"
  timeOut: 90
```
需要先在https://cloud.dify.ai/ 工作室中和知识库中创建属于自己的Chatflow和知识库并获取API密钥
<img width="2548" height="1328" alt="image" src="https://github.com/user-attachments/assets/3c732686-6969-4e0b-b8a5-502f6a9feaae" />
<img width="2542" height="1351" alt="image" src="https://github.com/user-attachments/assets/c4d2b412-1478-4bc5-b212-6daf2c9aef91" />

### 4. 配置 QQ 机器人

```yaml
qq:
  bot:
    self-id: 你的机器人QQ号
    http-base-url: "http://127.0.0.1:3000"
```
想要使用QQ机器人，需要使用NapCat开源项目作为中间件监听QQ消息  
项目地址：https://github.com/NapNeko/NapCatQQ  
下载完成并启动后进入 http://127.0.0.1:6099/webui/ 控制台登录QQ账号并授权后，在网络配置中进行以下配置
**务必使用小号登录NapCat，防止被封禁账号**  

NapCat 配置反向 WebSocket：`ws://localhost:8080/qq/ws`  
NapCat 配置 HTTP服务器 API：`http://localhost:8080/qq/api`
<img width="2520" height="1318" alt="image" src="https://github.com/user-attachments/assets/ae79d8ad-6241-4baf-8618-da10a48302ce" />
<img width="2538" height="1350" alt="image" src="https://github.com/user-attachments/assets/aa438de9-0baa-46ed-94b8-29ee106e12f7" />


### 5.配置企业微信智能机器人

````yaml
wechat:
  corp:
    # 基于webhook的连接配置
    # 企业微信回调Token
    stoken: 你的Token
    # 企业微信加解密AESKey
    sEncodingAESKey: 你的AesKey
````
配置信息在企业微信工作台智能机器人中创建，选择手动创建，选择API模式使用URL创建
<img width="415" height="421" alt="image" src="https://github.com/user-attachments/assets/ef2b6763-856d-4090-a79e-8bb012c16357" />  

URL需要于src/main/java/com/zxl/chatbase/controller/IntelligentRobotController.java中的verifyUrl接口一致
**注：由于基于回调的方式企微不支持本地地址ip，所以需要自己配置自己的服务器IP或者域名才能正常使用企微智能机器人功能或者自己扩展基于websocket长连接的接口创建机器人在本地使用测试**
````java
@RestController
@RequestMapping("intellrobot")
@Slf4j
public class IntelligentRobotController {
    @Resource
    private IntelligentRobotService intelligentRobotService;

    @GetMapping("callback/handle")
    public String verifyUrl(@RequestParam("msg_signature") String msgSignature,
                            @RequestParam("timestamp") String timestamp,
                            @RequestParam("nonce") String nonce,
                            @RequestParam("echostr") String echoStr
    ) {
        return intelligentRobotService.verifyUrl(msgSignature, timestamp, nonce, echoStr);
    }

//Urldecode
    @PostMapping("/callback/handle") // 路径与get检验URL相同
    public Map<String, String> handleMessage(@RequestParam("msg_signature") String msgSignature,
                                @RequestParam("timestamp") String timestamp,
                                @RequestParam("nonce") String nonce,
                                @RequestBody String postData
    ) {
        return Map.of("encrypt", intelligentRobotService.handleMessage(msgSignature, timestamp, nonce, postData));
    }
}
````



### 5. 启动

```bash
mvn spring-boot:run
```

## 核心接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/chat/ask` | GET | 简单问答 |
| `/api/chat/web` | POST | Web端聊天 |
| `/api/chat/im` | POST | IM机器人聊天 |
| `/api/chat/v1/files/upload` | POST | 文件上传 |
| `/api/console/overview` | GET | 采集概览 |
| `/api/console/messages` | GET | 消息分页 |

## 数据库表

### IM消息模块
| 表名 | 说明 |
|------|------|
| `group_message` | 群聊消息采集 |
| `im_group` | 群组信息 |
| `im_user` | 用户信息 |
| `group_kb_mapping` | 群组知识库映射 |

### 知识库模块
| 表名 | 说明 |
|------|------|
| `kb_category` | 分类管理 |
| `kb_knowledge_base` | 知识库 |
| `kb_document` | 文档管理 |
| `kb_conversation` | 会话记录 |
| `kb_file` | 文件管理 |
| `kb_faq` | 常见问答 |
| `kb_feedback` | 用户反馈 |
| `kb_statistics` | 每日统计 |

### 系统模块
| 表名 | 说明 |
|------|------|
| `sys_user` | 系统用户 |
| `sys_config` | 系统配置 |

## 数据流

```
QQ群消息 → NapCat → WebSocket(/qq/ws) → 入库 + 同步im_group/im_user
企业微信 → 回调 → /intellrobot/callback → 入库 + 同步im_group/im_user

群消息 → 定时任务 → Dify知识库(kb_document)
     ↓
 @机器人 → ChatService → Dify → 返回回答 → 发送回群里
```

## 配置说明

主要配置在 `application-local.yaml`(本地创建配置文件)：

```yaml
# Dify配置
difyApp:
  url: "https://api.dify.ai/v1"
  apiKey: ""
  datasetId: ""

# QQ配置
qq:
  bot:
    self-id: 123456789
    http-base-url: "http://127.0.0.1:3000"

# 限流配置
chat:
  rate-limit:
    window-seconds: 60
    max-requests: 30

# 清理配置
chat:
  cleanup:
    conversation-days: 30
    message-days: 90
```

## Docker 部署
1. 复制.env.example 为 .env 并修改配置,主要配置dify,qq_bot,wechat,mysql,redis
2. 执行docker compose命令启动：
```bash
docker compose up -d --build
```

服务端口：MySQL(3306)、Redis(6379)、Backend(8080)、NapCat(3000,3001)、Web(80)

## 常见问题

1. **QQ消息收到但不回答**：确认 @机器人 而不是只发消息
2. **发送失败(1404)**：确认 NapCat HTTP 端口配置正确
3. **超时问题**：增大 `difyApp.timeOut` 配置值
4. **企业微信回调**：需在企业微信后台配置服务器URL
