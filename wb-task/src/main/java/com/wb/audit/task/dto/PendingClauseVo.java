package com.wb.audit.task.dto;

import lombok.Data;

import java.util.List;

/**
 * 待交条款 Vo（材料工具 listPending）
 */
@Data
public class PendingClauseVo {

    /** 条款实例ID */
    private String clauseId;

    /** 条款名称 */
    private String clauseName;

    /** 区域 */
    private String region;

    /** 项目 */
    private String project;

    /** 子要素 */
    private String subElement;

    /** 条款态：TO_COLLECT */
    private String state;

    /** 必传材料标签（如 车间过程FTR问题跟踪管理表） */
    private List<String> requiredUploadLabels;

    /** 取数规则ID（如 FTR；空=仅上传） */
    private String pullRuleId;
}