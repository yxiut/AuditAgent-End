package com.wb.audit.audit.dto;

import java.util.List;

/**
 * writeConclusion 入参：单条问题
 */
public class IssueWriteDto {

    /** 规则ID（Word 审核点短名） */
    private String ruleId;

    /** 问题描述（进 BIP 问题描述列） */
    private String problemDesc;

    /** 依据（材料ID/月份/行号，不进 BIP） */
    private String evidence;

    /** 问题类型：标准类/其他/设备工装类/设计工艺类/执行类/综合类 */
    private String problemType;

    /** 问题得分：0/2/4/6/8/10 */
    private Integer score;

    /** 引用材料ID列表 */
    private List<String> refMaterials;

    /** 判定建议：不符合/待澄清 */
    private String suggestJudgment;

    /** 置信度：高/中/低 */
    private String confidence;

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
}