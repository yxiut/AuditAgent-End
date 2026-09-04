package com.wb.audit.task.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 确认提交出参（材料工具 confirm）
 */
@Data
public class ConfirmVo {

    /** 是否全部收齐（true → 可启动 runTask） */
    private Boolean allCollected;

    /** 条款态列表 */
    private List<Map<String, String>> clauseStates;

    /** 取数落库结果 */
    private List<Map<String, Object>> pulled;
}