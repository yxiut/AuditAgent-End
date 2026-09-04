package com.wb.audit.audit.dto;

import lombok.Data;

/**
 * 启动 AI 审核出参（执行工具 runTask）
 */
@Data
public class RunTaskVo {

    /** 执行批次ID */
    private String runId;

    /** 任务ID */
    private Long taskId;

    /** 任务态：AUDITING */
    private String state;
}