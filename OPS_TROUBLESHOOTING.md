# ChatBase 运维排障手册

> 记录生产环境常见故障的现象、根因与解决方案，供后续排障直接参考。
> 环境：阿里云轻量 1 核 1.6GB，Ubuntu + Docker Compose 部署；服务器 `47.93.233.131`（SSH 22 → 2234）。

---

## 目录

1. [opencode 隧道断链（Connection refused）](#1-opencode-隧道断链connection-refused)
2. [服务器 OOM 假死（反复重启 / 服务无响应）](#2-服务器-oom-假死反复重启--服务无响应)
3. [MySQL 连接池耗尽（HikariPool time out）](#3-mysql-连接池耗尽hikaripool-time-out)
4. [排障速查命令](#4-排障速查命令)

---

## 1. opencode 隧道断链（Connection refused）

### 现象

- 微信/QQ 私聊绑定「本地opencode」后不回复
- 后端日志反复刷：
  ```
  [business-thread-1] WARN c.z.c.opencode.OpencodeService - opencode 查询信息异常: sessionId=ses_xxx
  org.springframework.web.client.ResourceAccessException: I/O error on GET request for
  "http://172.17.0.1:14096/api/session/ses_xxx/message": Connection refused
  ```

### 根因

- 架构：服务器后端容器 → frp 反向隧道（`172.17.0.1:14096`）→ **开发者本机** `opencode serve`（`127.0.0.1:4096`）
- 服务器重启或本机重启后，**本机 `frpc` 客户端与 `opencode serve` 未自动恢复**，导致服务器 `14096` 端口无监听
- 排查确认：`ss -ltn | grep 14096` 无输出；本机 `Test-NetConnection 127.0.0.1 -Port 4096` 为 False；`Get-Process frpc` 为空

### 解决方案

本机（Windows）依次恢复两个进程：

```powershell
# 1. 启动 opencode serve（监听 4096）
$exe = "C:\nvm4w\nodejs\node_modules\opencode-ai\bin\opencode.exe"
Start-Process -FilePath $exe -ArgumentList "serve","--hostname","127.0.0.1","--port","4096" -WindowStyle Hidden

# 2. 启动 frp 客户端（本机 4096 → 服务器 14096）
Start-Process -FilePath "D:\tools\frp\frpc.exe" -ArgumentList "-c","D:\tools\frp\frpc.toml" -WindowStyle Hidden
```

`frpc.toml`（`D:\tools\frp\frpc.toml`）：

```toml
serverAddr = "47.93.233.131"
serverPort = 7000
auth.method = "token"
auth.token = "CHATBASE_OPENTUN_2026"
[[proxies]]
name = "opencode"
type = "tcp"
localIP = "127.0.0.1"
localPort = 4096
remotePort = 14096
```

### 验证

```bash
# 服务器侧（远端）
ss -ltn | grep 14096          # 应有 LISTEN
curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:14096/   # 应返回 200

# 后端日志不再出现 Connection refused
docker logs --since 5m chatbase-backend 2>&1 | grep -i 'Connection refused'
```

> 若 opencode 无响应但端口正常，检查 Basic Auth：用户名 `opencode`、密码即 `OPENCODE_SERVER_PASSWORD`（与服务器 `.env` 中 `OPENCODE_PASSWORD` 一致）。

---

## 2. 服务器 OOM 假死（反复重启 / 服务无响应）

### 现象

- SSH banner 超时（`Error reading SSH protocol banner`），但 TCP 端口 2234/8080/80 全部可达
- 后端 `/api/**` 请求长时间无响应或被中断
- 服务器短时间内多次重启（`last reboot` 能看到 1 分钟内两条记录）
- 系统日志出现 OOM 击杀：
  ```
  Aug 10 11:32:55 ... systemd invoked oom-killer:
  Out of memory: Killed process 1632 (mysqld) total-vm:1887240kB, anon-rss:388576kB
  ```

### 根因

- 阿里云轻量仅 **1.6GB 内存、无 Swap**
- 5 个容器（MySQL 397MB + backend 300MB + napcat 190MB + Redis + frontend）+ 系统本身，内存余量常低于 300MB
- MySQL 使用默认内存参数（`innodb_buffer_pool_size=128M`、`performance_schema=ON`），瞬间峰值触发内核 OOM killer，优先击杀 mysqld
- mysqld 被杀 → 后端 HikariPool `Connection refused` → 全服务假死 → 运维误判重启，恶性循环

### 解决方案（全部零成本，已实施）

**① 添加 2GB Swap**（服务器，立即生效，重启自动挂载）：

```bash
fallocate -l 2G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab
```

**② MySQL 内存瘦身**（`docker-compose.yml` mysql service）：

```yaml
command:
  - "--character-set-server=utf8mb4"
  - "--collation-server=utf8mb4_unicode_ci"
  - "--default-authentication-plugin=mysql_native_password"
  - "--innodb-buffer-pool-size=64M"
  - "--performance_schema=OFF"
  - "--table_open_cache=1000"
  - "--max_connections=60"
mem_limit: 400m
```

**③ 为所有容器设置内存上限**（防止单容器失控）：

| 容器 | mem_limit |
|------|-----------|
| mysql | 400m |
| redis | 150m |
| chatbase-backend | 600m |
| chatbase-frontend | 100m |
| napcat | 300m |

> 后端 JAVA 堆：`/opt/chatBase/.env` 中 `JAVA_OPTS=-Xms256m -Xmx512m`（默认 compose 的 2048m 会超物理内存，务必覆盖）。

**④ 应用重建**：

```bash
cd /opt/chatBase
docker compose config --quiet   # 先校验
docker compose up -d --remove-orphans
```

### 验证

```bash
free -m | head -3        # Swap: 2047
swapon --show            # /swapfile file 2G
docker stats --no-stream # 各容器占用量应明显下降
```

优化效果：整机可用内存 **293MB → 539MB**，MySQL 占用 **397MB → 142MB**。

---

## 3. MySQL 连接池耗尽（HikariPool time out）

### 现象

后端日志：

```
[pool-4-thread-1] ERROR o.s.s.s.TaskUtils$LoggingErrorHandler - Unexpected error occurred in scheduled task
### Error querying database. Cause: ... CannotGetJdbcConnectionException:
Failed to obtain JDBC Connection; nested exception is java.sql.SQLTransientConnectionException:
HikariPool-1 - Connection is not available, request timed out after 30000ms.
Caused by: com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure
```

### 根因

- 这是 **OOM 击杀了 mysqld 的次生故障**（见第 2 节），不是连接池本身配置问题
- Hikari 最大连接数 = 10（`maximum-pool-size`），当 MySQL 进程被杀后，旧连接全部失效，新连接 `Connection refused`

### 解决

- 优先按第 2 节处理 OOM（加 Swap + 限制 MySQL 内存 + mem_limit）
- MySQL 重建（容器数据在 named volume，不丢失）：

```bash
cd /opt/chatBase && docker compose up -d --remove-orphans
# 等待 mysql Healthy 后再观察后端日志
docker ps --format '{{.Names}}|{{.Status}}'
```

- 验证：`mysql -uroot -p<password> -e 'SELECT 1'` 应返回 1；后端日志不再有 `CannotGetJdbcConnection`

---

## 4. 排障速查命令

### 远程执行（本机 Windows，SSH 偶发 banner 超时，需重试）

```python
# 使用 paramiko：connect(timeout=45, banner_timeout=45, auth_timeout=45)，失败后 sleep 5~10s 重试
import paramiko, time
c = paramiko.SSHClient()
c.set_missing_host_key_policy(paramiko.AutoAddPolicy())
c.connect('47.93.233.131', port=2234, username='root', password='<PWD>',
          timeout=45, banner_timeout=45, auth_timeout=45)
```

### 服务器状态

```bash
uptime                      # load / uptime（刚重启 = 最近有过故障）
free -m                     # 内存（可用量 < 300MB 即高危）
swapon --show               # Swap 是否启用
df -h /                     # 磁盘（swap 占用 2G 后余量）
last reboot | head          # 重启记录（1 分钟内两条 = OOM 重启）
docker ps                   # 容器状态
docker stats --no-stream    # 各容器 CPU/内存占用
```

### 后端日志

```bash
docker logs --tail 300 chatbase-backend 2>&1
docker logs --since 5m chatbase-backend 2>&1 | grep -iE 'error|refused|hikari|oom'
```

### 系统 OOM 记录

```bash
grep -iE 'oom|killed process' /var/log/syslog | tail
```

### 常见误判

| 现象 | 真相 |
|------|------|
| SSH `Error reading SSH protocol banner` | 服务器过载/OOM 假死（不是网络问题），TCP 端口仍可达 |
| `/api/**` 请求超时 | 容器活着但线程被阻塞（可能 opencode 隧道断 / MySQL 被杀） |
| 后端 `Connection refused`（172.17.0.1:14096） | opencode frp 隧道断（见第 1 节） |
| 后端 `Connection refused`（MySQL） | mysqld 被 OOM 击杀（见第 2、3 节） |