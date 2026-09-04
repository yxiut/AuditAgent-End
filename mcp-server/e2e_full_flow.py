#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""E2E：真实后端 + MCP 5 工具，跑 DEMO ①②③④（一人两角）。结果写 _e2e_report.txt。"""
import json, os, subprocess, sys, datetime, io
try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass

SCRIPT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "wb_audit_mcp.py")
SAMPLE = r"E:\ai\workBuddy-audit-agent\02_上传材料_shang-chuan-cai-liao\assets\模拟系统数据\车间过程FTR问题跟踪管理表.xlsx"
REPORT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "_e2e_report.txt")

p = subprocess.Popen([sys.executable, SCRIPT], stdin=subprocess.PIPE,
                     stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                     text=True, encoding="utf-8", bufsize=1)
lines = []

def send(o):
    p.stdin.write(json.dumps(o, ensure_ascii=False) + "\n"); p.stdin.flush()

def recv():
    line = p.stdout.readline()
    return json.loads(line) if line.strip() else None

def call(tool, args, tag):
    send({"jsonrpc": "2.0", "id": len(lines) + 1, "method": "tools/call",
          "params": {"name": tool, "arguments": args}})
    r = recv()
    txt = ""
    is_err = False
    if r and "result" in r:
        txt = r["result"]["content"][0]["text"]
        is_err = bool(r["result"].get("isError"))
    else:
        txt = json.dumps(r, ensure_ascii=False)
    lines.append("===== %s =====\n%s" % (tag, txt))
    return txt, is_err

def data_of(txt):
    try:
        return json.loads(txt).get("data")
    except Exception:
        return None

# 0 握手
send({"jsonrpc": "2.0", "id": 0, "method": "initialize",
      "params": {"protocolVersion": "2025-06-18", "capabilities": {},
                 "clientInfo": {"name": "e2e", "version": "1"}}})
recv()
lines.append("===== 0 握手 OK / server 5 工具 =====")

# ① 张伟(5) 发起
txt, err = call("audit_task", {"action": "createAndDispatch", "actor": 5,
                 "clause_ids": ["HJ-GC-02"], "assignee_user_id": 4,
                 "region": "过程（焊接）", "factory_id": 1,
                 "period_type": "MONTH", "period_start": "2026-07-01", "period_end": "2026-07-31"},
                "① createAndDispatch 张伟发起(企微应推陈志强)")
d = data_of(txt)
task_id = (d or {}).get("taskId") if not err else None

# ② 陈志强(4) 交材料
txt2, err2 = call("audit_material", {"action": "listTasks", "actor": 4}, "②a 陈志强 listTasks")
txt3, err3 = call("audit_material", {"action": "listPending", "actor": 4, "task_id": task_id}, "②b 陈志强 listPending")
txt4, err4 = call("audit_material", {"action": "upload", "actor": 4, "task_id": task_id,
                                     "clause_id": "HJ-GC-02", "file_path": SAMPLE}, "②c 陈志强 upload FTR xlsx")
du = data_of(txt4)
material_id = (du or {}).get("materialId") if not err4 else None
txt5, err5 = call("audit_material", {"action": "confirm", "actor": 4, "task_id": task_id,
                                     "material_ids": [material_id]}, "②d 陈志强 confirm")

# ③ 张伟侧：开始 AI 审核（B 方案）
txt6, err6 = call("audit_execute", {"action": "pullQueue", "task_id": task_id}, "③a pullQueue 取待审")
txt7, err7 = call("audit_execute", {"action": "getRule", "clause_id": "HJ-GC-02"}, "③b getRule 规则全文")
txt8, err8 = call("audit_execute", {"action": "getMaterial", "task_id": task_id, "clause_id": "HJ-GC-02"}, "③c getMaterial 材料全文")
issues = [
    {"rule_id": "FTR连续两月低于目标",
     "problem_desc": "审核龙兴工厂焊接2026年7月过程FTR连续两个可审月低于目标",
     "evidence": "PULL：焊接 2026-06、2026-07 FTR 与目标对照",
     "problem_type": "执行类", "score": 6,
     "ref_materials": ["PULL-月度指标"], "suggest_judgment": "不符合", "confidence": "高"},
    {"rule_id": "对比审核-FTR TOP3问题管理一致性",
     "problem_desc": "FTR不达标触发后，问题管理项目与系统TOP3不一致",
     "evidence": "系统TOP3 与 FTR 管理表项目对照",
     "problem_type": "标准类", "score": 6,
     "ref_materials": ["PULL-系统TOP3", "M-FTR"], "suggest_judgment": "不符合", "confidence": "中"},
]
txt9, err9 = call("audit_execute", {"action": "writeConclusion", "task_id": task_id,
                                    "clause_id": "HJ-GC-02", "outcome": "scored",
                                    "issues": issues}, "③d writeConclusion AI 写结论(企微应推张伟)")

# ④ 张伟(5) 查进度
txt10, err10 = call("audit_execute", {"action": "getProgress", "task_id": task_id}, "④ getProgress 查进度")
d10 = data_of(txt10)

with open(REPORT, "w", encoding="utf-8") as fh:
    fh.write("\n\n".join(lines) + "\n")

# ASCII summary to console
print("create err=%s task_id=%s" % (err, task_id))
print("listTasks err=%s | listPending err=%s | upload err=%s material_id=%s | confirm err=%s" % (err2, err3, err4, material_id, err5))
print("pullQueue err=%s | getRule err=%s | getMaterial err=%s | writeConclusion err=%s" % (err6, err7, err8, err9))
print("getProgress err=%s" % err10)
if d10:
    print("phase=%s globalState=%s taskNo=%s" % (d10.get("phase"), d10.get("globalState"), d10.get("taskNo")))
    for cp in d10.get("clauses") or []:
        print("clause: path=%s issuesCount=%s suggestedScore=%s outcome=%s" % (
            cp.get("path"), cp.get("issuesCount"), cp.get("suggestedScore"), cp.get("outcome")))
    print("bipRows=%s" % len(d10.get("bipRows") or []))
print("report:", REPORT)
p.stdin.close()
p.wait(timeout=30)