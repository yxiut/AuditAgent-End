#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import json, threading, time, urllib.request, http.client, sys, os
try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import wb_audit_mcp as m

PORT = 18090
threading.Thread(target=m.run_http, args=(PORT,), daemon=True).start()
time.sleep(1)

def post(obj, accept="application/json", timeout=8):
    data = json.dumps(obj, ensure_ascii=False).encode("utf-8")
    req = urllib.request.Request("http://127.0.0.1:%d/mcp" % PORT, data=data, method="POST",
                                 headers={"Content-Type": "application/json", "Accept": accept})
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return r.status, r.read().decode("utf-8")

s, txt = post({"jsonrpc": "2.0", "id": 1, "method": "initialize",
               "params": {"protocolVersion": "2025-06-18", "capabilities": {},
                          "clientInfo": {"name": "http-test", "version": "1"}}})
print("init:", s, txt[:80])
s, txt = post({"jsonrpc": "2.0", "id": 2, "method": "tools/list"})
print("tools:", s, len(json.loads(txt)["result"]["tools"]))
s, txt = post({"jsonrpc": "2.0", "id": 3, "method": "tools/call",
               "params": {"name": "audit_execute",
                          "arguments": {"action": "getRule", "clause_id": "HJ-GC-02"}}})
print("getRule:", s, txt[:80])
s, txt = post({"jsonrpc": "2.0", "id": 4, "method": "ping"}, accept="text/event-stream")
print("SSE ping:", s, "first line:", txt.splitlines()[0] if txt else "")
# GET SSE：只读首行即断开
try:
    conn = http.client.HTTPConnection("127.0.0.1", PORT, timeout=3)
    conn.request("GET", "/mcp", headers={"Accept": "text/event-stream"})
    r = conn.getresponse()
    first = r.readline()
    print("GET SSE:", r.status, first[:60])
    conn.close()
except Exception as e:
    print("GET SSE err:", type(e).__name__, e)
print("HTTP test done")