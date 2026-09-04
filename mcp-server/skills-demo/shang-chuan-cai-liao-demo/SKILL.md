---
name: shang-chuan-cai-liao-demo
description: 提交材料（DEMO·被审核对象专用）。查我名下任务→查待交条款→上传证据并确认→收齐后引导切回审核员专家发起 AI 审核。用户说「提交材料」「交材料」「上传」等触发。
version: 0.6.0-demo
agent_created: false
---

# Skill · 提交材料（DEMO）

**只负责**：被审核人（陈志强）交齐当前任务材料并确认。**不查结论**（查结论→审核监控）；**不调 runTask**（AI 审核由审核员专家说「开始 AI 审核」触发）。

## 0. 后端能力（开工检查）

| 工具 | action | 何时用 |
|---|---|---|
| `audit_material` | `listTasks` | 我名下任务（倒序） |
| `audit_material` | `listPending` | 某任务待交条款 |
| `audit_material` | `upload` | 上传证据文件（强后果） |
| `audit_material` | `confirm` | 确认提交（不可逆，二次确认） |

**开工检查**：先调 `audit_material(action=listTasks)`；不可用 → 「后端能力未接入」，不编造任务/条款。

## 1. 随包样例

`assets/模拟系统数据/车间过程FTR问题跟踪管理表.xlsx` = 演示上传样例；`assets/模拟系统数据/sim_*.csv` 为系统取数模拟（由后端 confirm 内取数，**本 Skill 不读不展示**）。

## 2. 流程（DEMO 常态 1 任务 × 1 条款）

1. `listTasks` → 只有 1 个直接进入；多个用【单选清单】让用户选 → 锚定 `task_id`。
2. `listPending(task_id)` → 表格回显待交条款（HJ-GC-02 / 可上传）。
3. 要文件：请用户**在对话里拖入文件**（客户端会给本地绝对路径 `file_path`）；用户没附文件且演示需要，可提示用随包样例（路径由客户端提供，**不臆造路径**）。
4. `upload(task_id, clause_id, file_path)` → 回显 `materialId` + 归类条款。
5. 用户确认后 `confirm(task_id, material_ids=[materialId])`（不可逆，**强制二次确认**）。系统取数在 confirm 内由后端完成，不展示。
6. **收齐引导**：回显「材料已收齐 ✅，请切回**审核员专家**说『**开始 AI 审核**』」。（不调 audit_execute.runTask）

## 3. 铁律

- 只处理自己名下材料；识别到审核/结论/报告意图 → 引导找审核员专家。
- 上传回显后确认；confirm 不可逆二次确认。
- 后端错误如实回显，不假装成功。

## 4. Few-shot

> 用户：「提交材料」→ listTasks=1 条 → listPending → 用户拖入 FTR xlsx → upload → `materialId=…` → 用户「确认」→ confirm ✅ → 「材料已收齐，请切回审核员专家说『开始 AI 审核』」。