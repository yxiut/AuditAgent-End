-- ============================================================================
-- 演示数据（场景一·发起审核）—— 可重复执行（INSERT IGNORE，按主键/唯一键判重）
-- 前置：先执行 sql/schema.sql + sql/seed.sql
--
-- 写死用户映射：5=张伟(审核员/主)  4=陈志强(被审核人/主)   （历史任务 T100/T101 为张三建/王五收，仅作历史回显）
-- 工厂映射：    1=龙兴工厂     2=两江工厂     3=扬帆工厂
--
-- 本脚本补两条【历史已完成】任务，用于 WorkBuddy「发起审核」的历史回显：
--   T100 龙兴·焊接（2026-07）  ← 命中「龙兴 焊接审核」
--   T101 龙兴·零部件（2026-08）← 最近一次已完成任务
-- 进行中（COLLECTING）任务建议用 POST /api/tasks/create 现场创建，演示创建链路。
-- ============================================================================
USE wb_audit;

-- ============ ① 历史任务 T100：龙兴·焊接（2026-07，已完成） ============
INSERT IGNORE INTO wb_audit_task
  (id, task_no, factory_id, period_type, period_start, period_end, region, owner_id,
   global_state, material_all_collected, create_time, update_time)
VALUES
  (100, 'AUD-202607-LX-001', 1, 'MONTH', '2026-07-01', '2026-07-31', '过程（焊接）', 1,
   'COMPLETED', 1, '2026-07-01 09:00:00', '2026-07-20 18:00:00');

INSERT IGNORE INTO wb_task_clause
  (id, task_id, clause_id, clause_name, region, project, sub_element, assignee_id, node_type,
   state, material_state, audit_state, review_state, version, create_time, update_time)
VALUES
  (1100, 100, 'HJ-GC-01', 'MSA报告正确性', '过程（焊接）', '质量数据采集', '质量监视测量设备配置', 3, 'leaf',
   'COLLECTED', 'COLLECTED', 'CONCLUDED', 'CONFIRMED', 1, '2026-07-01 09:00:00', '2026-07-15 10:00:00'),
  (1101, 100, 'HJ-GC-02', '过程指标监控（整车）', '过程（焊接）', '质量数据运用', '指标监控分析', 3, 'leaf',
   'COLLECTED', 'COLLECTED', 'CONCLUDED', 'CONFIRMED', 1, '2026-07-01 09:00:00', '2026-07-15 10:00:00');

INSERT IGNORE INTO wb_task_assignment
  (id, task_id, clause_id, assigner_id, assignee_id, node_type, assigned_at, create_time, update_time)
VALUES
  (1200, 100, 'HJ-GC-01', 1, 3, 'leaf', '2026-07-01 09:05:00', '2026-07-01 09:05:00', '2026-07-01 09:05:00'),
  (1201, 100, 'HJ-GC-02', 1, 3, 'leaf', '2026-07-01 09:05:00', '2026-07-01 09:05:00', '2026-07-01 09:05:00');

INSERT IGNORE INTO wb_task_node_log
  (id, task_id, clause_id, node_type, from_state, to_state, operator_id, source, detail_json, create_time, update_time)
VALUES
  (1300, 100, NULL, 'TASK',   NULL,        'COLLECTING', 1, 'SYSTEM', NULL, '2026-07-01 09:00:00', '2026-07-01 09:00:00'),
  (1301, 100, 'HJ-GC-01', 'CLAUSE', NULL, 'TO_COLLECT', 1, 'SYSTEM', NULL, '2026-07-01 09:00:00', '2026-07-01 09:00:00'),
  (1302, 100, 'HJ-GC-02', 'CLAUSE', NULL, 'TO_COLLECT', 1, 'SYSTEM', NULL, '2026-07-01 09:00:00', '2026-07-01 09:00:00'),
  (1303, 100, 'HJ-GC-01', 'CLAUSE', 'TO_COLLECT', 'COLLECTED', 3, 'MANUAL', NULL, '2026-07-10 10:00:00', '2026-07-10 10:00:00'),
  (1304, 100, 'HJ-GC-02', 'CLAUSE', 'TO_COLLECT', 'COLLECTED', 3, 'MANUAL', NULL, '2026-07-10 10:00:00', '2026-07-10 10:00:00'),
  (1305, 100, NULL, 'TASK',   'COLLECTING', 'COMPLETED', 1, 'SYSTEM', NULL, '2026-07-20 18:00:00', '2026-07-20 18:00:00');

INSERT IGNORE INTO wb_notify_log
  (id, task_id, template_id, target_user_id, wecom_userid, notify_type, content, msg_id, channel, status, send_result, sent_at, create_time, update_time)
VALUES
  (1400, 100, 1, 3, 'wangwu', 'MATERIAL_PENDING',
   CONCAT('【材料待提交】', CHAR(10), '任务：AUD-202607-LX-001（龙兴工厂 · 2026-07-01 ~ 2026-07-31 · 过程（焊接））', CHAR(10), '你名下 2 条条款待提交材料。', CHAR(10), '请到 WorkBuddy「被审核对象专家」处理。', CHAR(10), '—— 质量体系审核'),
   'mock_demo_100', 'WECOM', 'SENT', 'mock sent', '2026-07-01 09:06:00', '2026-07-01 09:06:00', '2026-07-01 09:06:00');

-- ============ ② 历史任务 T101：龙兴·零部件（2026-08，已完成）—— 全局最近一次 ============
INSERT IGNORE INTO wb_audit_task
  (id, task_no, factory_id, period_type, period_start, period_end, region, owner_id,
   global_state, material_all_collected, create_time, update_time)
VALUES
  (101, 'AUD-202608-LX-001', 1, 'MONTH', '2026-08-01', '2026-08-31', '零部件', 1,
   'COMPLETED', 1, '2026-08-01 09:00:00', '2026-08-18 17:30:00');

INSERT IGNORE INTO wb_task_clause
  (id, task_id, clause_id, clause_name, region, project, sub_element, assignee_id, node_type,
   state, material_state, audit_state, review_state, version, create_time, update_time)
VALUES
  (1110, 101, 'LJ-01', '问题传递及时性', '零部件', '质量数据采集', '质量信息传递', 3, 'leaf',
   'COLLECTED', 'COLLECTED', 'CONCLUDED', 'CONFIRMED', 1, '2026-08-01 09:00:00', '2026-08-10 10:00:00'),
  (1111, 101, 'LJ-02', 'CR记录规范性', '零部件', '质量数据采集', '质量问题记录', 3, 'leaf',
   'COLLECTED', 'COLLECTED', 'CONCLUDED', 'CONFIRMED', 1, '2026-08-01 09:00:00', '2026-08-10 10:00:00');

INSERT IGNORE INTO wb_task_assignment
  (id, task_id, clause_id, assigner_id, assignee_id, node_type, assigned_at, create_time, update_time)
VALUES
  (1210, 101, 'LJ-01', 1, 3, 'leaf', '2026-08-01 09:05:00', '2026-08-01 09:05:00', '2026-08-01 09:05:00'),
  (1211, 101, 'LJ-02', 1, 3, 'leaf', '2026-08-01 09:05:00', '2026-08-01 09:05:00', '2026-08-01 09:05:00');

INSERT IGNORE INTO wb_task_node_log
  (id, task_id, clause_id, node_type, from_state, to_state, operator_id, source, detail_json, create_time, update_time)
VALUES
  (1310, 101, NULL, 'TASK',   NULL,        'COLLECTING', 1, 'SYSTEM', NULL, '2026-08-01 09:00:00', '2026-08-01 09:00:00'),
  (1311, 101, 'LJ-01', 'CLAUSE', NULL, 'TO_COLLECT', 1, 'SYSTEM', NULL, '2026-08-01 09:00:00', '2026-08-01 09:00:00'),
  (1312, 101, 'LJ-02', 'CLAUSE', NULL, 'TO_COLLECT', 1, 'SYSTEM', NULL, '2026-08-01 09:00:00', '2026-08-01 09:00:00'),
  (1313, 101, 'LJ-01', 'CLAUSE', 'TO_COLLECT', 'COLLECTED', 3, 'MANUAL', NULL, '2026-08-08 10:00:00', '2026-08-08 10:00:00'),
  (1314, 101, 'LJ-02', 'CLAUSE', 'TO_COLLECT', 'COLLECTED', 3, 'MANUAL', NULL, '2026-08-08 10:00:00', '2026-08-08 10:00:00'),
  (1315, 101, NULL, 'TASK',   'COLLECTING', 'COMPLETED', 1, 'SYSTEM', NULL, '2026-08-18 17:30:00', '2026-08-18 17:30:00');

INSERT IGNORE INTO wb_notify_log
  (id, task_id, template_id, target_user_id, wecom_userid, notify_type, content, msg_id, channel, status, send_result, sent_at, create_time, update_time)
VALUES
  (1410, 101, 1, 3, 'wangwu', 'MATERIAL_PENDING',
   CONCAT('【材料待提交】', CHAR(10), '任务：AUD-202608-LX-001（龙兴工厂 · 2026-08-01 ~ 2026-08-31 · 零部件）', CHAR(10), '你名下 2 条条款待提交材料。', CHAR(10), '请到 WorkBuddy「被审核对象专家」处理。', CHAR(10), '—— 质量体系审核'),
   'mock_demo_101', 'WECOM', 'SENT', 'mock sent', '2026-08-01 09:06:00', '2026-08-01 09:06:00', '2026-08-01 09:06:00');

-- ============ ③ 补充口语别名（项目/子要素层，演示 /dict/normalize） ============
INSERT IGNORE INTO wb_dict_entity_alias (entity_type, standard_value, alias, create_time, update_time) VALUES
('project', '质量数据采集', '数据采集', NOW(), NOW()),
('project', '质量数据运用', '数据运用', NOW(), NOW()),
('sub_element', '质量信息传递', '信息传递', NOW(), NOW()),
('sub_element', '质量问题记录', '问题记录', NOW(), NOW());

-- ============ ③ 场景二演示任务：龙兴·焊接（2026-08，COLLECTING，分派陈志强） ============
-- 张伟(5) 发起 → 陈志强(4) 提交材料 HJ-GC-03；也可用 POST /api/tasks/create 现场创建。
INSERT IGNORE INTO wb_audit_task
  (id, task_no, factory_id, period_type, period_start, period_end, region, owner_id,
   global_state, material_all_collected, create_time, update_time)
VALUES
  (210, 'AUD-202609-LX-010', 1, 'MONTH', '2026-08-01', '2026-08-31', '过程（焊接）', 5,
   'COLLECTING', 0, '2026-09-01 09:00:00', '2026-09-01 09:00:00');

INSERT IGNORE INTO wb_task_clause
  (id, task_id, clause_id, clause_name, region, project, sub_element, assignee_id, node_type,
   state, material_state, audit_state, review_state, version, create_time, update_time)
VALUES
  (1210, 210, 'HJ-GC-03', '指标监控分析落地', '过程（焊接）', '质量数据运用', '指标监控分析', 4, 'leaf',
   'TO_COLLECT', 'PENDING', 'PENDING', 'PENDING', 1, '2026-09-01 09:00:00', '2026-09-01 09:00:00');

INSERT IGNORE INTO wb_task_assignment
  (id, task_id, clause_id, assigner_id, assignee_id, node_type, assigned_at, create_time, update_time)
VALUES
  (1220, 210, 'HJ-GC-03', 5, 4, 'leaf', '2026-09-01 09:05:00', '2026-09-01 09:05:00', '2026-09-01 09:05:00');

INSERT IGNORE INTO wb_task_node_log
  (id, task_id, clause_id, node_type, from_state, to_state, operator_id, source, detail_json, create_time, update_time)
VALUES
  (1320, 210, NULL, 'TASK',   NULL,        'COLLECTING', 5, 'SYSTEM', NULL, '2026-09-01 09:00:00', '2026-09-01 09:00:00'),
  (1321, 210, 'HJ-GC-03', 'CLAUSE', NULL, 'TO_COLLECT', 5, 'SYSTEM', NULL, '2026-09-01 09:00:00', '2026-09-01 09:00:00');
