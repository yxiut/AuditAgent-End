-- 简单增删改查 SQL 示例（9 张表，表名 wb_ 前缀）
USE wb_audit;

-- ============ wb_dict_clause_tree ============
SELECT * FROM wb_dict_clause_tree WHERE id = 1;
SELECT * FROM wb_dict_clause_tree;
INSERT INTO wb_dict_clause_tree (parent_id, region, project, sub_element, clause_id, clause_name, node_type, level_no, status, create_time, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
UPDATE wb_dict_clause_tree SET update_time = NOW() WHERE id = 1;
DELETE FROM wb_dict_clause_tree WHERE id = 1;

-- ============ wb_dict_entity_alias ============
SELECT * FROM wb_dict_entity_alias WHERE id = 1;
SELECT * FROM wb_dict_entity_alias;
INSERT INTO wb_dict_entity_alias (entity_type, standard_value, alias, create_time, update_time) VALUES (?, ?, ?, ?, ?);
UPDATE wb_dict_entity_alias SET update_time = NOW() WHERE id = 1;
DELETE FROM wb_dict_entity_alias WHERE id = 1;

-- ============ wb_audit_task ============
SELECT * FROM wb_audit_task WHERE id = 1;
SELECT * FROM wb_audit_task;
INSERT INTO wb_audit_task (task_no, factory_id, period_type, period_start, period_end, region, owner_id, global_state, material_all_collected, create_time, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
UPDATE wb_audit_task SET update_time = NOW() WHERE id = 1;
DELETE FROM wb_audit_task WHERE id = 1;

-- ============ wb_task_clause ============
SELECT * FROM wb_task_clause WHERE id = 1;
SELECT * FROM wb_task_clause;
INSERT INTO wb_task_clause (task_id, clause_id, clause_name, region, project, sub_element, assignee_id, node_type, state, material_state, audit_state, review_state, version, create_time, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
UPDATE wb_task_clause SET update_time = NOW() WHERE id = 1;
DELETE FROM wb_task_clause WHERE id = 1;

-- ============ wb_task_assignment ============
SELECT * FROM wb_task_assignment WHERE id = 1;
SELECT * FROM wb_task_assignment;
INSERT INTO wb_task_assignment (task_id, clause_id, assigner_id, assignee_id, node_type, assigned_at, create_time, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?);
UPDATE wb_task_assignment SET update_time = NOW() WHERE id = 1;
DELETE FROM wb_task_assignment WHERE id = 1;

-- ============ wb_task_node_log ============
SELECT * FROM wb_task_node_log WHERE id = 1;
SELECT * FROM wb_task_node_log;
INSERT INTO wb_task_node_log (task_id, clause_id, node_type, from_state, to_state, operator_id, source, detail_json, create_time, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
UPDATE wb_task_node_log SET update_time = NOW() WHERE id = 1;
DELETE FROM wb_task_node_log WHERE id = 1;

-- ============ wb_notify_template ============
SELECT * FROM wb_notify_template WHERE id = 1;
SELECT * FROM wb_notify_template;
INSERT INTO wb_notify_template (code, title, content, wecom_template_id, status, version, updated_by, create_time, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
UPDATE wb_notify_template SET update_time = NOW() WHERE id = 1;
DELETE FROM wb_notify_template WHERE id = 1;

-- ============ wb_notify_log ============
SELECT * FROM wb_notify_log WHERE id = 1;
SELECT * FROM wb_notify_log;
INSERT INTO wb_notify_log (task_id, template_id, target_user_id, wecom_userid, notify_type, content, msg_id, channel, status, send_result, sent_at, create_time, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
UPDATE wb_notify_log SET update_time = NOW() WHERE id = 1;
DELETE FROM wb_notify_log WHERE id = 1;

-- ============ wb_sys_config ============
SELECT * FROM wb_sys_config WHERE id = 1;
SELECT * FROM wb_sys_config;
INSERT INTO wb_sys_config (config_key, config_value, description, create_time, update_time) VALUES (?, ?, ?, ?, ?);
UPDATE wb_sys_config SET update_time = NOW() WHERE id = 1;
DELETE FROM wb_sys_config WHERE id = 1;
