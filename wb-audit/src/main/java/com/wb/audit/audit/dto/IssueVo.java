package com.wb.audit.audit.dto;

import java.util.List;

/**
 * 问题明细出参（progress/人工确认用）
 */
public class IssueVo {

    /** 问题ID */
    private Long id;

    /** 规则ID */
    private String ruleId;

    /** 问题描述 */
    private String problemDesc;

    /** 依据 */
    private String evidence;

    /** 问题类型 */
    private String problemType;

    /** 问题得分 */
    private Integer score;

    /** 引用材料ID列表 */
    private List<String> refMaterials;

    /** 判定建议 */
    private String suggestJudgment;

    /** 置信度 */
    private String confidence;

    /** 人工复审态 */
    private String confirmStatus;

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public String getRuleId() { return ruleId; }

    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public String getProblemDesc() { return problemDesc; }

    public void setProblemDesc(String problemDesc) { this.problemDesc = problemDesc; }

    public String getEvidence() { return evidence; }

    public void setEvidence(String evidence) { this.evidence = evidence; }

    public String getProblemType() { return problemType; }

    public void setProblemType(String problemType) { this.problemType = problemType; }

    public Integer getScore() { return score; }

    public void setScore(Integer score) { this.score = score; }

    public List<String> getRefMaterials() { return refMaterials; }

    public void setRefMaterials(List<String> refMaterials) { this.refMaterials = refMaterials; }

    public String getSuggestJudgment() { return suggestJudgment; }

    public void setSuggestJudgment(String suggestJudgment) { this.suggestJudgment = suggestJudgment; }

    public String getConfidence() { return confidence; }

    public void setConfidence(String confidence) { this.confidence = confidence; }

    public String getConfirmStatus() { return confirmStatus; }

    public void setConfirmStatus(String confirmStatus) { this.confirmStatus = confirmStatus; }
}