USE wb_audit;

CREATE TABLE IF NOT EXISTS wb_dict_clause_tree (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  parent_id   BIGINT DEFAULT 0 COMMENT '父节点ID',
  region      VARCHAR(64)   COMMENT '区域：零部件/过程（焊接）/过程（冲压）',
  project     VARCHAR(64)   COMMENT '项目：质量数据采集/质量数据运用/质量改进/过程问题管理',
  sub_element VARCHAR(64)   COMMENT '子要素：质量信息传递/质量问题记录/质量监视测量设备配置',
  clause_id   VARCHAR(32)   NOT NULL COMMENT '条款实例ID：LJ-01 / HJ-GC-01',
  clause_name VARCHAR(128)  NOT NULL COMMENT '条款名称',
  required_upload_label VARCHAR(255) COMMENT '必传材料标签（如 车间过程FTR问题跟踪管理表）',
  pull_rule_id VARCHAR(32) COMMENT '取数规则ID（如 FTR；空=仅上传）',
  node_type   VARCHAR(16)   NOT NULL COMMENT '节点类型：REGION/PROJECT/SUB_ELEMENT/CLAUSE',
  level_no    INT           COMMENT '层级',
  status      TINYINT DEFAULT 1 COMMENT '状态：1启用 0停用',
  create_time DATETIME      COMMENT '创建时间',
  update_time DATETIME      COMMENT '修改时间',
  UNIQUE KEY uk_clause (clause_id),
  KEY idx_parent (parent_id)
) COMMENT='条款树（四级）';

CREATE TABLE IF NOT EXISTS wb_dict_entity_alias (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  entity_type    VARCHAR(16) NOT NULL COMMENT '实体类型：base/region/project/sub_element/clause',
  standard_value VARCHAR(128) NOT NULL COMMENT '标准值',
  alias          VARCHAR(128) NOT NULL COMMENT '口语别名',
  create_time    DATETIME COMMENT '创建时间',
  update_time    DATETIME COMMENT '修改时间',
  UNIQUE KEY uk_alias (entity_type, alias)
) COMMENT='口语别名表';

CREATE TABLE IF NOT EXISTS wb_audit_task (
  id                    BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  task_no               VARCHAR(64) NOT NULL COMMENT '任务编号：AUD-202608-LX-001',
  factory_id            BIGINT NOT NULL COMMENT '制造基地ID',
  period_type           VARCHAR(16) NOT NULL COMMENT '周期粒度：MONTH/QUARTER/HALF_YEAR/YEAR/CUSTOM',
  period_start          DATE COMMENT '周期起',
  period_end            DATE COMMENT '周期止',
  region                VARCHAR(255) COMMENT '区域集合（多选，逗号分隔）',
  owner_id              BIGINT NOT NULL COMMENT '发布审核员ID（5=张伟）',
  global_state          VARCHAR(32) NOT NULL DEFAULT 'COLLECTING' COMMENT '全局态：COLLECTING/AUDITING/REVIEWING/REPORTING/COMPLETED',
  material_all_collected TINYINT DEFAULT 0 COMMENT '材料是否全部收齐：0否 1是',
  create_time           DATETIME COMMENT '创建时间',
  update_time           DATETIME COMMENT '修改时间',
  UNIQUE KEY uk_task_no (task_no),
  KEY idx_state (global_state),
  KEY idx_factory_period (factory_id, period_start, period_end)
) COMMENT='审核任务';

CREATE TABLE IF NOT EXISTS wb_task_clause (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  task_id        BIGINT NOT NULL COMMENT '任务ID',
  clause_id      VARCHAR(32) NOT NULL COMMENT '条款实例ID',
  clause_name    VARCHAR(128) NOT NULL COMMENT '条款名称',
  region         VARCHAR(64) COMMENT '区域（区域隔离：与分组区域一致）',
  project        VARCHAR(64) COMMENT '项目',
  sub_element    VARCHAR(64) COMMENT '子要素',
  assignee_id    BIGINT COMMENT '当前责任节点ID（5=张伟/4=陈志强）',
  node_type      VARCHAR(16) COMMENT '节点类型：leaf/juror（后端判定写入）',
  state          VARCHAR(32) NOT NULL DEFAULT 'TO_COLLECT' COMMENT '条款态：TO_COLLECT等',
  material_state VARCHAR(32) DEFAULT 'PENDING' COMMENT '材料态：PENDING/COLLECTED/CHANGED',
  audit_state    VARCHAR(32) DEFAULT 'PENDING' COMMENT '审核态：PENDING/AUDITING/CONCLUDED/CLARIFY',
  review_state   VARCHAR(32) DEFAULT 'PENDING' COMMENT '复审态：PENDING/CONFIRMED/REJECTED',
  clause_score   INT COMMENT '条款分（人工确认后=已确认问题最低分，无问题10）',
  version        INT DEFAULT 1 COMMENT '版本号',
  create_time    DATETIME COMMENT '创建时间',
  update_time    DATETIME COMMENT '修改时间',
  UNIQUE KEY uk_task_clause (task_id, clause_id),
  KEY idx_assignee (assignee_id, state)
) COMMENT='任务条款实例';

CREATE TABLE IF NOT EXISTS wb_task_assignment (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  task_id     BIGINT NOT NULL COMMENT '任务ID',
  clause_id   VARCHAR(32) NOT NULL COMMENT '条款实例ID',
  assigner_id BIGINT NOT NULL COMMENT '分派人（审核员）ID',
  assignee_id BIGINT NOT NULL COMMENT '承接人ID',
  node_type   VARCHAR(16) COMMENT '节点类型：leaf/juror',
  assigned_at DATETIME COMMENT '分派时间',
  create_time DATETIME COMMENT '创建时间',
  update_time DATETIME COMMENT '修改时间',
  KEY idx_task (task_id, clause_id)
) COMMENT='分派记录';

CREATE TABLE IF NOT EXISTS wb_task_node_log (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  task_id     BIGINT NOT NULL COMMENT '任务ID',
  clause_id   VARCHAR(32) COMMENT '条款实例ID（任务级为空）',
  node_type   VARCHAR(16) NOT NULL COMMENT '节点类型：TASK/CLAUSE',
  from_state  VARCHAR(32) COMMENT '原状态',
  to_state    VARCHAR(32) COMMENT '新状态',
  operator_id BIGINT COMMENT '操作人ID',
  source      VARCHAR(16) COMMENT '来源：AI/MANUAL/AUTOMATION/SYSTEM',
  detail_json JSON COMMENT '详情',
  create_time DATETIME COMMENT '创建时间',
  update_time DATETIME COMMENT '修改时间',
  KEY idx_task (task_id)
) COMMENT='流程节点记录';

CREATE TABLE IF NOT EXISTS wb_notify_template (
  id               BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  code             VARCHAR(64) NOT NULL UNIQUE COMMENT '模板编码：MATERIAL_PENDING/TASK_FORWARD',
  title            VARCHAR(255) COMMENT '模板标题',
  content          TEXT COMMENT '正文，含占位符 {taskNo}/{factory}/{period}/{region}/{clauseCount}',
  wecom_template_id VARCHAR(128) COMMENT '企微模板ID',
  status           TINYINT DEFAULT 1 COMMENT '状态：1启用 0停用',
  version          INT DEFAULT 1 COMMENT '版本号',
  updated_by       BIGINT COMMENT '更新人ID',
  create_time      DATETIME COMMENT '创建时间',
  update_time      DATETIME COMMENT '修改时间'
) COMMENT='企微通知模板';

CREATE TABLE IF NOT EXISTS wb_notify_log (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  task_id        BIGINT COMMENT '任务ID',
  template_id    BIGINT COMMENT '模板ID',
  target_user_id BIGINT COMMENT '目标用户ID',
  wecom_userid   VARCHAR(64) COMMENT '企微userid（接收人）',
  notify_type    VARCHAR(32) COMMENT '通知类型：MATERIAL_PENDING/TASK_FORWARD等',
  content        TEXT COMMENT '渲染后消息内容',
  msg_id         VARCHAR(255) COMMENT '企微消息msgid（真实msgid较长，255位）',
  channel        VARCHAR(16) DEFAULT 'WECOM' COMMENT '渠道',
  status         VARCHAR(16) DEFAULT 'SENT' COMMENT '发送状态：SENT/FAILED/RETRY',
  send_result    TEXT COMMENT '发送结果',
  sent_at        DATETIME COMMENT '发送时间',
  create_time    DATETIME COMMENT '创建时间',
  update_time    DATETIME COMMENT '修改时间',
  KEY idx_task (task_id)
) COMMENT='通知流水';

CREATE TABLE IF NOT EXISTS wb_sys_config (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  config_key   VARCHAR(64) NOT NULL UNIQUE COMMENT '配置键',
  config_value VARCHAR(512) NOT NULL COMMENT '配置值',
  description  VARCHAR(255) COMMENT '说明',
  create_time  DATETIME COMMENT '创建时间',
  update_time  DATETIME COMMENT '修改时间'
) COMMENT='系统配置';

-- ============ 场景二：提交材料 ============

CREATE TABLE IF NOT EXISTS wb_material (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  task_id     BIGINT NOT NULL COMMENT '任务ID',
  clause_id   VARCHAR(32) NOT NULL COMMENT '条款实例ID',
  file_name   VARCHAR(255) NOT NULL COMMENT '原始文件名',
  file_path   VARCHAR(512) COMMENT '存储路径（本地 uploads/ 下）',
  file_size   BIGINT COMMENT '文件大小(字节)',
  file_type   VARCHAR(32) COMMENT '扩展名/类型：xlsx/pdf/jpg...',
  uploader_id BIGINT NOT NULL COMMENT '上传人ID（陈志强=4）',
  parsed_text MEDIUMTEXT COMMENT '上传文件解析文本（xlsx→文本，规则执行 getMaterial 用）',
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
  full_content MEDIUMTEXT COMMENT '取数文件全文（规则执行 getMaterial 用）',
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

-- ============ 场景三：规则执行（shen-he-gui-ze） ============

CREATE TABLE IF NOT EXISTS wb_rule_doc (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  clause_id   VARCHAR(32) NOT NULL UNIQUE COMMENT '条款实例ID（HJ-GC-02）',
  rule_code   VARCHAR(64) COMMENT '知识库编号（GC-YY-01-01）',
  rule_title  VARCHAR(255) COMMENT '规则文档标题',
  region      VARCHAR(64) COMMENT '适用区域',
  project     VARCHAR(64) COMMENT '项目',
  sub_element VARCHAR(64) COMMENT '子要素',
  version     VARCHAR(32) COMMENT '版本号（V1.0）',
  doc_text    MEDIUMTEXT COMMENT '规则全文文本（docx 转文本，getRule 返回）',
  file_name   VARCHAR(255) COMMENT '原 docx 文件名',
  file_path   VARCHAR(512) COMMENT '原 docx 留档路径',
  status      INT DEFAULT 1 COMMENT '状态：1启用 0停用',
  create_time DATETIME COMMENT '创建时间',
  update_time DATETIME COMMENT '修改时间'
) COMMENT='条款规则文档（getRule 数据源）';

CREATE TABLE IF NOT EXISTS wb_audit_conclusion (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  run_id         VARCHAR(64) COMMENT '执行批次ID（关联 wb_audit_run）',
  task_id        BIGINT NOT NULL COMMENT '任务ID',
  clause_id      VARCHAR(32) NOT NULL COMMENT '条款实例ID',
  outcome        VARCHAR(16) NOT NULL COMMENT '结果态：scored/blocked',
  blocked_reason VARCHAR(500) COMMENT '阻塞原因（blocked 时必填）',
  notes          JSON COMMENT '备注（Word 写明跳过的点等）',
  issue_count    INT DEFAULT 0 COMMENT '问题条数',
  model_raw      JSON COMMENT '模型结论原文（留档）',
  create_time    DATETIME COMMENT '创建时间',
  update_time    DATETIME COMMENT '修改时间',
  KEY idx_task_clause (task_id, clause_id)
) COMMENT='条款AI审核结论';

CREATE TABLE IF NOT EXISTS wb_audit_issue (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  conclusion_id   BIGINT NOT NULL COMMENT '结论ID',
  task_id         BIGINT NOT NULL COMMENT '任务ID',
  clause_id       VARCHAR(32) NOT NULL COMMENT '条款实例ID',
  rule_id         VARCHAR(128) COMMENT '规则ID（Word 审核点短名）',
  problem_desc    VARCHAR(1000) COMMENT '问题描述（进 BIP 问题描述列）',
  evidence        TEXT COMMENT '依据（材料ID/月份/行号，不进 BIP）',
  problem_type    VARCHAR(32) COMMENT '问题类型（标准类/执行类等）',
  score           INT COMMENT '问题得分（0/2/4/6/8/10）',
  ref_materials   JSON COMMENT '引用材料ID列表',
  suggest_judgment VARCHAR(16) COMMENT '判定建议：不符合/待澄清',
  confidence      VARCHAR(8) COMMENT '置信度：高/中/低',
  confirm_status  VARCHAR(16) DEFAULT 'PENDING' COMMENT '人工复审：PENDING/CONFIRMED/REJECTED',
  create_time     DATETIME COMMENT '创建时间',
  update_time     DATETIME COMMENT '修改时间',
  KEY idx_conclusion (conclusion_id),
  KEY idx_task_clause (task_id, clause_id)
) COMMENT='AI审核问题明细（人工复审确认后算条款分）';