package com.wb.audit.audit.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 人工复审整表确认（confirmReview）入参。
 * 结构对齐 shen-he-jian-kong-demo scripts/parse_bip_xlsx.py 的输出：
 *   remove=按基线序号的删除列表；add=新增问题行；fieldChanges=[{序号, 字段, from, to}]。
 */
public class ReviewConfirmDto {

    /** 任务ID */
    @NotNull(message = "taskId 不能为空")
    private Long taskId;

    /** 相对基线删除的行序号（对应 AI 问题置 REJECTED，不再进入 BIP） */
    private List<Integer> remove;

    /** 新增问题行（审核员人工发现的问题，落库后 confirmStatus=CONFIRMED） */
    private List<BipAddRow> add;

    /** 改动 {序号, 字段, from, to}（字段白名单：问题描述 / 严重度（赋分）/ 问题属性） */
    private List<BipChange> fieldChanges;

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }
    public List<Integer> getRemove() { return remove; }
    public void setRemove(List<Integer> remove) { this.remove = remove; }
    public List<BipAddRow> getAdd() { return add; }
    public void setAdd(List<BipAddRow> add) { this.add = add; }
    public List<BipChange> getFieldChanges() { return fieldChanges; }
    public void setFieldChanges(List<BipChange> fieldChanges) { this.fieldChanges = fieldChanges; }

    /** 新增问题行（BIP 10 列中除 时间/制造基地/序号 之外的业务列） */
    public static class BipAddRow {
        @JsonProperty("区域") private String region;
        @JsonProperty("项目") private String project;
        @JsonProperty("子要素") private String subElement;
        /** 条款名称（与任务条款 clause_name 匹配定位） */
        @JsonProperty("条款") private String clause;
        @JsonProperty("问题描述") private String problemDesc;
        @JsonProperty("严重度（赋分）") private Integer score;
        @JsonProperty("问题属性") private String problemType;

        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public String getProject() { return project; }
        public void setProject(String project) { this.project = project; }
        public String getSubElement() { return subElement; }
        public void setSubElement(String subElement) { this.subElement = subElement; }
        public String getClause() { return clause; }
        public void setClause(String clause) { this.clause = clause; }
        public String getProblemDesc() { return problemDesc; }
        public void setProblemDesc(String problemDesc) { this.problemDesc = problemDesc; }
        public Integer getScore() { return score; }
        public void setScore(Integer score) { this.score = score; }
        public String getProblemType() { return problemType; }
        public void setProblemType(String problemType) { this.problemType = problemType; }
    }

    /** 单元格改动 */
    public static class BipChange {
        @JsonProperty("序号") private Integer seq;
        @JsonProperty("字段") private String field;
        @JsonProperty("from") private Object from;
        @JsonProperty("to") private Object to;

        public Integer getSeq() { return seq; }
        public void setSeq(Integer seq) { this.seq = seq; }
        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        public Object getFrom() { return from; }
        public void setFrom(Object from) { this.from = from; }
        public Object getTo() { return to; }
        public void setTo(Object to) { this.to = to; }
    }
}
