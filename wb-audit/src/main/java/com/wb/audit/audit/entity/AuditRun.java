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
 * wb_audit_run AI审核执行记录实体
 */
@Data
@NoArgsConstructor
@TableName("wb_audit_run")
public class AuditRun {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 执行批次ID：RUN-YYYYMMDD-XXXX */
    private String runId;

    /** 任务ID */
    private Long taskId;

    /** 状态：AUDITING等 */
    private String status;

    /** 启动时间 */
    private LocalDateTime startedAt;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 修改时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}