package com.wb.audit.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 预览取数入参（材料工具 previewPull）
 */
@Data
public class PullPreviewDto {

    /** 任务ID */
    @NotNull(message = "不能为空")
    private Long taskId;

    /** 条款实例ID */
    @NotBlank(message = "不能为空")
    private String clauseId;
}
