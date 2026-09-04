# 企微回调服务器（独立 PY 脚本）容器镜像
FROM python:3.10-slim

WORKDIR /app

# 依赖（cryptography 含 AES-CBC 所需算法）
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 回调脚本
COPY wecom_callback_server.py .

# 容器内监听端口（宿主映射由启动脚本/编排决定）
ENV WECOM_PORT=8280
EXPOSE 8280

# 读取环境变量 WECOM_TOKEN / WECOM_AES_KEY
CMD ["python", "wecom_callback_server.py"]