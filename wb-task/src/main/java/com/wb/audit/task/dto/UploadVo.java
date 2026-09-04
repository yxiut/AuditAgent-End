package com.wb.audit.task.dto;

import lombok.Data;

/**
 * 上传材料出参（材料工具 upload）
 */
@Data
public class UploadVo {

    /** 材料ID */
    private Long materialId;

    /** 任务ID */
    private Long taskId;

    /** 条款实例ID */
    private String clauseId;

    /** 原始文件名 */
    private String fileName;

    /** 文件大小(字节) */
    private Long fileSize;

    /** 是否按必传标签自动归类 */
    private Boolean classified;

    /** 材料态：UPLOADED */
    private String status;
}