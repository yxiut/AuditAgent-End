package com.wb.audit.task.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 预览取数出参（材料工具 previewPull，不落库）
 */
@Data
public class PullPreviewVo {

    /** 取数规则ID */
    private String ruleId;

    /** 来源文件列表（sim_*.csv） */
    private List<String> files;

    /** 总行数（不含表头） */
    private Integer rows;

    /** 样例行（前 2 行） */
    private List<Map<String, String>> sample;
}