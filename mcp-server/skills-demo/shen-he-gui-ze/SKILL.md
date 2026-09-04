---
name: shen-he-gui-ze
description: 开始 AI 审核（DEMO·B 方案·审核员专家触发）。材料收齐后，按条款读规则 Word 全文与材料全文，由 WorkBuddy 模型对照判定，产出问题列表写回，任务进入人工复审。用户说「开始 AI 审核」「AI 审一下」「开审」等触发。写结论为强后果，执行前回显确认一次。
version: 0.5.0-demo
agent_created: true
---

# Skill · 开始 AI 审核（DEMO）

**前置**：被审核对象已确认提交材料（任务条款 audit_state=PENDING）。**本 Skill** 由审核员专家在对话里触发（B 方案：客户端模型驱动执行）。

## 0. 后端能力（开工检查）

| 工具 | action | 何时用 |
|---|---|---|
| `audit_execute` | `pullQueue` | 取待审条款（task_id；材料收齐才返回 PENDING 条款） |
| `audit_execute` | `getRule` | 取规则 Word 全文（clause_id） |
| `audit_execute` | `getMaterial` | 取材料全文（task_id + clause_id；系统取数快照 + 上传解析） |
| `audit_execute` | `writeConclusion` | 写回结论（强后果） |

**开工检查**：`pullQueue(task_id)` 拿不到待审条款 → 说明材料未收齐/无待审，请被审核对象先交材料。**规则只在 Word 里**：以 `getRule` 返回全文为准，不抄细则、不用训练记忆补工厂数字。

## 1. 流程

1. 确认要审的任务（会话已锚定则用之；多个让用户选）→ `pullQueue(task_id)` → `clauseIds[]`。
2. 对该条款（DEMO：HJ-GC-02）：
   - `getRule(clause_id)` → 规则 Word **全文**（含 R01~R08 判异规则、门槛、赋分）。
   - `getMaterial(task_id, clause_id)` → 材料**全文**（取数 CSV 快照 + 上传的 FTR 表解析）。
3. **模型对照判定**（客观比对，产出建议结论，不代替审核员终审）：
   - 命中问题 → `outcome=scored` + `issues[]`；
   - 应评材料缺失 → `outcome=blocked` + `blockedReason`（不退补）；
   - 材料在但口径模糊 → 该条 `判定建议=待澄清`，仍 `scored` 交人工。
4. **写回前回显确认**（问题几条 + 判定建议摘要）→ 用户确认后 `writeConclusion(task_id, clause_id, outcome, issues)`。
5. 回执：任务 → **人工复审中（HUMAN_REVIEW）**，张伟已收企微提醒。引导：「说『查进度』查看 AI 结论与 BIP 表」。

## 2. issues[] 键（中英文都认）

`ruleId/rule_id`（对应 Word 审核点短名）、`问题描述/problemDesc`、`依据/evidence`、`问题类型/problemType`、`问题得分/score`(0-10)、`引用资料/refMaterials`、`判定建议/suggestJudgment`(符合/不符合/待澄清)、`置信度/confidence`(高/中/低)。
**不写** `clauseScore`（条款分在人工确认后由后端取已确认问题最低分）。BIP 只落问题描述。

## 3. Few-shot（示意，以 getRule/getMaterial 实际全文为准）

```json
{ "clauseId": "HJ-GC-02", "outcome": "scored", "blockedReason": null, "notes": [],
  "issues": [
    { "ruleId": "FTR连续两月低于目标", "问题描述": "审核龙兴工厂焊接2026年7月过程FTR连续两个可审月低于目标",
      "依据": "月度指标：焊接 2026-06、2026-07 FTR 与目标对照", "问题类型": "执行类", "问题得分": 6,
      "引用资料": ["PULL-月度指标"], "判定建议": "不符合", "置信度": "高" },
    { "ruleId": "对比审核-FTR TOP3问题管理一致性", "问题描述": "FTR不达标触发后，问题管理项目与系统TOP3不一致",
      "依据": "系统TOP3 与 FTR 管理表项目对照", "问题类型": "标准类", "问题得分": 6,
      "引用资料": ["PULL-系统TOP3", "M-FTR"], "判定建议": "不符合", "置信度": "中" }
  ] }
```

## 4. 异常

| 情况 | 处理 |
|---|---|
| 工具未接入 | 「后端能力未接入」，不编造 issues |
| 取不到规则/材料 | outcome=blocked，写原因，不硬编问题 |
| 模型判定不确定 | 判定建议=待澄清，交人工复审，不退补 |