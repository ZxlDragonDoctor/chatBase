#!/usr/bin/env bash
# ChatBase 一键部署脚本
# 用法：
#   bash scripts/deploy.sh              # 构建并启动核心服务
#   bash scripts/deploy.sh --with-qq    # 同时启动 NapCat（QQ Bot）
#   bash scripts/deploy.sh --rebuild    # 强制重新构建镜像
#   bash scripts/deploy.sh --down       # 停止全部服务
set -euo pipefail

cd "$(dirname "$0")/.."

WITH_QQ=false
REBUILD=false
DOWN=false
for arg in "$@"; do
  case "$arg" in
    --with-qq) WITH_QQ=true ;;
    --rebuild) REBUILD=true ;;
    --down) DOWN=true ;;
    -h|--help)
      sed -n '2,8p' "$0"
      exit 0
      ;;
    *)
      echo "未知参数: $arg（--help 查看用法）" >&2
      exit 1
      ;;
  esac
done

if [ "$DOWN" = true ]; then
  echo "==> 停止全部服务"
  docker compose --profile qq down
  exit 0
fi

if [ ! -f .env ]; then
  echo "==> 未找到 .env，从 .env.example 复制"
  cp .env.example .env
  echo "⚠️  请编辑 .env 填写 MYSQL_ROOT_PASSWORD / MYSQL_PASSWORD / DIFYAPP_API_KEY 等必填项后重新运行"
  exit 1
fi

# 校验必填项
required=(MYSQL_ROOT_PASSWORD MYSQL_PASSWORD DIFYAPP_API_KEY DIFYAPP_DATASET_API_KEY)
missing=()
for key in "${required[@]}"; do
  if ! grep -qE "^${key}=.+" .env; then
    missing+=("$key")
  fi
done
if [ ${#missing[@]} -gt 0 ]; then
  echo "❌ .env 缺少必填项: ${missing[*]}" >&2
  exit 1
fi

PROFILES=""
if [ "$WITH_QQ" = true ]; then
  PROFILES="--profile qq"
  echo "==> 启用 QQ Bot (NapCat) profile"
fi

if [ "$REBUILD" = true ]; then
  echo "==> 强制重新构建镜像"
  docker compose $PROFILES build --no-cache
fi

echo "==> 启动服务（首次会自动构建）"
docker compose $PROFILES up -d --build

echo "==> 等待后端健康检查..."
for i in $(seq 1 30); do
  if curl -fsS http://127.0.0.1:8080/api/health >/dev/null 2>&1; then
    echo "✅ 后端已就绪"
    break
  fi
  if [ "$i" = 30 ]; then
    echo "⚠️  后端 30s 内未就绪，请查看日志: docker compose logs -f chatbase-backend" >&2
  fi
  sleep 2
done

echo ""
echo "部署完成 ✿"
echo "  前端:  http://127.0.0.1:8081  （生产经 Nginx 80/443）"
echo "  后端:  http://127.0.0.1:8080/api/health"
echo "  日志:  docker compose logs -f chatbase-backend"
echo ""
echo "可选 · opencode 反向桥接（无需 frp）："
echo "  1. .env 设置 OPENCODE_ENABLED=true 与 OPENCODE_BRIDGE_TOKEN=<随机串>"
echo "  2. 本机: BRIDGE_URL=wss://<域名>/opencode/bridge BRIDGE_TOKEN=<随机串> node bridge/opencode-bridge.mjs"
