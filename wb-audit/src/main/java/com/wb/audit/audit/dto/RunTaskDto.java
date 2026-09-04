package com.wb.audit.audit.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 启动 AI 审核入参（执行工具 runTask）
 */
@Data
public class RunTaskDto {

    /** 任务ID */
    @NotNull(message = "不能为空")
    private Long taskId;
}
