# 执行侧 · shen-he-gui-ze（03 规则执行）

- 本 Skill **不挂专家、无对话**，由 `audit_execute` 的 runTask/getRule/getMaterial/writeConclusion 在执行侧使用。
- `references/规则文档/*.docx` 是 DEMO 规则 Word 拷贝：MCP `getRule(HJ-GC-02)` 读此文件转全文文本喂模型。
- 材料由第 2 步 confirm 落库，`getMaterial(HJ-GC-02)` 从后端取（见 mcp-server 契约）。
- 来源：E:\ai\workBuddy-audit-agent\03_规则执行_shen-he-gui-ze（最新版交付，勿直接改源目录）。