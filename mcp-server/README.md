# WorkBuddy 连接器(MCP) -> 审核后端 接入说明（v2 · 5 聚合工具 · stdio + HTTP 双模式）

## 两种连接模式
- **stdio（本机/有 Python）**：客户端用 command 拉起本 py。
- **HTTP（服务机常驻 / 对端零安装）**：`python wb_audit_mcp.py --http 8090` → 监听 `0.0.0.0:8090/mcp`，对端 WorkBuddy 连接器用
  `{"type":"streamableHttp","url":"http://<S_IP>:8090/mcp"}`，不装 Python、不装本文件。

## 目录/文件清单（本目录真实内容）
- `wb_audit_mcp.py` —— MCP Server（零第三方依赖；默认 stdio；`--http [--port 8090]` 启 HTTP 模式）
- `workbuddy-user-mcp.json` —— 用户级 mcp.json 样例（stdio 版）
- `test_mcp_handshake.py` / `test_http_mode.py` —— 本地自测脚本
- `execution/shen-he-gui-ze/` —— 03 规则执行副本（getRule 后端规则为空时回退读 docx）
- `skills-demo/` —— 按 DEMO 链路适配的 4 个 SKILL 副本（源 01/02/03/04 未动）
- `README.md` —— 本说明

## 5 个聚合工具（工具内 action 分发）
| 工具 | actions |
|---|---|
| audit_meta | canPublish / normalize / listFactories / clauseScope / searchPerson |
| audit_task | createAndDispatch（历史/复制/基线=会话上下文，DEMO 无后端动作） |
| audit_notify | notifyDispatch（创建后提醒后端自动发，留作显式催交/通知） |
| audit_material | listTasks / listPending / previewPull / upload / confirm |
| audit_execute | pullQueue / getRule / getMaterial / writeConclusion / getProgress |

## 服务机 S 需保持运行
1. 后端 `:8080`（java -jar wb-app-0.0.1-SNAPSHOT.jar；含 MySQL wb_audit）
2. MCP 适配器 `:8090/mcp`：`python wb_audit_mcp.py --http 8090`
3. Windows 防火墙放行 8080、8090；企微可信 IP = S 公网出口 IP。

## DEMO 跑通
① 审核员专家「发起…审核」→ ② 被审核对象专家「提交材料」(拖 FTR xlsx→确认)
→ ③ 审核员专家「开始 AI 审核」(getRule→getMaterial→writeConclusion→HUMAN_REVIEW+企微)
→ ④ 审核员专家「查进度」→ 对话区摘要 + 结果区 BIP 2 行。
## 周期/必填约束（2026-09-03 修复）
- createAndDispatch 不再静默默认：period_type/period_start/period_end/factory_id/region/clause_ids[]/assignee_user_id 缺一即报错（防“用户填6月被默认成7月”）。
- getMaterial/writeConclusion 的 clause_id 必填（取 pullQueue 返回的条款）。
- 每次 tools/call 写入 _mcp_calls.log（同目录，含 args/结果），便于排查。