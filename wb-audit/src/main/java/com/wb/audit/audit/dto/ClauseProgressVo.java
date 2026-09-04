package com.wb.audit.audit.dto;

import java.util.List;

/**
 * 进度出参：单条款
 */
public class ClauseProgressVo {

    /** 四级路径：region › project › subElement › clauseName */
    private String path;

    /** 条款实例ID */
    private String clauseId;

    /** 条款名称 */
    private String clauseName;

    /** 审核态 */
    private String auditState;

    /** 复审态 */
    private String reviewState;

    /** 条款分（人工确认后） */
    private Integer clauseScore;

    /** 结论结果态：scored/blocked */
    private String outcome;

    /** 阻塞原因 */
    private String blockedReason;

    /** 问题条数（对话区摘要） */
    private Integer issuesCount;

    /** 条款分建议（已写问题最低分；无问题 scored=10，blocked=null） */
    private Integer suggestedScore;

    /** 问题明细 */
    private List<IssueVo> issues;

    public String getPath() { return path; }

    public void setPath(String path) { this.path = path; }
    public String getClauseId() { return clauseId; }

    public void setClauseId(String clauseId) { this.clauseId = clauseId; }

    public String getClauseName() { return clauseName; }

    public void setClauseName(String clauseName) { this.clauseName = clauseName; }

    public String getAuditState() { return auditState; }

    public void setAuditState(String auditState) { this.auditState = auditState; }

    public String getReviewState() { return reviewState; }

    public void setReviewState(String reviewState) { this.reviewState = reviewState; }

    public Integer getClauseScore() { return clauseScore; }

    public void setClauseScore(Integer clauseScore) { this.clauseScore = clauseScore; }

    public Integer getIssuesCount() { return issuesCount; }

    public void setIssuesCount(Integer issuesCount) { this.issuesCount = issuesCount; }

    public Integer getSuggestedScore() { return suggestedScore; }

    public void setSuggestedScore(Integer suggestedScore) { this.suggestedScore = suggestedScore; }
    public String getOutcome() { return outcome; }

    public void setOutcome(String outcome) { this.outcome = outcome; }

    public String getBlockedReason() { return blockedReason; }

    public void setBlockedReason(String blockedReason) { this.blockedReason = blockedReason; }

    public List<IssueVo> getIssues() { return issues; }

    public void setIssues(List<IssueVo> issues) { this.issues = issues; }
}