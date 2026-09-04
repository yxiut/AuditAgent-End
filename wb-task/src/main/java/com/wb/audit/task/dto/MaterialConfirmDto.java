package com.wb.audit.task.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 确认提交入参（材料工具 confirm）
 */
@Data
public class MaterialConfirmDto {

    /** 任务ID */
    @NotNull(message = "不能为空")
    private Long taskId;

    /** 本轮确认的材料ID列表 */
    @NotEmpty(message = "不能为空")
    private List<Long> materialIds;
}
