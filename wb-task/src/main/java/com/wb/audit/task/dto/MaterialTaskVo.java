package com.wb.audit.task.dto;

import lombok.Data;

/**
 * 待办任务 Vo（材料工具 listTasks）
 */
@Data
public class MaterialTaskVo {

    /** 任务ID */
    private Long taskId;

    /** 任务编号 */
    private String taskNo;

    /** 制造基地名 */
    private String factoryName;

    /** 周期描述 */
    private String period;

    /** 区域集合 */
    private String region;

    /** 全局态 */
    private String globalState;

    /** 当前登录人待交条款数 */
    private Integer pendingCount;
}