package com.wb.audit.audit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 人工复审确认入参
 */
public class IssueConfirmDto {

    /** 任务ID */
    @NotNull(message = "taskId 不能为空")
    private Long taskId;

    /** 条款实例ID */
    @NotBlank(message = "clauseId 不能为空")
    private String clauseId;

    /** 问题ID列表（为空=该条款无问题，直接确认条款） */
    private List<Long> issueIds;

    /** true=确认 false=驳回 */
    @NotNull(message = "confirmed 不能为空")
    private Boolean confirmed;

    public Long getTaskId() { return taskId; }

    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public String getClauseId() { return clauseId; }

    public void setClauseId(String clauseId) { this.clauseId = clauseId; }

    public List<Long> getIssueIds() { return issueIds; }

    public void setIssueIds(List<Long> issueIds) { this.issueIds = issueIds; }

    public Boolean getConfirmed() { return confirmed; }

    public void setConfirmed(Boolean confirmed) { this.confirmed = confirmed; }
}