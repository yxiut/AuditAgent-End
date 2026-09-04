package com.wb.audit.audit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * writeConclusion 入参：条款AI审核结论
 */
public class ConclusionWriteDto {

    /** 任务ID */
    @NotNull(message = "taskId 不能为空")
    private Long taskId;

    /** 条款实例ID */
    @NotBlank(message = "clauseId 不能为空")
    private String clauseId;

    /** 结果态：scored/blocked */
    @NotBlank(message = "outcome 不能为空")
    private String outcome;

    /** 阻塞原因（blocked 时必填） */
    private String blockedReason;

    /** 备注（Word 写明跳过的点等） */
    private List<String> notes;

    /** 问题列表（scored 命中才有） */
    private List<IssueWriteDto> issues;

    public Long getTaskId() { return taskId; }

    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public String getClauseId() { return clauseId; }

    public void setClauseId(String clauseId) { this.clauseId = clauseId; }

    public String getOutcome() { return outcome; }

    public void setOutcome(String outcome) { this.outcome = outcome; }

    public String getBlockedReason() { return blockedReason; }

    public void setBlockedReason(String blockedReason) { this.blockedReason = blockedReason; }

    public List<String> getNotes() { return notes; }

    public void setNotes(List<String> notes) { this.notes = notes; }

    public List<IssueWriteDto> getIssues() { return issues; }

    public void setIssues(List<IssueWriteDto> issues) { this.issues = issues; }
}