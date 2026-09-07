#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
wb-audit-mcp v2 —— WorkBu ================================================
对齐最新版 Skill 契约（01 发起 / 02 交材料 / 03 规则执行 / 04 监控），
把 Spring Boot REST 后端包装成 5 个按域聚合的 MCP 工具，工具内用 `action` 分发：

  audit_meta     审计元数据：canPublish / normalize / listFactories / clauseScope / searchPerson
  audit_task     任务：createAndDispatch（历史/复制/基线 = 会话上下文，DEMO 不走后端）
  audit_notify   通知：notifyDispatch（创建后企微由后端自动发；本工具留作显式催交/通知）
  audit_material 材料：listTasks / listPending / previewPull / upload / confirm
  audit_execute  执行：pullQueue / getRule / getMaterial / writeConclusion / getProgress

身份：后端请求头 X-User-Id（4=陈志强·被审核人，5=张伟·审核员）。各域默认身份：
      audit_meta/audit_task/audit_notify/audit_execute 默认 5；audit_material 默认 4。
仅 Python 标准库；stdio 传输；每行一个 JSON-RPC 2.0 报文。
环境变量：WB_AUDIT_BASE（默认 http://127.0.0.1:8080/api）、WB_AUDIT_TIMEOUT(默认30)、
          WB_RULE_DOCX（规则 Word 绝对路径；缺省自动找 execution/shen-he-gui-ze 下 docx）
"""
import html
import json
import os
import re
import sys
import uuid
import zipfile
import mimetypes
import urllib.request
import urllib.parse
import urllib.error

SERVER_NAME = "wb-audit-mcp"
SERVER_VERSION = "2.0.0"
DEFAULT_BASE = "http://127.0.0.1:8080/api"
BASE = os.environ.get("WB_AUDIT_BASE", DEFAULT_BASE).rstrip("/")
TIMEOUT = float(os.environ.get("WB_AUDIT_TIMEOUT", "30"))
DEMO_CLAUSE = "HJ-GC-02"

# 会话内“当前任务”锚点：createAndDispatch / pullQueue 命中后记录，供缺省 task_id 用
ACTIVE_TASK = None
# ---------------------------------------------------------------- HTTP 基础
def _request(method, path, headers=None, body_bytes=None):
    url = BASE + path
    req = urllib.request.Request(url, data=body_bytes, method=method)
    req.add_header("Accept", "application/json")
    for k, v in (headers or {}).items():
        req.add_header(k, str(v))
    _opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))  # 本地后端直连，禁用代理(防网关502)
    try:
        with _opener.open(req, timeout=TIMEOUT) as resp:
            payload = resp.read()
    except urllib.error.HTTPError as e:
        return {"http_error": True, "status": e.code,
                "text": e.read().decode("utf-8", "replace")}
    except Exception as e:
        return {"http_error": True, "status": 0,
                "text": "%s: %s" % (type(e).__name__, e)}
    try:
        return {"http_error": False, "json": json.loads(payload.decode("utf-8"))}
    except Exception:
        return {"http_error": False, "text": payload.decode("utf-8", "replace")}


def _auth_headers(actor):
    if actor in (None, ""):
        return {}
    return {"X-User-Id": str(int(actor))}


def call_json(method, path, body=None, actor=None):
    data = None
    headers = _auth_headers(actor)
    if body is not None:
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        headers.setdefault("Content-Type", "application/json")
    r = _request(method, path, headers=headers, body_bytes=data)
    if r.get("http_error"):
        return False, "后端请求失败(%s) %s" % (r.get("status"), r.get("text"))
    if "json" in r:
        env = r["json"]
        if isinstance(env, dict) and env.get("code") not in (None, 200):
            return False, "后端返回错误 code=%s message=%s" % (env.get("code"), env.get("message"))
        return True, json.dumps(env, ensure_ascii=False, indent=1)
    return False, "后端响应非 JSON: " + r.get("text", "")


def call_get(path, actor=None):
    return call_json("GET", path, actor=actor)


def upload_file(task_id, clause_id, file_path, actor):
    if not os.path.isfile(file_path):
        return False, "文件不存在: %s" % file_path
    boundary = "----wbAuditMCP" + uuid.uuid4().hex
    lines = []

    def add_field(name, value):
        lines.append(("--" + boundary).encode("utf-8"))
        lines.append(('Content-Disposition: form-data; name="%s"' % name).encode("utf-8"))
        lines.append(b"")
        lines.append(str(value).encode("utf-8"))

    add_field("taskId", task_id)
    add_field("clauseId", clause_id)
    fname = os.path.basename(file_path)
    ctype = mimetypes.guess_type(fname)[0] or "application/octet-stream"
    lines.append(("--" + boundary).encode("utf-8"))
    lines.append(('Content-Disposition: form-data; name="file"; filename="%s"' % fname).encode("utf-8"))
    lines.append(("Content-Type: %s" % ctype).encode("utf-8"))
    lines.append(b"")
    with open(file_path, "rb") as fh:
        lines.append(fh.read())
    lines.append(("--" + boundary + "--").encode("utf-8"))
    body = b"\r\n".join(lines)
    headers = _auth_headers(actor)
    headers["Content-Type"] = "multipart/form-data; boundary=" + boundary
    r = _request("POST", "/materials/upload", headers=headers, body_bytes=body)
    if r.get("http_error"):
        return False, "上传失败(%s) %s" % (r.get("status"), r.get("text"))
    env = r.get("json")
    if env is None:
        return False, "上传响应非 JSON"
    if env.get("code") not in (None, 200):
        return False, "上传返回错误 code=%s message=%s" % (env.get("code"), env.get("message"))
    return True, json.dumps(env, ensure_ascii=False, indent=1)


# ---------------------------------------------------------------- 参数小工具
def g(args, key, default=None, cast=None):
    v = args.get(key, default)
    if v is None or v == "":
        return default
    if cast is not None:
        try:
            return cast(v)
        except (TypeError, ValueError):
            return default
    return v


def g_int(args, key, default):
    v = args.get(key, default)
    try:
        return int(v)
    except (TypeError, ValueError):
        return default


def g_bool(args, key, default):
    v = args.get(key, default)
    if isinstance(v, bool):
        return v
    if isinstance(v, str):
        return v.strip().lower() in ("1", "true", "yes", "y")
    return bool(v)



def _norm_ints(v):
    """容错归一为 int 列表：支持 list / 单个数字 / 数字串 / 逗号·空格·分号分隔串。"""
    if v is None:
        return []
    if isinstance(v, list):
        out = []
        for x in v:
            if isinstance(x, bool):
                continue
            try:
                out.append(int(str(x).strip()))
            except (TypeError, ValueError):
                pass
        return out
    if isinstance(v, (int, float)) and not isinstance(v, bool):
        return [int(v)]
    s = str(v).strip()
    if not s:
        return []
    out = []
    for p in re.split(r"[,\s;]+", s):
        if not p:
            continue
        try:
            out.append(int(p))
        except (TypeError, ValueError):
            pass
    return out


def _norm_strs(v):
    """容错归一为字符串列表：支持 list / 单值 / 逗号·空格·分号分隔串。"""
    if v is None:
        return []
    if isinstance(v, list):
        return [str(x) for x in v if str(x) not in ("", "None")]
    s = str(v).strip()
    if not s or s == "None":
        return []
    return [p for p in re.split(r"[,\s;]+", s) if p]

def resolve_task_id(args):
    """优先取参数 task_id / taskId；否则用会话内已锚定任务。"""
    global ACTIVE_TASK
    tid = g_int(args, "task_id", 0) or g_int(args, "taskId", 0)
    if tid:
        ACTIVE_TASK = tid
        return tid
    if ACTIVE_TASK:
        return ACTIVE_TASK
    return None

# ---------------------------------------------------------------- 规则 Word -> 全文
def _find_rule_docx():
    env = os.environ.get("WB_RULE_DOCX", "").strip()
    if env and os.path.isfile(env):
        return env
    base = os.path.dirname(os.path.abspath(__file__))
    cand = os.path.join(base, "execution", "shen-he-gui-ze", "references", "规则文档")
    if os.path.isdir(cand):
        for fn in os.listdir(cand):
            if fn.lower().endswith(".docx"):
                return os.path.join(cand, fn)
    return None


def docx_to_text(path):
    """解出 word/document.xml，按 <w:p> 段落取全部 <w:t> 文本（ElementTree，无原始 XML 混入）。"""
    import xml.etree.ElementTree as ET
    try:
        with zipfile.ZipFile(path) as zf:
            xml_bytes = zf.read("word/document.xml")
    except Exception as e:
        return None, "读取 docx 失败: %s" % e
    try:
        root = ET.fromstring(xml_bytes)
    except Exception as e:
        return None, "解析 docx XML 失败: %s" % e
    W = "{http://schemas.openxmlformats.org/wordprocessingml/2006/main}"
    lines = []
    for para in root.iter(W + "p"):
        texts = [t.text or "" for t in para.iter(W + "t")]
        line = "".join(texts).strip()
        if line:
            lines.append(line)
    if not lines:
        return None, "docx 未提取到文本"
    return "\n".join(lines), None

# ---------------------------------------------------------------- audit_meta
def h_meta(args):
    action = g(args, "action", "")
    actor = g_int(args, "actor", 5)
    if action == "canPublish":
        return call_json("POST", "/auth/checkPublish", actor=actor)   # 能否发布/层级/可选基地
    if action == "normalize":
        text = g(args, "text", "")
        if not text:
            return False, "normalize 需要 text"
        return call_json("POST", "/dict/normalize", body={"text": text})
    if action == "listFactories":
        return call_get("/dict/factories")
    if action == "clauseScope":
        q = urllib.parse.urlencode({k: v for k, v in {
            "factory": args.get("factory"), "region": args.get("region")
        }.items() if v})
        return call_get("/dict/clauseTree" + ("?" + q if q else ""))
    if action == "searchPerson":
        qq = g(args, "q", "")
        if not qq:
            return False, "searchPerson 需要 q（姓名/工号）"
        return call_get("/users/search?q=%s" % urllib.parse.quote(qq))
    return False, "audit_meta 未知 action: %s（支持 canPublish/normalize/listFactories/clauseScope/searchPerson）" % action

# ---------------------------------------------------------------- audit_task

def _validate_task_payload(p):
    errs = []
    per = p.get("period")
    if not isinstance(per, dict) or not all(k in per and per.get(k) not in (None, "") for k in ("type", "start", "end")):
        errs.append("period 须为 {type,start,end}，type=MONTH/QUARTER/HALF_YEAR/YEAR/CUSTOM")
    else:
        for k in ("start", "end"):
            if not re.match(r"^\d{4}-\d{2}-\d{2}$", str(per.get(k, ""))):
                errs.append("period." + k + " 须为 yyyy-MM-dd")
    if not isinstance(p.get("factoryId"), int):
        errs.append("factoryId 须为整数（1龙兴/2两江/3扬帆）")
    regs = p.get("regions")
    if not isinstance(regs, list) or not regs:
        errs.append("regions 须为非空对象数组")
    else:
        for i, r in enumerate(regs):
            if not isinstance(r, dict) or not isinstance(r.get("region"), str) or not r.get("region"):
                errs.append("regions[%d] 须为 {region: 区域名, clauses:[...]}" % i)
                continue
            cls = r.get("clauses")
            if not isinstance(cls, list) or not cls:
                errs.append("regions[%d].clauses 须为非空数组 [{clauseId, assigneeId}]" % i)
                continue
            for j, c in enumerate(cls):
                if not isinstance(c, dict) or not isinstance(c.get("clauseId"), str) or not isinstance(c.get("assigneeId"), int):
                    errs.append("regions[%d].clauses[%d] 须为 {clauseId: string, assigneeId: int}" % (i, j))
    dt = p.get("dispatchTree")
    if not isinstance(dt, list) or not dt:
        errs.append("dispatchTree 须为非空对象数组 [{assigneeId, clauseIds:[...]}]")
    else:
        for i, e in enumerate(dt):
            if not isinstance(e, dict) or not isinstance(e.get("assigneeId"), int) or not isinstance(e.get("clauseIds"), list) or not e.get("clauseIds"):
                errs.append("dispatchTree[%d] 须为 {assigneeId: int, clauseIds: [string]}" % i)
    return "；".join(errs)

def h_task(args):
    global ACTIVE_TASK
    action = g(args, "action", "")
    actor = g_int(args, "actor", 5)
    if action == "searchHistory":
        # 历史检索走后端：当前审核员(owner)创建的任务，创建时间倒序、状态不限
        q = {}
        ms = args.get("matchedSlots") or {}
        if isinstance(ms, dict):
            q.setdefault("region", ms.get("region") or ms.get("区域") or args.get("region"))
            q.setdefault("clauseId", ms.get("clauseId") or ms.get("clause_id") or ms.get("条款") or args.get("clause_id"))
            q.setdefault("periodType", ms.get("periodType") or ms.get("period_type") or args.get("period_type"))
            q.setdefault("periodStart", ms.get("periodStart") or ms.get("period_start") or args.get("period_start"))
            q.setdefault("periodEnd", ms.get("periodEnd") or ms.get("period_end") or args.get("period_end"))
        else:
            for k in ("region", "clauseId", "periodType", "periodStart", "periodEnd"):
                if args.get(k):
                    q[k] = args[k]
        if args.get("factory_id") is not None:
            q["factoryId"] = args["factory_id"]
        qs = {k: v for k, v in q.items() if v not in (None, "")}
        path = "/tasks/queryHistoryList" + ("?" + urllib.parse.urlencode(qs) if qs else "")
        return call_get(path, actor=actor)
    if action in ("listCopyable", "copyStructure", "saveBaseline"):
        return False, ("DEMO 版不走后端：%s 由会话上下文承接（复制=沿用最近一次历史任务的范围与分派）。"
                       % action)
    if action == "createAndDispatch":
        # 方式1（推荐）：平铺高参 -> 适配器自动组装成后端 TaskDto，模型不用拼嵌套结构
        _cls = _norm_strs(args.get("clause_ids"))
        if not _cls and args.get("clause_id"):
            _cls = [str(args.get("clause_id"))]
        flat_ok = (len(_cls) > 0
                   and all(args.get(k) not in (None, "") for k in
                           ("period_type", "period_start", "period_end", "factory_id", "region", "assignee_user_id")))
        if flat_ok:
            clause_ids = _cls
            assignee = int(args["assignee_user_id"])
            body = {
                "period": {"type": args["period_type"], "start": args["period_start"], "end": args["period_end"]},
                "factoryId": int(args["factory_id"]),
                "regions": [{"region": args["region"],
                             "clauses": [{"clauseId": cid, "assigneeId": assignee} for cid in clause_ids]}],
                "dispatchTree": [{"assigneeId": assignee, "nodeType": "leaf", "clauseIds": clause_ids}],
            }
        else:
            payload = args.get("payload")
            if isinstance(payload, dict):
                err = _validate_task_payload(payload)
                if err:
                    return False, "createAndDispatch payload 结构不合法：" + err + "（正确结构见工具描述中的可跑示例）"
                body = payload
            else:
                need = []
                if not _cls:
                    need.append("clause_ids[]")
                for k in ("period_type", "period_start", "period_end", "factory_id", "region", "assignee_user_id"):
                    if args.get(k) in (None, ""):
                        need.append(k)
                return False, ("createAndDispatch 缺少必填: " + ", ".join(need) +
                               "（推荐平铺高参：clause_ids/region/factory_id/period_type(MONTH等)/period_start/period_end(yyyy-MM-dd)/assignee_user_id）")
        ok, text = call_json("POST", "/tasks/create", body=body, actor=actor)
        if ok:
            try:
                ACTIVE_TASK = int(json.loads(text)["data"]["taskId"])
            except Exception:
                pass
        return ok, text
    return False, "audit_task 未知 action: %s（DEMO 仅 createAndDispatch；历史/复制/基线走会话上下文）" % action

def h_notify(args):
    action = g(args, "action", "notifyDispatch")
    if action != "notifyDispatch":
        return False, "audit_notify 未知 action: %s" % action
    to_users = _norm_strs(args.get("to_users"))
    template_code = g(args, "template_code", "MATERIAL_PENDING")
    task_id = args.get("task_id")
    body = {"taskId": task_id, "templateCode": template_code,
            "toUsers": [str(u) for u in to_users], "vars": args.get("vars") or {}}
    if not to_users:
        return False, "notifyDispatch 需要 to_users（企微 userid 列表）"
    return call_json("POST", "/notify/send", body=body)

# ---------------------------------------------------------------- audit_material
def h_material(args):
    action = g(args, "action", "")
    actor = g_int(args, "actor", 4)
    if action == "listTasks":
        # 审核员(5)列自己名下审核任务；被审核对象(4)列自己名下材料任务；未显式给 actor 时自动判断
        if "actor" in args and str(args.get("actor")) in ("4", "4.0"):
            return call_get("/materials/tasks", actor=4)
        if "actor" in args and str(args.get("actor")) in ("5", "5.0"):
            return call_get("/audit/tasks", actor=5)
        ok, txt = call_get("/audit/tasks", actor=5)   # 先按审核员(张伟)列
        if ok:
            try:
                if (json.loads(txt).get("data") or {}).get("tasks"):
                    return True, txt
            except Exception:
                pass
        return call_get("/materials/tasks", actor=4)  # 空则按被审核对象(陈志强)列
    if action == "listPending":
        tid = resolve_task_id(args)
        if not tid:
            return False, "listPending 需要 task_id"
        return call_get("/materials/tasks/pending?taskId=%s" % tid, actor=actor)
    if action == "previewPull":
        tid = resolve_task_id(args)
        clause_id = g(args, "clause_id", "")
        if not tid or not clause_id:
            return False, "previewPull 需要 task_id + clause_id"
        return call_json("POST", "/materials/tasks/pullPreview",
                         body={"taskId": tid, "clauseId": clause_id}, actor=actor)
    if action == "upload":
        tid = resolve_task_id(args)
        clause_id = g(args, "clause_id", "")
        file_path = g(args, "file_path", "")
        if not tid or not clause_id or not file_path:
            return False, "upload 需要 task_id + clause_id + file_path(本机绝对路径)"
        return upload_file(tid, clause_id, file_path, actor)
    if action == "confirm":
        tid = resolve_task_id(args)
        material_ids = _norm_ints(args.get("material_ids"))
        if not material_ids:
            material_ids = _norm_ints(args.get("material_id"))
        if not tid or not material_ids:
            return False, "confirm 需要 task_id + material_ids（数组/单值/逗号串均可，来自 upload 返回）"
        return call_json("POST", "/materials/tasks/confirm",
                         body={"taskId": tid, "materialIds": material_ids},
                         actor=actor)
    return False, "audit_material 未知 action: %s（支持 listTasks/listPending/previewPull/upload/confirm）" % action

# ---------------------------------------------------------------- audit_execute
def _norm_issues(issues):
    """兼容 03 SKILL 的中文键与英文键，统一为后端 ConclusionWriteDto.issues。"""
    out = []
    for it in issues or []:
        def pick(*keys, default=None):
            for k in keys:
                if k in it and it[k] not in (None, ""):
                    return it[k]
            return default
        score = pick("score", "问题得分", "严重度", default=None)
        try:
            score = int(score)
        except (TypeError, ValueError):
            score = None
        out.append({
            "ruleId": pick("rule_id", "ruleId", "审核点", default=""),
            "problemDesc": pick("problem_desc", "problemDesc", "问题描述", default=""),
            "evidence": pick("evidence", "依据", default=""),
            "problemType": pick("problem_type", "problemType", "问题类型", default=""),
            "score": score,
            "refMaterials": pick("ref_materials", "refMaterials", "引用资料", default=[]) or [],
            "suggestJudgment": pick("suggest_judgment", "suggestJudgment", "判定建议", default=""),
            "confidence": pick("confidence", "置信度", default=""),
        })
    return out


def h_execute(args):
    global ACTIVE_TASK
    action = g(args, "action", "")
    if action == "runTask":
        # 02/03 原始 Skill 调用：受理 AI 审核（后端写 run + 任务态 AUDITING；不真审）
        tid = resolve_task_id(args)
        if not tid:
            return False, "runTask 需要 task_id"
        return call_json("POST", "/execute/tasks/run", body={"taskId": tid})
    if action == "queryReadyTasks":
        # 定时任务轮询：列材料已收齐、待 AI 审核的任务（只读）
        return call_get("/audit/ready-tasks")

    if action == "pullQueue":
        tid = resolve_task_id(args)
        if not tid:
            return False, "pullQueue 需要 task_id"
        return call_get("/audit/queue?taskId=%s" % tid, actor=5)
    if action == "getRule":
        clause_id = g(args, "clause_id", DEMO_CLAUSE)
        # 首选后端规则（HJ-GC-02 存库文本）；空则回退执行侧 Word 全文
        ok, text = call_get("/audit/rules?clauseId=%s" % urllib.parse.quote(clause_id))
        if ok:
            try:
                data = json.loads(text).get("data") or {}
                rule_text = data.get("content") or data.get("ruleText") or data.get("text") or ""
                if rule_text:
                    return True, json.dumps({"clauseId": clause_id, "ruleDoc": rule_text},
                                            ensure_ascii=False, indent=1)
            except Exception:
                pass
        path = _find_rule_docx()
        if not path:
            return False, "getRule：后端规则为空且找不到执行侧规则 Word"
        rule_text, err = docx_to_text(path)
        if err:
            return False, "getRule：%s" % err
        return True, json.dumps({"clauseId": clause_id, "ruleDoc": rule_text},
                                ensure_ascii=False, indent=1)
    if action == "getMaterial":
        tid = resolve_task_id(args)
        clause_id = g(args, "clause_id", "")
        if not tid or not clause_id:
            return False, "getMaterial 需要 task_id + clause_id（取 pullQueue 返回的条款，勿默认）"
        return call_get("/materials/content?taskId=%s&clauseId=%s" % (tid, urllib.parse.quote(clause_id)),
                        actor=4)
    if action == "writeConclusion":
        tid = resolve_task_id(args)
        clause_id = g(args, "clause_id", "")
        if not tid or not clause_id:
            return False, "writeConclusion 需要 task_id + clause_id（取 pullQueue 返回的条款，勿默认）"
        outcome = g(args, "outcome", "scored")
        body = {
            "taskId": tid,
            "clauseId": clause_id,
            "outcome": outcome,
            "blockedReason": g(args, "blocked_reason", None),
            "notes": args.get("notes") or [],
            "issues": _norm_issues(args.get("issues") or []),
        }
        return call_json("POST", "/audit/conclusion", body=body)
    if action == "getProgress":
        tid = resolve_task_id(args)
        if not tid:
            return False, "getProgress 需要 task_id"
        return call_get("/audit/progress?taskId=%s" % tid, actor=5)

    if action == "confirmReview":
        tid = resolve_task_id(args)
        if not tid:
            return False, "confirmReview 需要 task_id"
        body = {"taskId": tid}
        if args.get("remove") is not None:
            body["remove"] = args["remove"]
        if args.get("add") is not None:
            body["add"] = args["add"]
        if args.get("fieldChanges") is not None:
            body["fieldChanges"] = args["fieldChanges"]
        # remove=[基线序号], add=[{区域,项目,子要素,条款,问题描述,严重度（赋分）,问题属性}], fieldChanges=[{序号,字段,from,to}]
        return call_json("POST", "/audit/review/confirm", body=body, actor=5)
    return False, "audit_execute 未知 action: %s（支持 queryReadyTasks/pullQueue/runTask/getRule/getMaterial/writeConclusion/getProgress）" % action

# ---------------------------------------------------------------- 工具清单（5 聚合）
_COMMON = {
    "action": {"type": "string", "description": "动作名（必填），见工具描述"},
    "actor": {"type": "integer", "description": "身份用户ID：4=陈志强(被审核人)、5=张伟(审核员)"},
    "task_id": {"type": "integer", "description": "任务ID（可省略，会话内已锚定则自动用）"},
    "clause_id": {"type": "string", "description": "条款实例ID，如 HJ-GC-02"},
    "text": {"type": "string", "description": "口语原文/搜索词"},
    "q": {"type": "string", "description": "人员搜索词"},
    "factory": {"type": "string", "description": "制造基地，如 龙兴工厂"},
    "region": {"type": "string", "description": "区域，如 过程（焊接）"},
    "factory_id": {"type": "integer", "description": "制造基地ID：1=龙兴 2=两江 3=扬帆"},
    "period_type": {"type": "string", "description": "周期粒度：MONTH/QUARTER/HALF_YEAR/YEAR/CUSTOM"},
    "period_start": {"type": "string", "description": "周期起 yyyy-MM-dd，如 2026-06-01"},
    "period_end": {"type": "string", "description": "周期止 yyyy-MM-dd，如 2026-06-30"},
    "clause_ids": {"type": "array", "items": {"type": "string"}, "description": "条款编号列表，如 HJ-GC-02"},
    "assignee_user_id": {"type": "integer", "description": "分派人(被审核人)ID：4=陈志强"},
    "file_path": {"type": "string", "description": "本机文件绝对路径"},
    "material_ids": {"type": "array", "items": {"type": "integer"}, "description": "materialId 列表（若客户端数组校验有 bug，可用单值 material_id 或逗号串）"},
    "material_id": {"type": "integer", "description": "confirm 单个 materialId（与 material_ids 二选一，绕开数组校验问题）"},
    "to_users": {"type": "array", "items": {"type": "string"}, "description": "企微 userid 列表"},
    "template_code": {"type": "string", "description": "模板：MATERIAL_PENDING/REVIEW_PENDING/TASK_FORWARD"},
    "outcome": {"type": "string", "description": "scored/blocked"},
    "blocked_reason": {"type": "string"},
    "notes": {"type": "array", "items": {"type": "string"}},
    "issues": {"type": "array", "items": {"type": "object"},
               "description": "问题列表，键支持中文(问题描述/依据/问题类型/问题得分/引用资料/判定建议/置信度)或英文"},
    "payload": {"type": "object", "description": "createAndDispatch 可直接给 TaskDto 形状对象"},
}

TOOLS = [
    {"name": "audit_meta", "description": "审核元数据/权限（张伟侧）。action 必填："
        "canPublish=能否发布+层级+可选基地; normalize=口语归一(text)->standardValue/candidates; "
        "listFactories=工厂列表; clauseScope=条款树(可选 factory/region); searchPerson=人员搜索(q)。",
     "inputSchema": {"type": "object", "additionalProperties": True, "properties": dict(_COMMON), "required": ["action"]}},
    {"name": "audit_task", "description": '任务(张伟侧发起)。action=createAndDispatch：发起并下发审核任务，创建即自动企微通知被审核人。入参全部必填、无默认：period_type(MONTH/QUARTER/HALF_YEAR/YEAR/CUSTOM)、period_start/period_end(yyyy-MM-dd)、factory_id(1龙兴/2两江/3扬帆)、region(如 过程（焊接），须与条款所属区域一致)、clause_ids[](如 HJ-GC-02)、assignee_user_id(4=陈志强)。推荐用平铺高参；或 payload 传 TaskDto 完整对象，可跑示例：{"period":{"type":"MONTH","start":"2026-06-01","end":"2026-06-30"},"factoryId":1,"regions":[{"region":"过程（焊接）","clauses":[{"clauseId":"HJ-GC-02","assigneeId":4}]}],"dispatchTree":[{"assigneeId":4,"nodeType":"leaf","clauseIds":["HJ-GC-02"]}]}}。DEMO 无历史/复制/基线后端动作。',
     "inputSchema": {"type": "object", "additionalProperties": True, "properties": dict(_COMMON), "required": ["action"]}},
    {"name": "audit_notify", "description": "企微模板通知。action=notifyDispatch：to_users+template_code，"
        "vars 可选。创建任务后的「材料待提交」提醒后端已自动发，一般无需再调。",
     "inputSchema": {"type": "object", "additionalProperties": True, "properties": dict(_COMMON), "required": ["action"]}},
    {"name": "audit_material", "description": "材料(陈志强侧，默认actor=4)。action 必填："
        "listTasks=我名下任务; listPending=待交条款(task_id); previewPull=预览取数(task_id+clause_id); "
        "upload=上传(task_id+clause_id+file_path); confirm=确认提交(task_id+material_ids)。",
     "inputSchema": {"type": "object", "additionalProperties": True, "properties": dict(_COMMON), "required": ["action"]}},
    {"name": "audit_execute", "description": "审核执行（AI/张伟）。action 必填："
        "pullQueue=取待审条款(task_id，材料收齐即 PENDING); getRule=取规则全文(clause_id)；"
        "getMaterial=取材料全文(task_id+clause_id)；writeConclusion=写结论(task_id+clause_id+outcome+issues，"
        "成功即任务→HUMAN_REVIEW并企微通知审核员)；getProgress=查进度(task_id，返回 bipRows 10 列)；"
        "confirmReview=人工复审整表确认(task_id + remove[] + add[] + fieldChanges[]，相对当次 getProgress 基线；"
        "无改动传空即可=按现状落库；成功任务→REVIEWED)。可跑示例：confirmReview 无改动={\"taskId\":232}；"
        "改严重度=[{\"序号\":1,\"字段\":\"严重度（赋分）\",\"from\":6,\"to\":4}]；"
        "删除行={\"taskId\":232,\"remove\":[2]}；新增行={\"taskId\":232,\"add\":[{\"区域\":\"过程（焊接）\",\"项目\":\"质量数据运用\",\"子要素\":\"指标监控分析\",\"条款\":\"过程指标监控\",\"问题描述\":\"人工发现…\",\"严重度（赋分）\":8,\"问题属性\":\"执行类\"}]}。",
     "inputSchema": {"type": "object", "additionalProperties": True, "properties": dict(_COMMON), "required": ["action"]}},
]

TOOL_HANDLERS = {
    "audit_meta": h_meta,
    "audit_task": h_task,
    "audit_notify": h_notify,
    "audit_material": h_material,
    "audit_execute": h_execute,
}


def _log_call(name, args, ok, text):
    try:
        import time as _t
        logp = os.environ.get("WB_MCP_LOG") or os.path.join(os.path.dirname(os.path.abspath(__file__)), "_mcp_calls.log")
        rec = {"ts": _t.strftime("%Y-%m-%d %H:%M:%S"), "tool": name,
               "args": {k: v for k, v in (args or {}).items() if k != "file_path"},
               "ok": ok, "text": (text or "")[:300]}
        with open(logp, "a", encoding="utf-8") as fh:
            fh.write(json.dumps(rec, ensure_ascii=False) + "\n")
    except Exception:
        pass

# ---------------------------------------------------------------- JSON-RPC 分发（stdio/HTTP 共用）
def _payload(id_, result=None, error=None):
    msg = {"jsonrpc": "2.0", "id": id_}
    if error is not None:
        msg["error"] = error
    else:
        msg["result"] = result
    return msg


def process_message(msg):
    """处理一条 JSON-RPC 报文，返回待发送的 dict；通知类返回 None。"""
    method = msg.get("method")
    ident = msg.get("id")
    if method == "initialize" and ident is not None:
        params = msg.get("params") or {}
        return _payload(ident, result={
            "protocolVersion": params.get("protocolVersion") or "2025-06-18",
            "capabilities": {"tools": {}},
            "serverInfo": {"name": SERVER_NAME, "version": SERVER_VERSION},
        })
    if ident is None:
        return None
    if method == "ping":
        return _payload(ident, result={})
    if method == "tools/list":
        return _payload(ident, result={"tools": TOOLS})
    if method == "tools/call":
        params = msg.get("params") or {}
        name = params.get("name")
        args = params.get("arguments") or {}
        handler = TOOL_HANDLERS.get(name)
        if handler is None:
            return _payload(ident, error={"code": -32602, "message": "Unknown tool: %s" % name})
        try:
            ok, text = handler(args)
            _log_call(name, args, ok, text)
            res = {"content": [{"type": "text", "text": text}]}
            if not ok:
                res["isError"] = True
            return _payload(ident, result=res)
        except Exception as e:
            return _payload(ident, result={"content": [{"type": "text", "text": "tool error: %s" % e}],
                                           "isError": True})
    return _payload(ident, error={"code": -32601, "message": "Method not found: %s" % method})


def main():
    try:
        sys.stdin.reconfigure(encoding="utf-8", errors="replace")
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass
    for raw in sys.stdin:
        line = raw.strip()
        if not line:
            continue
        try:
            msg = json.loads(line)
        except Exception:
            continue
        payload = process_message(msg)
        if payload is not None:
            sys.stdout.write(json.dumps(payload, ensure_ascii=False) + "\n")
            sys.stdout.flush()


# ---------------------------------------------------------------- HTTP 模式（streamable HTTP /mcp）
def run_http(port):
    from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

    class Handler(BaseHTTPRequestHandler):
        protocol_version = "HTTP/1.1"

        def log_message(self, *a):
            pass

        def _read_msg(self):
            try:
                length = int(self.headers.get("Content-Length", "0"))
                body = self.rfile.read(length) if length else b"{}"
                return json.loads(body.decode("utf-8"))
            except Exception:
                return None

        def do_POST(self):
            msg = self._read_msg()
            payload = process_message(msg) if msg is not None else None
            data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
            accept = self.headers.get("Accept", "")
            if "text/event-stream" in accept:
                self.send_response(200)
                self.send_header("Content-Type", "text/event-stream; charset=utf-8")
                self.send_header("Cache-Control", "no-cache")
                self.send_header("Connection", "keep-alive")
                self.end_headers()
                self.wfile.write(b"event: message\ndata: " + data + b"\n\n")
            else:
                self.send_response(200)
                self.send_header("Content-Type", "application/json; charset=utf-8")
                self.send_header("Content-Length", str(len(data)))
                self.end_headers()
                self.wfile.write(data)
            self.wfile.flush()

            self.close_connection = True
        def do_GET(self):
            # 按 streamable HTTP：GET 打开服务端->客户端 SSE 流（本服务不主动推送，仅保持连接）
            self.send_response(200)
            self.send_header("Content-Type", "text/event-stream; charset=utf-8")
            self.send_header("Cache-Control", "no-cache")
            self.end_headers()
            try:
                self.wfile.write(b"event: endpoint\ndata: /mcp\n\n")
                self.wfile.flush()
                import time as _t
                while True:
                    _t.sleep(15)
                    self.wfile.write(b": keep-alive\n\n")
                    self.wfile.flush()
            except Exception:
                pass

    srv = ThreadingHTTPServer(("0.0.0.0", int(port)), Handler)
    print("wb-audit-mcp HTTP listening on 0.0.0.0:%s/mcp" % port, flush=True)
    srv.serve_forever()


if __name__ == "__main__":
    if "--http" in sys.argv:
        port = 8090
        for idx, a in enumerate(sys.argv):
            if a == "--port" and idx + 1 < len(sys.argv):
                port = int(sys.argv[idx + 1])
        run_http(port)
    else:
        main()