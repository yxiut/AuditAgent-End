USE wb_audit;

INSERT INTO wb_dict_clause_tree (region, project, sub_element, clause_id, clause_name, node_type, level_no, status) VALUES
('零部件','质量数据采集','质量信息传递','LJ-01','问题传递及时性','CLAUSE',4,1),
('零部件','质量数据采集','质量问题记录','LJ-02','CR记录规范性','CLAUSE',4,1),
('零部件','质量数据采集','质量问题记录','LJ-03','QR升级规范性','CLAUSE',4,1),
('零部件','质量数据运用','指标统计','LJ-04','零部件PPM统计','CLAUSE',4,1),
('零部件','质量数据运用','监控分析','LJ-05','PPM监控改进','CLAUSE',4,1),
('过程（焊接）','质量数据采集','质量监视测量设备配置','HJ-GC-01','MSA报告正确性','CLAUSE',4,1),
('过程（焊接）','质量数据运用','指标监控分析','HJ-GC-02','过程指标监控（整车）','CLAUSE',4,1),
('过程（冲压）','质量数据采集','质量监视测量设备配置','CY-GC-01','MSA报告正确性','CLAUSE',4,1),
('过程（冲压）','质量数据运用','指标监控分析','CY-GC-02','过程指标监控（整车）','CLAUSE',4,1),
('过程（焊接）','质量数据运用','指标监控分析','HJ-GC-03','指标监控分析落地','CLAUSE',4,1);

INSERT INTO wb_dict_entity_alias (entity_type, standard_value, alias) VALUES
('base','龙兴工厂','龙兴'),('base','龙兴工厂','龙兴工厂'),('base','两江工厂','两江'),('base','扬帆工厂','扬帆'),
('region','过程（焊接）','焊接'),('region','过程（焊接）','焊装'),('region','零部件','部品'),('region','过程（冲压）','冲压');

INSERT INTO wb_notify_template (code, title, content, wecom_template_id, status, version) VALUES
('MATERIAL_PENDING','材料待提交',CONCAT('【材料待提交】',CHAR(10),'任务：{taskNo}（{factory} · {period} · {region}）',CHAR(10),'你名下 {clauseCount} 条条款待提交材料。',CHAR(10),'请到 WorkBuddy「被审核对象专家」处理。',CHAR(10),'—— 质量体系审核'),'TPL_MATERIAL_PENDING',1,1),
('TASK_FORWARD','审核任务待转交',CONCAT('【审核任务待转交】',CHAR(10),'任务：{taskNo}（{factory} · {period}）',CHAR(10),'你名下 {clauseCount} 条条款待继续派发给被审核对象。',CHAR(10),'请到 WorkBuddy「审核员专家」使用「分派任务」Skill 继续往下派。',CHAR(10),'—— 质量体系审核'),'TPL_TASK_FORWARD',1,1);

INSERT INTO wb_sys_config (config_key, config_value, description) VALUES
('wecom.corpid','ww909f0f8c511a64ec','企业ID（真实）'),
('wecom.agentid','1000002','应用ID（真实）'),
('wecom.secret','HhBelNT4tEp1qX-H_1xGa_1dwipFMvIAMHdMdeqEXQc','应用密钥（真实）'),
('wecom.mock','0','1=模拟发送，0=真实企微'),
('wecom.token','pi8qSUTKf7KhoATI1djQ','接收消息Token'),
('wecom.aes_key','JWTDqgUe066jc4a8FzVuMOJDw1qLT0EdfAzAuqMFwPv','接收消息EncodingAESKey（43位）');

-- HJ-GC-03 必传标签与取数规则（场景二）
UPDATE wb_dict_clause_tree SET required_upload_label='车间过程FTR问题跟踪管理表', pull_rule_id='FTR' WHERE clause_id='HJ-GC-03';
