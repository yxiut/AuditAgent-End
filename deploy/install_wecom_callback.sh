#!/usr/bin/env bash
# 一键安装企微回调服务器（腾讯云 CentOS/Ubuntu 通用）
# 用法：bash install_wecom_callback.sh
set -euo pipefail

APP_DIR=/opt/wecom-callback
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "==> 1/5 复制脚本到 $APP_DIR"
mkdir -p "$APP_DIR"
cp "$SCRIPT_DIR/wecom_callback_server.py" "$APP_DIR/"

echo "==> 2/5 安装依赖 cryptography"
if ! python3 -c "import cryptography" 2>/dev/null; then
  python3 -m pip install --upgrade pip >/dev/null 2>&1 || true
  python3 -m pip install cryptography
fi

echo "==> 3/5 生成环境变量文件（请编辑填入 Token / AESKey）"
if [ ! -f "$APP_DIR/wecom-callback.env" ]; then
  cat > "$APP_DIR/wecom-callback.env" <<'EOF'
# 企微后台 → 应用(1000002) → 接收消息 → 设置API接收 里生成/填写的值
WECOM_TOKEN=pi8qSUTKf7KhoATI1djQ
WECOM_AES_KEY=JWTDqgUe066jc4a8FzVuMOJDw1qLT0EdfAzAuqMFwPv
WECOM_PORT=8280
EOF
  echo "    已生成模板: $APP_DIR/wecom-callback.env  →  vim 编辑填入真实 Token/AESKey"
fi

echo "==> 4/5 安装 systemd 服务"
cp "$SCRIPT_DIR/wecom-callback.service" /etc/systemd/system/
systemctl daemon-reload
systemctl enable wecom-callback >/dev/null 2>&1 || true

echo "==> 5/5 启动服务"
systemctl restart wecom-callback || { echo "!! 启动失败，请先编辑 $APP_DIR/wecom-callback.env 填入真实 Token/AESKey"; }
systemctl status wecom-callback --no-pager || true

echo ""
echo "完成。常用命令："
echo "  systemctl status wecom-callback     # 状态"
echo "  journalctl -u wecom-callback -f     # 实时日志"
echo "  systemctl restart wecom-callback    # 重启"
echo ""
echo "企微后台 URL 填: http://<服务器公网IP>:${WECOM_PORT:-8280}/callback"