package com.wb.audit.audit.dto;

/**
 * 规则文档出参（getRule）
 */
public class RuleVo {

    /** 条款实例ID */
    private String clauseId;

    /** 知识库编号 */
    private String ruleCode;

    /** 规则文档标题 */
    private String ruleTitle;

    /** 版本号 */
    private String version;

    /** 规则全文文本 */
    private String docText;

    public String getClauseId() { return clauseId; }

    public void setClauseId(String clauseId) { this.clauseId = clauseId; }

    public String getRuleCode() { return ruleCode; }

    public void setRuleCode(String ruleCode) { this.ruleCode = ruleCode; }

    public String getRuleTitle() { return ruleTitle; }

    public void setRuleTitle(String ruleTitle) { this.ruleTitle = ruleTitle; }

    public String getVersion() { return version; }

    public void setVersion(String version) { this.version = version; }

    public String getDocText() { return docText; }

    public void setDocText(String docText) { this.docText = docText; }
}