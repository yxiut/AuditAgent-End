#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import json, os, subprocess, sys
try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass
script = os.path.join(os.path.dirname(os.path.abspath(__file__)), "wb_audit_mcp.py")
p = subprocess.Popen([sys.executable, script], stdin=subprocess.PIPE,
                     stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                     text=True, encoding="utf-8")

def send(o):
    p.stdin.write(json.dumps(o, ensure_ascii=False) + "\n"); p.stdin.flush()

def recv():
    line = p.stdout.readline()
    return json.loads(line) if line.strip() else None

send({"jsonrpc": "2.0", "id": 1, "method": "initialize",
      "params": {"protocolVersion": "2025-06-18", "capabilities": {},
                 "clientInfo": {"name": "t", "version": "2"}}})
recv()
send({"jsonrpc": "2.0", "id": 2, "method": "tools/list"})
r = recv(); print("tools:", [t["name"] for t in r["result"]["tools"]])
send({"jsonrpc": "2.0", "id": 3, "method": "tools/call",
      "params": {"name": "audit_execute",
                 "arguments": {"action": "getRule", "clause_id": "HJ-GC-02"}}})
r3 = recv()
obj = json.loads(r3["result"]["content"][0]["text"])
rule = obj.get("ruleDoc", "")
print("getRule ok, ruleDoc chars =", len(rule))
out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "_rule_extract_preview.txt")
with open(out, "w", encoding="utf-8") as fh:
    fh.write(rule[:1500])
print("preview written:", out)
send({"jsonrpc": "2.0", "id": 9, "method": "ping"}); print("ping:", recv())
p.stdin.close(); p.wait(timeout=15)