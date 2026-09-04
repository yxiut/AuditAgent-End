package com.wb.audit.audit.dto;

import java.util.List;
import java.util.Map;

/**
 * 审核进度出参（getProgress，审核员查进度）
 */
public class ProgressVo {

    /** 任务ID */
    private Long taskId;

    /** 任务编号 */
    private String taskNo;

    /** 基地名 */
    private String factoryName;

    /** 周期描述 */
    private String period;

    /** 任务阶段（=globalState，兼容 04 审核监控 phase） */
    private String phase;

    /** 任务全局态 */
    private String globalState;

    /** 条款进度列表 */
    private List<ClauseProgressVo> clauses;

    /** 结果区 BIP 问题管理表行（12 列，见 04 审核监控） */
    private List<Map<String, Object>> bipRows;

    public Long getTaskId() { return taskId; }

    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public String getTaskNo() { return taskNo; }

    public void setTaskNo(String taskNo) { this.taskNo = taskNo; }

    public String getFactoryName() { return factoryName; }

    public void setFactoryName(String factoryName) { this.factoryName = factoryName; }

    public String getPeriod() { return period; }

    public void setPeriod(String period) { this.period = period; }

    public String getPhase() { return phase; }

    public void setPhase(String phase) { this.phase = phase; }
    public String getGlobalState() { return globalState; }

    public void setGlobalState(String globalState) { this.globalState = globalState; }

    public List<Map<String, Object>> getBipRows() { return bipRows; }

    public void setBipRows(List<Map<String, Object>> bipRows) { this.bipRows = bipRows; }
    public List<ClauseProgressVo> getClauses() { return clauses; }

    public void setClauses(List<ClauseProgressVo> clauses) { this.clauses = clauses; }
}