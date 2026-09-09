#!/bin/bash
# ChatBase 一键收尾部署（在服务器 47.93.233.131 上执行）
# 前置：源码 4 个修复文件已 SFTP 到 /opt/chatBase
set -e
cd /opt/chatBase

echo "==> 1. 确认修复文件已就位"
ls -lh src/main/java/com/zxl/chatbase/controller/HealthController.java \
       src/main/java/com/zxl/chatbase/wx/service/WxIlinkService.java \
       src/main/java/com/zxl/chatbase/wx/util/WxIlinkUtil.java \
       src/main/java/com/zxl/chatbase/wxroboot/webhook/config/WXBizJsonMsgCryptConfig.java

echo "==> 2. 结束可能卡住的 docker build（释放 1.6G 内存）"
pkill -9 -f 'docker compose build' || true
pkill -9 -f 'docker-buildx' || true
sleep 2

echo "==> 3. 方式A：若已上传 app.jar，直接热替换（最快）"
if [ -f /opt/chatBase/app.jar ]; then
  docker cp /opt/chatBase/app.jar chatbase-backend:/app/app.jar
  docker restart chatbase-backend
else
  echo "==> 3. 方式B：无 app.jar，则仅重建 backend（勿用 --build 全量）"
  docker compose build chatbase-backend
  docker compose up -d chatbase-backend
fi

echo "==> 4. 等待健康检查"
for i in $(seq 1 30); do
  sleep 3
  if curl -fsS http://127.0.0.1:8080/api/health >/tmp/h.json 2>/dev/null; then
    cat /tmp/h.json; echo; break
  fi
  echo "waiting $i..."
done

echo "==> 5. 容器状态与关键日志"
docker ps --format 'table {{.Names}}\t{{.Status}}'
docker logs --tail=40 chatbase-backend 2>&1 | tail -40

echo "完成。浏览器打开 http://47.93.233.131 用 zxl 登录验证。"
