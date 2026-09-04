# skills-demo（演示版 SKILL 副本，挂载到 WorkBuddy 专家的来源）

最新版交付(01/02/03/04)按 DEMO 链路适配后的副本。改这里 → 再同步到
`C:\Users\Administrator\.workbuddy\plugins\marketplaces\my-experts\plugins\<pkg>\skills\`。

| 目录 | 挂到 | 说明 |
|---|---|---|
| fa-qi-shen-he | 审核员专家 shen-he-yuan | 发起（历史/复制=会话上下文，无 Excel 文件改） |
| shen-he-gui-ze | 审核员专家 shen-he-yuan | B 方案：说「开始 AI 审核」→ pullQueue/getRule/getMaterial/writeConclusion |
| shen-he-jian-kong-demo | 审核员专家 shen-he-yuan | 只读查进度（源=04 原样） |
| shang-chuan-cai-liao-demo | 被审核对象专家 bei-shen-he-dui-xiang | 交材料（confirm 后引导切回审核员，不调 runTask）；assets 随包 |

源目录 01/02/03/04 保持不动。