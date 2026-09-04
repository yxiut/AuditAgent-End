#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""企微「接收消息服务器」回调验证服务器（独立 PY 脚本，仅需 cryptography）

用途
----
- 企微【未认证】企业：用「设置接收消息服务器URL」方式通过归属验证
- 验证通过后即可在企微后台配置「企业可信IP」，解除 message/send 的 60020
- 本脚本只处理企微回调验证/消息接收，不需要部署整个 Java 后端

依赖
----
pip install cryptography

启动
----
python wecom_callback_server.py --token=<企微后台Token> --aes-key=<43位EncodingAESKey> --port=8280
（也可用环境变量 WECOM_TOKEN / WECOM_AES_KEY / WECOM_PORT）

企微后台配置
------------
应用(agentId 1000002) → 接收消息 → 设置API接收：
  URL = http://<服务器公网IP>:<port>/callback
  Token / EncodingAESKey 与启动参数一致
点「保存」→ 企微 GET <URL>?... 验证，通过即开启成功

自测
----
python wecom_callback_server.py --selftest --token=xxx --aes-key=yyy
（本机验证加解密与签名，无需部署）
"""
import argparse
import base64
import hashlib
import logging
import os
import re
import sys
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs

from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes

log = logging.getLogger("wecom-callback")
logging.basicConfig(level=logging.INFO,
                    format="%(asctime)s %(levelname)s %(message)s")

ENCRYPT_RE = re.compile(r"<Encrypt><!\[CDATA\[(.*?)\]\]></Encrypt>", re.DOTALL)


# ---------------- 加解密 / 签名 ----------------

def _aes_key(encoding_aes_key):
    k = (encoding_aes_key or "").strip()
    if len(k) != 43:
        raise ValueError("EncodingAESKey 长度必须为 43 位")
    return base64.b64decode(k + "=")


def decrypt(encoding_aes_key, encrypted_b64):
    """解密：明文 = 16字节random + 4字节msg_len + msg + receiveId"""
    key = _aes_key(encoding_aes_key)
    dec = Cipher(algorithms.AES(key), modes.CBC(key[:16])).decryptor()
    raw = dec.update(base64.b64decode(encrypted_b64)) + dec.finalize()
    pad = raw[-1]
    if 1 <= pad <= 32:
        raw = raw[:-pad]
    if len(raw) < 20:
        raise ValueError("解密结果长度非法")
    msg_len = int.from_bytes(raw[16:20], "big")
    if 20 + msg_len > len(raw):
        raise ValueError("解密消息长度非法")
    msg = raw[20:20 + msg_len].decode("utf-8")
    receive_id = raw[20 + msg_len:].decode("utf-8")
    return msg, receive_id


def encrypt(encoding_aes_key, msg, receive_id):
    """加密（生成测试载荷用）"""
    import secrets
    key = _aes_key(encoding_aes_key)
    random16 = secrets.token_bytes(16)
    msg_b = msg.encode("utf-8")
    body = random16 + len(msg_b).to_bytes(4, "big") + msg_b + receive_id.encode("utf-8")
    pad = 32 - (len(body) % 32)
    body += bytes([pad]) * pad
    enc = Cipher(algorithms.AES(key), modes.CBC(key[:16])).encryptor()
    return base64.b64encode(enc.update(body) + enc.finalize()).decode("ascii")


def verify_signature(token, timestamp, nonce, encrypt, msg_signature):
    joined = "".join(sorted([token, timestamp, nonce, encrypt]))
    return hashlib.sha1(joined.encode("utf-8")).hexdigest() == msg_signature


def compute_signature(token, timestamp, nonce, encrypt):
    joined = "".join(sorted([token, timestamp, nonce, encrypt]))
    return hashlib.sha1(joined.encode("utf-8")).hexdigest()


# ---------------- HTTP 处理 ----------------

class Handler(BaseHTTPRequestHandler):
    token = ""
    aes_key = ""

    def _send(self, status, body, ctype="text/plain; charset=utf-8"):
        data = body.encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        parsed = urlparse(self.path)
        if parsed.path.rstrip("/") != "/callback":
            return self._send(404, "not found")
        q = parse_qs(parsed.query)
        try:
            msg_sig = q["msg_signature"][0]
            ts = q["timestamp"][0]
            nonce = q["nonce"][0]
            echostr = q["echostr"][0]
        except (KeyError, IndexError):
            return self._send(400, "missing params")
        try:
            if not verify_signature(self.token, ts, nonce, echostr, msg_sig):
                log.warning("URL 验证签名失败 timestamp=%s", ts)
                return self._send(401, "signature error")
            msg, receive_id = decrypt(self.aes_key, echostr)
        except Exception as e:  # noqa: BLE001
            log.error("URL 验证异常: %s", e)
            return self._send(500, "decrypt error")
        # 原样返回明文：不带引号 / BOM / 换行
        log.info("URL 验证成功 receiveId=%s msg=%s", receive_id, msg)
        self._send(200, msg)

    def do_POST(self):
        parsed = urlparse(self.path)
        if parsed.path.rstrip("/") != "/callback":
            return self._send(404, "not found")
        q = parse_qs(parsed.query)
        length = int(self.headers.get("Content-Length") or 0)
        body = self.rfile.read(length).decode("utf-8", errors="replace")
        m = ENCRYPT_RE.search(body)
        if not m:
            return self._send(400, "no Encrypt")
        encrypt = m.group(1)
        try:
            msg_sig = q["msg_signature"][0]
            ts = q["timestamp"][0]
            nonce = q["nonce"][0]
        except (KeyError, IndexError):
            return self._send(400, "missing params")
        try:
            if not verify_signature(self.token, ts, nonce, encrypt, msg_sig):
                log.warning("消息签名失败 timestamp=%s", ts)
                return self._send(401, "signature error")
            msg, receive_id = decrypt(self.aes_key, encrypt)
        except Exception as e:  # noqa: BLE001
            log.error("消息解密异常: %s", e)
            return self._send(500, "decrypt error")
        log.info("收到企微消息 receiveId=%s msg=%s", receive_id, msg)
        self._send(200, "success")

    def log_message(self, fmt, *args):  # 静默默认访问日志，用自研日志
        pass


def selftest(token, aes_key):
    """本机自测：加密 → 签名 → 验签 → 解密，校验一致性"""
    msg = "verify-ok-local-%s" % os.getpid()
    corp_id = "ww909f0f8c511a64ec"
    ts, nonce = "13500001234", "123412323"
    echostr = encrypt(aes_key, msg, corp_id)
    sig = compute_signature(token, ts, nonce, echostr)
    assert verify_signature(token, ts, nonce, echostr, sig), "验签失败"
    m, rid = decrypt(aes_key, echostr)
    assert m == msg and rid == corp_id, "解密不一致"
    assert not verify_signature(token, ts, nonce, echostr, "0" + sig[1:]), "错误签名未拦截"
    print("SELFTEST PASS  token=%s  aes_key=%s" % (token, aes_key))
    print("加密回显: %s" % m)


def main():
    ap = argparse.ArgumentParser(description="企微接收消息服务器（回调验证）")
    ap.add_argument("--token", default=os.environ.get("WECOM_TOKEN", ""))
    ap.add_argument("--aes-key", default=os.environ.get("WECOM_AES_KEY", ""))
    ap.add_argument("--port", type=int, default=int(os.environ.get("WECOM_PORT", "8280")))
    ap.add_argument("--host", default="0.0.0.0")
    ap.add_argument("--selftest", action="store_true", help="本机自测加解密/签名，不启动服务")
    a = ap.parse_args()

    if not a.token or not a.aes_key:
        print("缺少 --token 或 --aes-key（或环境变量 WECOM_TOKEN / WECOM_AES_KEY）")
        sys.exit(2)
    if len(a.aes_key.strip()) != 43:
        print("EncodingAESKey 必须为 43 位（企微后台「随机生成」）")
        sys.exit(2)

    if a.selftest:
        selftest(a.token, a.aes_key)
        return

    Handler.token, Handler.aes_key = a.token, a.aes_key.strip()
    srv = ThreadingHTTPServer((a.host, a.port), Handler)
    print("=" * 60)
    print("企微回调服务器已启动")
    print("  URL  : http://<服务器公网IP>:%d/callback" % a.port)
    print("  Token: %s" % a.token)
    print("  AES  : %s" % a.aes_key)
    print("企微后台 → 应用(1000002) → 接收消息 → 设置API接收，填上面 URL 与参数")
    print("=" * 60)
    try:
        srv.serve_forever()
    except KeyboardInterrupt:
        print("\n已停止")


if __name__ == "__main__":
    main()