package com.wb.audit.task.dto;


import java.util.List;
import java.util.Map;

/**
 * 创建并下发出参
 */
public class TaskVo {

    private Long taskId;
    private String taskNo;
    private String globalState;
    private List<Map<String, String>> clauseStates;

    public Long getTaskId() { return taskId; }

    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public String getTaskNo() { return taskNo; }

    public void setTaskNo(String taskNo) { this.taskNo = taskNo; }

    public String getGlobalState() { return globalState; }

    public void setGlobalState(String globalState) { this.globalState = globalState; }

    public List<Map<String, String>> getClauseStates() { return clauseStates; }

    public void setClauseStates(List<Map<String, String>> clauseStates) { this.clauseStates = clauseStates; }

}

