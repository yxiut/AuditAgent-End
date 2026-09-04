---
name: fa-qi-shen-he
description: 发起审核（DEMO·审核员专用）。从用户原话取周期/基地/区域等槽位，回显范围与分派概要，支持多轮对话改（基地/周期/区域/条款/分派人），确认后创建任务并下发（企微提醒后端自动发）。用户说「发起审核」「再来一次」「发起龙兴审核」等触发。历史/复制由会话上下文承接，不走后端。
version: 1.1.0-demo
agent_created: true
---

# Skill · 发起审核（DEMO）

**只负责到「创建并下发」。** 交材料→被审核对象专家；AI 审核→在本专家说「开始 AI 审核」（shen-he-gui-ze）；查进度→shen-he-jian-kong-demo。本 Skill 不代它们。

## 0. 后端能力（开工检查）

| 工具 | action | 何时用 |
|---|---|---|
| `audit_meta` | `canPublish` | 开工检查：能否发布/组织层级/可选基地（会话内缓存一次） |
| `audit_meta` | `listFactories` / `clauseScope` / `normalize` / `searchPerson` | 列基地 / 取条款树 / 口语归一 / 查分派人 |
| `audit_task` | `createAndDispatch` | 确认创建并下发（强后果） |

**开工检查**：先调 `audit_meta(action=canPublish)`；不可用 → 明说「后端能力未接入」，**不得**编造基地、条款、人员、任务。DEMO 无历史/复制/基线接口：说「再来一次 / 照上次」→ 用**本会话最近一次创建/回显的任务**范围与分派，不调后端。

## 1. DEMO 常量（默认，用户没说到就用）

| 槽位 | 默认 | 可改 |
|---|---|---|
| 周期 | 2026-07（MONTH） | 用户说哪月用哪月 |
| 基地 | 龙兴工厂（factory_id=1） | 两江/扬帆（`listFactories` 为准） |
| 区域 | 过程（焊接） | `clauseScope(region=…)` 为准 |
| 条款 | HJ-GC-02 | `clauseScope` 条款树选择 |
| 分派人（被审核人） | 陈志强（user_id=4） | `searchPerson` 查 |

## 2. 流程

1. **抽槽**：从原话抽 周期/基地/区域/条款/分派人；没说到的不猜，用 §1 默认值或反问。
2. **回显概要**：`基地 | 周期 | 区域 | 条款 | 分派人`（表格）。
3. **多轮改（对话改，本 DEMO 不用 Excel 文件）**：改基地/周期/区域/条款/分派人 → 每次改完重回显概要。
4. **确认**：用户确认后调 `audit_task(action=createAndDispatch)`（入参见 MCP schema；也可直接给高参 clause_ids/assignee_user_id/region/factory_id/period_*）。
5. **回执**：`taskId / taskNo / 阶段=COLLECTING`；企微「材料待提交」已自动发给被审核人（**无需再调 audit_notify**）。
6. **引导**：下一步「请切到被审核对象专家提交材料」。

## 3. 铁律

- 数据来自工具返回，四类（基地/条款/人员/任务）**绝不**用推测值顶替。
- `audit_meta(action=canPublish)` 无发布权 → 话术「不能发布审核任务。提交材料请进入被审核对象专家。」
- 创建是强后果：回显后**确认一次**才执行；执行失败如实回显后端 message。

## 4. Few-shot

> 用户：「发起一次龙兴 2026-07 焊接过程 HJ-GC-02 审核」→ canPublish ✅ → 回显 `龙兴工厂 | 2026-07 | 过程（焊接） | HJ-GC-02 | 陈志强` → 用户「确认」→ createAndDispatch → `taskId=… 已创建并下发，陈志强将收到材料待提交提醒`。