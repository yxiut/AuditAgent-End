package com.wb.audit.task.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * wb_audit_task 实体
 */
@Data
@NoArgsConstructor
@TableName("wb_audit_task")
public class AuditTask {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 任务编号：AUD-202608-LX-001 */
    private String taskNo;

    /** 制造基地ID */
    private Long factoryId;

    /** 周期粒度：MONTH/QUARTER/HALF_YEAR/YEAR/CUSTOM */
    private String periodType;

    /** 周期起 */
    private LocalDate periodStart;

    /** 周期止 */
    private LocalDate periodEnd;

    /** 区域集合（多选，逗号分隔） */
    private String region;

    /** 发布审核员ID（5=张伟） */
    private Long ownerId;

    /** 全局态：COLLECTING/AUDITING/REVIEWING/REPORTING/COMPLETED（本期无 DRAFT） */
    private String globalState;

    /** 材料是否全部收齐：0否 1是 */
    private Integer materialAllCollected;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 修改时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
