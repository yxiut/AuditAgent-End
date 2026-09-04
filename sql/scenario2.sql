-- ============================================================================
-- 场景二（提交材料）增量脚本 —— 已有库执行
-- 说明：wb_dict_clause_tree 加两列；新建 3 张表；补 HJ-GC-03 演示条款。
--       ALTER 若报「Duplicate column」说明已执行过，可忽略（用 mysql --force 跑）。
-- ============================================================================
USE wb_audit;

ALTER TABLE wb_dict_clause_tree
  ADD COLUMN required_upload_label VARCHAR(255) COMMENT '必传材料标签（如 车间过程FTR问题跟踪管理表）' AFTER clause_name,
  ADD COLUMN pull_rule_id VARCHAR(32) COMMENT '取数规则ID（如 FTR；空=仅上传）' AFTER required_upload_label;

CREATE TABLE IF NOT EXISTS wb_material (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  task_id     BIGINT NOT NULL COMMENT '任务ID',
  clause_id   VARCHAR(32) NOT NULL COMMENT '条款实例ID',
  file_name   VARCHAR(255) NOT NULL COMMENT '原始文件名',
  file_path   VARCHAR(512) COMMENT '存储路径（本地 uploads/ 下）',
  file_size   BIGINT COMMENT '文件大小(字节)',
  file_type   VARCHAR(32) COMMENT '扩展名/类型：xlsx/pdf/jpg...',
  uploader_id BIGINT NOT NULL COMMENT '上传人ID（陈志强=4）',
  state       VARCHAR(16) NOT NULL DEFAULT 'UPLOADED' COMMENT '材料态：UPLOADED/CONFIRMED',
  create_time DATETIME COMMENT '创建时间',
  update_time DATETIME COMMENT '修改时间',
  KEY idx_task_clause (task_id, clause_id)
) COMMENT='上传材料';

CREATE TABLE IF NOT EXISTS wb_data_pull (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  task_id      BIGINT NOT NULL COMMENT '任务ID',
  clause_id    VARCHAR(32) COMMENT '条款实例ID',
  rule_id      VARCHAR(32) NOT NULL COMMENT '取数规则ID：FTR等',
  file_name    VARCHAR(128) COMMENT '来源文件（sim_*.csv）',
  row_count    INT COMMENT '行数',
  summary_json JSON COMMENT '取数摘要（demo：样例行/计数，不存明细）',
  pulled_at    DATETIME COMMENT '取数时间',
  create_time  DATETIME COMMENT '创建时间',
  update_time  DATETIME COMMENT '修改时间',
  KEY idx_task (task_id)
) COMMENT='系统取数记录';

CREATE TABLE IF NOT EXISTS wb_audit_run (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  run_id      VARCHAR(64) NOT NULL UNIQUE COMMENT '执行批次ID：RUN-YYYYMMDD-XXXX',
  task_id     BIGINT NOT NULL COMMENT '任务ID',
  status      VARCHAR(16) NOT NULL DEFAULT 'AUDITING' COMMENT '状态：AUDITING等',
  started_at  DATETIME COMMENT '启动时间',
  create_time DATETIME COMMENT '创建时间',
  update_time DATETIME COMMENT '修改时间',
  KEY idx_task (task_id)
) COMMENT='AI审核执行记录';

-- 演示条款：HJ-GC-03 指标监控分析落地（贴合 SKILL 话术；uk_clause 冲突则跳过）
INSERT IGNORE INTO wb_dict_clause_tree
  (region, project, sub_element, clause_id, clause_name, node_type, level_no, status, required_upload_label, pull_rule_id)
VALUES
  ('过程（焊接）', '质量数据运用', '指标监控分析', 'HJ-GC-03', '指标监控分析落地', 'CLAUSE', 4, 1,
   '车间过程FTR问题跟踪管理表', 'FTR');