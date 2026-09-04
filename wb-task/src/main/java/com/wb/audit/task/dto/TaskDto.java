package com.wb.audit.task.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

/**
 * 创建并下发入参 DTO（区域→条款分组，区域隔离）
 */
public class TaskDto {

    /** 周期 */
    @NotNull(message = "周期不能为空")
    @Valid
    private Period period;

    /** 制造基地ID */
    @NotNull(message = "基地不能为空")
    private Long factoryId;

    /** 区域→条款分组：每个区域带自己的条款，互相隔离 */
    @NotEmpty(message = "区域不能为空")
    @Valid
    private List<RegionClauses> regions;

    /** 分派树（nodeType 以后端判定为准，不信任前端） */
    @NotEmpty(message = "分派树不能为空")
    @Valid
    private List<DispatchEntry> dispatchTree;

    public Period getPeriod() { return period; }
    public void setPeriod(Period period) { this.period = period; }

    public Long getFactoryId() { return factoryId; }
    public void setFactoryId(Long factoryId) { this.factoryId = factoryId; }

    public List<RegionClauses> getRegions() { return regions; }
    public void setRegions(List<RegionClauses> regions) { this.regions = regions; }

    public List<DispatchEntry> getDispatchTree() { return dispatchTree; }
    public void setDispatchTree(List<DispatchEntry> dispatchTree) { this.dispatchTree = dispatchTree; }

    /**
     * 周期
     */
    public static class Period {
        /** 周期粒度：MONTH/QUARTER/HALF_YEAR/YEAR/CUSTOM */
        @NotNull(message = "周期粒度不能为空")
        private String type;
        /** 周期起 */
        @NotNull(message = "周期起不能为空")
        private LocalDate start;
        /** 周期止 */
        @NotNull(message = "周期止不能为空")
        private LocalDate end;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public LocalDate getStart() { return start; }
        public void setStart(LocalDate start) { this.start = start; }

        public LocalDate getEnd() { return end; }
        public void setEnd(LocalDate end) { this.end = end; }
    }

    /**
     * 区域→条款分组
     */
    public static class RegionClauses {
        /** 区域 */
        @NotNull(message = "区域不能为空")
        private String region;
        /** 本区域条款 */
        @NotEmpty(message = "区域条款不能为空")
        @Valid
        private List<ClauseAssign> clauses;

        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }

        public List<ClauseAssign> getClauses() { return clauses; }
        public void setClauses(List<ClauseAssign> clauses) { this.clauses = clauses; }
    }

    /**
     * 条款分派
     */
    public static class ClauseAssign {
        /** 条款实例ID */
        @NotNull(message = "条款ID不能为空")
        private String clauseId;
        /** 分派人ID */
        @NotNull(message = "分派人不能为空")
        private Long assigneeId;

        public String getClauseId() { return clauseId; }
        public void setClauseId(String clauseId) { this.clauseId = clauseId; }

        public Long getAssigneeId() { return assigneeId; }
        public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    }

    /**
     * 分派树节点
     */
    public static class DispatchEntry {
        /** 分派人ID */
        @NotNull(message = "分派人不能为空")
        private Long assigneeId;
        /** 节点类型（后端判定为准） */
        private String nodeType;
        /** 条款列表 */
        @NotEmpty(message = "条款列表不能为空")
        private List<String> clauseIds;

        public Long getAssigneeId() { return assigneeId; }
        public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }

        public String getNodeType() { return nodeType; }
        public void setNodeType(String nodeType) { this.nodeType = nodeType; }

        public List<String> getClauseIds() { return clauseIds; }
        public void setClauseIds(List<String> clauseIds) { this.clauseIds = clauseIds; }
    }
}
