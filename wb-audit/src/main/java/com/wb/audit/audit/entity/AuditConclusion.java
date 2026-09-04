package com.wb.audit.audit.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * wb_audit_conclusion 条款AI审核结论（writeConclusion 落库）
 */
@Data
@NoArgsConstructor
@TableName("wb_audit_conclusion")
public class AuditConclusion {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 执行批次ID（关联 wb_audit_run） */
    private String runId;

    /** 任务ID */
    private Long taskId;

    /** 条款实例ID */
    private String clauseId;

    /** 结果态：scored/blocked */
    private String outcome;

    /** 阻塞原因（blocked 时必填） */
    private String blockedReason;

    /** 备注（JSON 数组字符串） */
    private String notes;

    /** 问题条数 */
    private Integer issueCount;

    /** 模型结论原文（JSON 留档） */
    private String modelRaw;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 修改时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}