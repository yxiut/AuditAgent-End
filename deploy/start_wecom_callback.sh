#!/usr/bin/env bash
# 一键构建并启动企微回调服务器（Docker 版）
# 用法：
#   bash start_wecom_callback.sh            # 构建(如缺)+启动
#   bash start_wecom_callback.sh --build    # 强制重新构建
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NAME=wecom-callback

command -v docker >/dev/null 2>&1 || { echo "!! 未安装 docker，请先安装（腾讯云可装 Docker 镜像）"; exit 1; }

# 载入配置（可被外部环境变量覆盖）
if [ -f "$DIR/wecom-callback.env" ]; then
  set -a; source "$DIR/wecom-callback.env"; set +a
fi
: "${WECOM_TOKEN:?请在 wecom-callback.env 设置 WECOM_TOKEN}"
: "${WECOM_AES_KEY:?请在 wecom-callback.env 设置 WECOM_AES_KEY}"
HOST_PORT="${HOST_PORT:-${WECOM_PORT:-8280}}"

# 构建镜像（缺省或 --build 时）
if [ "${1:-}" = "--build" ] || ! docker image inspect "$NAME" >/dev/null 2>&1; then
  echo "==> 构建镜像 $NAME"
  docker build -t "$NAME" "$DIR"
fi

# 停掉旧容器
docker rm -f "$NAME" >/dev/null 2>&1 || true

echo "==> 启动容器 (宿主 $HOST_PORT -> 容器 8280)"
docker run -d --name "$NAME" --restart unless-stopped \
  -p "$HOST_PORT:8280" \
  -e WECOM_TOKEN="$WECOM_TOKEN" \
  -e WECOM_AES_KEY="$WECOM_AES_KEY" \
  -e WECOM_PORT=8280 \
  "$NAME"

echo "已启动: http://<服务器公网IP>:$HOST_PORT/callback"
echo "日志:   docker logs -f $NAME"
echo "状态:   docker ps | grep $NAME"